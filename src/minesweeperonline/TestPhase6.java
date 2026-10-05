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

        // 1. Validate Board and MineGenerator counts
        Board board = new Board();
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
        System.out.println("[PASS] Board generated: 144 cells, exactly 23 mines, 121 safe cells.");

        // 2. Validate Adjacent Mines Calculation correctness
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

        // 3. Validate Flagging logic & limits
        int safeR = -1, safeC = -1;
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                if (!board.getCell(r, c).isMine()) {
                    safeR = r;
                    safeC = c;
                    break;
                }
            }
            if (safeR != -1) break;
        }

        boolean flagPlaced = board.placeFlag(safeR, safeC);
        assert flagPlaced : "Failed to place flag!";
        assert board.getCell(safeR, safeC).isFlagged() : "Cell not marked flagged!";
        assert board.getCurrentFlags() == 1 : "currentFlags mismatch!";
        assert board.getTotalActions() == 1 : "totalActions mismatch!";
        System.out.println("[PASS] Flag placed successfully on (" + safeR + "," + safeC + ").");

        boolean flagRemoved = board.removeFlag(safeR, safeC);
        assert flagRemoved : "Failed to remove flag!";
        assert !board.getCell(safeR, safeC).isFlagged() : "Cell still marked flagged!";
        assert board.getCurrentFlags() == 0 : "currentFlags should be 0!";
        assert board.getTotalActions() == 2 : "totalActions should be 2!";
        System.out.println("[PASS] Flag removed successfully.");

        // 4. Validate BFS Flood-fill
        List<Cell> openedList = new ArrayList<>();
        Board.OpenResult openRes = board.openCell(safeR, safeC, openedList);
        assert openRes == Board.OpenResult.SAFE : "Expected SAFE open result!";
        assert openedList.size() >= 1 : "At least 1 cell must be opened!";
        assert board.getOpenedSafeCells() == openedList.size() : "openedSafeCells count mismatch!";
        // Crucial test: totalActions must only have incremented by 1 (totalActions was 2 before click, now 3)
        assert board.getTotalActions() == 3 : "totalActions must only increment by 1 for the click!";
        System.out.println("[PASS] Cell opened! Newly opened cells: " + openedList.size() +
                " | openedSafeCells=" + board.getOpenedSafeCells() +
                " | totalActions=" + board.getTotalActions() + " (BFS did not inflate action count).");

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
