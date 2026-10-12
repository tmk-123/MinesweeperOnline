package minesweeperonline.common.model;

import java.io.Serializable;

/**
 * Đại diện cho trạng thái hiển thị được của một ô trên bàn cờ 12x12.
 */
public class CellState implements Serializable {

    private static final long serialVersionUID = 1L;

    private int row;
    private int col;
    private boolean opened;
    private boolean flagged;
    private int adjacentMines; // 0-8 khi đã mở, -1 khi còn ẩn
    private boolean mine;      // true nếu ô là mìn (ẩn cho đến khi lộ ra/kết thúc game)
    private boolean exploded;  // true nếu ô này bị mở và phát nổ

    public CellState() {
        this.opened = false;
        this.flagged = false;
        this.adjacentMines = -1;
        this.mine = false;
        this.exploded = false;
    }

    public CellState(int row, int col) {
        this.row = row;
        this.col = col;
        this.opened = false;
        this.flagged = false;
        this.adjacentMines = -1;
        this.mine = false;
        this.exploded = false;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
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

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public boolean isExploded() {
        return exploded;
    }

    public void setExploded(boolean exploded) {
        this.exploded = exploded;
    }

    /**
     * Tạo bản sao của ô này phù hợp để gửi cho đối thủ.
     * Ẩn thông tin ô chưa mở có phải là mìn hay không.
     */
    public CellState toOpponentView() {
        CellState copy = new CellState(row, col);
        copy.setOpened(this.opened);
        copy.setFlagged(this.flagged);
        if (this.opened) {
            copy.setAdjacentMines(this.adjacentMines);
            copy.setMine(this.mine);
            copy.setExploded(this.exploded);
        } else {
            // Che thông tin mìn và số mìn lân cận của các ô chưa mở
            copy.setAdjacentMines(-1);
            copy.setMine(false);
            copy.setExploded(false);
        }
        return copy;
    }

    @Override
    public String toString() {
        return "CellState{" +
                "r=" + row +
                ", c=" + col +
                ", opened=" + opened +
                ", flagged=" + flagged +
                ", adj=" + adjacentMines +
                ", mine=" + mine +
                ", exploded=" + exploded +
                '}';
    }
}
