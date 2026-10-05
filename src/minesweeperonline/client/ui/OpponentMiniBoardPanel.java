package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Miniature 12x12 board panel showing the opponent's game progression in real-time.
 * Displays opened cells and flagged cells without disclosing mine locations.
 */
public class OpponentMiniBoardPanel extends JPanel {

    private static final int ROWS = 12;
    private static final int COLS = 12;
    private static final int CELL_SIZE = 18;

    private static final byte STATE_HIDDEN = 0;
    private static final byte STATE_OPENED = 1;
    private static final byte STATE_FLAGGED = 2;

    private final byte[][] cellStates = new byte[ROWS][COLS];

    public OpponentMiniBoardPanel() {
        setPreferredSize(new Dimension(COLS * CELL_SIZE + 24, ROWS * CELL_SIZE + 45));
        setBorder(BorderFactory.createTitledBorder("Bàn đối thủ"));
        setBackground(new Color(245, 247, 250));
    }

    public void resetBoard() {
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
            repaint();
        }
    }

    public void setCellFlagged(int r, int c, boolean flagged) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
            if (cellStates[r][c] != STATE_OPENED) {
                cellStates[r][c] = flagged ? STATE_FLAGGED : STATE_HIDDEN;
                repaint();
            }
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
                    // Safe revealed cell
                    g2.setColor(new Color(220, 224, 230));
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2.setColor(new Color(180, 190, 200));
                    g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                    g2.setColor(new Color(100, 160, 220));
                    g2.fillOval(x + CELL_SIZE / 2 - 2, y + CELL_SIZE / 2 - 2, 4, 4);

                } else if (state == STATE_FLAGGED) {
                    // Flagged cell
                    g2.setColor(new Color(255, 230, 230));
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2.setColor(new Color(220, 140, 140));
                    g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                    // Mini red flag
                    g2.setColor(new Color(220, 30, 30));
                    int[] xPoints = {x + 5, x + 13, x + 5};
                    int[] yPoints = {y + 4, y + 8, y + 12};
                    g2.fillPolygon(xPoints, yPoints, 3);
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawLine(x + 5, y + 4, x + 5, y + 15);

                } else {
                    // Hidden cell
                    g2.setColor(new Color(195, 205, 218));
                    g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2.setColor(new Color(160, 175, 192));
                    g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                }
            }
        }
        g2.dispose();
    }
}
