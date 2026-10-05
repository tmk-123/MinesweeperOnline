package minesweeperonline.common.model;

/**
 * Represents a single ranking entry in the Leaderboard.
 */
public class LeaderboardEntry {

    private int rank;
    private int userId;
    private String username;
    private int totalScore;
    private int wins;
    private int totalMatches;

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(int rank, int userId, String username, int totalScore, int wins, int totalMatches) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.totalScore = totalScore;
        this.wins = wins;
        this.totalMatches = totalMatches;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public double getWinRate() {
        if (totalMatches <= 0) return 0.0;
        return (double) wins * 100.0 / totalMatches;
    }

    @Override
    public String toString() {
        return "LeaderboardEntry{" +
                "rank=" + rank +
                ", username='" + username + '\'' +
                ", totalScore=" + totalScore +
                ", wins=" + wins +
                ", totalMatches=" + totalMatches +
                ", winRate=" + String.format("%.1f%%", getWinRate()) +
                '}';
    }
}
