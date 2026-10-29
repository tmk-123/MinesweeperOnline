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
        setContentPane(new MinesweeperBackgroundPanel(MinesweeperBackgroundPanel.Scene.LOBBY, new BorderLayout(0, 0)));

        // Header phía trên
        JPanel headerPanel = new GlassPanel(new BorderLayout(5, 4), UITheme.BG_CARD, 95);
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

        tabbedPane.addTab("  Thể thức & Luật chơi  ", createRulesTab());
        tabbedPane.addTab("  Điểm số & Cấp bậc  ", createRankTab());
        tabbedPane.addTab("  Thao tác & Mẹo chơi  ", createControlsTab());
        UITheme.styleTabbedPane(tabbedPane);

        if (initialTabIndex >= 0 && initialTabIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(initialTabIndex);
        }

        JPanel centerContainer = new GlassPanel(new BorderLayout(), UITheme.BG_MAIN, 64);
        centerContainer.setBorder(new EmptyBorder(10, 14, 10, 14));
        centerContainer.add(tabbedPane, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);

        // Footer với nút Đóng
        JPanel footerPanel = new GlassPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10), UITheme.BG_CARD, 150);
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_CARD));

        JButton btnClose = new JButton("Đã hiểu");
        UITheme.styleButton(btnClose, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, new Color(28, 25, 20));
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
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #D97706;'>Tập sự</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>0 – 49 điểm</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Bậc xếp hạng khởi đầu</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #CBD6DC;'>Bình nhì</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>50 – 199 điểm</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Bắt đầu tích lũy thành tích thi đấu</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #5BAFF4;'>Chiến binh</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>200 – 499 điểm</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Đã có kinh nghiệm đối kháng</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #A484DF;'>Cao thủ</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>500 – 999 điểm</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Nhóm người chơi dẫn đầu</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #E2E8F0;'><b style='color: #F6B842;'>Huyền thoại</b></td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Từ 1000 điểm</td>"
                + "    <td style='border: 1px solid #E2E8F0;'>Cấp bậc cao nhất</td>"
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
        html = html.replace("#1E293B", "#CBD6DC")
                .replace("#1D4ED8", "#F6B842")
                .replace("#CBD5E1", "#344550")
                .replace("#F8FAFC", "#101A22")
                .replace("#FFFFFF", "#14212B")
                .replace("#F1F5F9", "#1D2B35")
                .replace("#E2E8F0", "#2C3B46")
                .replace("#DC2626", "#F55B5E")
                .replace("#059669", "#47C5A4")
                .replace("#D97706", "#F2A137")
                .replace("#2563EB", "#5BAFF4")
                .replace("#7C3AED", "#A484DF")
                .replace("#7F1D1D", "#F55B5E");
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.setBackground(UITheme.BG_PANEL);
        editorPane.setForeground(UITheme.TEXT_BODY);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setFont(UITheme.FONT_REGULAR);
        editorPane.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(editorPane);
        UITheme.styleScrollPane(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(UITheme.BG_PANEL);
        return scroll;
    }
}
