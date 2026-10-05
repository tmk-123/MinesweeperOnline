package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.Player;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel displaying the list of active online players and challenge actions.
 */
public class OnlinePlayersPanel extends JPanel {

    private final ServerConnection connection;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnChallenge;
    private JButton btnRefresh;

    public OnlinePlayersPanel(ServerConnection connection) {
        this.connection = connection;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Người chơi đang online"));

        String[] columns = {"Mã ID", "Tên người chơi", "Tổng điểm", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(460, 220));
        add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnRefresh = new JButton("Làm mới danh sách");
        btnChallenge = new JButton("Thách đấu");
        btnChallenge.setFont(new Font("Arial", Font.BOLD, 12));
        btnChallenge.setBackground(new Color(220, 240, 255));

        actionPanel.add(btnRefresh);
        actionPanel.add(btnChallenge);
        add(actionPanel, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshOnlinePlayers());

        btnChallenge.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn một người chơi trong danh sách để thách đấu!",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int targetUserId = (int) tableModel.getValueAt(selectedRow, 0);
            String targetUsername = (String) tableModel.getValueAt(selectedRow, 1);
            String status = (String) tableModel.getValueAt(selectedRow, 3);

            if (!Player.STATUS_IDLE.equals(status)) {
                JOptionPane.showMessageDialog(this,
                        "Người chơi này hiện đang bận hoặc đang thi đấu!",
                        "Không thể thách đấu", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn gửi lời thách đấu tới: " + targetUsername + "?",
                    "Xác nhận thách đấu", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                btnChallenge.setEnabled(false);
                btnChallenge.setText("Đang chờ phản hồi...");
                connection.sendMessage(JsonProtocol.createChallenge(targetUserId, targetUsername));
            }
        });
    }

    public void refreshOnlinePlayers() {
        connection.sendMessage(JsonProtocol.createGetOnlinePlayers());
    }

    public void updatePlayers(List<Player> players) {
        tableModel.setRowCount(0);
        if (players != null) {
            for (Player p : players) {
                tableModel.addRow(new Object[]{
                        p.getUserId(),
                        p.getUsername(),
                        p.getTotalScore(),
                        p.getStatus()
                });
            }
        }
        btnChallenge.setEnabled(true);
        btnChallenge.setText("Thách đấu");
    }

    public JButton getBtnChallenge() {
        return btnChallenge;
    }
}
