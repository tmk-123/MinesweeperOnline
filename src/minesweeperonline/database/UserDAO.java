package minesweeperonline.database;

import minesweeperonline.common.model.LeaderboardEntry;
import minesweeperonline.common.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data Access Object for User entity.
 * Uses MySQL database via DatabaseConnection.
 * Gracefully falls back to memory cache if MySQL is not yet configured or temporarily offline.
 */
public class UserDAO {

    // Fallback in-memory storage if MySQL is temporarily offline/unconfigured
    private static final ConcurrentHashMap<String, User> fallbackUsers = new ConcurrentHashMap<>();
    private static final AtomicInteger fallbackIdGen = new AtomicInteger(100);

    static {
        fallbackUsers.put("alice", new User(1, "alice", "123456", 25, "2026-10-05 20:00:00"));
        fallbackUsers.put("bob", new User(2, "bob", "123456", 15, "2026-10-05 20:00:00"));
        fallbackUsers.put("charlie", new User(3, "charlie", "123456", 10, "2026-10-05 20:00:00"));
    }

    /**
     * Authenticates a user by username and password.
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT id, username, password, totalScore, createdAt FROM `User` WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null; // Username or password wrong in MySQL
        } catch (SQLException e) {
            // MySQL error/offline -> Fallback to in-memory store
            System.err.println("[UserDAO] MySQL unavailable (" + e.getMessage() + "), using fallback in-memory store.");
            User u = fallbackUsers.get(username);
            if (u != null && u.getPassword().equals(password)) {
                return u;
            }
            return null;
        }
    }

    /**
     * Checks if a username already exists.
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM `User` WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return fallbackUsers.containsKey(username);
        }
    }

    /**
     * Registers a new user.
     */
    public boolean register(String username, String password) {
        if (existsByUsername(username)) {
            return false;
        }

        String sql = "INSERT INTO `User` (username, password, totalScore) VALUES (?, ?, 0)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, password);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        fallbackUsers.put(username, new User(id, username, password, 0, ""));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("[UserDAO] MySQL unavailable (" + e.getMessage() + "), registering into fallback store.");
            int id = fallbackIdGen.incrementAndGet();
            fallbackUsers.put(username, new User(id, username, password, 0, ""));
            return true;
        }
    }

    /**
     * Updates user's total score.
     */
    public boolean updateScore(int userId, int newTotalScore) {
        String sql = "UPDATE `User` SET totalScore = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, Math.max(0, newTotalScore));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            for (User u : fallbackUsers.values()) {
                if (u.getId() == userId) {
                    u.setTotalScore(Math.max(0, newTotalScore));
                    return true;
                }
            }
            return false;
        }
    }

    public User getUserById(int userId) {
        String sql = "SELECT id, username, password, totalScore, createdAt FROM `User` WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            for (User u : fallbackUsers.values()) {
                if (u.getId() == userId) return u;
            }
        }
        return null;
    }

    /**
     * Retrieves top users ordered by score descending, then by wins descending.
     */
    public List<LeaderboardEntry> getLeaderboard(int limit) {
        List<LeaderboardEntry> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.totalScore, " +
                "COUNT(CASE WHEN mp.result = 'VICTORY' THEN 1 END) AS wins, " +
                "COUNT(mp.id) AS totalMatches " +
                "FROM `User` u " +
                "LEFT JOIN `MatchPlayer` mp ON u.id = mp.userId " +
                "GROUP BY u.id, u.username, u.totalScore " +
                "ORDER BY u.totalScore DESC, wins DESC, u.username ASC LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String uname = rs.getString("username");
                    int score = rs.getInt("totalScore");
                    int wins = rs.getInt("wins");
                    int matches = rs.getInt("totalMatches");
                    list.add(new LeaderboardEntry(rank++, id, uname, score, wins, matches));
                }
                return list;
            }
        } catch (SQLException e) {
            // Fallback in-memory leaderboard
            List<User> allUsers = new ArrayList<>(fallbackUsers.values());
            allUsers.sort((u1, u2) -> {
                int scoreCmp = Integer.compare(u2.getTotalScore(), u1.getTotalScore());
                if (scoreCmp != 0) return scoreCmp;
                int[] s1 = MatchPlayerDAO.getFallbackStats(u1.getId());
                int[] s2 = MatchPlayerDAO.getFallbackStats(u2.getId());
                return Integer.compare(s2[0], s1[0]); // wins descending
            });

            int rank = 1;
            for (User u : allUsers) {
                int[] stats = MatchPlayerDAO.getFallbackStats(u.getId());
                list.add(new LeaderboardEntry(rank++, u.getId(), u.getUsername(), u.getTotalScore(), stats[0], stats[1]));
                if (list.size() >= limit) break;
            }
            return list;
        }
    }

    /**
     * Retrieves top users ordered by score descending.
     */
    public List<User> getTopRankings(int limit) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password, totalScore, createdAt FROM `User` ORDER BY totalScore DESC, id ASC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            list.addAll(fallbackUsers.values());
            list.sort((u1, u2) -> Integer.compare(u2.getTotalScore(), u1.getTotalScore()));
            if (list.size() > limit) {
                list = list.subList(0, limit);
            }
        }
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setTotalScore(rs.getInt("totalScore"));
        u.setCreatedAt(rs.getString("createdAt"));
        return u;
    }
}
