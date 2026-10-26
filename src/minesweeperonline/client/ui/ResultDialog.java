package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog kết quả hiển thị khi ván chơi kết thúc.
 * Hiển thị kết quả, thống kê, điểm cộng/trừ và các nút Chơi lại / Thoát.
 * Hỗ trợ bình chọn chơi lại (rematch) mà không làm đơ việc cập nhật giao diện.
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
        super(owner, "Kết Quả Trận Đấu", false); // Non-modal để các sự kiện mạng vẫn cập nhật được dialog
        this.connection = connection;
        this.gameId = gameId;
        this.listener = listener;

        initComponents(titleOutcome, reason, scoreDelta, openedSafe, totalActions, flagsPlaced);
    }

    private void initComponents(String titleOutcome, String reason, int scoreDelta,
                                int openedSafe, int totalActions, int flagsPlaced) {
        setSize(450, 390);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(UITheme.BG_MAIN);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        mainPanel.setBackground(UITheme.BG_CARD);

        // Tiêu đề kết quả
        JLabel lblOutcome = new JLabel(titleOutcome, SwingConstants.CENTER);
        lblOutcome.setFont(UITheme.FONT_LARGE_TITLE);

        if (titleOutcome.contains("CHIẾN THẮNG") || titleOutcome.contains("WIN")) {
            lblOutcome.setForeground(UITheme.COLOR_SUCCESS);
        } else if (titleOutcome.contains("THẤT BẠI") || titleOutcome.contains("LOSE")) {
            lblOutcome.setForeground(UITheme.COLOR_DANGER);
        } else {
            lblOutcome.setForeground(UITheme.COLOR_WARNING);
        }
        mainPanel.add(lblOutcome, BorderLayout.NORTH);

        // Chi tiết thống kê
        JPanel statsPanel = new JPanel(new GridLayout(5, 1, 6, 8));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1, true),
                        "Thống kê trận đấu",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        UITheme.FONT_BOLD,
                        UITheme.COLOR_PRIMARY
                ),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        statsPanel.setBackground(Color.WHITE);

        JLabel lblReason = new JLabel("Nguyên nhân: " + reason);
        lblReason.setFont(UITheme.FONT_REGULAR);

        JLabel lblScore = new JLabel("Điểm biến động: " + (scoreDelta >= 0 ? "+" : "") + scoreDelta);
        lblScore.setFont(UITheme.FONT_BOLD);
        lblScore.setForeground(scoreDelta >= 0 ? UITheme.COLOR_SUCCESS : UITheme.COLOR_DANGER);

        JLabel lblSafe = new JLabel("Ô an toàn đã mở: " + openedSafe + " / 121");
        lblSafe.setFont(UITheme.FONT_REGULAR);

        JLabel lblActions = new JLabel("Tổng số thao tác: " + totalActions);
        lblActions.setFont(UITheme.FONT_REGULAR);

        JLabel lblFlags = new JLabel("Số cờ đã đặt: " + flagsPlaced);
        lblFlags.setFont(UITheme.FONT_REGULAR);

        statsPanel.add(lblReason);
        statsPanel.add(lblScore);
        statsPanel.add(lblSafe);
        statsPanel.add(lblActions);
        statsPanel.add(lblFlags);
        mainPanel.add(statsPanel, BorderLayout.CENTER);

        // Phần chân: nút thao tác & trạng thái
        JPanel southPanel = new JPanel(new BorderLayout(8, 8));
        southPanel.setOpaque(false);

        lblStatusRematch = new JLabel(" ", SwingConstants.CENTER);
        lblStatusRematch.setFont(UITheme.FONT_SMALL);
        lblStatusRematch.setForeground(UITheme.COLOR_PRIMARY);
        southPanel.add(lblStatusRematch, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 5));
        btnPanel.setOpaque(false);

        btnPlayAgain = new JButton("Chơi lại ván mới");
        UITheme.styleButton(btnPlayAgain, UITheme.COLOR_SUCCESS, UITheme.COLOR_SUCCESS_HOVER, Color.WHITE);

        btnExit = new JButton("Thoát ra sảnh");
        UITheme.styleOutlineButton(btnExit, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY, UITheme.TEXT_TITLE);

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
