package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Bàn cờ Minesweeper 12x12 có thể tương tác.
 * Click trái để OPEN_CELL, click phải để PLACE_FLAG / REMOVE_FLAG.
 * Dùng icon vector chất lượng cao cho cờ và mìn, tránh lỗi hiển thị ký tự font của hệ điều hành.
 */
public class GameBoardPanel extends JPanel {

    public static final int ROWS = 12;
    public static final int COLS = 12;

    private final ServerConnection connection;
    private final String gameId;
    private final JButton[][] buttons;
    private final boolean[][] opened;
    private final boolean[][] flagged;

    private boolean interactive = false;

    // Màu số kiểu cổ điển
    private static final Color[] NUMBER_COLORS = {
            Color.BLACK,
            new Color(0, 0, 255),       // 1: Xanh dương
            new Color(0, 128, 0),       // 2: Xanh lá
            new Color(255, 0, 0),       // 3: Đỏ
            new Color(0, 0, 128),       // 4: Xanh navy
            new Color(128, 0, 0),       // 5: Đỏ sẫm
            new Color(0, 128, 128),     // 6: Xanh mòng két
            Color.BLACK,                // 7: Đen
            Color.GRAY                  // 8: Xám
    };

    // Ô chưa mở: xám xanh, nổi khối; ô đã mở: gần trắng, phẳng -> nhìn là phân biệt được
    private static final Color HIDDEN_BG = new Color(178, 190, 204);
    private static final Color OPENED_BG = new Color(250, 251, 252);
    private static final Color OPENED_BORDER = new Color(205, 211, 218);

    private static final Icon FLAG_ICON = new FlagIcon();
    private static final Icon MINE_NORMAL_ICON = new MineIcon(false);
    private static final Icon MINE_EXPLODED_ICON = new MineIcon(true);
    private static final Icon CROSSED_MINE_ICON = new CrossedMineIcon();

    public GameBoardPanel(ServerConnection connection, String gameId) {
        this.connection = connection;
        this.gameId = gameId;
        this.buttons = new JButton[ROWS][COLS];
        this.opened = new boolean[ROWS][COLS];
        this.flagged = new boolean[ROWS][COLS];

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridLayout(ROWS, COLS, 2, 2));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setBackground(new Color(180, 190, 200));

        Font cellFont = new Font("Arial", Font.BOLD, 14);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                final int row = r;
                final int col = c;

                JButton btn = new JButton("");
                btn.setFont(cellFont);
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setFocusPainted(false);
                btn.setFocusable(false);
                // Look&Feel của Windows tự vẽ nền nút và bỏ qua setBackground,
                // nên tắt phần nền đó và để nút tự tô màu (opaque) theo trạng thái ô
                btn.setContentAreaFilled(false);
                btn.setOpaque(true);
                btn.setBackground(HIDDEN_BG);
                btn.setBorder(BorderFactory.createRaisedBevelBorder());

                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (!interactive || opened[row][col]) return;

