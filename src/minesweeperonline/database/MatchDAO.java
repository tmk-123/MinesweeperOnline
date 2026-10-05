package minesweeperonline.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Data Access Object for Match entity.
 */
public class MatchDAO {

    /**
     * Inserts a completed Match record into the database within an existing Connection/Transaction.
     */
    public boolean insertMatch(Connection conn, String matchId, String status, long startedAt, long endedAt,
                               Integer winnerId, String result) throws SQLException {
        String sql = "INSERT INTO `Match` (id, status, startedAt, endedAt, winnerId, result) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matchId);
            ps.setString(2, status != null ? status : "FINISHED");
            ps.setTimestamp(3, startedAt > 0 ? new Timestamp(startedAt) : new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(4, endedAt > 0 ? new Timestamp(endedAt) : new Timestamp(System.currentTimeMillis()));

            if (winnerId != null && winnerId > 0) {
                ps.setInt(5, winnerId);
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, result);
            return ps.executeUpdate() > 0;
        }
    }
}
