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
public class LeaderboardPanel extends JPanel {

    private final ServerConnection connection;
    private final String currentUsername;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;

    public LeaderboardPanel(ServerConnection connection, String currentUsername) {
        this.connection = connection;
        this.currentUsername = currentUsername;

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBackground(UITheme.BG_CARD);
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
        table.getColumnModel().getColumn(0).setPreferredWidth(75);  // Hạng (🥇 Top 1, 🥈 Top 2, 🥉 Top 3, Top 4...)
        table.getColumnModel().getColumn(1).setPreferredWidth(125); // Tên người chơi
        table.getColumnModel().getColumn(2).setPreferredWidth(95);  // Cấp bậc
        table.getColumnModel().getColumn(3).setPreferredWidth(65);  // Điểm số
        table.getColumnModel().getColumn(4).setPreferredWidth(90);  // Trận thắng (không bị cắt chữ)
        table.getColumnModel().getColumn(5).setPreferredWidth(75);  // Tổng trận
        table.getColumnModel().getColumn(6).setPreferredWidth(85);  // Tỉ lệ thắng

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setFont(UITheme.FONT_REGULAR);
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

                String nameInRow = (String) tableModel.getValueAt(row, 1);
                int rank = (int) tableModel.getValueAt(row, 0);
                boolean isCurrentUser = currentUsername != null && currentUsername.equals(nameInRow);

                // 1. Cột Hạng: Huy chương 🥇 🥈 🥉 cho Top 3
                if (col == 0) {
                    setFont(UITheme.FONT_BOLD);
                    if (rank == 1) {
                        setText("🥇 Top 1");
                        if (!isSel) c.setForeground(new Color(180, 83, 9));       // Vàng kim (Amber 700)
                    } else if (rank == 2) {
                        setText("🥈 Top 2");
                        if (!isSel) c.setForeground(new Color(71, 85, 105));      // Bạc (Slate 600)
                    } else if (rank == 3) {
                        setText("🥉 Top 3");
                        if (!isSel) c.setForeground(new Color(194, 65, 12));      // Đồng (Orange 700)
                    } else {
                        setText("Top " + rank);
                        if (!isSel) c.setForeground(new Color(100, 116, 139));    // Top 4+: Xám Slate 500
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
                        c.setForeground(UITheme.getRankColor(score));
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

                // 2. Màu nền: Chỉ giữ màu nền cho Top 1, 2, 3; các top sau để nền trắng
                if (!isSel) {
                    if (rank == 1) {
                        c.setBackground(new Color(254, 249, 195)); // Gold light
                        if (col != 0 && col != 2 && col != 3) {
                            c.setForeground(isCurrentUser && col == 1 ? new Color(6, 95, 70) : new Color(133, 77, 14));
                        }
                    } else if (rank == 2) {
                        c.setBackground(new Color(226, 232, 240)); // Silver light (màu bạc rõ ràng)
                        if (col != 0 && col != 2 && col != 3) {
                            c.setForeground(isCurrentUser && col == 1 ? new Color(6, 95, 70) : new Color(51, 65, 85));
                        }
                    } else if (rank == 3) {
                        c.setBackground(new Color(255, 237, 213)); // Bronze light
                        if (col != 0 && col != 2 && col != 3) {
                            c.setForeground(isCurrentUser && col == 1 ? new Color(6, 95, 70) : new Color(154, 52, 18));
                        }
                    } else {
                        // Top 4 trở đi: Bỏ màu nền, để nền trắng
                        c.setBackground(Color.WHITE);
                        if (col != 0 && col != 2 && col != 3) {
                            if (isCurrentUser && col == 1) {
                                c.setForeground(new Color(6, 95, 70)); // Tên bạn vẫn nổi bật màu xanh lá
                            } else {
                                c.setForeground(UITheme.TEXT_TITLE);
                            }
                        }
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // Panel chân bảng: Trạng thái bên trái, Nút ? Quy tắc xếp hạng bên phải
        JPanel actionPanel = new JPanel(new BorderLayout(8, 0));
        actionPanel.setBackground(UITheme.BG_CARD);
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
