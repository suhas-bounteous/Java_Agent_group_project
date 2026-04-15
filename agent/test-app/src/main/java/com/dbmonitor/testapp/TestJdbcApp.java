package com.dbmonitor.testapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Self-contained JDBC application that exercises the full range of database
 * operations the DB Monitor Agent is designed to intercept.
 *
 * <h3>Operations performed</h3>
 * <ol>
 *   <li>DDL — {@code CREATE TABLE}</li>
 *   <li>DML — {@code INSERT} (3 rows via plain {@code Statement})</li>
 *   <li>Query — {@code SELECT ... WHERE salary > 70000}</li>
 *   <li>Update — {@code UPDATE ... WHERE id = 1}</li>
 *   <li>Committed transaction — INSERT row 4, commit</li>
 *   <li>Rolled-back transaction — INSERT row 5, rollback</li>
 * </ol>
 *
 * <p>All JDBC calls use the raw {@code java.sql.*} API with
 * {@link Statement} (not {@link java.sql.PreparedStatement}) so that the SQL
 * string is always available in {@code args[0]} of the intercepted method.
 * This makes the agent's SQL-masking assertions in the integration test
 * deterministic: every literal value ({@code 'Alice'}, {@code 75000.00}, etc.)
 * will be visible in the intercepted call and subsequently masked to {@code ?}.
 *
 * <p>The H2 in-memory database is opened with {@code DB_CLOSE_DELAY=-1} so that
 * the database survives the lifetime of any individual connection; the JVM exit
 * closes it automatically.
 */
public class TestJdbcApp {

    private static final String JDBC_URL =
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;TRACE_LEVEL_SYSTEM_OUT=0";

    public static void main(String[] args) throws Exception {

        // Step 1 — explicitly load the H2 driver so the agent can intercept
        // the class loading event and any DataSource / DriverManager calls.
        Class.forName("org.h2.Driver");

        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "")) {

            // ---------------------------------------------------------------
            // Step 2 — DDL: create the employees table
            // ---------------------------------------------------------------
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS employees ("
                        + "id INT PRIMARY KEY, "
                        + "name VARCHAR(100), "
                        + "salary DOUBLE)"
                );
            }

            // ---------------------------------------------------------------
            // Step 3 — DML: insert three seed rows
            // Using plain string concatenation (not PreparedStatement) so that
            // the full SQL with string literals is visible to the agent advice.
            // ---------------------------------------------------------------
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                        "INSERT INTO employees (id, name, salary) VALUES (1, 'Alice', 75000.00)");
                stmt.executeUpdate(
                        "INSERT INTO employees (id, name, salary) VALUES (2, 'Bob', 82000.00)");
                stmt.executeUpdate(
                        "INSERT INTO employees (id, name, salary) VALUES (3, 'Carol', 91000.50)");
            }

            // ---------------------------------------------------------------
            // Step 4 — Query: SELECT with a numeric WHERE predicate
            // ---------------------------------------------------------------
            try (Statement stmt = conn.createStatement();
                 ResultSet rs   = stmt.executeQuery(
                         "SELECT * FROM employees WHERE salary > 70000")) {

                System.out.println("--- Query results (salary > 70000) ---");
                while (rs.next()) {
                    System.out.printf("  id=%-3d  name=%-8s  salary=%.2f%n",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("salary"));
                }
            }

            // ---------------------------------------------------------------
            // Step 5 — Update: bump Alice's salary
            // ---------------------------------------------------------------
            try (Statement stmt = conn.createStatement()) {
                int updated = stmt.executeUpdate(
                        "UPDATE employees SET salary = 80000 WHERE id = 1");
                System.out.println("--- Updated " + updated + " row(s) ---");
            }

            // ---------------------------------------------------------------
            // Step 6 — Committed transaction: INSERT row 4 (Dave)
            // ---------------------------------------------------------------
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                        "INSERT INTO employees (id, name, salary) VALUES (4, 'Dave', 95000)");
                conn.commit();
                System.out.println("--- Committed transaction (inserted Dave) ---");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            // ---------------------------------------------------------------
            // Step 7 — Rolled-back transaction: INSERT row 5 (Eve) then rollback
            // ---------------------------------------------------------------
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                        "INSERT INTO employees (id, name, salary) VALUES (5, 'Eve', 50000)");
                conn.rollback();
                System.out.println("--- Rolled back transaction (Eve not committed) ---");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            // ---------------------------------------------------------------
            // Step 8 — Verify final state
            // ---------------------------------------------------------------
            try (Statement stmt = conn.createStatement();
                 ResultSet rs   = stmt.executeQuery("SELECT COUNT(*) FROM employees")) {
                if (rs.next()) {
                    System.out.println("--- Final row count: " + rs.getInt(1) + " ---");
                }
            }

        } // Connection.close() fires the agent's CloseAdvice here

        System.out.println("TestJdbcApp completed successfully");
    }
}
