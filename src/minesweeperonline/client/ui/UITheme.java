package minesweeperonline.client.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;

/** Giao diện màu thép tối và vàng dùng chung cho ứng dụng Minesweeper phía người chơi. */
public final class UITheme {
    /** Chặn khởi tạo đối tượng: lớp này chỉ cung cấp hằng số và các hàm định dạng tĩnh. */
    private UITheme() { }

    public static final Color COLOR_PRIMARY = new Color(246, 184, 66);
    public static final Color COLOR_PRIMARY_HOVER = new Color(255, 204, 96);
    public static final Color COLOR_SECONDARY = new Color(31, 45, 56);
    public static final Color COLOR_SECONDARY_HOVER = new Color(43, 61, 73);
    public static final Color COLOR_SUCCESS = new Color(71, 197, 164);
    public static final Color COLOR_SUCCESS_HOVER = new Color(48, 169, 139);
    public static final Color COLOR_WARNING = new Color(242, 161, 55);
    public static final Color COLOR_WARNING_HOVER = new Color(214, 132, 31);
    public static final Color COLOR_DANGER = new Color(245, 91, 94);
    public static final Color COLOR_DANGER_HOVER = new Color(213, 65, 71);

    public static final Color BG_MAIN = new Color(12, 19, 25);
    public static final Color BG_CARD = new Color(20, 31, 40);
    public static final Color BG_PANEL = new Color(16, 26, 34);
    public static final Color BG_ELEVATED = new Color(29, 43, 53);
    public static final Color BORDER_CARD = new Color(52, 69, 80);
    public static final Color BORDER_FOCUS = new Color(246, 184, 66);
    public static final Color TEXT_TITLE = new Color(237, 242, 245);
    public static final Color TEXT_BODY = new Color(203, 214, 220);
    public static final Color TEXT_MUTED = new Color(143, 159, 169);
    public static final Color COLOR_BLUE = new Color(91, 175, 244);
    public static final Color COLOR_SILVER = new Color(177, 192, 201);
    public static final Color COLOR_BRONZE = new Color(216, 137, 83);

