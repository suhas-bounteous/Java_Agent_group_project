import java.sql.*;

/**
 * Loops forever, exercising every kind of operation the agent intercepts:
 *  - getConnection / close
 *  - prepareStatement
 *  - executeQuery / executeUpdate / executeBatch
 *  - commit / rollback
 *
 * Uses an in-memory H2 database so no external DB is needed.
 */
public class DbTestApp {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";

        // one-time schema setup
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(100)," +
                "  email VARCHAR(200)" +
                ")"
            );
        }

        System.out.println("DbTestApp started — generating traffic. Ctrl+C to stop.");

        int counter = 0;
        while (true) {
            try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
                conn.setAutoCommit(false);

                // INSERT (executeUpdate)
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (name, email) VALUES (?, ?)")) {
                    ps.setString(1, "User-" + counter);
                    ps.setString(2, "user" + counter + "@example.com");
                    ps.executeUpdate();
                }

                // SELECT (executeQuery)
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, name FROM users WHERE name LIKE ?")) {
                    ps.setString(1, "User-%");
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) { rs.getString("name"); }
                    }
                }

                // BATCH (executeBatch)
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET email = ? WHERE id = ?")) {
                    for (int i = 0; i < 3; i++) {
                        ps.setString(1, "updated" + counter + "_" + i + "@example.com");
                        ps.setInt(2, Math.max(1, counter - i));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // occasional slow query — cross join makes it naturally slow
                if (counter % 5 == 0) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT COUNT(*) FROM SYSTEM_RANGE(1, 5000) a, SYSTEM_RANGE(1, 5000) b")) {
                        try (ResultSet rs = ps.executeQuery()) { rs.next(); }
                    }
                }

                // mix commits and rollbacks
                if (counter % 7 == 0) {
                    conn.rollback();
                    System.out.println("[" + counter + "] rolled back");
                } else {
                    conn.commit();
                    System.out.println("[" + counter + "] committed");
                }
            } catch (SQLException e) {
                System.err.println("DB error: " + e.getMessage());
            }

            counter++;
            Thread.sleep(500);
        }
    }
}
