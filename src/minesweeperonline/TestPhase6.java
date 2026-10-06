package minesweeperonline;

import minesweeperonline.common.model.Player;
import minesweeperonline.game.Board;
import minesweeperonline.game.Cell;
import minesweeperonline.game.GameRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Verification test for Phase 6:
 * 1. Board & MineGenerator validation (12x12, exactly 23 mines, 121 safe cells).
 * 2. BFS flood-fill opening validation.
 * 3. Flagging actions and limits (max 23 flags).
 * 4. GameRules timeout arbitration criteria.
 */
public class TestPhase6 {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("       RUNNING PHASE 6 GAME LOGIC VERIFICATION    ");
        System.out.println("==================================================");

        // 1. Validate Initial Board state (No mines generated yet)
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

        // 2. Validate Flagging logic & limits before first click
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

        // 3. Validate First Click (Lazy Mine Generation & Guaranteed 0-cell)
        int firstR = 5, firstC = 5;
        List<Cell> openedList = new ArrayList<>();
        Board.OpenResult openRes = board.openCell(firstR, firstC, openedList);
        assert openRes == Board.OpenResult.SAFE : "First click must always be SAFE!";
        assert board.isMinesGenerated() : "Mines must be generated after first click!";

        // 3a. Validate Mine count (exactly 23 mines, 121 safe cells)
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

        // 3b. Validate that first cell has 0 adjacent mines and 3x3 surrounding zone has no mines
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

        // 3c. Validate neighbor calculations across the entire board
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

        // 3d. Validate BFS Flood-fill opening
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

        // 3e. Validate that subsequent clicks do NOT regenerate mines
        boolean[][] mineMapBefore = new boolean[Board.ROWS][Board.COLS];
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                mineMapBefore[r][c] = board.getCell(r, c).isMine();
            }
        }
        // Find an unopened safe cell to click
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

        // 5. Validate GameRules.arbitrateTimeout rules
        Player p1 = new Player(10, "P1", 0);
        Player p2 = new Player(20, "P2", 0);

        // Rule 1: More opened safe cells wins
        p1.setOpenedSafeCells(50);
        p2.setOpenedSafeCells(40);
        assert GameRules.arbitrateTimeout(p1, p2) == 10 : "Rule 1 timeout arbitration failed!";

        // Rule 2: Equal safe cells -> fewer total actions wins
        p2.setOpenedSafeCells(50);
        p1.setTotalActions(20);
        p2.setTotalActions(25);
        assert GameRules.arbitrateTimeout(p1, p2) == 10 : "Rule 2 timeout arbitration failed!";

        // Rule 3: Equal safe cells & actions -> more flags placed wins
        p2.setTotalActions(20);
        p1.setFlagsPlaced(15);
        p2.setFlagsPlaced(10);
        assert GameRules.arbitrateTimeout(p1, p2) == 10 : "Rule 3 timeout arbitration failed!";

        // Rule 4: Everything equal -> DRAW (0)
        p2.setFlagsPlaced(15);
        assert GameRules.arbitrateTimeout(p1, p2) == 0 : "Rule 4 timeout arbitration failed!";
        System.out.println("[PASS] GameRules.arbitrateTimeout verified against all 4 ranking rules.");

        System.out.println("==================================================");
        System.out.println("   ALL PHASE 6 LOGIC TESTS PASSED SUCCESSFULLY!   ");
        System.out.println("==================================================");
    }
}
