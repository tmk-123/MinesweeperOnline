package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.LeaderboardEntry;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dialog bảng xếp hạng cao thủ, hiển thị cố định thứ hạng theo điểm số và số trận thắng.
 */
public class LeaderboardDialog extends JDialog {

    private final ServerConnection connection;
    private final String currentUsername;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;

    public LeaderboardDialog(Frame owner, ServerConnection connection, String currentUsername) {
        super(owner, "Bảng Xếp Hạng - Minesweeper Online", false);
        this.connection = connection;
        this.currentUsername = currentUsername;

        initComponents();
        refresh();
    }

    private void initComponents() {
        setSize(980, 570);
        setLocationRelativeTo(getOwner());
        setContentPane(new MinesweeperBackgroundPanel(MinesweeperBackgroundPanel.Scene.LOBBY, new BorderLayout(8, 8)));

        // Phần tiêu đề phía trên
        JPanel headerPanel = new GlassPanel(new BorderLayout(), UITheme.BG_CARD, 105);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));

        JLabel lblTitle = new JLabel("BẢNG XẾP HẠNG CAO THỦ", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_LARGE_TITLE);
        lblTitle.setForeground(UITheme.COLOR_PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Bảng ở giữa: Thêm cột "Cấp bậc"
        String[] columns = {"Hạng", "Tên người chơi", "Cấp bậc", "Điểm số", "Trận thắng", "Tổng trận", "Tỉ lệ thắng"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Độ rộng các cột
        table.getColumnModel().getColumn(0).setPreferredWidth(82);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(112);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getColumnModel().getColumn(4).setPreferredWidth(95);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);

        // Cell renderer cố định theo thứ hạng chuẩn
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setIcon(null);
                setIconTextGap(5);
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

                String nameInRow = (String) tableModel.getValueAt(row, 1);
                int rank = (int) tableModel.getValueAt(row, 0);
                boolean isCurrentUser = currentUsername != null && currentUsername.equals(nameInRow);

                // Định dạng hiển thị sạch đẹp
                if (col == 0) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    setText(String.format("%02d", rank));
                    setIcon(GameUiIcons.podiumMedalIcon(rank, 20));
                    if (!isSel) c.setForeground(rank == 1 ? UITheme.COLOR_PRIMARY : rank == 2 ? UITheme.COLOR_SILVER : rank == 3 ? UITheme.COLOR_BRONZE : UITheme.TEXT_MUTED);
                } else if (col == 1) {
                    if (isCurrentUser) {
                        setText(nameInRow + " (Bạn)");
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setText(nameInRow);
                    }
                } else if (col == 2) {
                    setFont(getFont().deriveFont(Font.BOLD, 12f));
                    if (val instanceof String) {
                        int score = (int) tableModel.getValueAt(row, 3);
                        setIcon(GameUiIcons.rankTierIcon(score, 18));
                        setIconTextGap(5);
                        setHorizontalAlignment(SwingConstants.LEFT);
                        if (!isSel) c.setForeground(UITheme.getRankColor(score));
                    }
                } else if (col == 3) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    if (!isSel) c.setForeground(UITheme.COLOR_PRIMARY);
                } else if (col == 6) {
                    Object totalObj = tableModel.getValueAt(row, 5);
                    int total = totalObj instanceof Number ? ((Number) totalObj).intValue() : 0;
                    if (total == 0) {
                        setText("-");
                    } else if (val instanceof Number) {
                        setText(String.format("%.1f%%", ((Number) val).doubleValue()));
                    }
                }

                if (!isSel) {
                    Color rowColor = rank == 1 ? new Color(53, 47, 34) : rank == 2 ? new Color(35, 46, 54) : rank == 3 ? new Color(49, 40, 35) : (row % 2 == 0 ? UITheme.BG_PANEL : UITheme.BG_CARD);
                    c.setBackground(UITheme.withAlpha(rowColor, rank <= 3 ? 198 : 178));
                    if (isCurrentUser && col == 1) c.setForeground(UITheme.COLOR_BLUE);
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        UITheme.styleScrollPane(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD));
        scrollPane.getViewport().setBackground(UITheme.BG_PANEL);
        JPanel centerPanel = new GlassPanel(new BorderLayout(12, 0), UITheme.BG_MAIN, 72);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(createRankPanel(), BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        // Panel phía dưới
        JPanel bottomPanel = new GlassPanel(new BorderLayout(10, 5), UITheme.BG_CARD, 150);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));

        lblStatus = new JLabel("Đang tải dữ liệu...");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        bottomPanel.add(lblStatus, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnHelp = new JButton("? Quy tắc");
        UITheme.styleOutlineButton(btnHelp, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
        btnHelp.setToolTipText("Xem các tiêu chí phân định thứ hạng trên bảng xếp hạng");
        btnHelp.addActionListener(e -> {
            Frame ownerFrame = getOwner() instanceof Frame ? (Frame) getOwner() : null;
            LeaderboardRulesDialog dialog = new LeaderboardRulesDialog(ownerFrame);
            dialog.setVisible(true);
        });

        JButton btnRefresh = new JButton("Làm mới");
        UITheme.styleOutlineButton(btnRefresh, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
        btnRefresh.addActionListener(e -> refresh());

        JButton btnClose = new JButton("Đóng");
        UITheme.styleButton(btnClose, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, new Color(28, 25, 20));
        btnClose.addActionListener(e -> dispose());

        btnPanel.add(btnHelp);
        btnPanel.add(btnRefresh);
        btnPanel.add(btnClose);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JComponent createRankPanel() {
        JPanel panel = new GlassPanel(new BorderLayout(), UITheme.BG_CARD, 150);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        panel.setPreferredSize(new Dimension(210, 0));
        JLabel title = new JLabel("HỆ THỐNG CẤP BẬC");
        title.setFont(UITheme.FONT_BOLD);
        title.setForeground(UITheme.COLOR_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(12));
        String[][] ranks = {{"Tập sự", "0 – 49 điểm"}, {"Bình nhì", "50 – 199 điểm"}, {"Chiến binh", "200 – 499 điểm"}, {"Cao thủ", "500 – 999 điểm"}, {"Huyền thoại", "1000+ điểm"}};
        int[] scores = {0, 50, 200, 500, 1000};
        for (int i = 0; i < ranks.length; i++) {
            JPanel row = new GlassPanel(new BorderLayout(9, 0), UITheme.BG_PANEL, 174);
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            JLabel emblem = new JLabel(GameUiIcons.rankTierIcon(scores[i], 34));
            emblem.setPreferredSize(new Dimension(38, 38));
            row.add(emblem, BorderLayout.WEST);

            JPanel labels = new JPanel(new GridLayout(2, 1, 0, 2));
            labels.setOpaque(false);
            JLabel name = new JLabel(ranks[i][0]);
            name.setFont(UITheme.FONT_BOLD);
            name.setForeground(UITheme.getRankColor(scores[i]));
            JLabel range = new JLabel(ranks[i][1]);
            range.setFont(UITheme.FONT_SMALL);
            range.setForeground(UITheme.TEXT_BODY);
            labels.add(name);
            labels.add(range);
            row.add(labels, BorderLayout.CENTER);
            panel.add(row);
            panel.add(Box.createVerticalStrut(7));
        }
        return panel;
    }

    public void refresh() {
        lblStatus.setText("Đang tải bảng xếp hạng từ máy chủ...");
        connection.sendMessage(JsonProtocol.createGetLeaderboard());
    }

    public void updateLeaderboard(List<LeaderboardEntry> list) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            if (list == null || list.isEmpty()) {
                lblStatus.setText("Chưa có dữ liệu người chơi.");
                return;
            }

            for (LeaderboardEntry e : list) {
                String rankTitle = UITheme.getRankTitle(e.getTotalScore());
                tableModel.addRow(new Object[]{
                        e.getRank(),
                        e.getUsername(),
                        rankTitle,
                        e.getTotalScore(),
                        e.getWins(),
                        e.getTotalMatches(),
                        e.getWinRate()
                });
            }

            if (list.size() < 10) {
                lblStatus.setText("Cập nhật thành công: " + list.size() + " người chơi.");
            } else {
                lblStatus.setText("Cập nhật thành công: Top 10 cao thủ.");
            }
        });
    }
}
