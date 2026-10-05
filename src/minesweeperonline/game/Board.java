package minesweeperonline.game;

import minesweeperonline.common.model.BoardState;
import minesweeperonline.common.model.CellState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Server-authoritative Minesweeper Board (12x12, 23 mines, 121 safe cells).
 * Implements game actions (open cell, place/remove flag) and BFS flood-fill opening.
 */
public class Board {

    public static final int ROWS = 12;
    public static final int COLS = 12;
    public static final int TOTAL_CELLS = ROWS * COLS; // 144
    public static final int TOTAL_MINES = 23;
    public static final int TOTAL_SAFE_CELLS = TOTAL_CELLS - TOTAL_MINES; // 121

    public enum OpenResult {
        INVALID,
        SAFE,
        MINE
    }

    private final Cell[][] grid;
    private int openedSafeCells;
    private int currentFlags;
    private int flagsPlaced;
    private int flagsRemoved;
    private int totalActions;

    public Board() {
        this.grid = new Cell[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                this.grid[r][c] = new Cell(r, c);
            }
        }
        initialize();
    }

    /**
     * Initializes the board and places 23 mines randomly.
     */
    public final void initialize() {
        this.openedSafeCells = 0;
        this.currentFlags = 0;
        this.flagsPlaced = 0;
        this.flagsRemoved = 0;
        this.totalActions = 0;

        MineGenerator.generateMines(grid, ROWS, COLS, TOTAL_MINES);
    }

    public boolean isValidCoordinate(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    public Cell getCell(int r, int c) {
        if (!isValidCoordinate(r, c)) return null;
        return grid[r][c];
    }

    /**
     * Executes OPEN_CELL action.
     *
     * @param r row coordinate (0-11)
     * @param c col coordinate (0-11)
     * @param openedList output list collecting all newly opened cells (for network updates)
     * @return OpenResult (INVALID, SAFE, or MINE)
     */
    public OpenResult openCell(int r, int c, List<Cell> openedList) {
        if (!isValidCoordinate(r, c)) {
            return OpenResult.INVALID;
        }

        Cell target = grid[r][c];
        if (target.isOpened() || target.isFlagged()) {
            return OpenResult.INVALID;
        }

        // Each valid player click increases totalActions by 1
        this.totalActions++;

        // Case 1: Opened a mine -> immediate defeat
        if (target.isMine()) {
            target.setExploded(true);
            target.setOpened(true);
            if (openedList != null) openedList.add(target);
            return OpenResult.MINE;
        }

        // Case 2: Opened numbered cell (adjacentMines > 0) -> single open
        if (target.getAdjacentMines() > 0) {
            target.setOpened(true);
            this.openedSafeCells++;
            if (openedList != null) openedList.add(target);
            return OpenResult.SAFE;
        }

        // Case 3: Opened blank cell (adjacentMines == 0) -> BFS expansion
        Queue<Cell> queue = new ArrayDeque<>();
        target.setOpened(true);
        this.openedSafeCells++;
        if (openedList != null) openedList.add(target);
        queue.add(target);

        while (!queue.isEmpty()) {
            Cell curr = queue.poll();

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = curr.getRow() + dr;
                    int nc = curr.getCol() + dc;

                    if (isValidCoordinate(nr, nc)) {
                        Cell neighbor = grid[nr][nc];
                        if (!neighbor.isOpened() && !neighbor.isFlagged() && !neighbor.isMine()) {
                            neighbor.setOpened(true);
                            this.openedSafeCells++;
                            if (openedList != null) openedList.add(neighbor);

                            // Only propagate queue for 0-cells
                            if (neighbor.getAdjacentMines() == 0) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        return OpenResult.SAFE;
    }

    /**
     * Executes PLACE_FLAG action.
     */
    public boolean placeFlag(int r, int c) {
        if (!isValidCoordinate(r, c)) return false;
        Cell cell = grid[r][c];
        if (cell.isOpened() || cell.isFlagged() || currentFlags >= TOTAL_MINES) {
            return false;
        }

        cell.setFlagged(true);
        this.currentFlags++;
        this.flagsPlaced++;
        this.totalActions++;
        return true;
    }

    /**
     * Executes REMOVE_FLAG action.
     */
    public boolean removeFlag(int r, int c) {
        if (!isValidCoordinate(r, c)) return false;
        Cell cell = grid[r][c];
        if (!cell.isFlagged()) {
            return false;
        }

        cell.setFlagged(false);
        if (this.currentFlags > 0) {
            this.currentFlags--;
        }
        this.flagsRemoved++;
        this.totalActions++;
        return true;
    }

    public boolean isAllSafeCellsOpened() {
        return openedSafeCells >= TOTAL_SAFE_CELLS;
    }

    /**
     * Converts this board to BoardState model.
     *
     * @param maskMines if true, hides mine positions of unopened cells (opponent view)
     */
    public BoardState toBoardState(boolean maskMines) {
        BoardState state = new BoardState();
        state.setOpenedSafeCells(this.openedSafeCells);
        state.setCurrentFlags(this.currentFlags);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Cell src = grid[r][c];
                CellState dst = new CellState(r, c);
                dst.setOpened(src.isOpened());
                dst.setFlagged(src.isFlagged());
                dst.setExploded(src.isExploded());

                if (src.isOpened() || !maskMines) {
                    dst.setMine(src.isMine());
                    dst.setAdjacentMines(src.getAdjacentMines());
                } else {
                    dst.setMine(false);
                    dst.setAdjacentMines(-1);
                }
                state.setCell(r, c, dst);
            }
        }
        return state;
    }

    public int getOpenedSafeCells() {
        return openedSafeCells;
    }

    public int getCurrentFlags() {
        return currentFlags;
    }

    public int getFlagsPlaced() {
        return flagsPlaced;
    }

    public int getFlagsRemoved() {
        return flagsRemoved;
    }

    public int getTotalActions() {
        return totalActions;
    }
}
