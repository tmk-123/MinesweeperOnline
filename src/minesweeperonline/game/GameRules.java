package minesweeperonline.game;

import minesweeperonline.common.model.Player;

/**
 * Phân xử kết quả trận đấu theo luật thi đấu chính thức.
 */
public class GameRules {

    /**
     * Xác định người thắng khi hết giờ (TIMEOUT) theo thứ tự ưu tiên nghiêm ngặt:
     * 1. openedSafeCells giảm dần (càng nhiều càng tốt)
     * 2. totalActions tăng dần (càng ít càng tốt)
     * 3. flagsPlaced giảm dần (càng nhiều càng tốt)
     * 4. DRAW (hòa) nếu tất cả tiêu chí đều bằng nhau
     *
     * @param p1 người chơi 1
     * @param p2 người chơi 2
     * @return userId của người thắng, hoặc 0 nếu hòa (DRAW)
     */
    public static int arbitrateTimeout(Player p1, Player p2) {
        // Tiêu chí 1: Mở được nhiều ô an toàn nhất
        if (p1.getOpenedSafeCells() > p2.getOpenedSafeCells()) {
            return p1.getUserId();
        } else if (p2.getOpenedSafeCells() > p1.getOpenedSafeCells()) {
            return p2.getUserId();
        }

        // Tiêu chí 2: Tổng số thao tác ít nhất
        if (p1.getTotalActions() < p2.getTotalActions()) {
            return p1.getUserId();
        } else if (p2.getTotalActions() < p1.getTotalActions()) {
            return p2.getUserId();
        }

        // Tiêu chí 3: Cắm được nhiều cờ nhất
        if (p1.getFlagsPlaced() > p2.getFlagsPlaced()) {
            return p1.getUserId();
        } else if (p2.getFlagsPlaced() > p1.getFlagsPlaced()) {
            return p2.getUserId();
        }

        // Tiêu chí 4: Bằng nhau hoàn toàn -> hòa (DRAW)
        return 0;
    }
}
