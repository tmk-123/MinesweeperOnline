package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Panel bàn cờ 12x12 thu nhỏ, hiển thị tiến trình chơi của đối thủ theo thời gian thực.
 * Hiển thị các ô đã mở và ô đã cắm cờ mà không để lộ vị trí mìn.
 */
public class OpponentMiniBoardPanel extends JPanel {

    private static final int ROWS = 12;
    private static final int COLS = 12;
    private static final int CELL_SIZE = 18;

    private static final byte STATE_HIDDEN = 0;
    private static final byte STATE_OPENED = 1;
    private static final byte STATE_FLAGGED = 2;

    private final byte[][] cellStates = new byte[ROWS][COLS];

    // Điểm nhấn trực quan nước đi gần nhất của đối thủ (Real-time Highlight)
    private int highlightRow = -1;
    private int highlightCol = -1;
    private Timer highlightTimer;

    public OpponentMiniBoardPanel() {
        setPreferredSize(new Dimension(COLS * CELL_SIZE + 24, ROWS * CELL_SIZE + 45));
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD),
                "Bàn đối thủ (Trực tiếp)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, UITheme.FONT_SMALL_BOLD, UITheme.TEXT_TITLE));
        setBackground(UITheme.BG_CARD);
    }

    public void resetBoard() {
        if (highlightTimer != null && highlightTimer.isRunning()) {
            highlightTimer.stop();
        }
        highlightRow = -1;
        highlightCol = -1;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                cellStates[r][c] = STATE_HIDDEN;
            }
        }
        repaint();
    }

    public void revealCell(int r, int c) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
            cellStates[r][c] = STATE_OPENED;
            highlightCell(r, c);
            repaint();
        }
    }

    public void setCellFlagged(int r, int c, boolean flagged) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
            if (cellStates[r][c] != STATE_OPENED) {
                cellStates[r][c] = flagged ? STATE_FLAGGED : STATE_HIDDEN;
                highlightCell(r, c);
                repaint();
            }
        }
    }

    /**
     * Kích hoạt hiệu ứng nhấp nháy/viền sáng tại ô đối thủ vừa thao tác (duy trì 1.2 giây).
     */
    public void highlightCell(int r, int c) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
            this.highlightRow = r;
            this.highlightCol = c;
            if (highlightTimer != null && highlightTimer.isRunning()) {
                highlightTimer.stop();
            }
            highlightTimer = new Timer(1200, e -> {
                highlightRow = -1;
                highlightCol = -1;
                repaint();
            });
            highlightTimer.setRepeats(false);
            highlightTimer.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int startX = (getWidth() - COLS * CELL_SIZE) / 2;
        int startY = 22 + (getHeight() - 25 - ROWS * CELL_SIZE) / 2;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = startX + c * CELL_SIZE;
                int y = startY + r * CELL_SIZE;
                byte state = cellStates[r][c];

                if (state == STATE_OPENED) {
                    // Ô an toàn đã mở
                    g2.setColor(new Color(211, 222, 227));
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2.setColor(new Color(150, 169, 178));
                    g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                    g2.setColor(UITheme.COLOR_BLUE);
                    g2.fillOval(x + CELL_SIZE / 2 - 2, y + CELL_SIZE / 2 - 2, 4, 4);

                } else if (state == STATE_FLAGGED) {
                    // Ô đã cắm cờ
                    g2.setColor(new Color(70, 37, 40));
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2.setColor(new Color(131, 69, 73));
                    g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                    // Lá cờ đỏ thu nhỏ
                    g2.setColor(new Color(220, 30, 30));
                    int[] xPoints = {x + 5, x + 13, x + 5};
                    int[] yPoints = {y + 4, y + 8, y + 12};
                    g2.fillPolygon(xPoints, yPoints, 3);
                    g2.setColor(UITheme.TEXT_BODY);
                    g2.drawLine(x + 5, y + 3, x + 5, y + 14);
                    g2.drawLine(x + 3, y + 14, x + 7, y + 14);

                } else {
                    // Ô chưa mở
                    g2.setColor(new Color(70, 84, 94));
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2.setColor(new Color(48, 63, 72));
                    g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // Vẽ hiệu ứng viền sáng nổi bật (Highlight) lên ô nước đi gần nhất của đối thủ
        if (highlightRow >= 0 && highlightRow < ROWS && highlightCol >= 0 && highlightCol < COLS) {
            int hx = startX + highlightCol * CELL_SIZE;
            int hy = startY + highlightRow * CELL_SIZE;

            // Lớp phủ màu cam sáng bán trong suốt
            g2.setColor(new Color(245, 158, 11, 90));
            g2.fillRect(hx, hy, CELL_SIZE, CELL_SIZE);

            // Viền vàng cam nổi bật 2px
            g2.setColor(new Color(245, 158, 11));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRect(hx, hy, CELL_SIZE - 1, CELL_SIZE - 1);
        }

        g2.dispose();
    }
}
