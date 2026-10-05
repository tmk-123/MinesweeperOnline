package minesweeperonline.client.ui;

import minesweeperonline.common.model.Player;

import javax.swing.*;
import java.awt.*;

/**
 * Top header panel of GameFrame showing both players' statistics side by side.
 */
public class PlayerInfoPanel extends JPanel {

    private JLabel lblPlayerName;
    private JLabel lblPlayerSafe;
    private JLabel lblPlayerFlags;

    private JLabel lblOpponentName;
    private JLabel lblOpponentSafe;
    private JLabel lblOpponentFlags;

    public PlayerInfoPanel(String playerName, String opponentName) {
        initComponents(playerName, opponentName);
    }

    private void initComponents(String playerName, String opponentName) {
        setLayout(new GridLayout(1, 2, 15, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setBackground(new Color(245, 248, 252));

        // Player (Self) Panel - Left
        JPanel selfPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        selfPanel.setBorder(BorderFactory.createTitledBorder("Bạn"));
        selfPanel.setBackground(Color.WHITE);

        lblPlayerName = new JLabel("Người chơi: " + playerName, SwingConstants.LEFT);
        lblPlayerName.setFont(new Font("Arial", Font.BOLD, 12));
        lblPlayerName.setForeground(new Color(0, 100, 200));

        lblPlayerSafe = new JLabel("Ô an toàn đã mở: 0 / 121", SwingConstants.LEFT);
        lblPlayerFlags = new JLabel("Cờ đang cắm: 0 / 23", SwingConstants.LEFT);

        selfPanel.add(lblPlayerName);
        selfPanel.add(lblPlayerSafe);
        selfPanel.add(lblPlayerFlags);
        add(selfPanel);

        // Opponent Panel - Right
        JPanel oppPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        oppPanel.setBorder(BorderFactory.createTitledBorder("Đối thủ"));
        oppPanel.setBackground(Color.WHITE);

        lblOpponentName = new JLabel("Người chơi: " + opponentName, SwingConstants.LEFT);
        lblOpponentName.setFont(new Font("Arial", Font.BOLD, 12));
        lblOpponentName.setForeground(new Color(180, 50, 0));

        lblOpponentSafe = new JLabel("Ô an toàn đã mở: 0 / 121", SwingConstants.LEFT);
        lblOpponentFlags = new JLabel("Cờ đang cắm: 0 / 23", SwingConstants.LEFT);

        oppPanel.add(lblOpponentName);
        oppPanel.add(lblOpponentSafe);
        oppPanel.add(lblOpponentFlags);
        add(oppPanel);
    }

    public void updateSelfStats(int safeCells, int flags) {
        lblPlayerSafe.setText("Ô an toàn đã mở: " + safeCells + " / 121");
        lblPlayerFlags.setText("Cờ đang cắm: " + flags + " / 23");
    }

    public void updateOpponentStats(int safeCells, int flags) {
        lblOpponentSafe.setText("Ô an toàn đã mở: " + safeCells + " / 121");
        lblOpponentFlags.setText("Cờ đang cắm: " + flags + " / 23");
    }
}
