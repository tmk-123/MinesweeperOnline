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
 * Panel hiển thị bảng xếp hạng cao thủ, gắn trực tiếp vào sảnh chờ MainMenuFrame.
 */
public class LeaderboardPanel extends GlassPanel {

    private final ServerConnection connection;
    private final String currentUsername;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;

    public LeaderboardPanel(ServerConnection connection, String currentUsername) {
        super(new BorderLayout(8, 8), UITheme.BG_CARD, 142);
        this.connection = connection;
        this.currentUsername = currentUsername;

        initComponents();
    }

    private void initComponents() {
        setBorder(UITheme.createCardBorder("Bảng xếp hạng cao thủ (Top 10)"));

        // Bảng dữ liệu: Thêm cột "Cấp bậc" (Rank Tier)
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
        table.getColumnModel().getColumn(0).setPreferredWidth(82);  // Hạng với huy hiệu Top 3
        table.getColumnModel().getColumn(1).setPreferredWidth(125); // Tên người chơi
        table.getColumnModel().getColumn(2).setPreferredWidth(110); // Huy hiệu cấp bậc và tên hạng
        table.getColumnModel().getColumn(3).setPreferredWidth(65);  // Điểm số
        table.getColumnModel().getColumn(4).setPreferredWidth(90);  // Trận thắng (không bị cắt chữ)
        table.getColumnModel().getColumn(5).setPreferredWidth(75);  // Tổng trận
        table.getColumnModel().getColumn(6).setPreferredWidth(85);  // Tỉ lệ thắng

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setFont(UITheme.FONT_REGULAR);
                setIcon(null);
                setIconTextGap(5);
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

                String nameInRow = (String) tableModel.getValueAt(row, 1);
                int rank = (int) tableModel.getValueAt(row, 0);
                boolean isCurrentUser = currentUsername != null && currentUsername.equals(nameInRow);

                // 1. Cột Hạng: giữ huy chương ở dạng chữ, không phụ thuộc font emoji.
                if (col == 0) {
                    setFont(UITheme.FONT_BOLD);
                    setIcon(GameUiIcons.podiumMedalIcon(rank, 20));
                    if (rank == 1) {
                        setText("01");
                        if (!isSel) c.setForeground(UITheme.COLOR_PRIMARY);
                    } else if (rank == 2) {
                        setText("02");
                        if (!isSel) c.setForeground(UITheme.COLOR_SILVER);
                    } else if (rank == 3) {
                        setText("03");
                        if (!isSel) c.setForeground(UITheme.COLOR_BRONZE);
                    } else {
                        setText(String.format("%02d", rank));
                        if (!isSel) c.setForeground(UITheme.TEXT_MUTED);
                    }
                } else if (col == 1) {
                    // Cột Tên: gắn thẻ "(Bạn)" nếu là người dùng hiện tại
                    if (isCurrentUser) {
                        setText(nameInRow + " (Bạn)");
                        setFont(UITheme.FONT_BOLD);
                    } else {
                        setText(nameInRow);
                    }
                } else if (col == 2) {
                    // Cột Cấp bậc (Rank Tier)
                    setFont(UITheme.FONT_SMALL_BOLD);
                    if (val instanceof String && !isSel) {
                        int score = (int) tableModel.getValueAt(row, 3);
                        setIcon(GameUiIcons.rankTierIcon(score, 18));
                        setHorizontalAlignment(SwingConstants.LEFT);
                        c.setForeground(UITheme.getRankColor(score));
                    } else if (val instanceof String) {
                        int score = (int) tableModel.getValueAt(row, 3);
                        setIcon(GameUiIcons.rankTierIcon(score, 18));
                        setHorizontalAlignment(SwingConstants.LEFT);
                    }
                } else if (col == 3) {
                    // Cột Điểm số
                    setFont(UITheme.FONT_BOLD);
                    if (!isSel) c.setForeground(UITheme.COLOR_PRIMARY);
                } else if (col == 6) {
                    // Cột Tỉ lệ thắng: Hiện "-" nếu chưa đấu trận nào
                    Object totalObj = tableModel.getValueAt(row, 5);
                    int total = totalObj instanceof Number ? ((Number) totalObj).intValue() : 0;
                    if (total == 0) {
                        setText("-");
                    } else if (val instanceof Number) {
                        setText(String.format("%.1f%%", ((Number) val).doubleValue()));
                    }
                }

                // 2. Các hàng đầu có sắc độ kim loại tối, còn hàng người dùng được nhấn nhẹ.
                if (!isSel) {
                    if (rank == 1) {
                        c.setBackground(UITheme.withAlpha(new Color(53, 47, 34), 198));
                    } else if (rank == 2) {
                        c.setBackground(UITheme.withAlpha(new Color(35, 46, 54), 198));
                    } else if (rank == 3) {
                        c.setBackground(UITheme.withAlpha(new Color(49, 40, 35), 198));
                    } else {
                        c.setBackground(UITheme.withAlpha(row % 2 == 0 ? UITheme.BG_PANEL : UITheme.BG_CARD, 178));
                    }
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
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1));
        scrollPane.getViewport().setBackground(UITheme.withAlpha(UITheme.BG_PANEL, 158));
        add(scrollPane, BorderLayout.CENTER);

        // Panel chân bảng: Trạng thái bên trái, Nút ? Quy tắc xếp hạng bên phải
        JPanel actionPanel = new GlassPanel(new BorderLayout(8, 0), UITheme.BG_CARD, 150);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        lblStatus = new JLabel("Đang tải dữ liệu bảng xếp hạng...");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        actionPanel.add(lblStatus, BorderLayout.WEST);

        JButton btnHelp = new JButton("? Quy tắc xếp hạng");
        UITheme.styleOutlineButton(btnHelp, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY, UITheme.TEXT_TITLE);
        btnHelp.setFont(UITheme.FONT_SMALL);
        btnHelp.setToolTipText("Xem các tiêu chí phân định thứ hạng trên bảng xếp hạng");
        btnHelp.addActionListener(e -> {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            LeaderboardRulesDialog dialog = new LeaderboardRulesDialog(parentFrame);
            dialog.setVisible(true);
        });
        actionPanel.add(btnHelp, BorderLayout.EAST);

        add(actionPanel, BorderLayout.SOUTH);
    }

    public void refreshLeaderboard() {
        if (connection != null && connection.isConnected()) {
            lblStatus.setText("Đang làm mới bảng xếp hạng...");
            connection.sendMessage(JsonProtocol.createGetLeaderboard());
        }
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
                lblStatus.setText("Cập nhật thành công: Top 10 cao thủ hàng đầu.");
            }
        });
    }
}
