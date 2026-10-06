package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 12x12 Minesweeper Interactive Game Board.
 * Left-click to OPEN_CELL, Right-click to PLACE_FLAG / REMOVE_FLAG.
 * Uses high-quality vector icons for flags and mines, avoiding OS font glyph issues.
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

    // Classic number colors
    private static final Color[] NUMBER_COLORS = {
            Color.BLACK,
            new Color(0, 0, 255),       // 1: Blue
            new Color(0, 128, 0),       // 2: Green
            new Color(255, 0, 0),       // 3: Red
            new Color(0, 0, 128),       // 4: Navy
            new Color(128, 0, 0),       // 5: Dark Red
            new Color(0, 128, 128),     // 6: Teal
            Color.BLACK,                // 7: Black
            Color.GRAY                  // 8: Gray
    };

    private static final Icon FLAG_ICON = new FlagIcon();
    private static final Icon MINE_NORMAL_ICON = new MineIcon(false);
    private static final Icon MINE_EXPLODED_ICON = new MineIcon(true);

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
                btn.setBackground(new Color(230, 235, 240));
                btn.setBorder(BorderFactory.createRaisedBevelBorder());

                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (!interactive || opened[row][col]) return;

                        if (SwingUtilities.isLeftMouseButton(e)) {
                            // Left click -> Open Cell
                            if (!flagged[row][col]) {
                                connection.sendMessage(JsonProtocol.createOpenCell(gameId, row, col));
                            }
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            // Right click -> Toggle Flag
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
     * Updates and reveals a cell after receiving confirmation from Server.
     */
    public void revealCell(int r, int c, int adjacentMines, boolean isMine, boolean isExploded) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return;

        JButton btn = buttons[r][c];
        opened[r][c] = true;
        flagged[r][c] = false;

        btn.setRolloverEnabled(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(210, 215, 220), 1));

        if (isMine) {
            Icon mIcon = isExploded ? MINE_EXPLODED_ICON : MINE_NORMAL_ICON;
            btn.setIcon(mIcon);
            btn.setDisabledIcon(mIcon);
            btn.setText("");
            btn.setBackground(isExploded ? new Color(255, 80, 80) : new Color(255, 180, 180));
        } else {
            btn.setIcon(null);
            btn.setDisabledIcon(null);
            btn.setBackground(new Color(245, 245, 245));
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
     * Vector icon for rendering crisp red flag with dark flagpole and base stand.
     */
    private static class FlagIcon implements Icon {
        private final int width = 20;
        private final int height = 20;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Flag pole
            g2.setColor(new Color(50, 50, 50));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 5, y + 2, x + 5, y + 17);

            // Gold finial ball on top of pole
            g2.setColor(new Color(220, 180, 30));
            g2.fillOval(x + 4, y + 1, 3, 3);

            // Pole base stand
            g2.setColor(new Color(50, 50, 50));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 17, x + 10, y + 17);

            // Red flag triangle (pennant pointing right)
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
     * Vector icon for rendering classic Minesweeper naval mine.
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

            // Spikes (cross & diagonals)
            g2.setColor(exploded ? new Color(140, 20, 20) : new Color(40, 40, 40));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx - r - 3, cy, cx + r + 3, cy);
            g2.drawLine(cx, cy - r - 3, cx, cy + r + 3);
            int diag = 6;
            g2.drawLine(cx - diag, cy - diag, cx + diag, cy + diag);
            g2.drawLine(cx - diag, cy + diag, cx + diag, cy - diag);

            // Mine body
            g2.setColor(exploded ? new Color(220, 30, 30) : new Color(30, 30, 30));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            // Specular light highlight
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 3, cy - 3, 2, 2);

            g2.dispose();
        }

        @Override
        public int getIconWidth() { return width; }

        @Override
        public int getIconHeight() { return height; }
    }
}
