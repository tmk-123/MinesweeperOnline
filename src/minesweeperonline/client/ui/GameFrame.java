package minesweeperonline.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.client.sound.SoundManager;
import minesweeperonline.common.model.User;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cửa sổ chơi chính cho các trận đấu 12x12 đang diễn ra.
 * Hiển thị bàn cờ của người chơi, bàn cờ thu nhỏ của đối thủ, thống kê trận và đồng hồ thời gian thực.
 */
public class GameFrame extends JFrame {

    private final ServerConnection connection;
    private final User currentUser;
    private final String opponentName;
    private final String gameId;
    private final JFrame parentLobby;

    private PlayerInfoPanel infoPanel;
    private GameBoardPanel boardPanel;
    private OpponentMiniBoardPanel miniBoardPanel;
    private JLabel lblTimer;
    private JLabel lblStatus;
    private JButton btnSound;
    private JButton btnForfeit;

    private ResultDialog activeResultDialog;
    private ServerConnection.MessageListener gameMessageListener;
    private DisconnectOverlayPanel disconnectOverlay;

    private Timer gameClock;
    private Timer readyCountdownTimer;
    private Timer opponentDisconnectTimer;
    private int opponentDisconnectSeconds = 15;
    private int readySeconds = 5;
    private int remainingSeconds = 300;
    private int mySafeCells = 0;
    private int myFlags = 0;
    private int myActions = 0;
    private int oppSafeCells = 0;
    private int oppFlags = 0;
    private int oppActions = 0;

    private Rectangle normalBounds;

    public GameFrame(ServerConnection connection, User currentUser, String opponentName, String gameId, JFrame parentLobby) {
        this(connection, currentUser, opponentName, gameId, parentLobby, Frame.MAXIMIZED_BOTH, null);
    }

    public GameFrame(ServerConnection connection, User currentUser, String opponentName, String gameId, JFrame parentLobby,
                     int initialExtendedState, Rectangle initialNormalBounds) {
        this.connection = connection;
        this.currentUser = currentUser;
        this.opponentName = opponentName;
        this.gameId = gameId;
        this.parentLobby = parentLobby;

        initComponents(initialExtendedState, initialNormalBounds);
        setupNetworkListener();
        startReadyCountdownVisual();
    }

