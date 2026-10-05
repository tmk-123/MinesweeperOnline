package minesweeperonline.game;

import minesweeperonline.common.model.Player;

/**
 * Arbitrates match results according to the official tournament rules.
 */
public class GameRules {

    /**
     * Arbitrates winner on TIMEOUT based on strict precedence:
     * 1. openedSafeCells descending (higher is better)
     * 2. totalActions ascending (fewer is better)
     * 3. flagsPlaced descending (higher is better)
     * 4. DRAW if all criteria are equal
     *
     * @param p1 player 1
     * @param p2 player 2
     * @return winner's userId, or 0 if DRAW
     */
    public static int arbitrateTimeout(Player p1, Player p2) {
        // Criterion 1: Most opened safe cells
        if (p1.getOpenedSafeCells() > p2.getOpenedSafeCells()) {
            return p1.getUserId();
        } else if (p2.getOpenedSafeCells() > p1.getOpenedSafeCells()) {
            return p2.getUserId();
        }

        // Criterion 2: Fewest total actions
        if (p1.getTotalActions() < p2.getTotalActions()) {
            return p1.getUserId();
        } else if (p2.getTotalActions() < p1.getTotalActions()) {
            return p2.getUserId();
        }

        // Criterion 3: Most flags placed
        if (p1.getFlagsPlaced() > p2.getFlagsPlaced()) {
            return p1.getUserId();
        } else if (p2.getFlagsPlaced() > p1.getFlagsPlaced()) {
            return p2.getUserId();
        }

        // Criterion 4: Completely equal -> DRAW
        return 0;
    }
}