    /** Trả về màu có độ trong suốt để tô các thẻ nội dung và hàng trong bảng. */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static final String FONT_FAMILY = selectFontFamily();
    public static final Font FONT_LARGE_TITLE = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);

    /** Chọn phông có sẵn trên máy theo thứ tự ưu tiên, dùng phông dự phòng nếu cần. */
    private static String selectFontFamily() {
        String sample = "Tiếng Việt: Đăng nhập, Hướng dẫn, Đầu hàng, Ắ Ằ Ễ Ộ Ử Ỹ";
        for (String family : new String[]{"Segoe UI", "Arial", Font.SANS_SERIF}) {
            Font font = new Font(family, Font.PLAIN, 14);
            if (font.canDisplayUpTo(sample) == -1) return font.getFamily();
        }
        return Font.DIALOG;
    }

    /** Cài màu cho các thành phần Swing sử dụng cấu hình giao diện mặc định thay vì định dạng riêng. */
    public static void installDefaults() {
        // Nếu không cấu hình, thành phần của hệ điều hành sẽ giữ phông chữ khác và nền nút sáng.
        for (Object key : UIManager.getDefaults().keySet().toArray()) {
            if (UIManager.get(key) instanceof Font) {
                UIManager.put(key, new FontUIResource(FONT_REGULAR));
            }
        }
        UIManager.put("OptionPaneUI", ThemedOptionPaneUI.class.getName());
        UIManager.put("OptionPane.messageFont", new FontUIResource(FONT_REGULAR));
        UIManager.put("OptionPane.buttonFont", new FontUIResource(FONT_BOLD));
        UIManager.put("OptionPane.yesButtonText", "Có");
        UIManager.put("OptionPane.noButtonText", "Không");
        UIManager.put("OptionPane.cancelButtonText", "Hủy");
        UIManager.put("OptionPane.okButtonText", "Đồng ý");
        UIManager.put("Panel.background", BG_MAIN);
        UIManager.put("Panel.foreground", TEXT_BODY);
        UIManager.put("Label.foreground", TEXT_BODY);
        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_TITLE);
        UIManager.put("Button.background", BG_ELEVATED);
        UIManager.put("Button.foreground", TEXT_TITLE);
        UIManager.put("TextField.background", BG_PANEL);
        UIManager.put("TextField.foreground", TEXT_TITLE);
        UIManager.put("PasswordField.background", BG_PANEL);
        UIManager.put("PasswordField.foreground", TEXT_TITLE);
        UIManager.put("Table.background", BG_PANEL);
        UIManager.put("Table.foreground", TEXT_BODY);
        UIManager.put("TableHeader.background", BG_ELEVATED);
        UIManager.put("TableHeader.foreground", TEXT_TITLE);
        UIManager.put("TabbedPane.background", BG_MAIN);
        UIManager.put("TabbedPane.foreground", TEXT_BODY);
        UIManager.put("TabbedPane.selected", BG_ELEVATED);
        UIManager.put("TabbedPane.unselectedBackground", BG_CARD);
        UIManager.put("TabbedPane.unselectedForeground", TEXT_MUTED);
        UIManager.put("TabbedPane.contentAreaColor", BG_PANEL);
        UIManager.put("TabbedPane.borderColor", BORDER_CARD);
        UIManager.put("TabbedPane.light", BG_CARD);
        UIManager.put("TabbedPane.highlight", BG_ELEVATED);
        UIManager.put("TabbedPane.shadow", BORDER_CARD);
        UIManager.put("TabbedPane.darkShadow", BORDER_CARD);
        UIManager.put("ScrollPane.background", BG_PANEL);
        UIManager.put("Viewport.background", BG_PANEL);
        UIManager.put("ToolTip.background", BG_ELEVATED);
        UIManager.put("ToolTip.foreground", TEXT_TITLE);
    }

    /** Giữ kích thước tối thiểu đủ dùng và điều chỉnh cửa sổ vừa vùng làm việc của màn hình hiện tại. */
    public static void configureWindow(Window window, int width, int height, int minWidth, int minHeight) {
        GraphicsConfiguration config = window.getGraphicsConfiguration();
        Rectangle area = new Rectangle(config.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
        area.width -= insets.left + insets.right;
        area.height -= insets.top + insets.bottom;
        window.setMinimumSize(new Dimension(Math.min(minWidth, area.width), Math.min(minHeight, area.height)));
        window.setSize(Math.min(width, area.width), Math.min(height, area.height));
        if (window instanceof Frame frame) frame.setResizable(true);
        if (window instanceof Dialog dialog) dialog.setResizable(true);
    }

    /** Hộp thoại trên Windows không có nút phóng to mặc định; cung cấp thao tác tương đương qua phím F11. */
    public static JButton createDialogSizeButton(JDialog dialog) {
        JButton button = new JButton("Phóng to");
        styleOutlineButton(button, BORDER_CARD, COLOR_SECONDARY_HOVER, TEXT_TITLE);
        Rectangle[] restoredBounds = new Rectangle[1];
        AbstractAction toggle = new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (restoredBounds[0] == null) {
                    restoredBounds[0] = dialog.getBounds();
                    GraphicsConfiguration config = dialog.getGraphicsConfiguration();
                    Rectangle area = new Rectangle(config.getBounds());
                    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
                    area.x += insets.left;
                    area.y += insets.top;
                    area.width -= insets.left + insets.right;
                    area.height -= insets.top + insets.bottom;
                    dialog.setBounds(area);
                    button.setText("Thu gọn");
                } else {
                    dialog.setBounds(restoredBounds[0]);
                    restoredBounds[0] = null;
                    button.setText("Phóng to");
                }
            }
        };
        button.addActionListener(toggle);
        button.setToolTipText("Phóng to / Thu gọn (F11)");
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("F11"), "toggleMaximize");
        dialog.getRootPane().getActionMap().put("toggleMaximize", toggle);
        return button;
    }

    /** Hiển thị nội dung HTML trong vùng cuộn với kiểu chữ và màu của ứng dụng. */
    public static JScrollPane createHtmlScrollPane(String html) {
        JEditorPane editor = new JEditorPane();
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.getStyleSheet().addRule("body { color: #CBD6DC; margin: 16px; font-size: 12pt; }");
        kit.getStyleSheet().addRule("h2 { font-size: 16pt; margin-top: 16px; margin-bottom: 8px; }");
        kit.getStyleSheet().addRule("p, li { margin-top: 6px; margin-bottom: 8px; }");
        editor.setEditorKit(kit);
        editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editor.setFont(FONT_REGULAR);
        editor.setEditable(false);
        editor.setBackground(BG_PANEL);
        editor.setForeground(TEXT_BODY);
        editor.setText(html);
        editor.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(editor);
        styleScrollPane(scroll);
        editor.setOpaque(true);
        scroll.getViewport().setOpaque(true);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CARD));
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    /** Cài màu, kiểu chữ và hiệu ứng di chuột cho nút nền đặc. */
    public static void styleButton(JButton btn, Color bg, Color hoverBg, Color fg) {
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(new LineBorder(bg.darker(), 1, true), new EmptyBorder(7, 14, 7, 14)));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hoverBg); }
            @Override public void mouseExited(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(bg); }
        });
    }

    /** Cài viền, màu và hiệu ứng di chuột cho nút dạng đường viền. */
    public static void styleOutlineButton(JButton btn, Color borderColor, Color hoverBg, Color fg) {
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setFont(FONT_BOLD);
        btn.setBackground(BG_ELEVATED);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(new LineBorder(borderColor, 1, true), new EmptyBorder(6, 12, 6, 12)));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hoverBg); }
            @Override public void mouseExited(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(BG_ELEVATED); }
        });
    }

    /** Áp dụng màu nền, chữ và đường viền dùng chung cho ô nhập. */
    public static void styleTextField(JTextField field) {
        field.setFont(FONT_REGULAR);
        field.setBackground(BG_PANEL);
        field.setForeground(TEXT_TITLE);
        field.setCaretColor(COLOR_PRIMARY);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 38));
        field.setBorder(new CompoundBorder(new LineBorder(BORDER_CARD, 1, true), new EmptyBorder(7, 10, 7, 10)));
    }

    /** Áp dụng kiểu ô nhập chung cho ô mật khẩu. */
    public static void stylePasswordField(JPasswordField field) {
        styleTextField(field);
    }

    /** Cài kiểu bảng, tiêu đề, chiều cao dòng và cách hiển thị ô theo giao diện chung. */
    public static void styleTable(JTable table) {
        styleTable(table, false);
    }

    /** Bảng gọn trong sảnh luôn vừa vùng hiển thị, kể cả sau khi làm mới dữ liệu. */
    public static void styleCompactTable(JTable table, int... columnWidths) {
        styleTable(table, true);
        for (int i = 0; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }
    }

    /** Cài kiểu bảng, tiêu đề, chiều cao dòng và cách hiển thị ô theo giao diện chung. */
    private static void styleTable(JTable table, boolean compact) {
        table.setRowHeight(compact ? 28 : 34);
        table.setFont(compact ? FONT_REGULAR.deriveFont(12f) : FONT_REGULAR);
        table.setForeground(TEXT_BODY);
        table.setBackground(withAlpha(BG_PANEL, 178));
        table.setOpaque(false);
        table.setGridColor(BORDER_CARD);
        table.setSelectionBackground(new Color(69, 58, 34));
        table.setSelectionForeground(TEXT_TITLE);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        if (compact) {
            // Không đổi chế độ co giãn cột khi vùng hiển thị đổi kích thước: việc hiện/ẩn thanh cuộn có thể
            // kích hoạt bố trí lại liên tục và khiến các cột dịch qua lại.
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        } else {
            // Bảng chi tiết rộng hơn vẫn cho phép cuộn ngang khi cần.
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.addHierarchyListener(event -> {
                if ((event.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0
                        && table.getParent() instanceof JViewport viewport) {
                    viewport.addComponentListener(new ComponentAdapter() {
                        @Override public void componentResized(ComponentEvent event) {
                            fitTableColumns(table, viewport);
                        }
                    });
                    SwingUtilities.invokeLater(() -> fitTableColumns(table, viewport));
                }
            });
        }
        JTableHeader header = table.getTableHeader();
        header.setUI(new BasicTableHeaderUI());
        header.setFont(table.getFont().deriveFont(Font.BOLD));
        header.setBackground(BG_ELEVATED);
        header.setForeground(TEXT_TITLE);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(0, compact ? 38 : 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CARD));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setOpaque(true);
                setBackground(BG_ELEVATED);
                setForeground(TEXT_TITLE);
                setFont(t.getTableHeader().getFont());
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_CARD),
                        BorderFactory.createEmptyBorder(0, compact ? 3 : 6, 0, compact ? 3 : 6)));
                String title = String.valueOf(value);
                setToolTipText(title);
                if (compact && getFontMetrics(getFont()).stringWidth(title) + 8
                        > t.getColumnModel().getColumn(column).getWidth()) {
                    int split = title.lastIndexOf(' ');
                    if (split > 0) {
                        setText("<html><center>" + title.substring(0, split) + "<br>"
                                + title.substring(split + 1) + "</center></html>");
                    }
                }
                return c;
            }
        });
    }

    /** Phân bổ chiều rộng cột theo vùng nhìn thấy của bảng. */
    private static void fitTableColumns(JTable table, JViewport viewport) {
        FontMetrics metrics = table.getFontMetrics(FONT_BOLD);
        int preferredWidth = 0;
        for (int index = 0; index < table.getColumnCount(); index++) {
            javax.swing.table.TableColumn column = table.getColumnModel().getColumn(index);
            int headerWidth = metrics.stringWidth(String.valueOf(column.getHeaderValue())) + 24;
            column.setMinWidth(headerWidth);
            if (column.getPreferredWidth() < headerWidth) column.setPreferredWidth(headerWidth);
            preferredWidth += column.getPreferredWidth();
        }
        table.setAutoResizeMode(viewport.getWidth() >= preferredWidth
                ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF);
    }

    /** Tạo đường viền và khoảng đệm cho khối nội dung có tiêu đề. */
    public static Border createCardBorder(String title) {
        return BorderFactory.createTitledBorder(new LineBorder(BORDER_CARD, 1, true), title,
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, FONT_TITLE, COLOR_PRIMARY);
    }

    /** Cài màu nền, viền và thanh cuộn cho vùng cuộn. */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBackground(withAlpha(BG_PANEL, 160));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(withAlpha(BG_PANEL, 160));
        scrollPane.getViewport().setOpaque(false);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
    }

    /** Cài kiểu hiển thị các tab theo bảng màu chung. */
    public static void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setOpaque(false);
        tabbedPane.setBackground(BG_PANEL);
        tabbedPane.setForeground(TEXT_BODY);
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setBackgroundAt(i, BG_CARD);
            tabbedPane.setForegroundAt(i, TEXT_BODY);
        }
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                g.setColor(withAlpha(isSelected ? BG_ELEVATED : BG_CARD, isSelected ? 204 : 164));
                g.fillRect(x, y, w, h);
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                g.setColor(isSelected ? COLOR_PRIMARY : BORDER_CARD);
                g.drawRect(x, y, w - 1, h - 1);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // Vùng cuộn con tự vẽ viền tối; ẩn khung nội dung sáng mặc định của hệ điều hành
                // để các tab có giao diện thống nhất trên Windows.
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                               int tabIndex, Rectangle iconRect, Rectangle textRect,
                                               boolean isSelected) {
                // Tránh khung chấm sáng đánh dấu tiêu điểm của giao diện hệ điều hành.
            }
        });
    }

    /** Cài màu và hình thức thanh cuộn để đồng bộ với giao diện. */
    private static void styleScrollBar(JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = BG_ELEVATED;
                trackColor = BG_PANEL;
            }
            @Override protected JButton createDecreaseButton(int orientation) { return createEmptyScrollButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createEmptyScrollButton(); }
        });
        bar.setOpaque(true);
        bar.setBackground(BG_PANEL);
    }

    /** Tạo nút rỗng để bỏ mũi tên mặc định của thanh cuộn. */
    private static JButton createEmptyScrollButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    /** Tên cấp bậc và các khoảng điểm hiển thị theo thiết kế. */
    public static String getRankTitle(int score) {
        if (score >= 1000) return "Huyền thoại";
        if (score >= 500) return "Cao thủ";
        if (score >= 200) return "Chiến binh";
        if (score >= 50) return "Bình nhì";
        return "Tập sự";
    }

    public static class StatusDotIcon implements Icon {
        private final Color color;
        private final int size;
        public StatusDotIcon(Color color) { this(color, 8); }
        public StatusDotIcon(Color color, int size) { this.color = color; this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y + (getIconHeight() - size) / 2, size, size);
            g2.dispose();
        }
        @Override public int getIconWidth() { return size + 5; }
        @Override public int getIconHeight() { return 16; }
    }

    public static Color getRankColor(int score) {
        if (score >= 1000) return COLOR_PRIMARY;
        if (score >= 500) return new Color(164, 132, 223);
        if (score >= 200) return COLOR_BLUE;
        if (score >= 50) return COLOR_SILVER;
        return COLOR_BRONZE;
    }
}
