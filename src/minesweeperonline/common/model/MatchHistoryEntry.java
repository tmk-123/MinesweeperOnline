package minesweeperonline.common.model;

import java.io.Serializable;

/**
 * Data Transfer Object representing a single completed match record for a player.
 */
public class MatchHistoryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String matchId;
    private String opponentName;
    private String result;            // VICTORY, DEFEAT, DRAW, FORFEIT
    private int scoreDelta;           // e.g. +5, -3, -5, +1
    private int openedSafeCells;      // e.g. 42 / 121
    private int flagsPlaced;          // e.g. 15
    private int totalActions;         // e.g. 57
    private int durationSeconds;      // match duration in seconds
    private String playedAt;          // formatted date/time string e.g. "05/10/2026 23:45:00"
    private String outcomeReason;     // e.g. "Đối thủ mở trúng mìn", "Hết giờ", etc.

    public MatchHistoryEntry() {
    }

    public MatchHistoryEntry(String matchId, String opponentName, String result, int scoreDelta,
                             int openedSafeCells, int flagsPlaced, int totalActions,
                             int durationSeconds, String playedAt, String outcomeReason) {
        this.matchId = matchId;
        this.opponentName = (opponentName != null && !opponentName.isEmpty()) ? opponentName : "Đối thủ";
        this.result = result;
        this.scoreDelta = scoreDelta;
        this.openedSafeCells = openedSafeCells;
        this.flagsPlaced = flagsPlaced;
        this.totalActions = totalActions;
        this.durationSeconds = durationSeconds;
        this.playedAt = playedAt;
        this.outcomeReason = (outcomeReason != null) ? outcomeReason : "";
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getScoreDelta() {
        return scoreDelta;
    }

    public void setScoreDelta(int scoreDelta) {
        this.scoreDelta = scoreDelta;
    }

    public int getOpenedSafeCells() {
        return openedSafeCells;
    }

    public void setOpenedSafeCells(int openedSafeCells) {
        this.openedSafeCells = openedSafeCells;
    }

    public int getFlagsPlaced() {
        return flagsPlaced;
    }

    public void setFlagsPlaced(int flagsPlaced) {
        this.flagsPlaced = flagsPlaced;
    }

    public int getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(int totalActions) {
        this.totalActions = totalActions;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(String playedAt) {
        this.playedAt = playedAt;
    }

    public String getOutcomeReason() {
        return outcomeReason;
    }

    public void setOutcomeReason(String outcomeReason) {
        this.outcomeReason = outcomeReason;
    }

    /**
     * Helper to return formatted duration e.g. "1m 45s" or "32s".
     */
    public String getFormattedDuration() {
        if (durationSeconds < 60) {
            return durationSeconds + "s";
        }
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return minutes + "m " + seconds + "s";
    }

    @Override
    public String toString() {
        return "MatchHistoryEntry{" +
                "matchId='" + matchId + '\'' +
                ", opponentName='" + opponentName + '\'' +
                ", result='" + result + '\'' +
                ", scoreDelta=" + scoreDelta +
                ", openedSafeCells=" + openedSafeCells +
                ", playedAt='" + playedAt + '\'' +
                '}';
    }
}
