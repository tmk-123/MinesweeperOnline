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
    private LeaderboardPanel leaderboardPanel;
    private JLabel lblUserInfo;
    private JButton btnQuickMatch;
    private boolean isSearchingMatch = false;
    private Timer autoRefreshTimer;
    private LeaderboardDialog leaderboardDialog;
    private MatchHistoryDialog matchHistoryDialog;
    private ServerConnection.MessageListener lobbyListener;

    public MainMenuFrame(ServerConnection connection, User currentUser) {
        this.connection = connection;
        this.currentUser = currentUser;
        initComponents();
        setupNetworkListener();

        // Gửi yêu cầu lấy danh sách người chơi online và BXH ban đầu
        onlinePanel.refreshOnlinePlayers();
        if (leaderboardPanel != null) {
            leaderboardPanel.refreshLeaderboard();
        }

        // Tự động làm mới danh sách online và BXH ở sảnh chờ mỗi 4 giây
        autoRefreshTimer = new Timer(4000, e -> {
            if (this.isVisible()) {
                onlinePanel.refreshOnlinePlayers();
                if (leaderboardPanel != null) {
                    leaderboardPanel.refreshLeaderboard();
                }
            }
        });
        autoRefreshTimer.start();
    }

    private void initComponents() {
        setTitle("Minesweeper Online - Sảnh Chờ (" + currentUser.getUsername() + ")");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Mở toàn màn hình theo yêu cầu người dùng

        JPanel mainPanel = new MinesweeperBackgroundPanel(MinesweeperBackgroundPanel.Scene.LOBBY, new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        // Header phía trên: Thông tin người chơi bên trái, Nút thao tác bên phải
        JPanel headerPanel = new GlassPanel(new BorderLayout(15, 0), UITheme.BG_CARD, 150);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));

        String rank = UITheme.getRankTitle(currentUser.getTotalScore());
        lblUserInfo = new JLabel("Tài khoản: " + currentUser.getUsername() + "   |   Hạng: " + rank + "   |   Điểm số: " + currentUser.getTotalScore());
        lblUserInfo.setFont(UITheme.FONT_TITLE);
        lblUserInfo.setForeground(UITheme.COLOR_PRIMARY);
        headerPanel.add(lblUserInfo, BorderLayout.WEST);

        JPanel rightHeaderBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeaderBtns.setOpaque(false);

        btnQuickMatch = new JButton("Tìm trận nhanh");
        UITheme.styleButton(btnQuickMatch, UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_HOVER, new Color(28, 25, 20));
        btnQuickMatch.setToolTipText("Hệ thống tự động tìm và ghép ván với người chơi khác đang online");
        btnQuickMatch.addActionListener(e -> toggleQuickMatch());

        JButton btnRefreshAll = new JButton("Làm mới");
        UITheme.styleOutlineButton(btnRefreshAll, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
        btnRefreshAll.setToolTipText("Làm mới danh sách người chơi online và bảng xếp hạng");
        btnRefreshAll.addActionListener(e -> {
            if (onlinePanel != null) {
                onlinePanel.refreshOnlinePlayers();
            }
            if (leaderboardPanel != null) {
                leaderboardPanel.refreshLeaderboard();
            }
        });

        JButton btnMatchHistory = new JButton("Lịch sử đấu");
        UITheme.styleOutlineButton(btnMatchHistory, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
        btnMatchHistory.addActionListener(e -> {
            if (matchHistoryDialog == null || !matchHistoryDialog.isDisplayable()) {
                matchHistoryDialog = new MatchHistoryDialog(this, connection, currentUser.getId());
                matchHistoryDialog.setVisible(true);
            } else {
                matchHistoryDialog.toFront();
                matchHistoryDialog.refresh();
            }
        });

        JButton btnHelp = new JButton("? Hướng dẫn");
        UITheme.styleOutlineButton(btnHelp, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
        btnHelp.setToolTipText("Xem cẩm nang luật chơi 1v1, quy tắc xếp hạng và mẹo chơi");
        btnHelp.addActionListener(e -> {
            HelpDialog helpDialog = new HelpDialog(this, 0);
            helpDialog.setVisible(true);
        });

        JButton btnLeaderboard = new JButton("Bảng xếp hạng");
        UITheme.styleOutlineButton(btnLeaderboard, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_TITLE);
        btnLeaderboard.addActionListener(e -> {
            if (leaderboardDialog == null || !leaderboardDialog.isDisplayable()) {
                leaderboardDialog = new LeaderboardDialog(this, connection, currentUser.getUsername());
                leaderboardDialog.setVisible(true);
            } else {
                leaderboardDialog.toFront();
                leaderboardDialog.refresh();
            }
        });

        JButton btnLogout = new JButton("Đăng xuất");
        UITheme.styleOutlineButton(btnLogout, new Color(113, 55, 59), new Color(67, 36, 41), UITheme.COLOR_DANGER);
        btnLogout.addActionListener(e -> {
            if (isSearchingMatch) {
                connection.sendMessage(JsonProtocol.createCancelQuickMatch());
                isSearchingMatch = false;
            }
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

        rightHeaderBtns.add(btnQuickMatch);
        rightHeaderBtns.add(btnRefreshAll);
        rightHeaderBtns.add(btnMatchHistory);
        rightHeaderBtns.add(btnHelp);
        rightHeaderBtns.add(btnLeaderboard);
        rightHeaderBtns.add(btnLogout);
        headerPanel.add(rightHeaderBtns, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Ở giữa: Chia đôi màn hình - Bên trái là OnlinePlayersPanel, Bên phải là LeaderboardPanel
        JPanel centerSplitPanel = new JPanel(new GridBagLayout());
        centerSplitPanel.setOpaque(false);

        onlinePanel = new OnlinePlayersPanel(connection);
        leaderboardPanel = new LeaderboardPanel(connection, currentUser.getUsername());

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = 0; left.weightx = 0.55; left.weighty = 1;
        left.fill = GridBagConstraints.BOTH; left.insets = new Insets(0, 0, 0, 8);
        centerSplitPanel.add(onlinePanel, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = 0; right.weightx = 0.45; right.weighty = 1;
        right.fill = GridBagConstraints.BOTH; right.insets = new Insets(0, 8, 0, 0);
        centerSplitPanel.add(leaderboardPanel, right);

        mainPanel.add(centerSplitPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void toggleQuickMatch() {
        if (!isSearchingMatch) {
            isSearchingMatch = true;
            btnQuickMatch.setText("Đang tìm trận... (Hủy)");
            btnQuickMatch.setBackground(UITheme.COLOR_WARNING);
            connection.sendMessage(JsonProtocol.createQuickMatch());
        } else {
            isSearchingMatch = false;
            btnQuickMatch.setText("Tìm trận nhanh");
            btnQuickMatch.setBackground(UITheme.COLOR_PRIMARY);
            connection.sendMessage(JsonProtocol.createCancelQuickMatch());
        }
    }

    @Override
    public void dispose() {
        if (isSearchingMatch) {
            connection.sendMessage(JsonProtocol.createCancelQuickMatch());
            isSearchingMatch = false;
        }
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

            case GAME_RECONNECTED:
                handleGameReconnected(message);
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
                onlinePanel.resetChallengeState();
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
    // Trạng thái và kích thước cửa sổ trận đấu (mặc định mở đầu phóng to toàn màn hình)
    private int lastGameExtendedState = Frame.MAXIMIZED_BOTH;
    private Rectangle lastGameBounds = null;

    public void saveGameWindowState(int extendedState, Rectangle bounds) {
        this.lastGameExtendedState = extendedState;
        if (bounds != null && bounds.width > 200 && bounds.height > 200) {
            this.lastGameBounds = new Rectangle(bounds);
        }
    }

    private void handleMatchCreated(Message message) {
        JsonObject payload = message.getPayload();
        String gameId = payload.get("gameId").getAsString();
        String opponentName = payload.has("opponentUsername") ?
                payload.get("opponentUsername").getAsString() : "Đối thủ";

        onlinePanel.resetChallengeState();
        if (isSearchingMatch) {
            isSearchingMatch = false;
            btnQuickMatch.setText("Tìm trận nhanh");
            btnQuickMatch.setBackground(UITheme.COLOR_PRIMARY);
        }

        int targetState = lastGameExtendedState;
        Rectangle targetBounds = lastGameBounds;

        if (currentGameFrame != null && currentGameFrame.isDisplayable()) {
            targetState = currentGameFrame.getCurrentExtendedState();
            targetBounds = currentGameFrame.getNormalBounds();
            lastGameExtendedState = targetState;
            lastGameBounds = targetBounds;
            currentGameFrame.dispose();
        }

        currentGameFrame = new GameFrame(connection, currentUser, opponentName, gameId, this, targetState, targetBounds);
        currentGameFrame.setVisible(true);
        this.setVisible(false);
    }

    private void handleGameReconnected(Message message) {
        JsonObject payload = message.getPayload();
        String gameId = payload.get("gameId").getAsString();
        String opponentName = payload.has("opponentUsername") ?
                payload.get("opponentUsername").getAsString() : "Đối thủ";

        onlinePanel.resetChallengeState();
        if (isSearchingMatch) {
            isSearchingMatch = false;
            btnQuickMatch.setText("Tìm trận nhanh");
            btnQuickMatch.setBackground(UITheme.COLOR_PRIMARY);
        }

        int targetState = lastGameExtendedState;
        Rectangle targetBounds = lastGameBounds;

        if (currentGameFrame != null && currentGameFrame.isDisplayable()) {
            targetState = currentGameFrame.getCurrentExtendedState();
            targetBounds = currentGameFrame.getNormalBounds();
            lastGameExtendedState = targetState;
            lastGameBounds = targetBounds;
            currentGameFrame.dispose();
        }

        currentGameFrame = new GameFrame(connection, currentUser, opponentName, gameId, this, targetState, targetBounds);
        currentGameFrame.restoreGameState(payload);
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

        onlinePanel.resetChallengeState();
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

        if (leaderboardPanel != null) {
            leaderboardPanel.updateLeaderboard(list);
        }
        if (leaderboardDialog != null && leaderboardDialog.isDisplayable()) {
            leaderboardDialog.updateLeaderboard(list);
        }
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            if (onlinePanel != null) onlinePanel.refreshOnlinePlayers();
            if (leaderboardPanel != null) leaderboardPanel.refreshLeaderboard();
        }
    }

    private void handleMatchHistory(Message message) {
        if (matchHistoryDialog != null && matchHistoryDialog.isDisplayable()) {
            matchHistoryDialog.updateHistoryFromMessage(message);
        }
    }
}
