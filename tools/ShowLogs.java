package tools;

import common.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ShowLogs {
    public static void main(String[] args) {
        int limit = 100;
        if (args.length > 0) {
            try { limit = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }

        String sql = "SELECT id, client_ip, file_name, created_at FROM download_logs ORDER BY created_at DESC LIMIT ?";

        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("id | client_ip | file_name | created_at");
                System.out.println("-------------------------------------------");
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String ip = rs.getString("client_ip");
                    String fn = rs.getString("file_name");
                    String at = rs.getString("created_at");
                    System.out.printf("%d | %s | %s | %s\n", id, ip, fn, at);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to query download_logs: " + e.getMessage());
            System.err.println("Make sure MySQL is running and credentials are provided via environment variables or config.properties.");
            System.err.println("Env vars: FILETRANS_DB_URL, FILETRANS_DB_USER, FILETRANS_DB_PASS");
            System.err.println("Or copy config.properties.example -> config.properties and edit dbUrl/dbUser/dbPass.");
            e.printStackTrace(System.err);
        }
    }
}
