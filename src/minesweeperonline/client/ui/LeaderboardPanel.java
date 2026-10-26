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

        // Bảng dữ liệu
        String[] columns = {"Hạng", "Tên người chơi", "Điểm số", "Số trận thắng", "Tổng trận", "Tỉ lệ thắng"};
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
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(75);
        table.getColumnModel().getColumn(3).setPreferredWidth(105);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setFont(UITheme.FONT_REGULAR);
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

                String nameInRow = (String) tableModel.getValueAt(row, 1);
                int rank = (int) tableModel.getValueAt(row, 0);
                boolean isCurrentUser = currentUsername != null && currentUsername.equals(nameInRow);

                if (col == 0) {
                    setFont(UITheme.FONT_BOLD);
                    if (rank == 1) setText("Top 1");
                    else if (rank == 2) setText("Top 2");
                    else if (rank == 3) setText("Top 3");
                    else setText("#" + rank);
                } else if (col == 5 && val instanceof Number) {
                    setText(String.format("%.1f%%", ((Number) val).doubleValue()));
                }

                if (!isSel) {
                    if (isCurrentUser) {
                        c.setBackground(new Color(236, 253, 245)); // Soft Emerald
                        c.setForeground(new Color(6, 95, 70));
                        setFont(UITheme.FONT_BOLD);
                    } else if (rank == 1) {
                        c.setBackground(new Color(254, 249, 195)); // Gold light
                        c.setForeground(new Color(133, 77, 14));
                    } else if (rank == 2) {
                        c.setBackground(new Color(241, 245, 249)); // Silver light
                        c.setForeground(new Color(51, 65, 85));
                    } else if (rank == 3) {
                        c.setBackground(new Color(255, 237, 213)); // Bronze light
                        c.setForeground(new Color(154, 52, 18));
                    } else if (row % 2 == 1) {
                        c.setBackground(UITheme.BG_MAIN);
                        c.setForeground(UITheme.TEXT_TITLE);
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(UITheme.TEXT_TITLE);
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

        // Panel chân bảng
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actionPanel.setBackground(UITheme.BG_CARD);
        lblStatus = new JLabel("Đang tải dữ liệu bảng xếp hạng...");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        actionPanel.add(lblStatus);

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
                lblStatus.setText("Cập nhật thành công: Top 10 cao thủ hàng đầu.");
            }
        });
    }
}
