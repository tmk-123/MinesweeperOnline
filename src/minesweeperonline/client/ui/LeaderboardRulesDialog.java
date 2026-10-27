package minesweeperonline.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Hộp thoại chuyên biệt hiển thị Quy tắc & Tiêu chí xếp hạng của Leaderboard.
 */
public class LeaderboardRulesDialog extends JDialog {

    public LeaderboardRulesDialog(Frame owner) {
        super(owner, "Quy Tắc Xếp Hạng - Minesweeper Online", false);
        initComponents();
    }

    private void initComponents() {
        setSize(580, 440);
        setMinimumSize(new Dimension(500, 380));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(UITheme.BG_MAIN);

        // Header phía trên
        JPanel headerPanel = new JPanel(new BorderLayout(5, 4));
        headerPanel.setBackground(UITheme.BG_CARD);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
                new EmptyBorder(14, 20, 12, 20)
        ));

        JLabel lblTitle = new JLabel("QUY TẮC BẢNG XẾP HẠNG", SwingConstants.LEFT);
        lblTitle.setFont(UITheme.FONT_LARGE_TITLE);
        lblTitle.setForeground(UITheme.COLOR_PRIMARY);

        JLabel lblSubtitle = new JLabel("Các tiêu chí phân định thứ bậc người chơi trên Leaderboard", SwingConstants.LEFT);
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Nội dung chính
        String html = "<html><body style='font-family: Segoe UI, Arial, sans-serif; font-size: 13px; color: #1E293B; margin: 12px; line-height: 1.5;'>"
                + "<h2 style='color: #1D4ED8; margin-top: 0;'>Tiêu chí Xếp hạng (Theo thứ tự ưu tiên)</h2>"
                + "<p>Bảng xếp hạng hiển thị Top 10 cao thủ. Khi có nhiều người chơi bằng điểm, vị trí thứ hạng được xét tuần tự theo các tiêu chí sau:</p>"

                + "<table cellpadding='8' cellspacing='0' style='width: 100%; border: 1px solid #CBD5E1; background: #FFFFFF; margin-bottom: 14px;'>"
                + "  <tr style='background: #F1F5F9; font-weight: bold;'>"
                + "    <td style='border: 1px solid #CBD5E1; width: 35%;'>Mức ưu tiên</td>"
                + "    <td style='border: 1px solid #CBD5E1;'>Tiêu chí phân định</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #2563EB;'>1. Ưu tiên 1</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b>Tổng điểm số (Score):</b> Điểm cao hơn luôn xếp trên.</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #2563EB;'>2. Ưu tiên 2</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b>Tỉ lệ thắng (Win Rate %):</b> Tỉ lệ % cao hơn xếp trên khi bằng điểm.</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #2563EB;'>3. Ưu tiên 3</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b>Số trận thắng (Wins):</b> Người có nhiều trận thắng hơn xếp trên.</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #2563EB;'>4. Ưu tiên 4</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b>Tổng số trận đấu:</b> Người tích cực thi đấu nhiều trận hơn xếp trên.</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #2563EB;'>5. Ưu tiên 5</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b>Tên người chơi (Username):</b> Xếp theo thứ tự chữ cái A-Z nếu tất cả chỉ số bằng nhau.</td>"
                + "  </tr>"
                + "</table>"

                + "<h2 style='color: #1D4ED8; margin-top: 10px;'>Vinh danh Top 3 Cao Thủ</h2>"
                + "<ul style='padding-left: 20px; margin-bottom: 8px;'>"
                + "  <li><b>🥇 Top 1:</b> Quán quân bảng xếp hạng - Huy hiệu và nền Vàng hoàng kim.</li>"
                + "  <li><b>🥈 Top 2:</b> Á quân bảng xếp hạng - Huy hiệu và nền Bạc ánh kim.</li>"
                + "  <li><b>🥉 Top 3:</b> Hạng ba bảng xếp hạng - Huy hiệu và nền Đồng ấm áp.</li>"
                + "</ul>"
                + "</body></html>";

        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setOpaque(true);
        editorPane.setBackground(Color.WHITE);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setFont(UITheme.FONT_REGULAR);
        editorPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // Footer với nút Đóng
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        footerPanel.setBackground(UITheme.BG_CARD);
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        JButton btnClose = new JButton("Đã hiểu");
        UITheme.styleButton(btnClose, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, Color.WHITE);
        btnClose.setPreferredSize(new Dimension(100, 34));
        btnClose.addActionListener(e -> dispose());
        footerPanel.add(btnClose);

        add(footerPanel, BorderLayout.SOUTH);
    }
}
