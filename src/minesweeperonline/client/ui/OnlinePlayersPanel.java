package minesweeperonline.client.ui;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.Player;
import minesweeperonline.common.protocol.JsonProtocol;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel hiển thị danh sách người chơi đang online.
 * Hỗ trợ thách đấu nhanh bằng cách nháy vào ô/dòng người chơi trên bảng.
 */
public class OnlinePlayersPanel extends GlassPanel {

    private final ServerConnection connection;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblHint;
    private JButton btnChallenge = new JButton("Thách đấu");
    private boolean isChallenging = false;

    public OnlinePlayersPanel(ServerConnection connection) {
        super(new BorderLayout(8, 8), UITheme.BG_CARD, 142);
        this.connection = connection;
        initComponents();
    }

    /** Dựng bố cục, định dạng component và gắn các sự kiện của màn hình. */
    private void initComponents() {
        setBorder(UITheme.createCardBorder("Người chơi đang online"));

        String[] columns = {"Mã ID", "Tên người chơi", "Cấp bậc", "Tổng điểm", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UITheme.styleCompactTable(table, 42, 110, 100, 48, 100);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Custom Cell Renderer đẹp mắt, sử dụng StatusDotIcon thay vì emoji để tránh lỗi ô vuông
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setFont(t.getFont());
                putClientProperty("html.disable", Boolean.TRUE);
                setToolTipText(null);
                setIcon(null);
                setIconTextGap(3);
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

                // Màu xen kẽ dòng hiện đại
                if (!isSel) {
                    c.setBackground(UITheme.withAlpha(row % 2 == 1 ? UITheme.BG_PANEL : UITheme.BG_CARD, 178));
                    c.setForeground(UITheme.TEXT_TITLE);
                }

                // Cột Tên
                if (col == 1) {
                    setFont(t.getFont().deriveFont(Font.BOLD));
                    setToolTipText(getText());
                }

                // Cột Cấp bậc
                if (col == 2 && val instanceof String) {
                    setFont(t.getFont().deriveFont(Font.BOLD));
                    int score = (int) tableModel.getValueAt(row, 3);
                    setIcon(GameUiIcons.rankTierIcon(score, 16));
                    setHorizontalAlignment(SwingConstants.LEFT);
                    if (!isSel) {
                        c.setForeground(UITheme.getRankColor(score));
                    }
                }

                // Cột Điểm
                if (col == 3) {
                    setFont(t.getFont().deriveFont(Font.BOLD));
                }

                // Cột Trạng thái vẽ chấm tròn vector chuẩn AWT (không bao giờ bị lỗi ô vuông font)
                if (col == 4 && val instanceof String) {
                    setFont(t.getFont().deriveFont(Font.BOLD));
                    String rawStatus = (String) val;
                    if (Player.STATUS_IDLE.equals(rawStatus) || rawStatus.contains("Sẵn sàng")) {
                        setIcon(new UITheme.StatusDotIcon(UITheme.COLOR_SUCCESS, 8));
                        setText("Sẵn sàng");
                        if (!isSel) c.setForeground(UITheme.COLOR_SUCCESS);
                    } else if (Player.STATUS_QUEUED.equals(rawStatus) || rawStatus.contains("tìm trận")) {
                        setIcon(new UITheme.StatusDotIcon(UITheme.COLOR_WARNING, 8));
                        setText("Đang tìm trận");
                        if (!isSel) c.setForeground(UITheme.COLOR_WARNING);
                    } else if (Player.STATUS_PLAYING.equals(rawStatus) || rawStatus.contains("thi đấu")) {
                        setIcon(new UITheme.StatusDotIcon(UITheme.COLOR_DANGER, 8));
                        setText("Đang thi đấu");
                        if (!isSel) c.setForeground(UITheme.COLOR_DANGER);
                    } else if (Player.STATUS_CHALLENGING.equals(rawStatus) || rawStatus.contains("thách đấu") || rawStatus.contains("mời")) {
                        setIcon(new UITheme.StatusDotIcon(UITheme.COLOR_WARNING, 8));
                        setText("Đang mời");
                        if (!isSel) c.setForeground(UITheme.COLOR_WARNING);
                    }
                }

                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // Nháy vào ô người chơi để thách đấu trực tiếp
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    performChallengeAtRow(row);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        UITheme.styleScrollPane(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1));
        scrollPane.getViewport().setBackground(UITheme.withAlpha(UITheme.BG_PANEL, 158));
        add(scrollPane, BorderLayout.CENTER);

        // Chân bảng: dòng hướng dẫn nháy vào người chơi
        JPanel actionPanel = new GlassPanel(new BorderLayout(), UITheme.BG_CARD, 150);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        lblHint = new JLabel("• Nháy chuột vào người chơi để gửi lời thách đấu");
        lblHint.setFont(UITheme.FONT_SMALL);
        lblHint.setForeground(UITheme.TEXT_MUTED);
        lblHint.setToolTipText("Nháy chuột vào người chơi để gửi lời thách đấu");
        actionPanel.add(lblHint);

        add(actionPanel, BorderLayout.SOUTH);
    }

