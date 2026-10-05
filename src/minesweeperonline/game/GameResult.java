package minesweeperonline.game;

import minesweeperonline.server.ServerConfig;

/**
 * Outcome states and corresponding score adjustments for matches.
 */
public enum GameResult {
    VICTORY(ServerConfig.SCORE_VICTORY),
    DEFEAT(ServerConfig.SCORE_DEFEAT),
    FORFEIT(ServerConfig.SCORE_FORFEIT),
    DRAW(ServerConfig.SCORE_DRAW),
    ONGOING(0);

    private final int scoreDelta;

    GameResult(int scoreDelta) {
        this.scoreDelta = scoreDelta;
    }

    public int getScoreDelta() {
        return scoreDelta;
    }
}
