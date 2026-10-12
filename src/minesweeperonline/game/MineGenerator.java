package minesweeperonline.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Sinh mìn ngẫu nhiên không trùng lặp và tính số mìn xung quanh mỗi ô.
 * Hỗ trợ an toàn ở lần click đầu: đảm bảo ô được click đầu tiên có 0 mìn xung quanh (vùng an toàn 3x3).
 */
public class MineGenerator {

    private static final Random random = new Random();

    /**
     * Sinh mìn không có vùng an toàn (bản cũ/fallback).
     */
    public static void generateMines(Cell[][] grid, int rows, int cols, int mineCount) {
        generateMines(grid, rows, cols, mineCount, -1, -1);
    }

    /**
     * Đặt ngẫu nhiên đúng số lượng mìn trên lưới, không trùng lặp,
     * đảm bảo ô (safeRow, safeCol) và 8 ô xung quanh nó (vùng 3x3)
     * KHÔNG có mìn, để ô được click đầu tiên có số mìn xung quanh bằng 0.
     * Sau đó tính adjacentMines cho tất cả các ô an toàn trên bàn cờ.
     *
     * @param grid lưới ô 2 chiều
     * @param rows số hàng
     * @param cols số cột
     * @param mineCount số mìn cần đặt
     * @param safeRow hàng của lần click đầu tiên (-1 nếu không có vùng an toàn)
     * @param safeCol cột của lần click đầu tiên (-1 nếu không có vùng an toàn)
     */
    public static void generateMines(Cell[][] grid, int rows, int cols, int mineCount, int safeRow, int safeCol) {
        int totalCells = rows * cols;
        if (mineCount >= totalCells) {
            throw new IllegalArgumentException("Mine count must be strictly less than total cells.");
        }

        // Xóa mìn và số mìn xung quanh hiện có, giữ nguyên trạng thái cờ và ô đã mở
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setMine(false);
                grid[r][c].setAdjacentMines(0);
            }
        }

        // Gom các ô ứng viên nằm ngoài vùng an toàn (ô an toàn + 8 ô xung quanh)
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

        // Đặt ngẫu nhiên mineCount quả mìn
        Collections.shuffle(candidates, random);
        for (int i = 0; i < mineCount; i++) {
            int[] pos = candidates.get(i);
            grid[pos[0]][pos[1]].setMine(true);
        }

        // Tính số mìn xung quanh cho tất cả các ô
        calculateAdjacentMines(grid, rows, cols);

        // Kiểm tra lại ô an toàn
        if (safeRow >= 0 && safeRow < rows && safeCol >= 0 && safeCol < cols) {
            if (grid[safeRow][safeCol].isMine() || grid[safeRow][safeCol].getAdjacentMines() != 0) {
                throw new IllegalStateException("First click cell (" + safeRow + ", " + safeCol + ") must be safe and have 0 adjacent mines!");
            }
        }
    }

    /**
     * Kiểm tra ô (r, c) có nằm trong vùng an toàn 3x3 có tâm tại (safeRow, safeCol) hay không.
     */
    public static boolean isInSafeZone(int r, int c, int safeRow, int safeCol) {
        if (safeRow < 0 || safeCol < 0) {
            return false;
        }
        return Math.abs(r - safeRow) <= 1 && Math.abs(c - safeCol) <= 1;
    }

    /**
     * Tính số mìn xung quanh cho tất cả các ô an toàn trên lưới.
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
