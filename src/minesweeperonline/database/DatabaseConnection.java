package minesweeperonline.database;

import minesweeperonline.server.ServerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC Database Connection using settings centralized in ServerConfig.
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
     * Obtains a new JDBC Connection to MySQL.
     *
     * @return active java.sql.Connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                ServerConfig.DB_URL,
                ServerConfig.DB_USER,
                ServerConfig.DB_PASSWORD
        );
    }

    /**
     * Tests whether the MySQL database is currently reachable.
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
     * Safely closes resources (Connection, Statement, ResultSet).
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
