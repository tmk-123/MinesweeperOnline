package minesweeperonline.common.model;

/**
 * Lifecycle states of a GameSession.
 */
public enum GameState {
    /** 5-second preparation and countdown phase before active gameplay. */
    READY,

    /** Active playing state with 5-minute countdown timer. */
    PLAYING,

    /** Timer expired at 00:00; game actions locked and arbitration starts. */
    TIMEOUT,

    /** Final state after victory, defeat, draw, forfeit, or timeout settlement. */
    FINISHED
}
