package minesweeperonline.server;

/**
 * Server configuration constants and parameters.
 */
public class ServerConfig {

    // Network settings
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 2209;

    // Database settings
    public static final String DB_HOST = "localhost";
    public static final int DB_PORT = 3306;
    public static final String DB_NAME = "minesweeper_online";
    public static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "";

    // Gameplay timers (in seconds)
    public static final int READY_COUNTDOWN_SECONDS = 5;
    public static final int MATCH_DURATION_SECONDS = 300; // 5 minutes
    public static final int CHALLENGE_TIMEOUT_SECONDS = 30;

    // Board configuration
    public static final int BOARD_ROWS = 12;
    public static final int BOARD_COLS = 12;
    public static final int TOTAL_MINES = 23;
    public static final int TOTAL_SAFE_CELLS = 121;

    // Score rules
    public static final int SCORE_VICTORY = 5;
    public static final int SCORE_DEFEAT = -3;
    public static final int SCORE_FORFEIT = -5;
    public static final int SCORE_DRAW = 1;
}
