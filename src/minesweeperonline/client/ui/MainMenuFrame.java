package minesweeperonline.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.LeaderboardEntry;
import minesweeperonline.common.model.Player;
import minesweeperonline.common.model.User;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cửa sổ menu chính và sảnh chờ sau khi đăng nhập thành công.
 */
public class MainMenuFrame extends JFrame {

    private final ServerConnection connection;
    private final User currentUser;
    private OnlinePlayersPanel onlinePanel;
    private JLabel lblUserInfo;
    private Timer autoRefreshTimer;
    private LeaderboardDialog leaderboardDialog;
    private MatchHistoryDialog matchHistoryDialog;
    private ServerConnection.MessageListener lobbyListener;

    public MainMenuFrame(ServerConnection connection, User currentUser) {
        this.connection = connection;
        this.currentUser = currentUser;
        initComponents();
        setupNetworkListener();

        // Gửi yêu cầu lấy danh sách người chơi lần đầu
        onlinePanel.refreshOnlinePlayers();

        // Tự động làm mới danh sách online ở sảnh chờ mỗi 4 giây
        autoRefreshTimer = new Timer(4000, e -> {
            if (this.isVisible()) {
                onlinePanel.refreshOnlinePlayers();
            }
        });
        autoRefreshTimer.start();
    }

    private void initComponents() {
        setTitle("Minesweeper Online - Sảnh Chờ (" + currentUser.getUsername() + ")");
        setSize(520, 380);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Header 2 hàng: thông tin người chơi ở trên, các nút ở dưới.
        // Đặt cùng một hàng thì cụm nút đè lên nhãn khi cửa sổ hẹp (520px).
        JPanel headerPanel = new JPanel(new BorderLayout(0, 8));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        headerPanel.setBackground(new Color(245, 248, 255));

        lblUserInfo = new JLabel("Người chơi: " + currentUser.getUsername() + "   |   Điểm số: " + currentUser.getTotalScore());
        lblUserInfo.setFont(new Font("Arial", Font.BOLD, 13));
        lblUserInfo.setForeground(new Color(20, 50, 100));

        JPanel rightHeaderBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rightHeaderBtns.setOpaque(false);

        JButton btnLeaderboard = new JButton("🏆 Bảng xếp hạng");
        btnLeaderboard.setBackground(new Color(255, 250, 230));
        btnLeaderboard.addActionListener(e -> {
            if (leaderboardDialog == null || !leaderboardDialog.isDisplayable()) {
                leaderboardDialog = new LeaderboardDialog(this, connection, currentUser.getUsername());
                leaderboardDialog.setVisible(true);
            } else {
                leaderboardDialog.toFront();
                leaderboardDialog.refresh();
            }
        });

        JButton btnMatchHistory = new JButton("📜 Lịch sử đấu");
        btnMatchHistory.setBackground(new Color(240, 248, 255));
        btnMatchHistory.addActionListener(e -> {
            if (matchHistoryDialog == null || !matchHistoryDialog.isDisplayable()) {
                matchHistoryDialog = new MatchHistoryDialog(this, connection, currentUser.getId());
                matchHistoryDialog.setVisible(true);
            } else {
                matchHistoryDialog.toFront();
                matchHistoryDialog.refresh();
            }
        });

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.addActionListener(e -> {
            connection.sendMessage(JsonProtocol.createLogout());
            if (autoRefreshTimer != null) autoRefreshTimer.stop();
            if (leaderboardDialog != null && leaderboardDialog.isDisplayable()) {
                leaderboardDialog.dispose();
            }
            if (matchHistoryDialog != null && matchHistoryDialog.isDisplayable()) {
                matchHistoryDialog.dispose();
            }
            this.dispose();
            LoginFrame loginFrame = new LoginFrame(connection);
            loginFrame.setVisible(true);
        });

        rightHeaderBtns.add(btnLeaderboard);
        rightHeaderBtns.add(btnMatchHistory);
        rightHeaderBtns.add(btnLogout);
        headerPanel.add(lblUserInfo, BorderLayout.NORTH);
        headerPanel.add(rightHeaderBtns, BorderLayout.CENTER);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Ở giữa: bảng người chơi online
        onlinePanel = new OnlinePlayersPanel(connection);
        mainPanel.add(onlinePanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    @Override
    public void dispose() {
        // Nếu không gỡ listener, sảnh chờ cũ vẫn tiếp tục bật dialog CHALLENGE_REQUEST sau khi đăng xuất
        connection.removeMessageListener(lobbyListener);
        super.dispose();
    }

    private void setupNetworkListener() {
        lobbyListener = message -> {
            SwingUtilities.invokeLater(() -> handleServerMessage(message));
        };
        connection.addMessageListener(lobbyListener);
    }

    private void handleServerMessage(Message message) {
        if (message == null || message.getType() == null) return;

        MessageType type = message.getType();

        switch (type) {
            case ONLINE_PLAYERS:
                handleOnlinePlayers(message);
                break;

            case CHALLENGE_REQUEST:
                handleChallengeRequest(message);
                break;

            case MATCH_CREATED:
                handleMatchCreated(message);
                break;

            case REJECT:
                handleChallengeRejected(message);
                break;

            case LEADERBOARD:
                handleLeaderboard(message);
                break;

            case MATCH_HISTORY:
                handleMatchHistory(message);
                break;

            case ERROR:
                String err = message.getPayload().has("errorMessage") ?
                        message.getPayload().get("errorMessage").getAsString() : "Đã có lỗi xảy ra!";
                JOptionPane.showMessageDialog(this, err, "Thông báo từ máy chủ", JOptionPane.WARNING_MESSAGE);
                onlinePanel.getBtnChallenge().setEnabled(true);
                onlinePanel.getBtnChallenge().setText("Thách đấu");
                break;

            default:
                break;
        }
    }

    private void handleOnlinePlayers(Message message) {
        JsonObject payload = message.getPayload();
        List<Player> players = new ArrayList<>();
        if (payload.has("players") && payload.get("players").isJsonArray()) {
            JsonArray arr = payload.getAsJsonArray("players");
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    int id = obj.get("userId").getAsInt();
                    String name = obj.get("username").getAsString();
                    int score = obj.get("totalScore").getAsInt();
                    String status = obj.has("status") ? obj.get("status").getAsString() : Player.STATUS_IDLE;

                    Player p = new Player(id, name, score);
                    p.setStatus(status);
                    players.add(p);
                }
            }
        }
        onlinePanel.updatePlayers(players);
    }

