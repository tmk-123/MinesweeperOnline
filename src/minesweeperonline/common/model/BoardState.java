package minesweeperonline.common.model;

import java.io.Serializable;

/**
 * Represents the full observable state of a 12x12 Minesweeper board.
 */
public class BoardState implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int ROWS = 12;
    public static final int COLS = 12;
    public static final int TOTAL_CELLS = ROWS * COLS; // 144
    public static final int TOTAL_MINES = 23;
    public static final int TOTAL_SAFE_CELLS = TOTAL_CELLS - TOTAL_MINES; // 121

    private CellState[][] cells;
    private int openedSafeCells;
    private int currentFlags;

    public BoardState() {
        this.cells = new CellState[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                this.cells[r][c] = new CellState(r, c);
            }
        }
        this.openedSafeCells = 0;
        this.currentFlags = 0;
    }

    public CellState getCell(int r, int c) {
        if (isValidCoordinate(r, c)) {
            return cells[r][c];
        }
        return null;
    }

    public void setCell(int r, int c, CellState cell) {
        if (isValidCoordinate(r, c)) {
            cells[r][c] = cell;
        }
    }

    public static boolean isValidCoordinate(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    public boolean isAllSafeCellsOpened() {
        return openedSafeCells >= TOTAL_SAFE_CELLS;
    }

    public CellState[][] getCells() {
        return cells;
    }

    public void setCells(CellState[][] cells) {
        this.cells = cells;
    }

    public int getOpenedSafeCells() {
        return openedSafeCells;
    }

    public void setOpenedSafeCells(int openedSafeCells) {
        this.openedSafeCells = openedSafeCells;
    }

    public int getCurrentFlags() {
        return currentFlags;
    }

    public void setCurrentFlags(int currentFlags) {
        this.currentFlags = Math.max(0, currentFlags);
    }

    /**
     * Produces a masked BoardState for the opponent.
     * All unrevealed cells have their mine and adjacent count hidden.
     */
    public BoardState toOpponentView() {
        BoardState view = new BoardState();
        view.setOpenedSafeCells(this.openedSafeCells);
        view.setCurrentFlags(this.currentFlags);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                view.cells[r][c] = this.cells[r][c].toOpponentView();
            }
        }
        return view;
    }
}
