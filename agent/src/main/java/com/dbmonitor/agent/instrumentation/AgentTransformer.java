package com.dbmonitor.agent.instrumentation;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.logging.AgentLogger;
import com.dbmonitor.agent.sender.AsyncEventQueue;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Wires all {@link Advice} classes to their target JDBC types via Byte Buddy's
 * {@link AgentBuilder} API and installs the resulting transformation on the JVM
 * {@link Instrumentation} handle.
 *
 * <h3>Transformation rules</h3>
 * <table border="1">
 *   <tr><th>Target type</th><th>Matched methods</th><th>Advice class</th></tr>
 *   <tr><td>{@link Statement} subtypes</td>
 *       <td>{@code execute, executeQuery, executeUpdate, executeBatch}</td>
 *       <td>{@link StatementAdvice}</td></tr>
 *   <tr><td>{@link DataSource} subtypes</td>
 *       <td>{@code getConnection}</td>
 *       <td>{@link ConnectionAdvice}</td></tr>
 *   <tr><td>{@link Connection} subtypes</td>
 *       <td>{@code commit, rollback}</td>
 *       <td>{@link TransactionAdvice}</td></tr>
 *   <tr><td>{@link Connection} subtypes</td>
 *       <td>{@code close}</td>
 *       <td>{@link CloseAdvice}</td></tr>
 * </table>
 *
 * <h3>Static field wiring</h3>
 * <p>The static holder fields on each Advice class ({@code queue} and
 * {@code config}) are set <em>before</em> {@link #install()} calls
 * {@code installOn(instrumentation)}.  This ensures that the very first
 * intercepted JDBC call sees valid references rather than {@code null}.
 */
public final class AgentTransformer {

    private final Instrumentation instrumentation;
    private final AgentConfig     config;
    private final AsyncEventQueue queue;

    /**
     * Constructs an {@code AgentTransformer} ready to be installed.
     *
     * @param instrumentation the JVM instrumentation handle from {@code premain}
     * @param config          validated agent configuration
     * @param queue           the event queue to which all captured events flow
     */
    public AgentTransformer(
            Instrumentation instrumentation,
            AgentConfig config,
            AsyncEventQueue queue) {
        this.instrumentation = instrumentation;
        this.config          = config;
        this.queue           = queue;
    }

    /**
     * Wires the Advice classes to their JDBC targets and installs the
     * transformation on the JVM.
     *
     * <p>The static fields on every Advice class are populated first, then
     * {@code installOn(instrumentation)} is called.  After this method returns,
     * every subsequently loaded (or retransformed) JDBC class matching one of
     * the target matchers will have the appropriate advice inlined.
     */
    public void install() {
        // -----------------------------------------------------------------------
        // Populate static holder fields on every Advice class BEFORE installation
        // so that the very first intercepted call has valid queue/config refs.
        // -----------------------------------------------------------------------
        StatementAdvice.queue    = queue;
        StatementAdvice.config   = config;
        ConnectionAdvice.queue   = queue;
        ConnectionAdvice.config  = config;
        TransactionAdvice.queue  = queue;
        TransactionAdvice.config = config;
        CloseAdvice.queue        = queue;
        CloseAdvice.config       = config;

        // -----------------------------------------------------------------------
        // Build and install the agent.
        // -----------------------------------------------------------------------
        new AgentBuilder.Default()
                // Retransform classes already loaded by the JVM at attach time.
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                // Use the NoOp initialisation strategy — our advice does not
                // require any per-class initialisation code.
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                // Redefine existing class bytecode rather than creating subclasses.
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)

                // ---------------------------------------------------------------
                // Exclusions — never instrument agent internals or JDK internals.
                // ---------------------------------------------------------------
                .ignore(nameStartsWith("com.dbmonitor"))
                .ignore(nameStartsWith("net.bytebuddy"))
                .ignore(nameStartsWith("sun."))
                .ignore(nameStartsWith("jdk."))

                // ---------------------------------------------------------------
                // Rule 1: Statement.execute / executeQuery / executeUpdate / executeBatch
                // ---------------------------------------------------------------
                .type(isSubTypeOf(Statement.class))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(
                                Advice.to(StatementAdvice.class)
                                      .on(namedOneOf(
                                              "execute",
                                              "executeQuery",
                                              "executeUpdate",
                                              "executeBatch"))))

                // ---------------------------------------------------------------
                // Rule 2: DataSource.getConnection
                // ---------------------------------------------------------------
                .type(isSubTypeOf(DataSource.class))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(
                                Advice.to(ConnectionAdvice.class)
                                      .on(named("getConnection"))))

                // ---------------------------------------------------------------
                // Rule 3: Connection.commit / Connection.rollback
                // ---------------------------------------------------------------
                .type(isSubTypeOf(Connection.class))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(
                                Advice.to(TransactionAdvice.class)
                                      .on(namedOneOf("commit", "rollback"))))

                // ---------------------------------------------------------------
                // Rule 4: Connection.close
                // ---------------------------------------------------------------
                .type(isSubTypeOf(Connection.class))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(
                                Advice.to(CloseAdvice.class)
                                      .on(named("close"))))

                .installOn(instrumentation);

        AgentLogger.info("db-monitor-agent: AgentTransformer installed — JDBC instrumentation active");
    }
}
