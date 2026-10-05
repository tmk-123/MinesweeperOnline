package minesweeperonline.game;

import java.util.Random;

/**
 * Generates random non-overlapping mines and calculates adjacent mine counts.
 */
public class MineGenerator {

    private static final Random random = new Random();

    /**
     * Places exact number of mines randomly without duplicates across the grid,
     * and calculates adjacentMines for all remaining safe cells.
     */
    public static void generateMines(Cell[][] grid, int rows, int cols, int mineCount) {
        int totalCells = rows * cols;
        if (mineCount >= totalCells) {
            throw new IllegalArgumentException("Mine count must be strictly less than total cells.");
        }

        // Reset all cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].reset();
            }
        }

        // Randomly place mineCount mines
        int placed = 0;
        while (placed < mineCount) {
            int r = random.nextInt(rows);
            int c = random.nextInt(cols);
            if (!grid[r][c].isMine()) {
                grid[r][c].setMine(true);
                placed++;
            }
        }

        // Calculate adjacent mines for all cells
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
                }
            }
        }
    }
}