                        if (SwingUtilities.isLeftMouseButton(e)) {
                            // Click trái -> mở ô
                            if (!flagged[row][col]) {
                                connection.sendMessage(JsonProtocol.createOpenCell(gameId, row, col));
                            }
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            // Click phải -> bật/tắt cờ
                            if (flagged[row][col]) {
                                connection.sendMessage(JsonProtocol.createRemoveFlag(gameId, row, col));
                            } else {
                                connection.sendMessage(JsonProtocol.createPlaceFlag(gameId, row, col));
                            }
                        }
                    }
                });

                buttons[r][c] = btn;
                add(btn);
            }
        }
    }

    /**
     * Cập nhật và hiển thị một ô sau khi nhận xác nhận từ Server.
     */
    public void revealCell(int r, int c, int adjacentMines, boolean isMine, boolean isExploded) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return;

        JButton btn = buttons[r][c];
        opened[r][c] = true;
        flagged[r][c] = false;

        btn.setRolloverEnabled(false);
        btn.setBorder(BorderFactory.createLineBorder(OPENED_BORDER, 1));

        if (isMine) {
            Icon mIcon = isExploded ? MINE_EXPLODED_ICON : MINE_NORMAL_ICON;
            btn.setIcon(mIcon);
            btn.setDisabledIcon(mIcon);
            btn.setText("");
            btn.setBackground(isExploded ? new Color(255, 80, 80) : new Color(255, 180, 180));
        } else {
            btn.setIcon(null);
            btn.setDisabledIcon(null);
            btn.setBackground(OPENED_BG);
            if (adjacentMines > 0) {
                btn.setText(String.valueOf(adjacentMines));
                int colorIdx = Math.min(adjacentMines, NUMBER_COLORS.length - 1);
                btn.setForeground(NUMBER_COLORS[colorIdx]);
            } else {
                btn.setText("");
            }
        }
    }

    public void setCellFlagged(int r, int c, boolean isFlagged) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS || opened[r][c]) return;

        flagged[r][c] = isFlagged;
        JButton btn = buttons[r][c];
        if (isFlagged) {
            btn.setIcon(FLAG_ICON);
            btn.setDisabledIcon(FLAG_ICON);
            btn.setText("");
        } else {
            btn.setIcon(null);
            btn.setDisabledIcon(null);
            btn.setText("");
        }
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    /**
     * Khi trận kết thúc: hiện tất cả mìn trên bàn của mình theo danh sách server gửi kèm.
     * - Ô mìn chưa mở, không cắm cờ -> hiện quả mìn
     * - Mọi ô đang cắm cờ           -> chuyển sang nền "đã mở", hiện quả mìn có dấu X đỏ
     * Ô đã mở (kể cả quả mìn vừa nổ) giữ nguyên.
     *
     * @param mines tọa độ {row, col} của các ô có mìn
     */
    public void revealAllMines(List<int[]> mines) {
        boolean[][] isMine = new boolean[ROWS][COLS];
        for (int[] m : mines) {
            if (m[0] >= 0 && m[0] < ROWS && m[1] >= 0 && m[1] < COLS) {
                isMine[m[0]][m[1]] = true;
            }
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (opened[r][c]) continue;
                JButton btn = buttons[r][c];
                Icon icon = null;
                if (flagged[r][c]) {
                    icon = CROSSED_MINE_ICON;
                    btn.setBackground(OPENED_BG);
                    btn.setBorder(BorderFactory.createLineBorder(OPENED_BORDER, 1));
                } else if (isMine[r][c]) {
                    icon = MINE_NORMAL_ICON;
                }
                if (icon != null) {
                    btn.setIcon(icon);
                    btn.setDisabledIcon(icon);
                    btn.setText("");
                }
            }
        }
    }

    /**
     * Icon vector vẽ lá cờ đỏ sắc nét với cán cờ màu tối và đế cờ.
     */
    private static class FlagIcon implements Icon {
        private final int width = 20;
        private final int height = 20;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Cán cờ
            g2.setColor(new Color(50, 50, 50));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 5, y + 2, x + 5, y + 17);

            // Quả cầu vàng trên đỉnh cán cờ
            g2.setColor(new Color(220, 180, 30));
            g2.fillOval(x + 4, y + 1, 3, 3);

            // Đế cán cờ
            g2.setColor(new Color(50, 50, 50));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 17, x + 10, y + 17);

            // Lá cờ tam giác màu đỏ (đuôi nheo hướng sang phải)
            Polygon flag = new Polygon();
            flag.addPoint(x + 6, y + 2);
            flag.addPoint(x + 17, y + 6);
            flag.addPoint(x + 6, y + 11);

            g2.setColor(new Color(235, 30, 30));
            g2.fill(flag);
            g2.setColor(new Color(180, 15, 15));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(flag);

            g2.dispose();
        }

        @Override
        public int getIconWidth() { return width; }

        @Override
        public int getIconHeight() { return height; }
    }

    /**
     * Icon vector vẽ quả mìn kiểu Minesweeper cổ điển.
     */
    private static class MineIcon implements Icon {
        private final int width = 20;
        private final int height = 20;
        private final boolean exploded;

        public MineIcon(boolean exploded) {
            this.exploded = exploded;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = x + width / 2;
            int cy = y + height / 2;
            int r = 6;

            // Các gai mìn (chữ thập & đường chéo)
            g2.setColor(exploded ? new Color(140, 20, 20) : new Color(40, 40, 40));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx - r - 3, cy, cx + r + 3, cy);
            g2.drawLine(cx, cy - r - 3, cx, cy + r + 3);
            int diag = 6;
            g2.drawLine(cx - diag, cy - diag, cx + diag, cy + diag);
            g2.drawLine(cx - diag, cy + diag, cx + diag, cy - diag);

            // Thân quả mìn
            g2.setColor(exploded ? new Color(220, 30, 30) : new Color(30, 30, 30));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            // Điểm phản quang
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 3, cy - 3, 2, 2);

            g2.dispose();
        }

        @Override
        public int getIconWidth() { return width; }

        @Override
        public int getIconHeight() { return height; }
    }

    /**
     * Icon cho ô cắm cờ khi kết thúc trận: quả mìn bị gạch chéo bằng dấu X đỏ.
     */
    private static class CrossedMineIcon implements Icon {
        private final Icon mine = new MineIcon(false);

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            mine.paintIcon(c, g, x, y);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(230, 40, 30));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 3, x + getIconWidth() - 3, y + getIconHeight() - 3);
            g2.drawLine(x + 3, y + getIconHeight() - 3, x + getIconWidth() - 3, y + 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return mine.getIconWidth(); }

        @Override
        public int getIconHeight() { return mine.getIconHeight(); }
    }
}
