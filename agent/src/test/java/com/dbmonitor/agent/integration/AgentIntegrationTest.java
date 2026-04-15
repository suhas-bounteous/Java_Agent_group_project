package com.dbmonitor.agent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end integration test that verifies the complete agent pipeline:
 *
 * <pre>
 *   TestJdbcApp (subprocess) ──JDBC──▶ H2
 *       │
 *       │ (agent intercepts JDBC calls)
 *       ▼
 *   AsyncEventQueue ──flush──▶ HttpMetricSender ──POST JSON──▶ WireMock (8081)
 * </pre>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@code @BeforeAll} starts a {@link WireMockServer} on port 8081 and
 *       stubs {@code POST /api/events} → 200.</li>
 *   <li>The test launches {@code TestJdbcApp} in a child JVM with the agent
 *       shaded JAR as {@code -javaagent}, waits up to 15 s for the process to
 *       finish, then waits up to 5 s for WireMock to receive at least one
 *       payload.</li>
 *   <li>The captured request body is parsed with Jackson and asserted
 *       against the expected structure.</li>
 *   <li>{@code @AfterAll} stops WireMock.</li>
 * </ol>
 *
 * <h3>Running</h3>
 * <p>This test is tagged {@code integration} and excluded from the Surefire
 * unit-test run.  It is executed by the Failsafe plugin during the
 * {@code integration-test} lifecycle phase:
 * <pre>
 *   mvn verify -pl agent
 * </pre>
 *
 * <p>Both the agent shaded JAR and the test-app JAR must be built before this
 * test runs.  The test uses {@link org.junit.jupiter.api.Assumptions} to skip
 * gracefully rather than fail when the JARs are absent.
 */
@Tag("integration")
public class AgentIntegrationTest {

    private static final int    WIREMOCK_PORT      = 8081;
    private static final String COLLECTOR_PATH     = "/api/events";
    private static final String COLLECTOR_ENDPOINT =
            "http://localhost:" + WIREMOCK_PORT + COLLECTOR_PATH;

    /** WireMock is started once for all tests in this class. */
    private static WireMockServer wireMockServer;

