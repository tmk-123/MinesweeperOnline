package minesweeperonline.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Hộp thoại Hướng dẫn người chơi (Cẩm nang tân thủ & cao thủ):
 * - Thể thức & Luật thi đấu đối kháng 1v1
 * - Quy tắc tính điểm, tiêu chí Bảng xếp hạng và các mốc Cấp bậc
 * - Thao tác điều khiển chuột và mẹo chơi
 */
public class HelpDialog extends JDialog {

    private final int initialTabIndex;

    public HelpDialog(Frame owner, int initialTabIndex) {
        super(owner, "Hướng Dẫn & Luật Chơi - Minesweeper Online", false);
        this.initialTabIndex = initialTabIndex;

        initComponents();
    }

    public HelpDialog(Frame owner) {
        this(owner, 0);
    }

    private void initComponents() {
        setSize(780, 560);
        setMinimumSize(new Dimension(650, 480));
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

        JLabel lblTitle = new JLabel("CẨM NANG NGƯỜI CHƠI", SwingConstants.LEFT);
        lblTitle.setFont(UITheme.FONT_LARGE_TITLE);
        lblTitle.setForeground(UITheme.COLOR_PRIMARY);

        JLabel lblSubtitle = new JLabel("Luật thi đấu 1v1 đối kháng, quy tắc tính điểm, cấp bậc và mẹo gỡ mìn", SwingConstants.LEFT);
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(UITheme.TEXT_MUTED);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // TabbedPane ở giữa
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("  🎮 Thể thức & Luật chơi  ", createRulesTab());
        tabbedPane.addTab("  🏆 Điểm số & Cấp bậc  ", createRankTab());
        tabbedPane.addTab("  🖱️ Thao tác & Mẹo chơi  ", createControlsTab());

        if (initialTabIndex >= 0 && initialTabIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(initialTabIndex);
        }

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBorder(new EmptyBorder(10, 14, 10, 14));
        centerContainer.setBackground(UITheme.BG_MAIN);
        centerContainer.add(tabbedPane, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);

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

    private JComponent createRulesTab() {
        String html = "<html><body style='font-family: Segoe UI, Arial, sans-serif; font-size: 13px; color: #1E293B; margin: 12px; line-height: 1.5;'>"
                + "<h2 style='color: #1D4ED8; margin-top: 0;'>1. Tổng quan Trận đấu Đối Kháng 1v1</h2>"
                + "<p>Minesweeper Online là tựa game dò mìn thi đấu đối kháng trực tuyến thời gian thực giữa 2 người chơi qua giao thức mạng TCP Socket.</p>"
                + "<table cellpadding='8' cellspacing='0' style='width: 100%; border: 1px solid #CBD5E1; background: #F8FAFC; margin-bottom: 12px;'>"
                + "  <tr><td style='border-bottom: 1px solid #E2E8F0;'><b>Kích thước bàn cờ:</b></td><td style='border-bottom: 1px solid #E2E8F0;'><b>12 hàng x 12 cột</b> (Tổng cộng 144 ô)</td></tr>"
                + "  <tr><td style='border-bottom: 1px solid #E2E8F0;'><b>Số lượng mìn ẩn:</b></td><td style='border-bottom: 1px solid #E2E8F0;'><span style='color: #DC2626; font-weight: bold;'>23 quả mìn</span> được rải ngẫu nhiên</td></tr>"
                + "  <tr><td style='border-bottom: 1px solid #E2E8F0;'><b>Tổng số ô an toàn:</b></td><td style='border-bottom: 1px solid #E2E8F0;'><span style='color: #059669; font-weight: bold;'>121 ô an toàn</span> cần mở để hoàn thành</td></tr>"
                + "  <tr><td><b>Thời gian thi đấu:</b></td><td><b>5 phút (300 giây)</b> đếm ngược</td></tr>"
                + "</table>"

                + "<h2 style='color: #1D4ED8; margin-top: 14px;'>2. Điều kiện Thắng - Thua</h2>"
                + "<ul style='padding-left: 20px;'>"
                + "  <li><b>💥 Mở trúng mìn:</b> Người chơi nào nhấp phải ô có mìn sẽ <span style='color: #DC2626; font-weight: bold;'>Thua ngay lập tức</span>. Đối thủ giành chiến thắng.</li>"
                + "  <li><b>🏁 Mở sạch 121 ô an toàn:</b> Người chơi mở hết toàn bộ 121 ô an toàn trước sẽ giành <span style='color: #059669; font-weight: bold;'>Chiến thắng tuyệt đối</span>.</li>"
                + "  <li><b>⏱️ Hết giờ (5 phút):</b> Hệ thống tự động so sánh số ô an toàn đã mở giữa 2 bên:"
                + "    <ul>"
                + "      <li>Ai mở được <b>nhiều ô an toàn hơn</b> sẽ Thắng.</li>"
                + "      <li>Nếu số ô an toàn bằng nhau: Tính kết quả <b>Hòa</b>.</li>"
                + "    </ul>"
                + "  </li>"
                + "  <li><b>🏃 Rời trận / Mất kết nối:</b> Người chơi thoát game hoặc mất mạng giữa trận sẽ bị xử <span style='color: #DC2626;'>Bỏ cuộc (Forfeit)</span> và bị phạt điểm nặng hơn.</li>"
                + "</ul>"

                + "<h2 style='color: #1D4ED8; margin-top: 14px;'>3. Đồng bộ & Theo dõi đối thủ</h2>"
                + "<p>Mỗi người chơi có bàn cờ riêng, nhưng trong suốt trận đấu, bạn có thể quan sát <b>thanh tiến độ</b> và <b>bàn cờ thu nhỏ</b> của đối thủ ở góc màn hình để nắm bắt tình hình và điều chỉnh nhịp độ thi đấu.</p>"
                + "</body></html>";

        return createHtmlScrollPane(html);
    }

    private JComponent createRankTab() {
        String html = "<html><body style='font-family: Segoe UI, Arial, sans-serif; font-size: 13px; color: #1E293B; margin: 12px; line-height: 1.5;'>"
                + "<h2 style='color: #1D4ED8; margin-top: 0;'>1. Cơ chế Cộng / Trừ Điểm sau mỗi trận</h2>"
                + "<table cellpadding='8' cellspacing='0' style='width: 100%; border: 1px solid #CBD5E1; background: #FFFFFF; margin-bottom: 12px;'>"
                + "  <tr style='background: #F1F5F9; font-weight: bold;'>"
                + "    <td style='border: 1px solid #CBD5E1;'>Kết quả</td>"
                + "    <td style='border: 1px solid #CBD5E1;'>Điểm cộng / trừ</td>"
                + "    <td style='border: 1px solid #CBD5E1;'>Ghi chú</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #059669;'>Chiến thắng (Victory)</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #059669;'>+5 điểm</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Mở sạch ô trước hoặc đối thủ dẫm mìn</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #D97706;'>Hòa trận (Draw)</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #D97706;'>+1 điểm</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Hết 5 phút và số ô mở ngang nhau</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #DC2626;'>Thất bại (Defeat)</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #DC2626;'>-3 điểm</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Dẫm phải mìn hoặc mở ít ô hơn khi hết giờ</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #7F1D1D;'>Bỏ cuộc (Forfeit)</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #7F1D1D;'>-5 điểm</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Chủ động thoát trận hoặc ngắt kết nối</td>"
                + "  </tr>"
                + "</table>"

                + "<h2 style='color: #1D4ED8; margin-top: 14px;'>2. Hệ thống Cấp bậc (Rank Tiers)</h2>"
                + "<table cellpadding='8' cellspacing='0' style='width: 100%; border: 1px solid #CBD5E1; background: #FFFFFF; margin-bottom: 12px;'>"
                + "  <tr style='background: #F1F5F9; font-weight: bold;'>"
                + "    <td style='border: 1px solid #CBD5E1;'>Cấp bậc</td>"
                + "    <td style='border: 1px solid #CBD5E1;'>Mốc điểm số</td>"
                + "    <td style='border: 1px solid #CBD5E1;'>Đặc điểm</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #059669;'>🟢 Tập sự</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>0 – 29 điểm</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Bậc xếp hạng khởi đầu cho người chơi mới</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #2563EB;'>🔵 Chiến binh</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>30 – 69 điểm</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Đã tích lũy kinh nghiệm và có tỉ lệ thắng tốt</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #7C3AED;'>🟣 Cao thủ</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>70 – 149 điểm</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Gỡ mìn tốc độ cao, khả năng phán đoán sắc bén</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #D97706;'>🟡 Đại kiện tướng</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Từ 150 điểm trở lên</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Đỉnh cao kỹ năng, thuộc nhóm cao thủ hàng đầu</td>"
                + "  </tr>"
                + "</table>"
                + "</body></html>";

        return createHtmlScrollPane(html);
    }

    private JComponent createControlsTab() {
        String html = "<html><body style='font-family: Segoe UI, Arial, sans-serif; font-size: 13px; color: #1E293B; margin: 12px; line-height: 1.5;'>"
                + "<h2 style='color: #1D4ED8; margin-top: 0;'>1. Thao tác Chuột trong ván đấu</h2>"
                + "<table cellpadding='8' cellspacing='0' style='width: 100%; border: 1px solid #CBD5E1; background: #F8FAFC; margin-bottom: 12px;'>"
                + "  <tr>"
                + "    <td style='border-bottom: 1px solid #E2E8F0; width: 150px;'><b>🖱️ Chuột Trái (Left Click):</b></td>"
                + "    <td style='border-bottom: 1px solid #E2E8F0;'>"
                + "      <b>Mở ô bí mật</b>.<br>"
                + "      - Nếu là ô an toàn: Hiện số quả mìn xung quanh (từ 1 đến 8).<br>"
                + "      - Nếu là ô trống (số 0): Tự động mở lan nhanh các ô an toàn lân cận.<br>"
                + "      - Nếu mở phải mìn: Kích nổ và kết thúc ván đấu."
                + "    </td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td><b>🚩 Chuột Phải (Right Click):</b></td>"
                + "    <td>"
                + "      <b>Cắm cờ / Gỡ cờ</b>.<br>"
                + "      - Đánh dấu ô mà bạn nghi ngờ có mìn để tránh vô tình bấm nhầm.<br>"
                + "      - Ô đã cắm cờ sẽ bị khóa thao tác mở chuột trái cho đến khi bạn gỡ cờ."
                + "    </td>"
                + "  </tr>"
                + "</table>"

                + "<h2 style='color: #1D4ED8; margin-top: 14px;'>2. Ý nghĩa con số trên ô</h2>"
                + "<ul style='padding-left: 20px;'>"
                + "  <li>Số hiển thị trên mỗi ô đã mở (từ 1 đến 8) đại diện cho <b>chính xác số quả mìn</b> nằm trong 8 ô tiếp giáp xung quanh nó.</li>"
                + "  <li>Ví dụ: Ô số <b>1</b> mà đã tiếp giáp 1 ô cắm cờ chắc chắn là mìn, thì 7 ô còn lại xung quanh đều là ô an toàn 100%!</li>"
                + "</ul>"

                + "<h2 style='color: #1D4ED8; margin-top: 14px;'>3. Chiến thuật & Mẹo thi đấu Đối kháng</h2>"
                + "<ul style='padding-left: 20px;'>"
                + "  <li>🎯 <b>Chính xác quan trọng hơn tốc độ:</b> Chỉ một cú click sai vào mìn là bạn thua ngay và đối thủ thắng ván, do đó đừng bấm ẩu khi chưa chắc chắn.</li>"
                + "  <li>👀 <b>Canh chừng thanh tiến độ đối thủ:</b> Nếu đối phương mở ô rất nhanh, bạn cần đẩy cao nhịp độ; nếu đối phương đang chậm, hãy cẩn thận giữ an toàn để không tự dẫm mìn.</li>"
                + "  <li>🚩 <b>Cắm cờ chiến thuật:</b> Cắm cờ không chỉ tránh bấm nhầm mà còn giúp não bộ loại trừ ô nghi vấn nhanh hơn gấp nhiều lần.</li>"
                + "</ul>"
                + "</body></html>";

        return createHtmlScrollPane(html);
    }

    private JScrollPane createHtmlScrollPane(String html) {
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setOpaque(true);
        editorPane.setBackground(Color.WHITE);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setFont(UITheme.FONT_REGULAR);
        editorPane.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(editorPane);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }
}