    /** Lấy người chơi ở dòng được chọn, xác nhận với người dùng rồi gửi lời mời thách đấu. */
    public void performChallengeAtRow(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) {
            return;
        }

        if (isChallenging) {
            JOptionPane.showMessageDialog(this,
                    "Bạn đang chờ phản hồi lời mời thách đấu trước đó!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int targetUserId = (int) tableModel.getValueAt(selectedRow, 0);
        String targetUsername = (String) tableModel.getValueAt(selectedRow, 1);
        String status = (String) tableModel.getValueAt(selectedRow, 4);

        if (Player.STATUS_QUEUED.equals(status) || status.contains("tìm trận")) {
            JOptionPane.showMessageDialog(this,
                    "Người chơi [" + targetUsername + "] hiện đang trong hàng đợi tìm trận tự động!",
                    "Không thể thách đấu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Player.STATUS_IDLE.equals(status) && !status.contains("Sẵn sàng")) {
            JOptionPane.showMessageDialog(this,
                    "Người chơi [" + targetUsername + "] hiện đang bận hoặc đang thi đấu!",
                    "Không thể thách đấu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (ChallengeConfirmDialog.confirm(this, targetUsername)) {
            isChallenging = true;
            btnChallenge.setEnabled(false);
            btnChallenge.setText("Đang chờ phản hồi...");
            lblHint.setText("Đang đợi [" + targetUsername + "] phản hồi lời mời...");
            lblHint.setForeground(UITheme.COLOR_WARNING);
            connection.sendMessage(JsonProtocol.createChallenge(targetUserId, targetUsername));
        }
    }

    /** Xóa trạng thái đang gửi lời mời để người chơi có thể thao tác thách đấu tiếp. */
    public void resetChallengeState() {
        this.isChallenging = false;
        btnChallenge.setEnabled(true);
        btnChallenge.setText("Thách đấu");
        if (lblHint != null) {
            lblHint.setText("• Nháy chuột vào người chơi để gửi lời thách đấu");
            lblHint.setForeground(UITheme.TEXT_MUTED);
        }
    }

    /** Gửi GET_ONLINE_PLAYERS; bảng chỉ thay đổi khi nhận phản hồi từ server. */
    public void refreshOnlinePlayers() {
        connection.sendMessage(JsonProtocol.createGetOnlinePlayers());
    }

    /** Thay danh sách hiển thị bằng dữ liệu người chơi mới nhất do server cung cấp. */
    public void updatePlayers(List<Player> players) {
        tableModel.setRowCount(0);
        if (players != null) {
            for (Player p : players) {
                tableModel.addRow(new Object[]{
                        p.getUserId(),
                        p.getUsername(),
                        UITheme.getRankTitle(p.getTotalScore()),
                        p.getTotalScore(),
                        p.getStatus()
                });
            }
        }
        if (!isChallenging) {
            resetChallengeState();
        }
    }

    public JButton getBtnChallenge() {
        return btnChallenge;
    }
}
