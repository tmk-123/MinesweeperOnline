package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Panel tiêu đề phía trên của GameFrame, hiển thị song song thống kê của hai người chơi,
 * gồm số ô an toàn đã mở, số cờ đang cắm và tổng số thao tác với giao diện thẻ hiện đại.
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
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        setBackground(UITheme.BG_MAIN);

        // Panel của người chơi (bản thân) - bên trái
        JPanel selfPanel = new JPanel(new GridLayout(4, 1, 2, 3));
        selfPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true), // Blue 200
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        selfPanel.setBackground(Color.WHITE);

        lblPlayerName = new JLabel("Bạn: " + playerName, SwingConstants.LEFT);
        lblPlayerName.setFont(UITheme.FONT_TITLE);
        lblPlayerName.setForeground(UITheme.COLOR_PRIMARY);

        lblPlayerSafe = new JLabel("Ô an toàn: 0 / 121", SwingConstants.LEFT);
        lblPlayerSafe.setFont(UITheme.FONT_REGULAR);
        lblPlayerSafe.setForeground(UITheme.TEXT_TITLE);

        lblPlayerFlags = new JLabel("Cờ đang cắm: 0 / 23", SwingConstants.LEFT);
        lblPlayerFlags.setFont(UITheme.FONT_REGULAR);
        lblPlayerFlags.setForeground(UITheme.TEXT_TITLE);

        lblPlayerActions = new JLabel("Số thao tác: 0", SwingConstants.LEFT);
        lblPlayerActions.setFont(UITheme.FONT_SMALL);
        lblPlayerActions.setForeground(UITheme.TEXT_MUTED);

        selfPanel.add(lblPlayerName);
        selfPanel.add(lblPlayerSafe);
        selfPanel.add(lblPlayerFlags);
        selfPanel.add(lblPlayerActions);
        add(selfPanel);

        // Panel của đối thủ - bên phải
        JPanel oppPanel = new JPanel(new GridLayout(4, 1, 2, 3));
        oppPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202), 1, true), // Red 200
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        oppPanel.setBackground(Color.WHITE);

        lblOpponentName = new JLabel("Đối thủ: " + opponentName, SwingConstants.LEFT);
        lblOpponentName.setFont(UITheme.FONT_TITLE);
        lblOpponentName.setForeground(UITheme.COLOR_DANGER);

        lblOpponentSafe = new JLabel("Ô an toàn: 0 / 121", SwingConstants.LEFT);
        lblOpponentSafe.setFont(UITheme.FONT_REGULAR);
        lblOpponentSafe.setForeground(UITheme.TEXT_TITLE);

        lblOpponentFlags = new JLabel("Cờ đang cắm: 0 / 23", SwingConstants.LEFT);
        lblOpponentFlags.setFont(UITheme.FONT_REGULAR);
        lblOpponentFlags.setForeground(UITheme.TEXT_TITLE);

        lblOpponentActions = new JLabel("Số thao tác: 0", SwingConstants.LEFT);
        lblOpponentActions.setFont(UITheme.FONT_SMALL);
        lblOpponentActions.setForeground(UITheme.TEXT_MUTED);

        oppPanel.add(lblOpponentName);
        oppPanel.add(lblOpponentSafe);
        oppPanel.add(lblOpponentFlags);
        oppPanel.add(lblOpponentActions);
        add(oppPanel);
    }

    public void updateSelfStats(int safeCells, int flags, int actions) {
        lblPlayerSafe.setText("Ô an toàn: " + safeCells + " / 121");
        lblPlayerFlags.setText("Cờ đang cắm: " + flags + " / 23");
        lblPlayerActions.setText("Số thao tác: " + actions);
    }

    public void updateSelfStats(int safeCells, int flags) {
        lblPlayerSafe.setText("Ô an toàn: " + safeCells + " / 121");
        lblPlayerFlags.setText("Cờ đang cắm: " + flags + " / 23");
    }

    public void updateOpponentStats(int safeCells, int flags, int actions) {
        lblOpponentSafe.setText("Ô an toàn: " + safeCells + " / 121");
        lblOpponentFlags.setText("Cờ đang cắm: " + flags + " / 23");
        lblOpponentActions.setText("Số thao tác: " + actions);
    }

    public void updateOpponentStats(int safeCells, int flags) {
        lblOpponentSafe.setText("Ô an toàn: " + safeCells + " / 121");
        lblOpponentFlags.setText("Cờ đang cắm: " + flags + " / 23");
    }
}
