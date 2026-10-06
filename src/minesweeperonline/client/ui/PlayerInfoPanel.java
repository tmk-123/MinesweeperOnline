package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Top header panel of GameFrame showing both players' statistics side by side,
 * including opened safe cells, current flags, and total actions.
 */
public class PlayerInfoPanel extends JPanel {

    private JLabel lblPlayerName;
    private JLabel lblPlayerSafe;
    private JLabel lblPlayerFlags;
    private JLabel lblPlayerActions;

    private JLabel lblOpponentName;
    private JLabel lblOpponentSafe;
    private JLabel lblOpponentFlags;
    private JLabel lblOpponentActions;

    public PlayerInfoPanel(String playerName, String opponentName) {
        initComponents(playerName, opponentName);
    }

    private void initComponents(String playerName, String opponentName) {
        setLayout(new GridLayout(1, 2, 16, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setBackground(new Color(245, 248, 252));

        Font titleFont = new Font("Arial", Font.BOLD, 13);
        Font statFont = new Font("Arial", Font.PLAIN, 12);

        // Player (Self) Panel - Left
        JPanel selfPanel = new JPanel(new GridLayout(4, 1, 2, 2));
        selfPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 210, 240), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        selfPanel.setBackground(Color.WHITE);

        lblPlayerName = new JLabel("Bạn: " + playerName, SwingConstants.LEFT);
        lblPlayerName.setFont(titleFont);
        lblPlayerName.setForeground(new Color(0, 102, 204));

        lblPlayerSafe = new JLabel("Ô an toàn đã mở: 0 / 121", SwingConstants.LEFT);
        lblPlayerSafe.setFont(statFont);

        lblPlayerFlags = new JLabel("Cờ đang cắm: 0 / 23", SwingConstants.LEFT);
        lblPlayerFlags.setFont(statFont);

        lblPlayerActions = new JLabel("Số thao tác: 0", SwingConstants.LEFT);
        lblPlayerActions.setFont(statFont);
        lblPlayerActions.setForeground(new Color(70, 70, 70));

        selfPanel.add(lblPlayerName);
        selfPanel.add(lblPlayerSafe);
        selfPanel.add(lblPlayerFlags);
        selfPanel.add(lblPlayerActions);
        add(selfPanel);

        // Opponent Panel - Right
        JPanel oppPanel = new JPanel(new GridLayout(4, 1, 2, 2));
        oppPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(240, 200, 190), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        oppPanel.setBackground(Color.WHITE);

        lblOpponentName = new JLabel("Đối thủ: " + opponentName, SwingConstants.LEFT);
        lblOpponentName.setFont(titleFont);
        lblOpponentName.setForeground(new Color(204, 51, 0));

        lblOpponentSafe = new JLabel("Ô an toàn đã mở: 0 / 121", SwingConstants.LEFT);
        lblOpponentSafe.setFont(statFont);

        lblOpponentFlags = new JLabel("Cờ đang cắm: 0 / 23", SwingConstants.LEFT);
        lblOpponentFlags.setFont(statFont);

        lblOpponentActions = new JLabel("Số thao tác: 0", SwingConstants.LEFT);
        lblOpponentActions.setFont(statFont);
        lblOpponentActions.setForeground(new Color(70, 70, 70));

        oppPanel.add(lblOpponentName);
        oppPanel.add(lblOpponentSafe);
        oppPanel.add(lblOpponentFlags);
        oppPanel.add(lblOpponentActions);
        add(oppPanel);
    }

    public void updateSelfStats(int safeCells, int flags, int actions) {
        lblPlayerSafe.setText("Ô an toàn đã mở: " + safeCells + " / 121");
        lblPlayerFlags.setText("Cờ đang cắm: " + flags + " / 23");
        lblPlayerActions.setText("Số thao tác: " + actions);
    }

    public void updateSelfStats(int safeCells, int flags) {
        lblPlayerSafe.setText("Ô an toàn đã mở: " + safeCells + " / 121");
        lblPlayerFlags.setText("Cờ đang cắm: " + flags + " / 23");
    }

    public void updateOpponentStats(int safeCells, int flags, int actions) {
        lblOpponentSafe.setText("Ô an toàn đã mở: " + safeCells + " / 121");
        lblOpponentFlags.setText("Cờ đang cắm: " + flags + " / 23");
        lblOpponentActions.setText("Số thao tác: " + actions);
    }

    public void updateOpponentStats(int safeCells, int flags) {
        lblOpponentSafe.setText("Ô an toàn đã mở: " + safeCells + " / 121");
        lblOpponentFlags.setText("Cờ đang cắm: " + flags + " / 23");
    }
}
