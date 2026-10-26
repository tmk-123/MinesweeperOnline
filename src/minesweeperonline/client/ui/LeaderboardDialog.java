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
        setSize(620, 480);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(8, 8));

        // Phần tiêu đề phía trên
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        headerPanel.setBackground(new Color(245, 248, 255));

        JLabel lblTitle = new JLabel("BẢNG XẾP HẠNG CAO THỦ", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(new Color(20, 60, 140));
        headerPanel.add(lblTitle, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Bảng ở giữa
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
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Độ rộng các cột (đảm bảo không bị co chữ "Số trận thắng")
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(115);
        table.getColumnModel().getColumn(4).setPreferredWidth(85);
        table.getColumnModel().getColumn(5).setPreferredWidth(95);

        // Cell renderer cố định theo thứ hạng chuẩn
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

                String nameInRow = (String) tableModel.getValueAt(row, 1);
                int rank = (int) tableModel.getValueAt(row, 0);
                boolean isCurrentUser = currentUsername != null && currentUsername.equals(nameInRow);

                // Định dạng hiển thị sạch đẹp
                if (col == 0) {
                    if (rank == 1) setText("Top 1");
                    else if (rank == 2) setText("Top 2");
                    else if (rank == 3) setText("Top 3");
                    else setText("#" + rank);
                } else if (col == 5 && val instanceof Number) {
                    setText(String.format("%.1f%%", ((Number) val).doubleValue()));
                }

                if (!isSel) {
                    if (isCurrentUser) {
                        c.setBackground(new Color(230, 245, 230));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (rank == 1) {
                        c.setBackground(new Color(255, 250, 230)); // Top 1 tô màu vàng kim
                    } else if (rank == 2) {
                        c.setBackground(new Color(245, 245, 250)); // Top 2 tô màu bạc
                    } else if (rank == 3) {
                        c.setBackground(new Color(253, 245, 238)); // Top 3 tô màu đồng
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

        // Panel phía dưới
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
                lblStatus.setText("Chưa có dữ liệu người chơi.");
                return;
            }

            for (LeaderboardEntry e : list) {
                tableModel.addRow(new Object[]{
                        e.getRank(),
                        e.getUsername(),
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
