package minesweeperonline;

import minesweeperonline.common.model.Player;
import minesweeperonline.game.Board;
import minesweeperonline.game.Cell;
import minesweeperonline.game.GameRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Test kiểm chứng cho Phase 6:
 * 1. Kiểm tra Board và MineGenerator (12x12, đúng 23 mìn, 121 ô an toàn).
 * 2. Kiểm tra mở ô loang (flood-fill) bằng BFS.
 * 3. Thao tác cắm cờ và giới hạn (tối đa 23 cờ).
 * 4. Các tiêu chí phân xử khi hết giờ của GameRules.
 */
public class TestPhase6 {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("       RUNNING PHASE 6 GAME LOGIC VERIFICATION    ");
        System.out.println("==================================================");

        // 1. Kiểm tra trạng thái ban đầu của Board (chưa sinh mìn)
        Board board = new Board();
        assert !board.isMinesGenerated() : "Mines must NOT be generated when game starts!";
        int initialMines = 0;
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Cell cell = board.getCell(r, c);
                if (cell.isMine()) initialMines++;
                assert cell.getAdjacentMines() == 0 : "Adjacent mines must be 0 before generation!";
                assert !cell.isOpened() : "Cell must be unopened at start!";
            }
        }
        assert initialMines == 0 : "Board must have 0 mines before first click!";
        System.out.println("[PASS] Board initially created: 144 clean cells, no mines generated yet.");

        // 2. Kiểm tra logic cắm cờ và giới hạn trước lần click đầu tiên
        int testFlagR = 0, testFlagC = 0;
        boolean flagPlaced = board.placeFlag(testFlagR, testFlagC);
        assert flagPlaced : "Failed to place flag!";
        assert board.getCell(testFlagR, testFlagC).isFlagged() : "Cell not marked flagged!";
        assert board.getCurrentFlags() == 1 : "currentFlags mismatch!";
        assert board.getTotalActions() == 1 : "totalActions mismatch!";
        System.out.println("[PASS] Flag placed successfully on (" + testFlagR + "," + testFlagC + ").");

        boolean flagRemoved = board.removeFlag(testFlagR, testFlagC);
        assert flagRemoved : "Failed to remove flag!";
        assert !board.getCell(testFlagR, testFlagC).isFlagged() : "Cell still marked flagged!";
        assert board.getCurrentFlags() == 0 : "currentFlags should be 0!";
        assert board.getTotalActions() == 2 : "totalActions should be 2!";
        System.out.println("[PASS] Flag removed successfully.");

        // 3. Kiểm tra lần click đầu tiên (sinh mìn trễ và đảm bảo ô số 0)
        int firstR = 5, firstC = 5;
        List<Cell> openedList = new ArrayList<>();
        Board.OpenResult openRes = board.openCell(firstR, firstC, openedList);
        assert openRes == Board.OpenResult.SAFE : "First click must always be SAFE!";
        assert board.isMinesGenerated() : "Mines must be generated after first click!";

        // 3a. Kiểm tra số lượng mìn (đúng 23 mìn, 121 ô an toàn)
        int mineCount = 0;
        int safeCount = 0;
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Cell cell = board.getCell(r, c);
                if (cell.isMine()) {
                    mineCount++;
                } else {
                    safeCount++;
                }
            }
        }
        assert mineCount == Board.TOTAL_MINES : "Mine count mismatch! Expected 23, got " + mineCount;
        assert safeCount == Board.TOTAL_SAFE_CELLS : "Safe count mismatch! Expected 121, got " + safeCount;
        System.out.println("[PASS] 23 mines placed on first click (121 safe cells total).");

        // 3b. Kiểm tra ô đầu tiên có 0 mìn kề và vùng 3x3 xung quanh không có mìn
        Cell firstCell = board.getCell(firstR, firstC);
        assert !firstCell.isMine() : "First clicked cell must NOT be a mine!";
        assert firstCell.getAdjacentMines() == 0 : "First clicked cell must have 0 adjacent mines! Got: " + firstCell.getAdjacentMines();

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = firstR + dr;
                int nc = firstC + dc;
                if (board.isValidCoordinate(nr, nc)) {
                    assert !board.getCell(nr, nc).isMine() : "3x3 neighborhood around first click must not contain mines! Mine found at (" + nr + "," + nc + ")";
                }
            }
        }
        System.out.println("[PASS] First clicked cell (5,5) is guaranteed 0, and entire 3x3 zone is free of mines.");

        // 3c. Kiểm tra việc tính số mìn kề trên toàn bộ bàn chơi
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Cell cell = board.getCell(r, c);
                if (!cell.isMine()) {
                    int actualMinesAround = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) continue;
                            int nr = r + dr;
                            int nc = c + dc;
                            if (board.isValidCoordinate(nr, nc) && board.getCell(nr, nc).isMine()) {
                                actualMinesAround++;
                            }
                        }
                    }
                    assert cell.getAdjacentMines() == actualMinesAround :
                            "Adjacent mine calculation error at (" + r + "," + c + ")";
                }
            }
        }
        System.out.println("[PASS] Neighbor mine calculations verified for all cells.");

        // 3d. Kiểm tra mở ô loang (flood-fill) bằng BFS
        assert openedList.size() >= 9 : "BFS on interior 0-cell must open at least the 3x3 area (9 cells)! Opened: " + openedList.size();
        assert board.getOpenedSafeCells() == openedList.size() : "openedSafeCells count mismatch!";
        assert board.getTotalActions() == 3 : "totalActions must only increment by 1 for the click!";
        for (Cell opened : openedList) {
            assert !opened.isMine() : "Opened cell must never be a mine!";
            assert opened.isOpened() : "Opened cell must be marked opened!";
        }
        System.out.println("[PASS] BFS flood-fill automatically opened " + openedList.size() +
                " cells | openedSafeCells=" + board.getOpenedSafeCells() +
                " | totalActions=" + board.getTotalActions() + " (actions not inflated).");

        // 3e. Kiểm tra các lần click sau KHÔNG sinh lại mìn
        boolean[][] mineMapBefore = new boolean[Board.ROWS][Board.COLS];
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                mineMapBefore[r][c] = board.getCell(r, c).isMine();
            }
        }
        // Tìm một ô an toàn chưa mở để click
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Cell cCell = board.getCell(r, c);
                if (!cCell.isOpened() && !cCell.isMine()) {
                    List<Cell> nextOpened = new ArrayList<>();
                    board.openCell(r, c, nextOpened);
                    break;
                }
            }
        }
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                assert board.getCell(r, c).isMine() == mineMapBefore[r][c] : "Mines must NOT change positions on subsequent clicks!";
            }
        }
        System.out.println("[PASS] Mine positions remain strictly fixed on subsequent clicks.");

        // 5. Kiểm tra các quy tắc của GameRules.arbitrateTimeout
        Player p1 = new Player(10, "P1", 0);
        Player p2 = new Player(20, "P2", 0);

        // Quy tắc 1: Mở được nhiều ô an toàn hơn thì thắng
        p1.setOpenedSafeCells(50);
        p2.setOpenedSafeCells(40);
        assert GameRules.arbitrateTimeout(p1, p2) == 10 : "Rule 1 timeout arbitration failed!";

        // Quy tắc 2: Bằng số ô an toàn -> ít tổng thao tác hơn thì thắng
        p2.setOpenedSafeCells(50);
        p1.setTotalActions(20);
        p2.setTotalActions(25);
        assert GameRules.arbitrateTimeout(p1, p2) == 10 : "Rule 2 timeout arbitration failed!";

        // Quy tắc 3: Bằng số ô an toàn và thao tác -> cắm nhiều cờ hơn thì thắng
        p2.setTotalActions(20);
        p1.setFlagsPlaced(15);
        p2.setFlagsPlaced(10);
        assert GameRules.arbitrateTimeout(p1, p2) == 10 : "Rule 3 timeout arbitration failed!";

        // Quy tắc 4: Mọi chỉ số bằng nhau -> HÒA (0)
        p2.setFlagsPlaced(15);
        assert GameRules.arbitrateTimeout(p1, p2) == 0 : "Rule 4 timeout arbitration failed!";
        System.out.println("[PASS] GameRules.arbitrateTimeout verified against all 4 ranking rules.");

        System.out.println("==================================================");
        System.out.println("   ALL PHASE 6 LOGIC TESTS PASSED SUCCESSFULLY!   ");
        System.out.println("==================================================");
    }
}
