package minesweeperonline.common.protocol;

/**
 * Message types for client-server communication in MinesweeperOnline.
 */
public enum MessageType {
    // --- Authentication ---
    REGISTER,
    REGISTER_SUCCESS,
    REGISTER_FAILED,
    LOGIN,
    LOGIN_SUCCESS,
    LOGIN_FAILED,

    // --- Lobby & Online Users ---
    GET_ONLINE_PLAYERS,
    ONLINE_PLAYERS,

    // --- Matchmaking & Challenge ---
    CHALLENGE,
    CHALLENGE_REQUEST,
    ACCEPT,
    REJECT,
    MATCH_CREATED,

    // --- Game Lifecycle ---
    GAME_READY,
    GAME_START,

    // --- Game Actions ---
    OPEN_CELL,
    CELL_OPENED,
    PLACE_FLAG,
    FLAG_PLACED,
    REMOVE_FLAG,
    FLAG_REMOVED,

    // --- State Synchronization ---
    GAME_UPDATE,

    // --- Game Outcomes ---
    GAME_WIN,
    GAME_LOSE,
    GAME_DRAW,
    FORFEIT,
    OPPONENT_DISCONNECTED,

    // --- Post Game ---
    PLAY_AGAIN,
    EXIT,

    // --- Leaderboard & Ranking ---
    GET_LEADERBOARD,
    LEADERBOARD,

    // --- Match History ---
    GET_MATCH_HISTORY,
    MATCH_HISTORY,

    // --- Generic Error ---
    ERROR
}
