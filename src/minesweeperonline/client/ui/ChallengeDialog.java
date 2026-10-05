package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog prompt displayed when another player challenges this user.
 * Features a 30-second countdown timer before automatic rejection.
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
        setSize(360, 200);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel(new GridLayout(3, 1, 5, 5));
        content.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JLabel lblMsg = new JLabel("Người chơi [" + challengerUsername + "] thách đấu bạn!", SwingConstants.CENTER);
        lblMsg.setFont(new Font("Arial", Font.BOLD, 14));
        content.add(lblMsg);

        JLabel lblSub = new JLabel("Bạn có chấp nhận thi đấu đối kháng không?", SwingConstants.CENTER);
        content.add(lblSub);

        lblTimer = new JLabel("Thời gian phản hồi còn lại: " + remainingSeconds + "s", SwingConstants.CENTER);
        lblTimer.setForeground(Color.RED);
        lblTimer.setFont(new Font("Arial", Font.BOLD, 12));
        content.add(lblTimer);

        add(content, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnAccept = new JButton("Chấp nhận");
        btnAccept.setFont(new Font("Arial", Font.BOLD, 12));
        btnAccept.setBackground(new Color(200, 245, 200));

        JButton btnReject = new JButton("Từ chối");
        btnReject.setBackground(new Color(255, 220, 220));

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
