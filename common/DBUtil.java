package common;

import java.sql.*;

public class DBUtil {
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Load MySQL driver
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static String[] resolveCredentials() {
        // precedence: environment variables -> config.properties -> sensible defaults (none)
        String url = System.getenv("FILETRANS_DB_URL");
        String user = System.getenv("FILETRANS_DB_USER");
        String pass = System.getenv("FILETRANS_DB_PASS");

        if ((url == null || url.isEmpty()) || (user == null || user.isEmpty())) {
            try {
                server.Config cfg = server.Config.load();
                if (cfg != null) {
                    if (url == null || url.isEmpty()) url = cfg.dbUrl;
                    if (user == null || user.isEmpty()) user = cfg.dbUser;
                    if (pass == null || pass.isEmpty()) pass = cfg.dbPass;
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (url == null) url = "jdbc:mysql://localhost:3306/file_server";
        if (user == null) user = "";
        if (pass == null) pass = "";

        return new String[]{url, user, pass};
    }

    public static Connection getConnection() throws SQLException {
        String[] creds = resolveCredentials();
        return DriverManager.getConnection(creds[0], creds[1], creds[2]);
    }

    // Log download
    public static void logDownload(String clientIp, String fileName) {
        String sql = "INSERT INTO download_logs(client_ip, file_name) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientIp);
            ps.setString(2, fileName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Test the DB connection and return null if ok, or the error message
    public static String testConnection() {
        try {
            String[] creds = resolveCredentials();
            try (Connection c = DriverManager.getConnection(creds[0], creds[1], creds[2])) {
                if (c != null && !c.isClosed()) return null;
                return "Connection failed";
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // Ensure the required schema/table exists. Safe to call multiple times.
    public static void initSchema() {
        String create = "CREATE TABLE IF NOT EXISTS download_logs ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "client_ip VARCHAR(64),"
                + "file_name VARCHAR(255),"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = getConnection(); Statement s = conn.createStatement()) {
            s.execute(create);
        } catch (SQLException e) {
            // don't throw — log to stderr so caller can continue
            System.err.println("DB schema init failed: " + e.getMessage());
        }
    }
}