    private void initComponents(int initialExtendedState, Rectangle initialNormalBounds) {
        setTitle("Minesweeper Online - Trận đấu [" + gameId + "] (" + currentUser.getUsername() + " vs " + opponentName + ")");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        if (initialNormalBounds != null && initialNormalBounds.width > 200 && initialNormalBounds.height > 200) {
            setBounds(initialNormalBounds);
            this.normalBounds = new Rectangle(initialNormalBounds);
        } else {
            setSize(820, 740);
            setLocationRelativeTo(null);
            this.normalBounds = getBounds();
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateNormalBounds();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                updateNormalBounds();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (btnForfeit != null && btnForfeit.isEnabled()) {
                    btnForfeit.doClick();
                }
            }
        });

        if ((initialExtendedState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            setExtendedState(Frame.MAXIMIZED_BOTH);
        } else {
            setExtendedState(Frame.NORMAL);
        }

        JPanel mainPanel = new JPanel(new BorderLayout(10, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // 1. Phía trên (North): thống kê người chơi
        infoPanel = new PlayerInfoPanel(currentUser.getUsername(), opponentName);
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // 2. Ở giữa (Center): bàn cờ 12x12 của người chơi + bàn cờ thu nhỏ của đối thủ
        JPanel boardsContainer = new JPanel(new BorderLayout(12, 0));
        boardPanel = new GameBoardPanel(connection, gameId);
        miniBoardPanel = new OpponentMiniBoardPanel();

        boardsContainer.add(boardPanel, BorderLayout.CENTER);
        boardsContainer.add(miniBoardPanel, BorderLayout.EAST);
        mainPanel.add(boardsContainer, BorderLayout.CENTER);

        // 3. Phía dưới (South): trạng thái, đồng hồ và nút đầu hàng
        JPanel southPanel = new JPanel(new BorderLayout(10, 5));
        southPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        southPanel.setBackground(UITheme.BG_CARD);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        statusPanel.setOpaque(false);

        lblTimer = new JLabel("Thời gian: 05:00");
        lblTimer.setFont(UITheme.FONT_TITLE);
        lblTimer.setForeground(UITheme.COLOR_DANGER);

        lblStatus = new JLabel("Trạng thái: CHUẨN BỊ (5s)");
        lblStatus.setFont(UITheme.FONT_BOLD);
        lblStatus.setForeground(UITheme.COLOR_PRIMARY);

        statusPanel.add(lblTimer);
        statusPanel.add(lblStatus);

        JPanel actionControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionControlPanel.setOpaque(false);

        btnSound = new JButton(SoundManager.getInstance().isMuted() ? "Âm thanh: Tắt" : "Âm thanh: Bật");
        UITheme.styleOutlineButton(btnSound, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY, UITheme.TEXT_BODY);
        btnSound.addActionListener(e -> {
            boolean muted = SoundManager.getInstance().toggleMute();
            btnSound.setText(muted ? "Âm thanh: Tắt" : "Âm thanh: Bật");
            if (!muted) {
                SoundManager.getInstance().playClick();
            }
        });

        btnForfeit = new JButton("Đầu hàng");
        UITheme.styleButton(btnForfeit, new Color(254, 226, 226), new Color(254, 202, 202), UITheme.COLOR_DANGER);
        btnForfeit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn đầu hàng không?\n(Bạn sẽ bị xử thua và trừ 5 điểm)",
                    "Xác nhận đầu hàng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                connection.sendMessage(JsonProtocol.createForfeit(gameId));
            }
        });

        actionControlPanel.add(btnSound);
        actionControlPanel.add(btnForfeit);

        southPanel.add(statusPanel, BorderLayout.CENTER);
        southPanel.add(actionControlPanel, BorderLayout.EAST);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Khởi tạo Overlay che màn hình khi đối thủ mất kết nối
        disconnectOverlay = new DisconnectOverlayPanel();
        setGlassPane(disconnectOverlay);
        disconnectOverlay.setVisible(false);
    }

    private void setupNetworkListener() {
        gameMessageListener = message -> {
            SwingUtilities.invokeLater(() -> handleServerGameMessage(message));
        };
        connection.addMessageListener(gameMessageListener);
    }

    private void handleServerGameMessage(Message message) {
        if (message == null || message.getType() == null) return;

        MessageType type = message.getType();

        switch (type) {
            case GAME_START:
                onGameStart();
                break;

            case CELL_OPENED:
                onCellOpened(message.getPayload());
                break;

            case FLAG_PLACED:
                onFlagPlaced(message.getPayload());
                break;

            case FLAG_REMOVED:
                onFlagRemoved(message.getPayload());
                break;

            case GAME_UPDATE:
                onGameUpdate(message.getPayload());
                break;

            case GAME_WIN:
                onGameEnd("BẠN ĐÃ CHIẾN THẮNG!", message.getPayload(), true);
                break;

            case GAME_LOSE:
                onGameEnd("BẠN ĐÃ THẤT BẠI!", message.getPayload(), false);
                break;

            case GAME_DRAW:
                onGameEnd("KẾT QUẢ HÒA!", message.getPayload(), false);
                break;

            case OPPONENT_DISCONNECTED:
                JsonObject pDc = message.getPayload();
                if (pDc != null && "WAITING_RECONNECT".equals(pDc.has("status") ? pDc.get("status").getAsString() : "")) {
                    int sec = pDc.has("countdownSeconds") ? pDc.get("countdownSeconds").getAsInt() : 15;
                    String dcName = pDc.has("disconnectedUsername") ? pDc.get("disconnectedUsername").getAsString() : opponentName;
                    startOpponentDisconnectCountdown(sec, dcName);
                } else {
                    hideDisconnectOverlay();
                    if (opponentDisconnectTimer != null) opponentDisconnectTimer.stop();
                    onGameEnd("BẠN ĐÃ CHIẾN THẮNG!", message.getPayload(), true);
                }
                break;

            case OPPONENT_RECONNECTED:
                hideDisconnectOverlay();
                onOpponentReconnected(message.getPayload());
                break;

            case PLAY_AGAIN:
                if (activeResultDialog != null && activeResultDialog.isDisplayable()) {
                    JsonObject p = message.getPayload();
                    if (p != null && "REQUESTED".equals(p.has("status") ? p.get("status").getAsString() : "")) {
                        String reqName = p.has("requestedByUsername") ? p.get("requestedByUsername").getAsString() : opponentName;
                        activeResultDialog.setOpponentWantsRematch(reqName);
                    }
                }
                break;

            case EXIT:
                if (activeResultDialog != null && activeResultDialog.isDisplayable()) {
                    activeResultDialog.setOpponentExited();
                }
                break;

            default:
                break;
        }
    }

    private void startReadyCountdownVisual() {
        readySeconds = 5;
        lblStatus.setText("Trạng thái: CHUẨN BỊ (Bắt đầu sau " + readySeconds + "s...)");
        lblStatus.setForeground(new Color(220, 100, 0));
        SoundManager.getInstance().playTick();

        readyCountdownTimer = new Timer(1000, e -> {
            readySeconds--;
            if (readySeconds > 0) {
                lblStatus.setText("Trạng thái: CHUẨN BỊ (Bắt đầu sau " + readySeconds + "s...)");
                SoundManager.getInstance().playTick();
            } else {
                lblStatus.setText("Trạng thái: ĐANG BẮT ĐẦU...");
                if (readyCountdownTimer != null) readyCountdownTimer.stop();
            }
        });
        readyCountdownTimer.start();
    }

    private void onGameStart() {
        if (readyCountdownTimer != null) {
            readyCountdownTimer.stop();
        }
        lblStatus.setText("Trạng thái: ĐANG THI ĐẤU");
        lblStatus.setForeground(UITheme.COLOR_SUCCESS);
        boardPanel.setInteractive(true);

        remainingSeconds = 300;
        if (gameClock != null) gameClock.stop();
        gameClock = new Timer(1000, e -> {
            remainingSeconds--;
            int mins = remainingSeconds / 60;
            int secs = remainingSeconds % 60;
            lblTimer.setText(String.format("Thời gian: %02d:%02d", mins, secs));
            if (remainingSeconds <= 5 && remainingSeconds > 0) {
                SoundManager.getInstance().playTick();
            }
            if (remainingSeconds <= 0) {
                gameClock.stop();
            }
        });
        gameClock.start();
    }

    private void onCellOpened(JsonObject p) {
        boolean exploded = false;
        if (p.has("openedCells")) {
            JsonArray arr = p.getAsJsonArray("openedCells");
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                int r = o.get("row").getAsInt();
                int c = o.get("col").getAsInt();
                int adj = o.get("adjacentMines").getAsInt();
                boolean isMine = o.get("isMine").getAsBoolean();
                boolean isExp = o.get("isExploded").getAsBoolean();
                boardPanel.revealCell(r, c, adj, isMine, isExp);
                if (isExp) exploded = true;
            }
        }
        if (p.has("openedSafeCells")) {
            mySafeCells = p.get("openedSafeCells").getAsInt();
        }
        if (p.has("totalActions")) {
            myActions = p.get("totalActions").getAsInt();
        }
        infoPanel.updateSelfStats(mySafeCells, myFlags, myActions);

        if (exploded) {
            SoundManager.getInstance().playExplosion();
        } else {
            SoundManager.getInstance().playClick();
        }
    }

    private void onFlagPlaced(JsonObject p) {
        int r = p.get("row").getAsInt();
        int c = p.get("col").getAsInt();
        boardPanel.setCellFlagged(r, c, true);
        if (p.has("currentFlags")) {
            myFlags = p.get("currentFlags").getAsInt();
        }
        if (p.has("totalActions")) {
            myActions = p.get("totalActions").getAsInt();
        }
        infoPanel.updateSelfStats(mySafeCells, myFlags, myActions);
        SoundManager.getInstance().playFlag();
    }

    private void onFlagRemoved(JsonObject p) {
        int r = p.get("row").getAsInt();
        int c = p.get("col").getAsInt();
        boardPanel.setCellFlagged(r, c, false);
        if (p.has("currentFlags")) {
            myFlags = p.get("currentFlags").getAsInt();
        }
        if (p.has("totalActions")) {
            myActions = p.get("totalActions").getAsInt();
        }
        infoPanel.updateSelfStats(mySafeCells, myFlags, myActions);
        SoundManager.getInstance().playFlag();
    }

    private void onGameUpdate(JsonObject p) {
        if (p.has("opponentOpenedSafeCells")) {
            oppSafeCells = p.get("opponentOpenedSafeCells").getAsInt();
        }
        if (p.has("opponentCurrentFlags")) {
            oppFlags = p.get("opponentCurrentFlags").getAsInt();
        }
        if (p.has("opponentTotalActions")) {
            oppActions = p.get("opponentTotalActions").getAsInt();
        }
        infoPanel.updateOpponentStats(oppSafeCells, oppFlags, oppActions);

        // Cập nhật bàn cờ thu nhỏ của đối thủ
        if (p.has("openedCoords") && p.get("openedCoords").isJsonArray()) {
            JsonArray coords = p.getAsJsonArray("openedCoords");
            for (JsonElement el : coords) {
                if (el.isJsonObject()) {
                    JsonObject co = el.getAsJsonObject();
                    miniBoardPanel.revealCell(co.get("r").getAsInt(), co.get("c").getAsInt());
                }
            }
        }
        if (p.has("flagRow") && p.has("flagCol") && p.has("flagState")) {
            miniBoardPanel.setCellFlagged(p.get("flagRow").getAsInt(), p.get("flagCol").getAsInt(), p.get("flagState").getAsBoolean());
        }
    }

    private void onGameEnd(String titleOutcome, JsonObject p, boolean isWin) {
        hideDisconnectOverlay();
        if (gameClock != null) gameClock.stop();
        if (opponentDisconnectTimer != null) opponentDisconnectTimer.stop();
        boardPanel.setInteractive(false);
        btnForfeit.setEnabled(false);
        lblStatus.setText("Trạng thái: KẾT THÚC");

        if (isWin) {
            SoundManager.getInstance().playVictory();
        } else {
            SoundManager.getInstance().playDefeat();
        }

        // Hiện hết bom trên bàn của mình (server chỉ gửi danh sách mìn khi trận đã kết thúc)
        if (p.has("mines") && p.get("mines").isJsonArray()) {
            List<int[]> mines = new ArrayList<>();
            for (JsonElement el : p.getAsJsonArray("mines")) {
                JsonObject o = el.getAsJsonObject();
                mines.add(new int[]{o.get("r").getAsInt(), o.get("c").getAsInt()});
            }
            boardPanel.revealAllMines(mines);
        }

        String reason = p.has("reason") ? p.get("reason").getAsString() : "Trận đấu kết thúc.";
        int delta = p.has("scoreDelta") ? p.get("scoreDelta").getAsInt() : 0;

        activeResultDialog = new ResultDialog(this, connection, gameId,
                titleOutcome, reason, delta, mySafeCells, myActions, myFlags,
                new ResultDialog.ResultActionListener() {
                    @Override
                    public void onPlayAgain() {
                        // Người dùng đã bấm chơi lại, dialog vẫn mở và hiển thị trạng thái chờ
                    }

                    @Override
                    public void onExit() {
                        if (parentLobby instanceof MainMenuFrame) {
                            ((MainMenuFrame) parentLobby).saveGameWindowState(getCurrentExtendedState(), getNormalBounds());
                        }
                        dispose();
                        if (parentLobby != null) {
                            parentLobby.setVisible(true);
                        }
                    }
                });
        activeResultDialog.setVisible(true);
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && (getCurrentExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    private void updateNormalBounds() {
        if (isVisible()) {
            int state = getExtendedState();
            if ((state & Frame.MAXIMIZED_BOTH) == 0 && (state & Frame.ICONIFIED) == 0) {
                normalBounds = getBounds();
            }
        }
    }

    /**
     * Lấy trạng thái cửa sổ hiện tại (bình thường hoặc phóng to toàn màn hình).
     * Nếu cửa sổ đang bị thu nhỏ (iconified) xuống taskbar, trả về trạng thái hiển thị mong muốn (MAXIMIZED hoặc NORMAL).
     */
    public int getCurrentExtendedState() {
        int state = getExtendedState();
        if ((state & Frame.ICONIFIED) != 0) {
            return (state & Frame.MAXIMIZED_BOTH) != 0 ? Frame.MAXIMIZED_BOTH : Frame.NORMAL;
        }
        return state;
    }

    /**
     * Lấy kích thước và vị trí cửa sổ ở chế độ thường.
     */
    public Rectangle getNormalBounds() {
        if (normalBounds != null && normalBounds.width > 200 && normalBounds.height > 200) {
            return new Rectangle(normalBounds);
        }
        return getBounds();
    }

    private void startOpponentDisconnectCountdown(int seconds, String dcName) {
        opponentDisconnectSeconds = seconds;
        lblStatus.setText("Trạng thái: ĐỐI THỦ MẤT KẾT NỐI (Chờ " + opponentDisconnectSeconds + "s...)");
        lblStatus.setForeground(UITheme.COLOR_DANGER);

        // 1. Khóa click trên bàn cờ
        boardPanel.setInteractive(false);

        // 2. Tạm dừng đồng hồ ván đấu
        if (gameClock != null) {
            gameClock.stop();
        }

        // 3. Kích hoạt Overlay che mờ toàn màn hình và khóa click
        if (disconnectOverlay != null) {
            disconnectOverlay.update(opponentDisconnectSeconds, dcName);
            disconnectOverlay.setVisible(true);
        }

        if (opponentDisconnectTimer != null) opponentDisconnectTimer.stop();
        opponentDisconnectTimer = new Timer(1000, e -> {
            opponentDisconnectSeconds--;
            if (opponentDisconnectSeconds > 0) {
                lblStatus.setText("Trạng thái: ĐỐI THỦ MẤT KẾT NỐI (Chờ " + opponentDisconnectSeconds + "s...)");
                if (disconnectOverlay != null) {
                    disconnectOverlay.update(opponentDisconnectSeconds, dcName);
                }
            } else {
                lblStatus.setText("Trạng thái: HẾT GIỜ KẾT NỐI LẠI");
                if (opponentDisconnectTimer != null) opponentDisconnectTimer.stop();
            }
        });
        opponentDisconnectTimer.start();
    }

    private void startOpponentDisconnectCountdown(int seconds) {
        startOpponentDisconnectCountdown(seconds, opponentName);
    }

    private void onOpponentReconnected(JsonObject p) {
        if (opponentDisconnectTimer != null) {
            opponentDisconnectTimer.stop();
        }

        // 1. Ẩn Overlay che màn hình
        hideDisconnectOverlay();

        // 2. Mở lại quyền tương tác bàn cờ
        boardPanel.setInteractive(true);

        // 3. Chạy tiếp đồng hồ ván đấu
        if (gameClock != null && remainingSeconds > 0) {
            gameClock.start();
        }

        lblStatus.setText("Trạng thái: ĐANG THI ĐẤU");
        lblStatus.setForeground(UITheme.COLOR_SUCCESS);
        SoundManager.getInstance().playTick();
        String oppName = (p != null && p.has("reconnectedUsername")) ? p.get("reconnectedUsername").getAsString() : opponentName;
        JOptionPane.showMessageDialog(this,
                "Đối thủ [" + oppName + "] đã kết nối lại ván đấu thành công!\nTrận đấu tiếp tục diễn ra.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void hideDisconnectOverlay() {
        if (disconnectOverlay != null && disconnectOverlay.isVisible()) {
            disconnectOverlay.setVisible(false);
        }
    }

    /**
     * Overlay GlassPane phủ mờ toàn bộ màn hình khi đối thủ rớt mạng:
     * - Chặn tuyệt đối mọi thao tác click/phím chuột
     * - Hiển thị Card thông báo nổi bật chính giữa màn hình với số đếm ngược to rõ
     */
    private class DisconnectOverlayPanel extends JPanel {
        private final JLabel lblOpponent;
        private final JLabel lblCountdown;

        public DisconnectOverlayPanel() {
            setOpaque(false);
            setLayout(new GridBagLayout()); // Căn giữa màn hình

            // Chặn toàn bộ tương tác chuột không xuyên xuống dưới
            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
            addMouseWheelListener(e -> {});
            setFocusable(true);

            // Card thông báo ở giữa
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(239, 68, 68), 2, true),
                    BorderFactory.createEmptyBorder(28, 40, 28, 40)
            ));

            JLabel lblIcon = new JLabel("⚠️", SwingConstants.CENTER);
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
            lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblTitle = new JLabel("ĐỐI THỦ BỊ MẤT KẾT NỐI", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
            lblTitle.setForeground(new Color(220, 38, 38));
            lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            lblOpponent = new JLabel("Đang chờ đối thủ kết nối lại...", SwingConstants.CENTER);
            lblOpponent.setFont(UITheme.FONT_REGULAR);
            lblOpponent.setForeground(UITheme.TEXT_BODY);
            lblOpponent.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Ô số đếm ngược to
            JPanel countdownBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            countdownBox.setBackground(new Color(254, 243, 199)); // Amber 100
            countdownBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(245, 158, 11), 1, true),
                    BorderFactory.createEmptyBorder(6, 26, 6, 26)
            ));
            countdownBox.setAlignmentX(Component.CENTER_ALIGNMENT);

            lblCountdown = new JLabel("15s", SwingConstants.CENTER);
            lblCountdown.setFont(new Font("Segoe UI", Font.BOLD, 42));
            lblCountdown.setForeground(new Color(217, 119, 6)); // Amber 600
            countdownBox.add(lblCountdown);

            JLabel lblNotice1 = new JLabel("Ván đấu đang tạm dừng. Bạn không thể thao tác trong lúc này.", SwingConstants.CENTER);
            lblNotice1.setFont(UITheme.FONT_SMALL);
            lblNotice1.setForeground(UITheme.TEXT_MUTED);
            lblNotice1.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblNotice2 = new JLabel("Nếu hết 15 giây đối thủ không vào lại, bạn sẽ được xử THẮNG (+5 điểm)!", SwingConstants.CENTER);
            lblNotice2.setFont(UITheme.FONT_BOLD);
            lblNotice2.setForeground(new Color(5, 150, 105));
            lblNotice2.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(lblIcon);
            card.add(Box.createVerticalStrut(8));
            card.add(lblTitle);
            card.add(Box.createVerticalStrut(10));
            card.add(lblOpponent);
            card.add(Box.createVerticalStrut(14));
            card.add(countdownBox);
            card.add(Box.createVerticalStrut(16));
            card.add(lblNotice1);
            card.add(Box.createVerticalStrut(6));
            card.add(lblNotice2);

            add(card);
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Lớp phủ bán trong suốt làm mờ toàn bộ giao diện
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(15, 23, 42, 185)); // Slate 900 với độ mờ 72%
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }

        public void update(int seconds, String oppName) {
            lblCountdown.setText(seconds + "s");
            lblOpponent.setText("Đang chờ đối thủ [" + oppName + "] kết nối lại...");
        }
    }

    /**
     * Khôi phục toàn bộ trạng thái ván đấu khi kết nối lại thành công.
     */
    public void restoreGameState(JsonObject p) {
        if (readyCountdownTimer != null) readyCountdownTimer.stop();

        // 1. Khôi phục đồng hồ
        if (p.has("remainingSeconds")) {
            this.remainingSeconds = p.get("remainingSeconds").getAsInt();
        }
        int mins = remainingSeconds / 60;
        int secs = remainingSeconds % 60;
        lblTimer.setText(String.format("Thời gian: %02d:%02d", mins, secs));

        if (gameClock != null) gameClock.stop();
        gameClock = new Timer(1000, e -> {
            remainingSeconds--;
            int m = remainingSeconds / 60;
            int s = remainingSeconds % 60;
            lblTimer.setText(String.format("Thời gian: %02d:%02d", m, s));
            if (remainingSeconds <= 5 && remainingSeconds > 0) {
                SoundManager.getInstance().playTick();
            }
            if (remainingSeconds <= 0) {
                gameClock.stop();
            }
        });
        gameClock.start();

        // 2. Khôi phục thống kê
        if (p.has("mySafeCells")) mySafeCells = p.get("mySafeCells").getAsInt();
        if (p.has("myFlags")) myFlags = p.get("myFlags").getAsInt();
        if (p.has("myActions")) myActions = p.get("myActions").getAsInt();
        infoPanel.updateSelfStats(mySafeCells, myFlags, myActions);

        if (p.has("oppSafeCells")) oppSafeCells = p.get("oppSafeCells").getAsInt();
        if (p.has("oppFlags")) oppFlags = p.get("oppFlags").getAsInt();
        if (p.has("oppActions")) oppActions = p.get("oppActions").getAsInt();
        infoPanel.updateOpponentStats(oppSafeCells, oppFlags, oppActions);

        // 3. Khôi phục các ô đã mở trên bàn của mình
        if (p.has("myOpenedCells") && p.get("myOpenedCells").isJsonArray()) {
            for (JsonElement el : p.getAsJsonArray("myOpenedCells")) {
                JsonObject o = el.getAsJsonObject();
                int r = o.get("row").getAsInt();
                int c = o.get("col").getAsInt();
                int adj = o.get("adjacentMines").getAsInt();
                boolean isMine = o.get("isMine").getAsBoolean();
                boolean isExp = o.get("isExploded").getAsBoolean();
                boardPanel.revealCell(r, c, adj, isMine, isExp);
            }
        }

        // 4. Khôi phục các ô cắm cờ trên bàn của mình
        if (p.has("myFlaggedCells") && p.get("myFlaggedCells").isJsonArray()) {
            for (JsonElement el : p.getAsJsonArray("myFlaggedCells")) {
                JsonObject o = el.getAsJsonObject();
                int r = o.get("row").getAsInt();
                int c = o.get("col").getAsInt();
                boardPanel.setCellFlagged(r, c, true);
            }
        }

        // 5. Khôi phục bàn cờ thu nhỏ của đối thủ
        if (p.has("oppOpenedCoords") && p.get("oppOpenedCoords").isJsonArray()) {
            for (JsonElement el : p.getAsJsonArray("oppOpenedCoords")) {
                JsonObject o = el.getAsJsonObject();
                miniBoardPanel.revealCell(o.get("r").getAsInt(), o.get("c").getAsInt());
            }
        }
        if (p.has("oppFlaggedCoords") && p.get("oppFlaggedCoords").isJsonArray()) {
            for (JsonElement el : p.getAsJsonArray("oppFlaggedCoords")) {
                JsonObject o = el.getAsJsonObject();
                miniBoardPanel.setCellFlagged(o.get("r").getAsInt(), o.get("c").getAsInt(), true);
            }
        }

        // 6. Cho phép tương tác bàn cờ
        boardPanel.setInteractive(true);
        lblStatus.setText("Trạng thái: ĐÃ KHÔI PHỤC VÁN ĐẤU");
        lblStatus.setForeground(UITheme.COLOR_SUCCESS);

        JOptionPane.showMessageDialog(this,
                "Đã khôi phục kết nối ván đấu thành công!\nVán đấu của bạn đang tiếp tục.",
                "Khôi phục trận đấu", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void dispose() {
        if (readyCountdownTimer != null) {
            readyCountdownTimer.stop();
        }
        if (gameClock != null) {
            gameClock.stop();
        }
        if (opponentDisconnectTimer != null) {
            opponentDisconnectTimer.stop();
        }
        if (activeResultDialog != null && activeResultDialog.isDisplayable()) {
            activeResultDialog.dispose();
        }
        if (gameMessageListener != null) {
            connection.removeMessageListener(gameMessageListener);
        }
        super.dispose();
    }
}
