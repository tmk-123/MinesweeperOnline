package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

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
        setSize(650, 420);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(UITheme.BG_MAIN);

        Color outcomeColor = getOutcomeColor(titleOutcome);
        JPanel mainPanel = new MinesweeperBackgroundPanel(MinesweeperBackgroundPanel.Scene.RESULT, new BorderLayout(12, 14));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1),
                BorderFactory.createEmptyBorder(20, 24, 18, 24)));

        JPanel outcomePanel = new GlassPanel(new BorderLayout(14, 0), UITheme.BG_CARD, 190);
        outcomePanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        String normalizedOutcome = titleOutcome == null ? "" : titleOutcome.toUpperCase();
        boolean victory = normalizedOutcome.contains("THẮNG") || normalizedOutcome.contains("WIN");
        boolean draw = normalizedOutcome.contains("HÒA") || normalizedOutcome.contains("DRAW");
        JLabel icon = new JLabel(new OutcomeIcon(outcomeColor, victory, draw));
        icon.setPreferredSize(new Dimension(60, 60));
        outcomePanel.add(icon, BorderLayout.WEST);

        JPanel titleStack = new JPanel(new GridLayout(2, 1, 0, 4));
        titleStack.setOpaque(false);
        JLabel lblOutcome = new JLabel(titleOutcome);
        lblOutcome.setFont(UITheme.FONT_LARGE_TITLE);
        lblOutcome.setForeground(outcomeColor);
        JLabel lblReason = new JLabel(reason);
        lblReason.setFont(UITheme.FONT_REGULAR);
        lblReason.setForeground(UITheme.TEXT_BODY);
        titleStack.add(lblOutcome);
        titleStack.add(lblReason);
        outcomePanel.add(titleStack, BorderLayout.CENTER);
        mainPanel.add(outcomePanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        JLabel lblStatsTitle = new JLabel("THỐNG KÊ TRẬN ĐẤU");
        lblStatsTitle.setFont(UITheme.FONT_SMALL_BOLD);
        lblStatsTitle.setForeground(UITheme.COLOR_PRIMARY);
        centerPanel.add(lblStatsTitle, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        statsPanel.setOpaque(false);
        statsPanel.add(createStatCard("ĐIỂM SỐ", (scoreDelta >= 0 ? "+" : "") + scoreDelta,
                "điểm", scoreDelta >= 0 ? UITheme.COLOR_SUCCESS : UITheme.COLOR_DANGER));
        statsPanel.add(createStatCard("Ô AN TOÀN", openedSafe + " / 121", "đã mở", UITheme.COLOR_BLUE));
        statsPanel.add(createStatCard("THAO TÁC", String.valueOf(totalActions),
                flagsPlaced + " cờ đã đặt", UITheme.TEXT_TITLE));
        centerPanel.add(statsPanel, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Phần chân: nút thao tác & trạng thái
        JPanel southPanel = new JPanel(new BorderLayout(8, 8));
        southPanel.setOpaque(false);
        southPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        lblStatusRematch = new JLabel(" ", SwingConstants.CENTER);
        lblStatusRematch.setFont(UITheme.FONT_SMALL);
        lblStatusRematch.setForeground(UITheme.COLOR_PRIMARY);
        southPanel.add(lblStatusRematch, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 5));
        btnPanel.setOpaque(false);

        btnPlayAgain = new JButton("Chơi lại ván mới");
        UITheme.styleButton(btnPlayAgain, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, new Color(28, 25, 20));
        btnPlayAgain.setPreferredSize(new Dimension(180, 40));

        btnExit = new JButton("Rời phòng");
        UITheme.styleOutlineButton(btnExit, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
        btnExit.setPreferredSize(new Dimension(160, 40));

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

    private JPanel createStatCard(String title, String value, String note, Color valueColor) {
        JPanel card = new GlassPanel(new GridLayout(3, 1, 0, 3), UITheme.BG_PANEL, 168);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_SMALL_BOLD);
        lblTitle.setForeground(UITheme.TEXT_MUTED);
        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 23));
        lblValue.setForeground(valueColor);
        JLabel lblNote = new JLabel(note, SwingConstants.CENTER);
        lblNote.setFont(UITheme.FONT_SMALL);
        lblNote.setForeground(UITheme.TEXT_BODY);

        card.add(lblTitle);
        card.add(lblValue);
        card.add(lblNote);
        return card;
    }

    private Color getOutcomeColor(String titleOutcome) {
        if (titleOutcome.contains("CHIẾN THẮNG") || titleOutcome.contains("WIN")) {
            return UITheme.COLOR_SUCCESS;
        }
        if (titleOutcome.contains("THẤT BẠI") || titleOutcome.contains("LOSE")) {
            return UITheme.COLOR_DANGER;
        }
        return UITheme.COLOR_WARNING;
    }

    private static class OutcomeIcon implements Icon {
        private final Color color;
        private final boolean victory;
        private final boolean draw;

        private OutcomeIcon(Color color, boolean victory, boolean draw) {
            this.color = color;
            this.victory = victory;
            this.draw = draw;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 38));
            g2.fillOval(x + 1, y + 1, 56, 56);
            if (victory) {
                paintTrophy(g2, x, y);
            } else if (draw) {
                paintCrossedFlags(g2, x, y);
            } else {
                paintBurstMine(g2, x, y);
            }
            g2.dispose();
        }

        private void paintTrophy(Graphics2D g2, int x, int y) {
            Color gold = UITheme.COLOR_PRIMARY;
            g2.setColor(new Color(84, 56, 20));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(x + 8, y + 12, 19, 23, 95, 175);
            g2.drawArc(x + 33, y + 12, 19, 23, -90, 175);

            Polygon cup = new Polygon(
                    new int[]{x + 15, x + 45, x + 41, x + 36, x + 24, x + 19},
                    new int[]{y + 10, y + 10, y + 29, y + 36, y + 36, y + 29}, 6);
            g2.setPaint(new GradientPaint(x + 17, y + 10, gold.brighter(), x + 42, y + 37, gold.darker()));
            g2.fillPolygon(cup);
            g2.setColor(new Color(96, 60, 19));
            g2.setStroke(new BasicStroke(2f));
            g2.drawPolygon(cup);
            g2.setColor(new Color(255, 238, 162));
            g2.fillOval(x + 22, y + 16, 7, 4);

            g2.setColor(gold.darker());
            g2.fillRoundRect(x + 27, y + 35, 7, 10, 3, 3);
            g2.fillRoundRect(x + 20, y + 44, 21, 5, 2, 2);
            g2.fillRoundRect(x + 17, y + 49, 27, 5, 2, 2);
            g2.setColor(new Color(255, 225, 128));
            g2.fillOval(x + 28, y + 21, 5, 5);
        }

        private void paintCrossedFlags(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(28, 42, 51));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 17, y + 43, x + 43, y + 17);
            g2.drawLine(x + 43, y + 43, x + 17, y + 17);
            g2.setColor(color);
            Polygon left = new Polygon(new int[]{x + 15, x + 27, x + 15}, new int[]{y + 12, y + 17, y + 23}, 3);
            Polygon right = new Polygon(new int[]{x + 49, x + 37, x + 49}, new int[]{y + 12, y + 17, y + 23}, 3);
            g2.fillPolygon(left);
            g2.fillPolygon(right);
            g2.setColor(new Color(245, 239, 218));
            g2.fillOval(x + 26, y + 26, 9, 9);
        }

        private void paintBurstMine(Graphics2D g2, int x, int y) {
            int cx = x + 30;
            int cy = y + 30;
            Path2D burst = new Path2D.Double();
            for (int i = 0; i < 16; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 8;
                double radius = (i % 2 == 0) ? 22 : 13;
                double px = cx + Math.cos(angle) * radius;
                double py = cy + Math.sin(angle) * radius;
                if (i == 0) burst.moveTo(px, py); else burst.lineTo(px, py);
            }
            burst.closePath();
            g2.setPaint(new GradientPaint(cx, y + 8, new Color(255, 218, 97), cx, y + 51, color.darker()));
            g2.fill(burst);
            g2.setColor(new Color(40, 29, 30));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx - 14, cy, cx + 14, cy);
            g2.drawLine(cx, cy - 14, cx, cy + 14);
            g2.drawLine(cx - 10, cy - 10, cx + 10, cy + 10);
            g2.drawLine(cx - 10, cy + 10, cx + 10, cy - 10);
            g2.setColor(new Color(30, 29, 30));
            g2.fillOval(cx - 8, cy - 8, 16, 16);
            g2.setColor(new Color(255, 241, 181));
            g2.fillOval(cx - 4, cy - 4, 3, 3);
        }

        @Override public int getIconWidth() { return 60; }
        @Override public int getIconHeight() { return 60; }
    }

    public void setOpponentWantsRematch(String oppUsername) {
        SwingUtilities.invokeLater(() -> {
            lblStatusRematch.setText("Đối thủ [" + oppUsername + "] muốn chơi lại!");
            lblStatusRematch.setForeground(UITheme.COLOR_SUCCESS);
            if (btnPlayAgain.isEnabled()) {
                btnPlayAgain.setBackground(UITheme.COLOR_SUCCESS);
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
