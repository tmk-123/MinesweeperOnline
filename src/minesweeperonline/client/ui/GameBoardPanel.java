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

        Font cellFont = new Font("Arial", Font.BOLD, 13);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                final int row = r;
                final int col = c;

                JButton btn = new JButton("");
                btn.setFont(cellFont);
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setFocusPainted(false);
                btn.setBackground(new Color(230, 235, 240));

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

        btn.setEnabled(false);

        if (isMine) {
            btn.setText("💣");
            btn.setBackground(isExploded ? new Color(255, 80, 80) : new Color(255, 180, 180));
        } else {
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
            btn.setText("🚩");
            btn.setForeground(Color.RED);
        } else {
            btn.setText("");
        }
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }
}
