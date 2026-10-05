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
 * Leaderboard Dialog displaying top players ranked by total score and victories.
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
        setSize(560, 480);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(8, 8));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        headerPanel.setBackground(new Color(245, 248, 255));

        JLabel lblTitle = new JLabel("🏆 BẢNG XẾP HẠNG CAO THỦ", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(new Color(20, 60, 140));
        headerPanel.add(lblTitle, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"Hạng", "Tên người chơi", "Điểm số", "Số trận thắng", "Tổng trận", "Tỉ lệ thắng"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(230, 238, 248));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(95);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);

        // Custom cell renderer for rankings & highlighting
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

                String nameInRow = (String) t.getValueAt(row, 1);
                boolean isCurrentUser = currentUsername != null && currentUsername.equals(nameInRow);

                if (!isSel) {
                    if (isCurrentUser) {
                        c.setBackground(new Color(230, 245, 230));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (row == 0) {
                        c.setBackground(new Color(255, 250, 230)); // Top 1 Gold tint
                    } else if (row == 1) {
                        c.setBackground(new Color(245, 245, 250)); // Top 2 Silver tint
                    } else if (row == 2) {
                        c.setBackground(new Color(253, 245, 238)); // Top 3 Bronze tint
                    } else if (row % 2 == 1) {
                        c.setBackground(new Color(248, 249, 251));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));

        lblStatus = new JLabel("Đang tải dữ liệu...");
        lblStatus.setFont(new Font("Arial", Font.ITALIC, 12));
        lblStatus.setForeground(Color.GRAY);
        bottomPanel.add(lblStatus, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.addActionListener(e -> refresh());

        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dispose());

        btnPanel.add(btnRefresh);
        btnPanel.add(btnClose);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refresh() {
        lblStatus.setText("Đang tải bảng xếp hạng từ máy chủ...");
        connection.sendMessage(JsonProtocol.createGetLeaderboard());
    }

    public void updateLeaderboard(List<LeaderboardEntry> list) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            if (list == null || list.isEmpty()) {
                lblStatus.setText("Chưa có dữ liệu xếp hạng.");
                return;
            }

            for (LeaderboardEntry e : list) {
                String rankStr;
                if (e.getRank() == 1) rankStr = "🥇 1";
                else if (e.getRank() == 2) rankStr = "🥈 2";
                else if (e.getRank() == 3) rankStr = "🥉 3";
                else rankStr = String.valueOf(e.getRank());

                tableModel.addRow(new Object[]{
                        rankStr,
                        e.getUsername(),
                        e.getTotalScore(),
                        e.getWins(),
                        e.getTotalMatches(),
                        String.format("%.1f%%", e.getWinRate())
                });
            }
            lblStatus.setText("Cập nhật thành công: " + list.size() + " người chơi.");
        });
    }
}
