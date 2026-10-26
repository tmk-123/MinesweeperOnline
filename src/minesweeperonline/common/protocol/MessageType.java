package minesweeperonline.common.protocol;

/**
 * Các loại message dùng cho giao tiếp client-server trong MinesweeperOnline.
 */
public enum MessageType {
    // --- Xác thực ---
    REGISTER,
    REGISTER_SUCCESS,
    REGISTER_FAILED,
    LOGIN,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,

    // --- Sảnh chờ & người chơi online ---
    GET_ONLINE_PLAYERS,
    ONLINE_PLAYERS,

    // --- Ghép trận & thách đấu ---
    CHALLENGE,
    CHALLENGE_REQUEST,
    ACCEPT,
    REJECT,
    MATCH_CREATED,
    QUICK_MATCH,
    CANCEL_QUICK_MATCH,

    // --- Vòng đời ván chơi ---
    GAME_READY,
    GAME_START,

    // --- Thao tác trong game ---
    OPEN_CELL,
    CELL_OPENED,
    PLACE_FLAG,
    FLAG_PLACED,
    REMOVE_FLAG,
    FLAG_REMOVED,

    // --- Đồng bộ trạng thái ---
    GAME_UPDATE,

    // --- Kết quả ván chơi ---
    GAME_WIN,
    GAME_LOSE,
    GAME_DRAW,
    FORFEIT,
    OPPONENT_DISCONNECTED,

    // --- Sau ván chơi ---
    PLAY_AGAIN,
    EXIT,

    // --- Bảng xếp hạng ---
    GET_LEADERBOARD,
    LEADERBOARD,

    // --- Lịch sử trận đấu ---
    GET_MATCH_HISTORY,
    MATCH_HISTORY,

    // --- Lỗi chung ---
    ERROR
}
