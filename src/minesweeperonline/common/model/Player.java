package minesweeperonline.common.model;

import java.io.Serializable;

/**
 * Represents an active player in the system (lobby or active match).
 * Tracks both profile info and in-game statistics for a match.
 */
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_CHALLENGING = "CHALLENGING";
    public static final String STATUS_PLAYING = "PLAYING";

    private int userId;
    private String username;
    private int totalScore;
    private String status;

    // Match-specific statistics
    private int openedSafeCells;
    private int currentFlags;
    private int flagsPlaced;
    private int flagsRemoved;
    private int totalActions;

    public Player() {
        this.status = STATUS_IDLE;
        resetMatchStats();
    }

    public Player(int userId, String username, int totalScore) {
        this.userId = userId;
        this.username = username;
        this.totalScore = totalScore;
        this.status = STATUS_IDLE;
        resetMatchStats();
    }

    public void resetMatchStats() {
        this.openedSafeCells = 0;
        this.currentFlags = 0;
        this.flagsPlaced = 0;
        this.flagsRemoved = 0;
        this.totalActions = 0;
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
        this.totalScore = Math.max(0, totalScore);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOpenedSafeCells() {
        return openedSafeCells;
    }

    public void setOpenedSafeCells(int openedSafeCells) {
        this.openedSafeCells = openedSafeCells;
    }

    public void incrementOpenedSafeCells() {
        this.openedSafeCells++;
    }

    public void addOpenedSafeCells(int count) {
        this.openedSafeCells += count;
    }

    public int getCurrentFlags() {
        return currentFlags;
    }

    public void setCurrentFlags(int currentFlags) {
        this.currentFlags = Math.max(0, currentFlags);
    }

    public int getFlagsPlaced() {
        return flagsPlaced;
    }

    public void setFlagsPlaced(int flagsPlaced) {
        this.flagsPlaced = flagsPlaced;
    }

    public int getFlagsRemoved() {
        return flagsRemoved;
    }

    public void setFlagsRemoved(int flagsRemoved) {
        this.flagsRemoved = flagsRemoved;
    }

    public int getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(int totalActions) {
        this.totalActions = totalActions;
    }

    public void incrementTotalActions() {
        this.totalActions++;
    }

    public void placeFlag() {
        this.currentFlags++;
        this.flagsPlaced++;
        this.totalActions++;
    }

    public void removeFlag() {
        if (this.currentFlags > 0) {
            this.currentFlags--;
        }
        this.flagsRemoved++;
        this.totalActions++;
    }

    @Override
    public String toString() {
        return "Player{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", totalScore=" + totalScore +
                ", status='" + status + '\'' +
                ", openedSafeCells=" + openedSafeCells +
                ", totalActions=" + totalActions +
                '}';
    }
}
