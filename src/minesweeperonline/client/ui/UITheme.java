package minesweeperonline.client.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Quản lý màu sắc, font chữ và giao diện hiện đại (Modern Flat UI) cho MinesweeperOnline.
 * Sử dụng thuần Swing/AWT của Java SE, không yêu cầu thư viện bên ngoài.
 */
public class UITheme {

    // Bảng màu hiện đại
    public static final Color COLOR_PRIMARY = new Color(37, 99, 235);        // #2563EB Royal Blue
    public static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);  // #1D4ED8
    public static final Color COLOR_SECONDARY = new Color(241, 245, 249);    // #F1F5F9 Slate 100
    public static final Color COLOR_SECONDARY_HOVER = new Color(226, 232, 240);
    public static final Color COLOR_SUCCESS = new Color(16, 185, 129);       // #10B981 Emerald
    public static final Color COLOR_SUCCESS_HOVER = new Color(5, 150, 105);
    public static final Color COLOR_WARNING = new Color(217, 119, 6);        // #D97706 Amber
    public static final Color COLOR_WARNING_HOVER = new Color(180, 83, 9);
    public static final Color COLOR_DANGER = new Color(220, 38, 38);         // #DC2626 Red
    public static final Color COLOR_DANGER_HOVER = new Color(185, 28, 28);

    public static final Color BG_MAIN = new Color(248, 250, 252);            // #F8FAFC Slate 50
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BORDER_CARD = new Color(226, 232, 240);        // #E2E8F0
    public static final Color BORDER_FOCUS = new Color(96, 165, 250);        // #60A5FA

    public static final Color TEXT_TITLE = new Color(15, 23, 42);            // #0F172A
    public static final Color TEXT_BODY = new Color(51, 65, 85);             // #334155
    public static final Color TEXT_MUTED = new Color(100, 116, 139);         // #64748B

    // Font chuẩn hệ thống
    public static final Font FONT_LARGE_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);

    /**
     * Định dạng nút bấm phẳng hiện đại với hiệu ứng đổi màu khi rê chuột.
     */
    public static void styleButton(JButton btn, Color bg, Color hoverBg, Color fg) {
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverBg);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(bg);
                }
            }
        });
    }

    /**
     * Nút bấm có viền (Outline button).
     */
    public static void styleOutlineButton(JButton btn, Color borderColor, Color hoverBg, Color fg) {
        btn.setFont(FONT_BOLD);
        btn.setBackground(Color.WHITE);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverBg);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(Color.WHITE);
                }
            }
        });
    }

    /**
     * Định dạng ô nhập văn bản với viền bo mềm và padding bên trong.
     */
    public static void styleTextField(JTextField field) {
        field.setFont(FONT_REGULAR);
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_TITLE);
        field.setCaretColor(COLOR_PRIMARY);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 36));
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_CARD, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
    }

    public static void stylePasswordField(JPasswordField field) {
        field.setFont(FONT_REGULAR);
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_TITLE);
        field.setCaretColor(COLOR_PRIMARY);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 36));
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_CARD, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
    }

    /**
     * Định dạng bảng chuẩn: chiều cao dòng rộng rãi, tiêu đề màu nhã nhặn.
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(FONT_REGULAR);
        table.setSelectionBackground(new Color(224, 242, 254)); // #E0F2FE Sky 100
        table.setSelectionForeground(TEXT_TITLE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(new Color(241, 245, 249)); // #F1F5F9 Slate 100
        table.getTableHeader().setForeground(TEXT_TITLE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CARD));
        table.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Tạo Border dạng Card hiện đại có tiêu đề.
     */
    public static Border createCardBorder(String title) {
        return BorderFactory.createTitledBorder(
                new LineBorder(BORDER_CARD, 1, true),
                title,
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                FONT_TITLE,
                COLOR_PRIMARY
        );
    }

    /**
     * Lấy danh hiệu phân hạng dựa trên điểm số (dạng văn bản thuần, không dùng emoji tránh lỗi font hệ thống).
     */
    public static String getRankTitle(int score) {
        if (score >= 150) {
            return "Đại kiện tướng";
        } else if (score >= 70) {
            return "Cao thủ";
        } else if (score >= 30) {
            return "Chiến binh";
        } else {
            return "Tập sự";
        }
    }

    /**
     * Icon chấm tròn màu vector thuần AWT/Swing để hiển thị trạng thái mà không bị lỗi ô vuông font.
     */
    public static class StatusDotIcon implements Icon {
        private final Color color;
        private final int size;

        public StatusDotIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        public StatusDotIcon(Color color) {
            this(color, 8);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int yPos = y + (getIconHeight() - size) / 2;
            g2.fillOval(x, yPos, size, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size + 5;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }

    /**
     * Lấy màu tương ứng với danh hiệu phân hạng.
     */
    public static Color getRankColor(int score) {
        if (score >= 150) {
            return new Color(14, 116, 144); // #0E7490 Cyan 700
        } else if (score >= 70) {
            return new Color(180, 83, 9);   // #B45309 Amber 700
        } else if (score >= 30) {
            return new Color(71, 85, 105);  // #475569 Slate 600
        } else {
            return new Color(194, 65, 12);  // #C2410C Orange 700
        }
    }
}
