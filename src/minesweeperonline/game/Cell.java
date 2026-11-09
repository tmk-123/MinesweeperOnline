package minesweeperonline.game;

/**
 * Đại diện cho một ô trên bàn cờ 12x12 do server quản lý.
 */
public class Cell {

    private final int row;
    private final int col;
    private boolean mine;
    private boolean opened;
    private boolean flagged;
    private int adjacentMines; // số quả mìn ở các ô xung quanh
    private boolean exploded; // hiện biểu tượng quả mìn nếu đã nổ

    /** Tạo ô tại hàng, cột cho trước rồi đặt trạng thái ban đầu. */
    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        reset();
    }

    /** Xóa mìn, cờ, trạng thái mở, trạng thái nổ và số mìn lân cận để tái sử dụng ô. */
    public void reset() {
        this.mine = false;
        this.opened = false;
        this.flagged = false;
        this.adjacentMines = 0;
        this.exploded = false;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public int getAdjacentMines() {
        return adjacentMines;
    }

    public void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }

    public boolean isExploded() {
        return exploded;
    }

    public void setExploded(boolean exploded) {
        this.exploded = exploded;
    }

    @Override
    public String toString() {
        return "Cell{" +
                "r=" + row +
                ", c=" + col +
                ", mine=" + mine +
                ", opened=" + opened +
                ", flagged=" + flagged +
                ", adj=" + adjacentMines +
                '}';
    }
}