    // -------------------------------------------------------------------------
    // WireMock lifecycle
    // -------------------------------------------------------------------------

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options().port(WIREMOCK_PORT));
        wireMockServer.start();

        // Stub: accept any POST to /api/events and return 200 OK.
        wireMockServer.stubFor(
                post(urlEqualTo(COLLECTOR_PATH))
                        .willReturn(aResponse().withStatus(200)));
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    // -------------------------------------------------------------------------
    // Integration test
    // -------------------------------------------------------------------------

    @Test
    void agentIsAttached_interceptsJdbcCalls_andPostsToCollector() throws Exception {

        // -----------------------------------------------------------------------
        // Step 1 — Locate JAR files; skip the test if they haven't been built yet.
        // -----------------------------------------------------------------------
        Path agentJar   = Paths.get("target/db-monitor-agent-1.0.0-shaded.jar");
        Path testAppJar = resolveTestAppJar();
        Path h2Jar      = resolveH2Jar();

        assumeTrue(Files.exists(agentJar),
                "Agent shaded JAR not found at " + agentJar.toAbsolutePath()
                + " — run 'mvn package -pl agent' first");
        assumeTrue(testAppJar != null && Files.exists(testAppJar),
                "Test-app JAR not found — run 'mvn package -pl agent/test-app' first");
        assumeTrue(h2Jar != null && Files.exists(h2Jar),
                "H2 JAR not found on classpath or in Maven local repository "
                + "— ensure H2 2.2.224 has been downloaded");

        // -----------------------------------------------------------------------
        // Step 2 — Build the child process command.
        // -----------------------------------------------------------------------
        //   -javaagent:<jar>=agentArgs  instructs the JVM to run the agent
        //   -cp <testApp>:<h2>          provides the application and its JDBC driver
        //   com.dbmonitor.testapp.TestJdbcApp  the main class to execute
        // -----------------------------------------------------------------------
        String javaAgentArg = "-javaagent:" + agentJar.toAbsolutePath()
                + "=appName=integration-test-app"
                + ",collectorEndpoint=" + COLLECTOR_ENDPOINT
                + ",reportingIntervalMs=500";

        String classpath = testAppJar.toAbsolutePath()
                + File.pathSeparator
                + h2Jar.toAbsolutePath();

        ProcessBuilder pb = new ProcessBuilder(
                resolveJavaExecutable(),
                javaAgentArg,
                "-cp", classpath,
                "com.dbmonitor.testapp.TestJdbcApp"
        );
        pb.redirectErrorStream(true);   // merge stderr into stdout for easy capture

        // -----------------------------------------------------------------------
        // Step 3 — Start the process and drain its output on a daemon thread.
        //           Without draining, the process can block when its OS pipe
        //           buffer fills up, causing waitFor() to hang.
        // -----------------------------------------------------------------------
        Process process = pb.start();

        StringBuilder processOutput = new StringBuilder();
        Thread outputDrainer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutput.append(line).append(System.lineSeparator());
                }
            } catch (IOException ignored) {
                // The stream is closed when the process exits — this is normal.
            }
        }, "output-drainer");
        outputDrainer.setDaemon(true);
        outputDrainer.start();

        // -----------------------------------------------------------------------
        // Step 4 — Wait for the process to finish (up to 15 s).
        // -----------------------------------------------------------------------
        boolean processFinished = process.waitFor(15, TimeUnit.SECONDS);
        outputDrainer.join(2_000);  // let the drainer catch up with the last lines

        if (!processFinished) {
            process.destroyForcibly();
            fail("TestJdbcApp did not finish within 15 seconds.\n"
                    + "Process output:\n" + processOutput);
        }

        assertEquals(0, process.exitValue(),
                "TestJdbcApp must exit with code 0.\n"
                + "Process output:\n" + processOutput);

        // -----------------------------------------------------------------------
        // Step 5 — Wait up to 5 s for the agent's async queue to flush at least
        //           one batch to WireMock.
        //           The agent's reportingIntervalMs is 500 ms; the process output
        //           "TestJdbcApp completed successfully" is printed before the JVM
        //           begins its shutdown-hook flush, so we poll here.
        // -----------------------------------------------------------------------
        long deadline = System.currentTimeMillis() + 5_000;
        while (wireMockServer.getAllServeEvents().isEmpty()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        // -----------------------------------------------------------------------
        // Step 6 — Assert WireMock received at least one POST.
        // -----------------------------------------------------------------------
        List<ServeEvent> serveEvents = wireMockServer.getAllServeEvents();
        assertFalse(serveEvents.isEmpty(),
                "WireMock did not receive any POST to " + COLLECTOR_PATH
                + " within 5 seconds after the process exited.\n"
                + "Process output:\n" + processOutput);

        // -----------------------------------------------------------------------
        // Step 7 — Parse the first captured request body and run structural
        //           assertions.  If there are multiple batches (because the queue
        //           flushed more than once) we check each one to find the
        //           required event types.
        // -----------------------------------------------------------------------
        ObjectMapper mapper      = new ObjectMapper();
        boolean      foundMetadata    = false;
        boolean      foundAppName     = false;
        boolean      foundQueryEvent  = false;

        for (ServeEvent serveEvent : serveEvents) {

            String body = serveEvent.getRequest().getBodyAsString();
            assertFalse(body == null || body.isBlank(),
                    "Every captured request body must be non-blank");

            JsonNode root = mapper.readTree(body);

            // ── Assertion: 'metadata' object is present ──────────────────────
            if (root.has("metadata") && !root.get("metadata").isNull()) {
                foundMetadata = true;
                JsonNode metadata = root.get("metadata");

                // ── Assertion: appName matches the agent argument ─────────────
                if (metadata.has("appName")
                        && "integration-test-app".equals(metadata.get("appName").asText())) {
                    foundAppName = true;
                }
            }

            // ── Assertion: 'events' array is present ─────────────────────────
            if (!root.has("events")) {
                continue;
            }

            JsonNode events = root.get("events");
            assertTrue(events.isArray(),
                    "'events' field must be a JSON array");

            for (JsonNode event : events) {

                // ── Assertion: at least one QUERY or EXECUTE event type ───────
                if (event.has("eventType")) {
                    String eventType = event.get("eventType").asText();
                    if (eventType.contains("QUERY") || eventType.contains("EXECUTE")) {
                        foundQueryEvent = true;
                    }
                }

                // ── Assertion: SQL literals are masked ───────────────────────
                // The TestJdbcApp inserts 'Alice', 'Bob', 'Carol', 'Dave', 'Eve'
                // as string literals.  With MaskingLevel.FULL (the default), every
                // quoted string is replaced with ?.  These values must never appear
                // in the payload.
                if (event.has("sql")
                        && !event.get("sql").isNull()
                        && event.get("sql").isTextual()) {

                    String sql = event.get("sql").asText();
                    assertFalse(sql.contains("'Alice'") || sql.contains("Alice"),
                            "Masked SQL must not contain literal 'Alice'; got: " + sql);
                    assertFalse(sql.contains("'Bob'") || sql.contains("Bob"),
                            "Masked SQL must not contain literal 'Bob'; got: " + sql);
                    assertFalse(sql.contains("'Carol'") || sql.contains("Carol"),
                            "Masked SQL must not contain literal 'Carol'; got: " + sql);
                    assertFalse(sql.contains("'Dave'") || sql.contains("Dave"),
                            "Masked SQL must not contain literal 'Dave'; got: " + sql);
                    assertFalse(sql.contains("'Eve'") || sql.contains("Eve"),
                            "Masked SQL must not contain literal 'Eve'; got: " + sql);
                }
            }
        }

        // ── Final cross-batch assertions ──────────────────────────────────────
        assertTrue(foundMetadata,
                "At least one payload must contain a 'metadata' object");
        assertTrue(foundAppName,
                "At least one payload must have appName = 'integration-test-app'");
        assertTrue(foundQueryEvent,
                "At least one event across all batches must have eventType containing "
                + "'QUERY' or 'EXECUTE' — agent may not have instrumented the JDBC calls");
    }

    // -------------------------------------------------------------------------
    // Helper — locate artefacts
    // -------------------------------------------------------------------------

    /**
     * Resolves the test-app JAR, trying both the Maven-default versioned name
     * and the {@code <finalName>} override used in the test-app POM.
     */
    private static Path resolveTestAppJar() {
        // Maven default: ${artifactId}-${version}.jar
        Path versioned = Paths.get("../test-app/target/db-monitor-test-app-1.0.0.jar");
        if (Files.exists(versioned)) {
            return versioned;
        }
        // Explicit <finalName> override in the test-app POM
        Path named = Paths.get("../test-app/target/db-monitor-test-app.jar");
        if (Files.exists(named)) {
            return named;
        }
        return null;
    }

    /**
     * Resolves the H2 JAR using two strategies:
     * <ol>
     *   <li>Scan the running JVM's {@code java.class.path} — H2 will be present
     *       if it is declared as a {@code test} dependency of the agent module.</li>
     *   <li>Fall back to the standard Maven local repository path
     *       ({@code ~/.m2/repository/...}), which is always populated after a
     *       successful Maven build of the test-app module.</li>
     * </ol>
     */
    private static Path resolveH2Jar() {
        // Strategy 1: look for H2 on the current JVM's classpath.
        String jvmClasspath = System.getProperty("java.class.path", "");
        for (String entry : jvmClasspath.split(File.pathSeparator)) {
            if (entry.contains("h2") && entry.endsWith(".jar")) {
                Path candidate = Paths.get(entry);
                if (Files.exists(candidate)) {
                    return candidate;
                }
            }
        }

        // Strategy 2: Maven local repository (standard layout).
        Path mavenRepoH2 = Paths.get(
                System.getProperty("user.home"),
                ".m2", "repository",
                "com", "h2database", "h2",
                "2.2.224",
                "h2-2.2.224.jar");
        if (Files.exists(mavenRepoH2)) {
            return mavenRepoH2;
        }

        // Not found.
        return null;
    }

    /**
     * Returns the absolute path to the {@code java} executable of the JVM that
     * is currently running the tests.  Using the same JVM version as the test
     * process ensures bytecode compatibility between the agent classes and the
     * target process.
     */
    private static String resolveJavaExecutable() {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        // Prefer the fully-qualified path; fall back to bare "java" on PATH.
        String binary = System.getProperty("os.name", "").toLowerCase().contains("windows")
                ? "java.exe" : "java";
        Path javaExe = javaHome.resolve("bin").resolve(binary);
        return Files.exists(javaExe) ? javaExe.toAbsolutePath().toString() : "java";
    }
}
