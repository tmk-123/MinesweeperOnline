package minesweeperonline.common.model;

import java.io.Serializable;

/**
 * Đại diện cho một người chơi đang hoạt động trong hệ thống (ở sảnh chờ hoặc đang trong trận).
 * Lưu cả thông tin hồ sơ lẫn thống kê trong một trận đấu.
 */
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_CHALLENGING = "CHALLENGING";
    public static final String STATUS_PLAYING = "PLAYING";
    public static final String STATUS_QUEUED = "QUEUED";

    private int userId;
    private String username;
    private int totalScore;
    private String status;

    // Thống kê riêng cho từng trận
    private int openedSafeCells;
    private int currentFlags;
    private int flagsPlaced;
    private int flagsRemoved;
    private int totalActions;

    /** Khởi tạo thông tin người chơi và các bộ đếm dùng trong trận đấu. */
    public Player() {
        this.status = STATUS_IDLE;
        resetMatchStats();
    }

    /** Khởi tạo thông tin người chơi và các bộ đếm dùng trong trận đấu. */
    public Player(int userId, String username, int totalScore) {
        this.userId = userId;
        this.username = username;
        this.totalScore = totalScore;
        this.status = STATUS_IDLE;
        resetMatchStats();
    }

    /** Đưa các bộ đếm thao tác và ô an toàn về trạng thái ban đầu cho ván mới. */
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

    /** Tăng bộ đếm ô an toàn đã mở thêm một ô. */
    public void incrementOpenedSafeCells() {
        this.openedSafeCells++;
    }

    /** Cộng số ô an toàn vừa mở, bao gồm các ô mở lan trong cùng thao tác. */
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

    /** Tăng tổng số thao tác được ghi nhận cho người chơi. */
    public void incrementTotalActions() {
        this.totalActions++;
    }

    /** Tăng số cờ hiện có, tổng lần cắm cờ và tổng thao tác của người chơi. */
    public void placeFlag() {
        this.currentFlags++;
        this.flagsPlaced++;
        this.totalActions++;
    }

    /** Ghi nhận một lần gỡ cờ và điều chỉnh các bộ đếm của người chơi. */
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
