package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog hiện ra khi có người chơi khác thách đấu người dùng này.
 * Có timer đếm ngược 30 giây, hết giờ sẽ tự động từ chối.
 */
public class ChallengeDialog extends JDialog {

    private final ServerConnection connection;
    private final int challengerUserId;
    private final String challengerUsername;
    private Timer countdownTimer;
    private int remainingSeconds = 30;
    private JLabel lblTimer;
    private boolean responded = false;

    public ChallengeDialog(Frame owner, ServerConnection connection, int challengerUserId, String challengerUsername) {
        super(owner, "Lời mời thách đấu", true);
        this.connection = connection;
        this.challengerUserId = challengerUserId;
        this.challengerUsername = challengerUsername;
        initComponents();
        startCountdown();
    }

    private void initComponents() {
        setSize(500, 340);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(0, 0));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(UITheme.BG_MAIN);

        JPanel content = new MinesweeperBackgroundPanel(MinesweeperBackgroundPanel.Scene.DUEL, new BorderLayout(8, 0));
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));

        JLabel lblTitle = new JLabel("LỜI MỜI THÁCH ĐẤU", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_LARGE_TITLE);
        lblTitle.setForeground(UITheme.COLOR_PRIMARY);
        content.add(lblTitle, BorderLayout.NORTH);

        JPanel matchup = new JPanel(new GridLayout(1, 3, 12, 0));
        matchup.setOpaque(false);
        matchup.add(createPlayerBadge(challengerUsername, new Color(78, 164, 231)));
        JLabel vs = new JLabel("VS", SwingConstants.CENTER);
        vs.setFont(new Font("Segoe UI", Font.BOLD, 24));
        vs.setForeground(UITheme.COLOR_PRIMARY);
        matchup.add(vs);
        matchup.add(createPlayerBadge("Bạn", UITheme.COLOR_SUCCESS));
        JPanel center = new JPanel(new BorderLayout(6, 8));
        center.setOpaque(false);
        center.add(matchup, BorderLayout.CENTER);
        lblTimer = new JLabel("" + remainingSeconds + " giây còn lại", SwingConstants.CENTER);
        lblTimer.setForeground(UITheme.COLOR_PRIMARY);
        lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 18));
        center.add(lblTimer, BorderLayout.SOUTH);
        content.add(center, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        JPanel buttonPanel = new GlassPanel(new FlowLayout(FlowLayout.CENTER, 20, 12), UITheme.BG_CARD, 132);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        JButton btnAccept = new JButton("Đồng ý thách đấu");
        UITheme.styleButton(btnAccept, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, new Color(28, 25, 20));

        JButton btnReject = new JButton("Từ chối");
        UITheme.styleButton(btnReject, new Color(61, 35, 40), new Color(91, 42, 48), new Color(255, 216, 218));

        buttonPanel.add(btnAccept);
        buttonPanel.add(btnReject);
        content.add(buttonPanel, BorderLayout.SOUTH);

        btnAccept.addActionListener(e -> acceptChallenge());
        btnReject.addActionListener(e -> rejectChallenge());
    }

    private JComponent createPlayerBadge(String name, Color accent) {
        JPanel badge = new JPanel(new BorderLayout(4, 8));
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        JLabel avatar = new JLabel(GameUiIcons.playerAvatarIcon(name, accent, 72, false));
        avatar.setPreferredSize(new Dimension(72, 72));
        avatar.setToolTipText(name);
        JLabel playerName = new JLabel(name, SwingConstants.CENTER);
        playerName.setForeground(UITheme.TEXT_TITLE);
        playerName.setFont(UITheme.FONT_BOLD);
        badge.add(avatar, BorderLayout.CENTER);
        badge.add(playerName, BorderLayout.SOUTH);
        return badge;
    }

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            lblTimer.setText(remainingSeconds + " giây còn lại");
            if (remainingSeconds <= 0) {
                rejectChallenge();
            }
        });
        countdownTimer.start();
    }

    private synchronized void acceptChallenge() {
        if (responded) return;
        responded = true;
        if (countdownTimer != null) countdownTimer.stop();
        connection.sendMessage(JsonProtocol.createAccept(challengerUserId));
        dispose();
    }

    private synchronized void rejectChallenge() {
        if (responded) return;
        responded = true;
        if (countdownTimer != null) countdownTimer.stop();
        connection.sendMessage(JsonProtocol.createReject(challengerUserId));
        dispose();
    }
}
