package minesweeperonline.common.model;

/**
 * Các trạng thái trong vòng đời của một GameSession.
 */
public enum GameState {
    /** Giai đoạn chuẩn bị và đếm ngược 5 giây trước khi bắt đầu chơi. */
    READY,

    /** Đang chơi, có timer đếm ngược 5 phút. */
    PLAYING,

    /** Timer về 00:00; khóa mọi thao tác trong game và bắt đầu phân xử kết quả. */
    TIMEOUT,

    /** Trạng thái cuối sau khi thắng, thua, hòa, đầu hàng hoặc phân xử hết giờ. */
    FINISHED
}
