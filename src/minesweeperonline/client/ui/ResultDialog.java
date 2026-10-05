package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import java.awt.*;

/**
 * Result Dialog displayed when the game concludes.
 * Shows outcome, statistics, score delta, and Play Again / Exit buttons.
 * Supports interactive rematch voting without freezing GUI updates.
 */
public class ResultDialog extends JDialog {

    public interface ResultActionListener {
        void onPlayAgain();
        void onExit();
    }

    private final ServerConnection connection;
    private final String gameId;
    private final ResultActionListener listener;

    private JButton btnPlayAgain;
    private JButton btnExit;
    private JLabel lblStatusRematch;

    public ResultDialog(Frame owner, ServerConnection connection, String gameId,
                        String titleOutcome, String reason, int scoreDelta,
                        int openedSafe, int totalActions, int flagsPlaced,
                        ResultActionListener listener) {
        super(owner, "Kết Quả Trận Đấu", false); // Non-modal so network events can update dialog
        this.connection = connection;
        this.gameId = gameId;
        this.listener = listener;

        initComponents(titleOutcome, reason, scoreDelta, openedSafe, totalActions, flagsPlaced);
    }

    private void initComponents(String titleOutcome, String reason, int scoreDelta,
                                int openedSafe, int totalActions, int flagsPlaced) {
        setSize(430, 370);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Outcome Header
        JLabel lblOutcome = new JLabel(titleOutcome, SwingConstants.CENTER);
        lblOutcome.setFont(new Font("Arial", Font.BOLD, 22));

        if (titleOutcome.contains("CHIẾN THẮNG") || titleOutcome.contains("WIN")) {
            lblOutcome.setForeground(new Color(0, 150, 50));
        } else if (titleOutcome.contains("THẤT BẠI") || titleOutcome.contains("LOSE")) {
            lblOutcome.setForeground(new Color(200, 30, 30));
        } else {
            lblOutcome.setForeground(new Color(180, 120, 0));
        }
        mainPanel.add(lblOutcome, BorderLayout.NORTH);

        // Stats details
        JPanel statsPanel = new JPanel(new GridLayout(5, 1, 6, 6));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Thống kê trận đấu"),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblReason = new JLabel("Nguyên nhân: " + reason);
        JLabel lblScore = new JLabel("Điểm cộng/trừ: " + (scoreDelta >= 0 ? "+" : "") + scoreDelta);
        lblScore.setFont(new Font("Arial", Font.BOLD, 12));
        lblScore.setForeground(scoreDelta >= 0 ? new Color(0, 120, 0) : Color.RED);

        JLabel lblSafe = new JLabel("Số ô an toàn đã mở: " + openedSafe + " / 121");
        JLabel lblActions = new JLabel("Tổng số thao tác hợp lệ: " + totalActions);
        JLabel lblFlags = new JLabel("Số cờ đã đặt: " + flagsPlaced);

        statsPanel.add(lblReason);
        statsPanel.add(lblScore);
        statsPanel.add(lblSafe);
        statsPanel.add(lblActions);
        statsPanel.add(lblFlags);
        mainPanel.add(statsPanel, BorderLayout.CENTER);

        // Footer Actions & Status
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));

        lblStatusRematch = new JLabel(" ", SwingConstants.CENTER);
        lblStatusRematch.setFont(new Font("Arial", Font.ITALIC, 12));
        lblStatusRematch.setForeground(new Color(40, 100, 180));
        southPanel.add(lblStatusRematch, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        btnPlayAgain = new JButton("Chơi lại");
        btnPlayAgain.setFont(new Font("Arial", Font.BOLD, 12));
        btnPlayAgain.setBackground(new Color(220, 245, 220));

        btnExit = new JButton("Thoát ra sảnh");

        btnPanel.add(btnPlayAgain);
        btnPanel.add(btnExit);
        southPanel.add(btnPanel, BorderLayout.SOUTH);

        mainPanel.add(southPanel, BorderLayout.SOUTH);
        add(mainPanel);

        btnPlayAgain.addActionListener(e -> {
            btnPlayAgain.setEnabled(false);
            btnPlayAgain.setText("Đang chờ đối thủ...");
            lblStatusRematch.setText("Đã gửi yêu cầu chơi lại. Đang đợi đối thủ xác nhận...");
            connection.sendMessage(JsonProtocol.createPlayAgain(gameId));
            if (listener != null) listener.onPlayAgain();
        });

        btnExit.addActionListener(e -> {
            connection.sendMessage(JsonProtocol.createExit(gameId));
            dispose();
            if (listener != null) listener.onExit();
        });
    }

    public void setOpponentWantsRematch(String oppUsername) {
        SwingUtilities.invokeLater(() -> {
            lblStatusRematch.setText("Đối thủ [" + oppUsername + "] muốn chơi lại!");
            lblStatusRematch.setForeground(new Color(0, 140, 40));
            if (btnPlayAgain.isEnabled()) {
                btnPlayAgain.setBackground(new Color(180, 255, 180));
            }
        });
    }

    public void setOpponentExited() {
        SwingUtilities.invokeLater(() -> {
            lblStatusRematch.setText("Đối thủ đã thoát ra sảnh.");
            lblStatusRematch.setForeground(Color.RED);
            btnPlayAgain.setEnabled(false);
            btnPlayAgain.setText("Đối thủ đã thoát");
        });
    }
}
