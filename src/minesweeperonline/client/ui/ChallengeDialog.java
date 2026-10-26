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
        setSize(390, 220);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(UITheme.BG_MAIN);

        JPanel content = new JPanel(new GridLayout(3, 1, 6, 8));
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 10, 20));
        content.setBackground(UITheme.BG_CARD);

        JLabel lblMsg = new JLabel("Người chơi [" + challengerUsername + "] thách đấu bạn!", SwingConstants.CENTER);
        lblMsg.setFont(UITheme.FONT_TITLE);
        lblMsg.setForeground(UITheme.COLOR_PRIMARY);
        content.add(lblMsg);

        JLabel lblSub = new JLabel("Bạn có chấp nhận tham gia thi đấu đối kháng không?", SwingConstants.CENTER);
        lblSub.setFont(UITheme.FONT_REGULAR);
        lblSub.setForeground(UITheme.TEXT_BODY);
        content.add(lblSub);

        lblTimer = new JLabel("Thời gian phản hồi còn lại: " + remainingSeconds + "s", SwingConstants.CENTER);
        lblTimer.setForeground(UITheme.COLOR_DANGER);
        lblTimer.setFont(UITheme.FONT_SMALL_BOLD);
        content.add(lblTimer);

        add(content, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        buttonPanel.setBackground(UITheme.BG_CARD);

        JButton btnAccept = new JButton("Chấp nhận đấu");
        UITheme.styleButton(btnAccept, UITheme.COLOR_SUCCESS, UITheme.COLOR_SUCCESS_HOVER, Color.WHITE);

        JButton btnReject = new JButton("Từ chối");
        UITheme.styleButton(btnReject, new Color(254, 226, 226), new Color(254, 202, 202), UITheme.COLOR_DANGER);

        buttonPanel.add(btnAccept);
        buttonPanel.add(btnReject);
        add(buttonPanel, BorderLayout.SOUTH);

        btnAccept.addActionListener(e -> acceptChallenge());
        btnReject.addActionListener(e -> rejectChallenge());
    }

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            lblTimer.setText("Thời gian phản hồi còn lại: " + remainingSeconds + "s");
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
