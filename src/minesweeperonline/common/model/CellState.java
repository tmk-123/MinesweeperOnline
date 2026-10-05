package minesweeperonline.common.model;

import java.io.Serializable;

/**
 * Represents the observable state of a single cell in the 12x12 board.
 */
public class CellState implements Serializable {

    private static final long serialVersionUID = 1L;

    private int row;
    private int col;
    private boolean opened;
    private boolean flagged;
    private int adjacentMines; // 0-8 when opened, or -1 when hidden
    private boolean mine;      // true if cell is a mine (hidden until revealed/game end)
    private boolean exploded;  // true if this cell was opened and blew up

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
     * Creates a copy of this cell suitable for transmission to the opponent.
     * Hides whether a hidden cell has a mine.
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
            // Conceal mine info and adjacent count for hidden cells
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
