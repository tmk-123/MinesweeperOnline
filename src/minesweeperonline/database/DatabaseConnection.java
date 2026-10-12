package minesweeperonline.database;

import minesweeperonline.server.ServerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý JDBC connection tới database, dùng cấu hình tập trung trong ServerConfig.
 */
public class DatabaseConnection {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    /**
     * Lấy một JDBC connection mới tới MySQL.
     *
     * @return java.sql.Connection đang hoạt động
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                ServerConfig.DB_URL,
                ServerConfig.DB_USER,
                ServerConfig.DB_PASSWORD
        );
    }

    /**
     * Kiểm tra hiện tại có kết nối được tới database MySQL hay không.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Database connection test failed: " + e.getMessage());
            System.err.println("[DatabaseConnection] Please verify MySQL credentials in ServerConfig.java");
            return false;
        }
    }

    /**
     * Đóng an toàn các tài nguyên (Connection, Statement, ResultSet).
     */
    public static void close(AutoCloseable... closeables) {
        if (closeables == null) return;
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
