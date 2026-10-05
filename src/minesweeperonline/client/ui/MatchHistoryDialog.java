package minesweeperonline.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.MatchHistoryEntry;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog displaying player's recent match history and performance statistics.
 */
public class MatchHistoryDialog extends JDialog {

    private final ServerConnection connection;
    private final int currentUserId;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;
    private JLabel lblSummary;

    public MatchHistoryDialog(Frame owner, ServerConnection connection, int currentUserId) {
        super(owner, "Lịch Sử Đấu - Minesweeper Online", false);
        this.connection = connection;
        this.currentUserId = currentUserId;

        initComponents();
        refresh();
    }

    private void initComponents() {
        setSize(780, 520);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(8, 8));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 10, 16));
        headerPanel.setBackground(new Color(245, 248, 255));

        JLabel lblTitle = new JLabel("📜 LỊCH SỬ THI ĐẤU", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(new Color(20, 60, 140));

        JLabel lblSubtitle = new JLabel("Chi tiết kết quả, đối thủ và điểm số các trận gần nhất của bạn", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(100, 110, 130));

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"Thời gian", "Đối thủ", "Kết quả", "Điểm số", "Thời lượng", "Ô an toàn", "Cờ / Thao tác", "Ghi chú"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(230, 238, 248));
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(125); // Thoi gian
        table.getColumnModel().getColumn(1).setPreferredWidth(95);  // Doi thu
        table.getColumnModel().getColumn(2).setPreferredWidth(95);  // Ket qua
        table.getColumnModel().getColumn(3).setPreferredWidth(70);  // Diem
        table.getColumnModel().getColumn(4).setPreferredWidth(75);  // Thoi luong
        table.getColumnModel().getColumn(5).setPreferredWidth(80);  // O an toan
        table.getColumnModel().getColumn(6).setPreferredWidth(100); // Co / Thao tac
        table.getColumnModel().getColumn(7).setPreferredWidth(140); // Ghi chu

        // Custom Cell Renderer
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);

                if (!isSel) {
                    if (row % 2 == 1) {
                        c.setBackground(new Color(248, 249, 252));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }

                // Format Result column (Col 2)
                if (col == 2 && val != null) {
                    String res = val.toString();
                    setFont(getFont().deriveFont(Font.BOLD));
                    if ("VICTORY".equalsIgnoreCase(res) || res.contains("THẮNG")) {
                        c.setForeground(new Color(39, 174, 96));
                    } else if ("DEFEAT".equalsIgnoreCase(res) || res.contains("THUA")) {
                        c.setForeground(new Color(214, 48, 49));
                    } else if ("FORFEIT".equalsIgnoreCase(res) || res.contains("BỎ CUỘC")) {
                        c.setForeground(new Color(230, 126, 34));
                    } else {
                        c.setForeground(new Color(41, 128, 185)); // Draw
                    }
                } else if (col == 3 && val != null) { // Score column
                    String score = val.toString();
                    setFont(getFont().deriveFont(Font.BOLD));
                    if (score.startsWith("+")) {
                        c.setForeground(new Color(39, 174, 96));
                    } else if (score.startsWith("-")) {
                        c.setForeground(new Color(214, 48, 49));
                    } else {
                        c.setForeground(new Color(52, 73, 94));
                    }
                } else if (col == 1) {
                    setHorizontalAlignment(SwingConstants.LEFT);
                    c.setForeground(new Color(44, 62, 80));
                } else if (col == 7) {
                    setHorizontalAlignment(SwingConstants.LEFT);
                    c.setForeground(new Color(120, 120, 120));
                } else {
                    c.setForeground(new Color(44, 62, 80));
                }

                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Summary & Buttons Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));

        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        lblSummary = new JLabel("Tổng số trận: 0 | Thắng: 0 | Thua: 0 | Tỉ lệ thắng: 0%");
        lblSummary.setFont(new Font("Arial", Font.BOLD, 12));
        lblSummary.setForeground(new Color(30, 50, 90));

        lblStatus = new JLabel("Đang tải lịch sử đấu...");
        lblStatus.setFont(new Font("Arial", Font.ITALIC, 11));
        lblStatus.setForeground(Color.GRAY);

        statsPanel.add(lblSummary);
        statsPanel.add(lblStatus);
        bottomPanel.add(statsPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnRefresh = new JButton("🔄 Làm mới");
        btnRefresh.setFont(new Font("Arial", Font.PLAIN, 12));
        btnRefresh.addActionListener(e -> refresh());

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Arial", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dispose());

        btnPanel.add(btnRefresh);
        btnPanel.add(btnClose);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void refresh() {
        lblStatus.setText("Đang yêu cầu dữ liệu từ máy chủ...");
        if (connection != null && connection.isConnected()) {
            connection.sendMessage(JsonProtocol.createGetMatchHistory(50));
        } else {
            lblStatus.setText("Mất kết nối tới máy chủ!");
        }
    }

    /**
     * Parses MATCH_HISTORY Message from server and updates the table.
     */
    public void updateHistoryFromMessage(Message message) {
        if (message == null || message.getPayload() == null) return;
        JsonObject payload = message.getPayload();
        List<MatchHistoryEntry> entries = new ArrayList<>();

        if (payload.has("history") && payload.get("history").isJsonArray()) {
            JsonArray arr = payload.getAsJsonArray("history");
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    MatchHistoryEntry entry = JsonProtocol.getGson().fromJson(el, MatchHistoryEntry.class);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }
        }
        updateHistory(entries);
    }

    /**
     * Updates the UI table with the provided list of entries.
     */
    public void updateHistory(List<MatchHistoryEntry> entries) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            if (entries == null || entries.isEmpty()) {
                lblStatus.setText("Chưa có trận đấu nào được ghi nhận.");
                lblSummary.setText("Hãy tham gia thi đấu để lưu lại lịch sử chiến tích!");
                return;
            }

            int wins = 0;
            int defeats = 0;
            int draws = 0;

            for (MatchHistoryEntry e : entries) {
                String resultDisplay;
                String res = e.getResult();
                if ("VICTORY".equalsIgnoreCase(res)) {
                    resultDisplay = "CHIẾN THẮNG";
                    wins++;
                } else if ("DEFEAT".equalsIgnoreCase(res)) {
                    resultDisplay = "THẤT BẠI";
                    defeats++;
                } else if ("FORFEIT".equalsIgnoreCase(res)) {
                    resultDisplay = "BỎ CUỘC";
                    defeats++;
                } else {
                    resultDisplay = "HÒA";
                    draws++;
                }

                String scoreDisplay = (e.getScoreDelta() > 0 ? "+" : "") + e.getScoreDelta();
                String flagsAndActions = "🚩 " + e.getFlagsPlaced() + " | ⚡ " + e.getTotalActions();
                String safeCells = e.getOpenedSafeCells() + " / 121";

                tableModel.addRow(new Object[]{
                        e.getPlayedAt(),
                        "👤 " + e.getOpponentName(),
                        resultDisplay,
                        scoreDisplay,
                        e.getFormattedDuration(),
                        safeCells,
                        flagsAndActions,
                        translateOutcome(e.getOutcomeReason())
                });
            }

            int total = entries.size();
            double winRate = (total > 0) ? (wins * 100.0 / total) : 0.0;
            lblSummary.setText(String.format("Tổng số trận: %d | Thắng: %d | Thua: %d | Hòa: %d | Tỉ lệ thắng: %.1f%%",
                    total, wins, defeats, draws, winRate));
            lblStatus.setText("Cập nhật lúc: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
        });
    }

    private String translateOutcome(String reason) {
        if (reason == null || reason.isEmpty()) return "";
        if (reason.contains("MINE")) return "Mở trúng mìn";
        if (reason.contains("TIMEOUT")) return "Hết 5 phút thi đấu";
        if (reason.contains("FORFEIT_DISCONNECT")) return "Mất kết nối";
        if (reason.contains("FORFEIT")) return "Đầu hàng giữa chừng";
        if (reason.contains("CLEARED")) return "Mở sạch 121 ô an toàn";
        return reason;
    }
}