    private void handleChallengeRequest(Message message) {
        JsonObject payload = message.getPayload();
        int fromUserId = payload.get("fromUserId").getAsInt();
        String fromUsername = payload.get("fromUsername").getAsString();

        ChallengeDialog dialog = new ChallengeDialog(this, connection, fromUserId, fromUsername);
        dialog.setVisible(true);
    }

    private GameFrame currentGameFrame;

    private void handleMatchCreated(Message message) {
        JsonObject payload = message.getPayload();
        String gameId = payload.get("gameId").getAsString();
        String opponentName = payload.has("opponentUsername") ?
                payload.get("opponentUsername").getAsString() : "Đối thủ";

        onlinePanel.getBtnChallenge().setEnabled(true);
        onlinePanel.getBtnChallenge().setText("Thách đấu");

        if (currentGameFrame != null && currentGameFrame.isDisplayable()) {
            currentGameFrame.dispose();
        }

        currentGameFrame = new GameFrame(connection, currentUser, opponentName, gameId, this);
        currentGameFrame.setVisible(true);
        this.setVisible(false);
    }

    private void handleChallengeRejected(Message message) {
        JsonObject payload = message.getPayload();
        String rejectedByName = payload.has("rejectedByUsername") ?
                payload.get("rejectedByUsername").getAsString() : "Người chơi";

        JOptionPane.showMessageDialog(this,
                rejectedByName + " đã từ chối lời mời thách đấu của bạn.",
                "Thách đấu bị từ chối", JOptionPane.INFORMATION_MESSAGE);

        onlinePanel.getBtnChallenge().setEnabled(true);
        onlinePanel.getBtnChallenge().setText("Thách đấu");
        onlinePanel.refreshOnlinePlayers();
    }

    private void handleLeaderboard(Message message) {
        JsonObject payload = message.getPayload();
        List<LeaderboardEntry> list = new ArrayList<>();
        if (payload != null && payload.has("leaderboard") && payload.get("leaderboard").isJsonArray()) {
            JsonArray arr = payload.getAsJsonArray("leaderboard");
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject o = el.getAsJsonObject();
                    int rank = o.get("rank").getAsInt();
                    int userId = o.get("userId").getAsInt();
                    String uname = o.get("username").getAsString();
                    int score = o.get("totalScore").getAsInt();
                    int wins = o.get("wins").getAsInt();
                    int matches = o.get("totalMatches").getAsInt();
                    list.add(new LeaderboardEntry(rank, userId, uname, score, wins, matches));
                }
            }
        }

        if (leaderboardDialog != null && leaderboardDialog.isDisplayable()) {
            leaderboardDialog.updateLeaderboard(list);
        }
    }

    private void handleMatchHistory(Message message) {
        if (matchHistoryDialog != null && matchHistoryDialog.isDisplayable()) {
            matchHistoryDialog.updateHistoryFromMessage(message);
        }
    }
}
