package minesweeperonline.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates random non-overlapping mines and calculates adjacent mine counts.
 * Supports first-click safety: ensures the first clicked cell has 0 adjacent mines (3x3 safe zone).
 */
public class MineGenerator {

    private static final Random random = new Random();

    /**
     * Generates mines without a safe zone (legacy/fallback).
     */
    public static void generateMines(Cell[][] grid, int rows, int cols, int mineCount) {
        generateMines(grid, rows, cols, mineCount, -1, -1);
    }

    /**
     * Places exact number of mines randomly without duplicates across the grid,
     * ensuring that the cell at (safeRow, safeCol) and its 8 neighbors (3x3 zone)
     * contain NO mines, so that the first clicked cell has an adjacent mine count of 0.
     * Then calculates adjacentMines for all safe cells on the board.
     *
     * @param grid the 2D cell grid
     * @param rows number of rows
     * @param cols number of columns
     * @param mineCount number of mines to place
     * @param safeRow row of the first click (-1 if no safe zone)
     * @param safeCol column of the first click (-1 if no safe zone)
     */
    public static void generateMines(Cell[][] grid, int rows, int cols, int mineCount, int safeRow, int safeCol) {
        int totalCells = rows * cols;
        if (mineCount >= totalCells) {
            throw new IllegalArgumentException("Mine count must be strictly less than total cells.");
        }

        // Clear existing mines and adjacent counts without clearing flags or opened states
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setMine(false);
                grid[r][c].setAdjacentMines(0);
            }
        }

        // Collect candidate cells outside the safe zone (safe cell + 8 surrounding neighbors)
        List<int[]> candidates = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!isInSafeZone(r, c, safeRow, safeCol)) {
                    candidates.add(new int[]{r, c});
                }
            }
        }

        if (mineCount > candidates.size()) {
            throw new IllegalArgumentException("Mine count (" + mineCount + ") exceeds available candidates (" + candidates.size() + ").");
        }

        // Randomly place mineCount mines
        Collections.shuffle(candidates, random);
        for (int i = 0; i < mineCount; i++) {
            int[] pos = candidates.get(i);
            grid[pos[0]][pos[1]].setMine(true);
        }

        // Calculate adjacent mines for all cells
        calculateAdjacentMines(grid, rows, cols);

        // Verification check for safe cell
        if (safeRow >= 0 && safeRow < rows && safeCol >= 0 && safeCol < cols) {
            if (grid[safeRow][safeCol].isMine() || grid[safeRow][safeCol].getAdjacentMines() != 0) {
                throw new IllegalStateException("First click cell (" + safeRow + ", " + safeCol + ") must be safe and have 0 adjacent mines!");
            }
        }
    }

    /**
     * Checks if cell (r, c) falls within the 3x3 safe zone centered at (safeRow, safeCol).
     */
    public static boolean isInSafeZone(int r, int c, int safeRow, int safeCol) {
        if (safeRow < 0 || safeCol < 0) {
            return false;
        }
        return Math.abs(r - safeRow) <= 1 && Math.abs(c - safeCol) <= 1;
    }

    /**
     * Calculates the adjacent mine count for all safe cells on the grid.
     */
    public static void calculateAdjacentMines(Cell[][] grid, int rows, int cols) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].isMine()) {
                    int count = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) continue;
                            int nr = r + dr;
                            int nc = c + dc;
                            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc].isMine()) {
                                count++;
                            }
                        }
                    }
                    grid[r][c].setAdjacentMines(count);
                } else {
                    grid[r][c].setAdjacentMines(0);
                }
            }
        }
    }
}
