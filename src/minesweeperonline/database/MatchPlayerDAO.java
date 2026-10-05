package minesweeperonline.database;

import minesweeperonline.common.model.MatchHistoryEntry;
import minesweeperonline.common.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data Access Object for MatchPlayer entity.
 * Handles transactional recording of match players, user score updates, and match history retrieval.
 */
public class MatchPlayerDAO {

    public static class FallbackMatchRecord {
        public String matchId;
        public long startedAt;
        public long endedAt;
        public String reason;
        public int u1Id;
        public String u1Name;
        public String res1;
        public int opened1;
        public int actions1;
        public int flags1;
        public int scoreDelta1;
        public int u2Id;
        public String u2Name;
        public String res2;
        public int opened2;
        public int actions2;
        public int flags2;
        public int scoreDelta2;
    }

    private static final ConcurrentHashMap<Integer, int[]> fallbackStats = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<FallbackMatchRecord> fallbackMatches = new CopyOnWriteArrayList<>();

    /**
     * Inserts a player's performance record for a match within an active transaction.
     */
    public boolean insertMatchPlayer(Connection conn, String matchId, int userId, String result,
                                     int openedSafeCells, int totalActions, int flagsPlaced,
                                     int score) throws SQLException {
        String sql = "INSERT INTO `MatchPlayer` (matchId, userId, result, openedSafeCells, totalActions, flagsPlaced, score) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matchId);
            ps.setInt(2, userId);
            ps.setString(3, result);
            ps.setInt(4, openedSafeCells);
            ps.setInt(5, totalActions);
            ps.setInt(6, flagsPlaced);
            ps.setInt(7, score);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Overload for backward compatibility.
     */
    public boolean saveFullMatchTransaction(
            String matchId, String matchStatus, long startedAt, long endedAt, Integer winnerId, String outcomeDescription,
            int user1Id, String res1, int opened1, int actions1, int flags1, int scoreDelta1,
            int user2Id, String res2, int opened2, int actions2, int flags2, int scoreDelta2
    ) {
        UserDAO userDAO = new UserDAO();
        User u1 = userDAO.getUserById(user1Id);
        User u2 = userDAO.getUserById(user2Id);
        String n1 = (u1 != null) ? u1.getUsername() : ("User_" + user1Id);
        String n2 = (u2 != null) ? u2.getUsername() : ("User_" + user2Id);
        return saveFullMatchTransaction(
                matchId, matchStatus, startedAt, endedAt, winnerId, outcomeDescription,
                user1Id, n1, res1, opened1, actions1, flags1, scoreDelta1,
                user2Id, n2, res2, opened2, actions2, flags2, scoreDelta2
        );
    }

    /**
     * Saves the entire match result and updates both players' scores in a SINGLE TRANSACTION.
     */
    public boolean saveFullMatchTransaction(
            String matchId, String matchStatus, long startedAt, long endedAt, Integer winnerId, String outcomeDescription,
            int user1Id, String user1Name, String res1, int opened1, int actions1, int flags1, int scoreDelta1,
            int user2Id, String user2Name, String res2, int opened2, int actions2, int flags2, int scoreDelta2
    ) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Insert Match
            MatchDAO matchDAO = new MatchDAO();
            matchDAO.insertMatch(conn, matchId, matchStatus, startedAt, endedAt, winnerId, outcomeDescription);

            // 2. Insert MatchPlayer 1
            insertMatchPlayer(conn, matchId, user1Id, res1, opened1, actions1, flags1, scoreDelta1);

            // 3. Insert MatchPlayer 2
            insertMatchPlayer(conn, matchId, user2Id, res2, opened2, actions2, flags2, scoreDelta2);

            // 4. Update User 1 Score (Score cannot be negative: GREATEST(0, totalScore + delta))
            String sqlScore = "UPDATE `User` SET totalScore = GREATEST(0, totalScore + ?) WHERE id = ?";
            try (PreparedStatement ps1 = conn.prepareStatement(sqlScore)) {
                ps1.setInt(1, scoreDelta1);
                ps1.setInt(2, user1Id);
                ps1.executeUpdate();
            }

            // 5. Update User 2 Score
            try (PreparedStatement ps2 = conn.prepareStatement(sqlScore)) {
                ps2.setInt(1, scoreDelta2);
                ps2.setInt(2, user2Id);
                ps2.executeUpdate();
            }

            conn.commit(); // Commit Transaction
            System.out.println("[MatchPlayerDAO] Transaction committed successfully for match: " + matchId);
            recordFallbackMatchDetails(matchId, outcomeDescription, startedAt, endedAt,
                    user1Id, user1Name, res1, opened1, actions1, flags1, scoreDelta1,
                    user2Id, user2Name, res2, opened2, actions2, flags2, scoreDelta2);
            return true;

        } catch (SQLException e) {
            System.err.println("[MatchPlayerDAO] Transaction error, rolling back: " + e.getMessage());
            // Fallback: update in-memory user score and stats
            UserDAO userDAO = new UserDAO();
            User u1 = userDAO.getUserById(user1Id);
            if (u1 != null) {
                userDAO.updateScore(user1Id, u1.getTotalScore() + scoreDelta1);
            }
            User u2 = userDAO.getUserById(user2Id);
            if (u2 != null) {
                userDAO.updateScore(user2Id, u2.getTotalScore() + scoreDelta2);
            }
            recordFallbackMatchDetails(matchId, outcomeDescription, startedAt, endedAt,
                    user1Id, user1Name, res1, opened1, actions1, flags1, scoreDelta1,
                    user2Id, user2Name, res2, opened2, actions2, flags2, scoreDelta2);

            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rbEx) {
                    System.err.println("[MatchPlayerDAO] Rollback error: " + rbEx.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Retrieves match history for a given user, newest first.
     */
    public List<MatchHistoryEntry> getMatchHistory(int userId, int limit) {
        List<MatchHistoryEntry> list = new ArrayList<>();
        String sql = "SELECT " +
                "m.id AS matchId, " +
                "COALESCE(opp_u.username, 'Đối thủ') AS opponentName, " +
                "mp.result AS result, " +
                "mp.score AS scoreDelta, " +
                "mp.openedSafeCells AS openedSafeCells, " +
                "mp.flagsPlaced AS flagsPlaced, " +
                "mp.totalActions AS totalActions, " +
                "m.startedAt AS startedAt, " +
                "m.endedAt AS endedAt, " +
                "m.result AS outcomeReason " +
                "FROM `Match` m " +
                "JOIN `MatchPlayer` mp ON m.id = mp.matchId " +
                "LEFT JOIN `MatchPlayer` opp_mp ON m.id = opp_mp.matchId AND opp_mp.userId <> mp.userId " +
                "LEFT JOIN `User` opp_u ON opp_mp.userId = opp_u.id " +
                "WHERE mp.userId = ? " +
                "ORDER BY m.startedAt DESC, m.createdAt DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                while (rs.next()) {
                    String matchId = rs.getString("matchId");
                    String oppName = rs.getString("opponentName");
                    String result = rs.getString("result");
                    int scoreDelta = rs.getInt("scoreDelta");
                    int opened = rs.getInt("openedSafeCells");
                    int flags = rs.getInt("flagsPlaced");
                    int actions = rs.getInt("totalActions");
                    Timestamp startTs = rs.getTimestamp("startedAt");
                    Timestamp endTs = rs.getTimestamp("endedAt");
                    String outcomeReason = rs.getString("outcomeReason");

                    long startMs = (startTs != null) ? startTs.getTime() : System.currentTimeMillis();
                    long endMs = (endTs != null) ? endTs.getTime() : startMs;
                    int duration = (int) Math.max(0, (endMs - startMs) / 1000);
                    String playedAt = (startTs != null) ? sdf.format(startTs) : sdf.format(new Date());

                    list.add(new MatchHistoryEntry(
                            matchId, oppName, result, scoreDelta, opened, flags, actions, duration, playedAt, outcomeReason
                    ));
                }
                return list;
            }
        } catch (SQLException e) {
            System.err.println("[MatchPlayerDAO] MySQL unavailable (" + e.getMessage() + "), using fallback in-memory history.");
            return getFallbackMatchHistory(userId, limit);
        }
    }

    /**
     * Fallback match history when database is offline.
     */
    public static List<MatchHistoryEntry> getFallbackMatchHistory(int userId, int limit) {
        List<MatchHistoryEntry> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (int i = fallbackMatches.size() - 1; i >= 0 && list.size() < limit; i--) {
            FallbackMatchRecord rec = fallbackMatches.get(i);
            if (rec.u1Id == userId) {
                int duration = (int) Math.max(0, (rec.endedAt - rec.startedAt) / 1000);
                String playedAt = sdf.format(new Date(rec.startedAt > 0 ? rec.startedAt : System.currentTimeMillis()));
                list.add(new MatchHistoryEntry(
                        rec.matchId, rec.u2Name, rec.res1, rec.scoreDelta1,
                        rec.opened1, rec.flags1, rec.actions1, duration, playedAt, rec.reason
                ));
            } else if (rec.u2Id == userId) {
                int duration = (int) Math.max(0, (rec.endedAt - rec.startedAt) / 1000);
                String playedAt = sdf.format(new Date(rec.startedAt > 0 ? rec.startedAt : System.currentTimeMillis()));
                list.add(new MatchHistoryEntry(
                        rec.matchId, rec.u1Name, rec.res2, rec.scoreDelta2,
                        rec.opened2, rec.flags2, rec.actions2, duration, playedAt, rec.reason
                ));
            }
        }
        return list;
    }

    public static int[] getFallbackStats(int userId) {
        return fallbackStats.getOrDefault(userId, new int[]{0, 0});
    }

    public static void recordFallbackMatchDetails(
            String matchId, String reason, long startedAt, long endedAt,
            int u1Id, String u1Name, String r1, int opened1, int actions1, int flags1, int scoreDelta1,
            int u2Id, String u2Name, String r2, int opened2, int actions2, int flags2, int scoreDelta2
    ) {
        recordFallbackMatch(u1Id, r1, u2Id, r2);

        FallbackMatchRecord rec = new FallbackMatchRecord();
        rec.matchId = matchId;
        rec.startedAt = startedAt;
        rec.endedAt = endedAt;
        rec.reason = reason;
        rec.u1Id = u1Id;
        rec.u1Name = (u1Name != null && !u1Name.isEmpty()) ? u1Name : ("User_" + u1Id);
        rec.res1 = r1;
        rec.opened1 = opened1;
        rec.actions1 = actions1;
        rec.flags1 = flags1;
        rec.scoreDelta1 = scoreDelta1;

        rec.u2Id = u2Id;
        rec.u2Name = (u2Name != null && !u2Name.isEmpty()) ? u2Name : ("User_" + u2Id);
        rec.res2 = r2;
        rec.opened2 = opened2;
        rec.actions2 = actions2;
        rec.flags2 = flags2;
        rec.scoreDelta2 = scoreDelta2;

        fallbackMatches.add(rec);
    }

    public static void recordFallbackMatch(int u1, String r1, int u2, String r2) {
        fallbackStats.compute(u1, (k, v) -> {
            if (v == null) v = new int[]{0, 0};
            if ("VICTORY".equals(r1)) v[0]++;
            v[1]++;
            return v;
        });
        fallbackStats.compute(u2, (k, v) -> {
            if (v == null) v = new int[]{0, 0};
            if ("VICTORY".equals(r2)) v[0]++;
            v[1]++;
            return v;
        });
    }
}
