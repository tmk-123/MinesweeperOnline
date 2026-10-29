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
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Shared dark steel and gold presentation for the Minesweeper client. */
public final class UITheme {
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

    /** Returns a tinted color for translucent cards and table rows. */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static final Font FONT_LARGE_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);

    /** Installs colors for Swing controls that use UI defaults instead of explicit component styling. */
    public static void installDefaults() {
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
    }

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

    public static void styleTextField(JTextField field) {
        field.setFont(FONT_REGULAR);
        field.setBackground(BG_PANEL);
        field.setForeground(TEXT_TITLE);
        field.setCaretColor(COLOR_PRIMARY);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 38));
        field.setBorder(new CompoundBorder(new LineBorder(BORDER_CARD, 1, true), new EmptyBorder(7, 10, 7, 10)));
    }

    public static void stylePasswordField(JPasswordField field) {
        styleTextField(field);
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(FONT_REGULAR);
        table.setForeground(TEXT_BODY);
        table.setBackground(withAlpha(BG_PANEL, 178));
        table.setOpaque(false);
        table.setGridColor(BORDER_CARD);
        table.setSelectionBackground(new Color(69, 58, 34));
        table.setSelectionForeground(TEXT_TITLE);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setUI(new BasicTableHeaderUI());
        header.setFont(FONT_BOLD);
        header.setBackground(BG_ELEVATED);
        header.setForeground(TEXT_TITLE);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CARD));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                setOpaque(true);
                setBackground(BG_ELEVATED);
                setForeground(TEXT_TITLE);
                setFont(FONT_BOLD);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER_CARD),
                        BorderFactory.createEmptyBorder(0, 6, 0, 6)));
                return c;
            }
        });
    }

    public static Border createCardBorder(String title) {
        return BorderFactory.createTitledBorder(new LineBorder(BORDER_CARD, 1, true), title,
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, FONT_TITLE, COLOR_PRIMARY);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBackground(withAlpha(BG_PANEL, 160));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(withAlpha(BG_PANEL, 160));
        scrollPane.getViewport().setOpaque(false);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
    }

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
                // Child scroll panes draw their own dark border; suppress the platform's
                // default bright content frame so the tabs stay consistent on Windows.
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                               int tabIndex, Rectangle iconRect, Rectangle textRect,
                                               boolean isSelected) {
                // Avoid the light dotted focus box from the native look and feel.
            }
        });
    }

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

    private static JButton createEmptyScrollButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    /** Rank labels and score bands displayed in the supplied design. */
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
