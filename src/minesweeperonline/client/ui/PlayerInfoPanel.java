package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;

/** Match header showing both players, live progress, and the shared clock. */
public class PlayerInfoPanel extends JPanel {
    private final JLabel lblPlayerSafe;
    private final JLabel lblPlayerFlags;
    private final JLabel lblPlayerActions;
    private final JLabel lblOpponentSafe;
    private final JLabel lblOpponentFlags;
    private final JLabel lblOpponentActions;
    private final JLabel lblMatchTimer;
    private final JLabel lblMatchStatus;

    public PlayerInfoPanel(String playerName, String opponentName) {
        setLayout(new GridLayout(1, 3, 12, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
        setOpaque(false);

        JPanel self = createPlayerCard(playerName, new Color(78, 164, 231), "YOU");
        lblPlayerSafe = (JLabel) self.getClientProperty("safe");
        lblPlayerFlags = (JLabel) self.getClientProperty("flags");
        lblPlayerActions = (JLabel) self.getClientProperty("actions");
        add(self);

        JPanel center = new GlassPanel(new BorderLayout(), UITheme.BG_CARD, 164);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD), BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        JLabel caption = new JLabel("THỜI GIAN CÒN LẠI", SwingConstants.CENTER);
        caption.setFont(UITheme.FONT_SMALL_BOLD);
        caption.setForeground(UITheme.TEXT_MUTED);
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblMatchTimer = new JLabel("05:00", SwingConstants.CENTER);
        lblMatchTimer.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblMatchTimer.setForeground(UITheme.COLOR_PRIMARY);
        lblMatchTimer.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblMatchStatus = new JLabel("SẮP BẮT ĐẦU", SwingConstants.CENTER);
        lblMatchStatus.setFont(UITheme.FONT_SMALL_BOLD);
        lblMatchStatus.setForeground(UITheme.COLOR_WARNING);
        lblMatchStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(Box.createVerticalGlue());
        center.add(caption);
        center.add(Box.createVerticalStrut(2));
        center.add(lblMatchTimer);
        center.add(Box.createVerticalStrut(4));
        center.add(lblMatchStatus);
        center.add(Box.createVerticalGlue());
        add(center);

        JPanel opponent = createPlayerCard(opponentName, UITheme.COLOR_DANGER, "OPPONENT");
        lblOpponentSafe = (JLabel) opponent.getClientProperty("safe");
        lblOpponentFlags = (JLabel) opponent.getClientProperty("flags");
        lblOpponentActions = (JLabel) opponent.getClientProperty("actions");
        add(opponent);
    }

    private JPanel createPlayerCard(String name, Color accent, String role) {
        JPanel card = new GlassPanel(new BorderLayout(12, 0), UITheme.BG_CARD, 164);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, 1, true), BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel avatar = new JLabel(GameUiIcons.playerAvatarIcon(name, accent, 56));
        avatar.setPreferredSize(new Dimension(56, 56));
        avatar.setToolTipText(name);
        card.add(avatar, BorderLayout.WEST);

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        JLabel player = new JLabel(name + "  ·  " + role);
        player.setFont(UITheme.FONT_TITLE);
        player.setForeground(accent);
        JLabel safe = statLabel("Ô an toàn: 0 / 121");
        JLabel flags = statLabel("Cờ đang cắm: 0 / 23");
        JLabel actions = statLabel("Số thao tác: 0");
        details.add(player);
        details.add(Box.createVerticalStrut(3));
        details.add(safe);
        details.add(flags);
        details.add(actions);
        card.add(details, BorderLayout.CENTER);
        card.putClientProperty("safe", safe);
        card.putClientProperty("flags", flags);
        card.putClientProperty("actions", actions);
        return card;
    }

    private JLabel statLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_BODY);
        return label;
    }

    public void updateMatchTimer(String text) {
        lblMatchTimer.setText(text.replace("Thời gian: ", ""));
        if (text.contains("00:0")) lblMatchTimer.setForeground(UITheme.COLOR_DANGER);
        else lblMatchTimer.setForeground(UITheme.COLOR_PRIMARY);
    }

    public void updateMatchStatus(String text, Color color) {
        String value = text.replace("Trạng thái: ", "");
        value = value.replace("ĐANG THI ĐẤU", "ĐANG THI ĐẤU");
        lblMatchStatus.setText(value);
        lblMatchStatus.setForeground(color);
    }

    public void updateSelfStats(int safeCells, int flags, int actions) {
        lblPlayerSafe.setText("Ô an toàn: " + safeCells + " / 121");
        lblPlayerFlags.setText("Cờ đang cắm: " + flags + " / 23");
        lblPlayerActions.setText("Số thao tác: " + actions);
    }
    public void updateSelfStats(int safeCells, int flags) { updateSelfStats(safeCells, flags, 0); }
    public void updateOpponentStats(int safeCells, int flags, int actions) {
        lblOpponentSafe.setText("Ô an toàn: " + safeCells + " / 121");
        lblOpponentFlags.setText("Cờ đang cắm: " + flags + " / 23");
        lblOpponentActions.setText("Số thao tác: " + actions);
    }
    public void updateOpponentStats(int safeCells, int flags) { updateOpponentStats(safeCells, flags, 0); }
}
