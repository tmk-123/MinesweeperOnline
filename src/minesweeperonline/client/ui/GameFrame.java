package minesweeperonline.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.User;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import javax.swing.*;
import java.awt.*;

/**
 * Main gameplay window for active 12x12 matches.
 * Displays player's board, opponent's mini-board, match stats, and real-time game clock.
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
    private JButton btnForfeit;

    private ResultDialog activeResultDialog;
    private ServerConnection.MessageListener gameMessageListener;

    private Timer gameClock;
    private Timer readyCountdownTimer;
    private int readySeconds = 5;
    private int remainingSeconds = 300;
    private int mySafeCells = 0;
    private int myFlags = 0;
    private int myActions = 0;
    private int oppSafeCells = 0;
    private int oppFlags = 0;
    private int oppActions = 0;

    public GameFrame(ServerConnection connection, User currentUser, String opponentName, String gameId, JFrame parentLobby) {
        this.connection = connection;
        this.currentUser = currentUser;
        this.opponentName = opponentName;
        this.gameId = gameId;
        this.parentLobby = parentLobby;

        initComponents();
        setupNetworkListener();
        startReadyCountdownVisual();
    }

    private void initComponents() {
        setTitle("Minesweeper Online - Trận đấu [" + gameId + "] (" + currentUser.getUsername() + " vs " + opponentName + ")");
        setSize(820, 740);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // 1. North: Player stats
        infoPanel = new PlayerInfoPanel(currentUser.getUsername(), opponentName);
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // 2. Center: Player 12x12 Board + Opponent Mini-board
        JPanel boardsContainer = new JPanel(new BorderLayout(12, 0));
        boardPanel = new GameBoardPanel(connection, gameId);
        miniBoardPanel = new OpponentMiniBoardPanel();

        boardsContainer.add(boardPanel, BorderLayout.CENTER);
        boardsContainer.add(miniBoardPanel, BorderLayout.EAST);
        mainPanel.add(boardsContainer, BorderLayout.CENTER);

        // 3. South: Status & Timer & Forfeit
        JPanel southPanel = new JPanel(new BorderLayout(10, 5));
        southPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        lblTimer = new JLabel("Thời gian: 05:00");
        lblTimer.setFont(new Font("Arial", Font.BOLD, 14));
        lblTimer.setForeground(new Color(180, 0, 0));

        lblStatus = new JLabel("Trạng thái: CHUẨN BỊ (READY 5s)");
        lblStatus.setFont(new Font("Arial", Font.BOLD, 12));
        lblStatus.setForeground(new Color(20, 100, 180));

        statusPanel.add(lblTimer);
        statusPanel.add(lblStatus);

        btnForfeit = new JButton("Đầu hàng");
        btnForfeit.setBackground(new Color(255, 220, 220));
        btnForfeit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn đầu hàng không?\n(Bạn sẽ bị xử thua và trừ 5 điểm)",
                    "Xác nhận đầu hàng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                connection.sendMessage(JsonProtocol.createForfeit(gameId));
            }
        });

        southPanel.add(statusPanel, BorderLayout.CENTER);
        southPanel.add(btnForfeit, BorderLayout.EAST);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        add(mainPanel);
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
                onGameEnd("BẠN ĐÃ CHIẾN THẮNG!", message.getPayload(), true);
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

        readyCountdownTimer = new Timer(1000, e -> {
            readySeconds--;
            if (readySeconds > 0) {
                lblStatus.setText("Trạng thái: CHUẨN BỊ (Bắt đầu sau " + readySeconds + "s...)");
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
        lblStatus.setForeground(new Color(0, 150, 0));
        boardPanel.setInteractive(true);

        remainingSeconds = 300;
        if (gameClock != null) gameClock.stop();
        gameClock = new Timer(1000, e -> {
            remainingSeconds--;
            int mins = remainingSeconds / 60;
            int secs = remainingSeconds % 60;
            lblTimer.setText(String.format("Thời gian: %02d:%02d", mins, secs));
            if (remainingSeconds <= 0) {
                gameClock.stop();
            }
        });
        gameClock.start();
    }

    private void onCellOpened(JsonObject p) {
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
            }
        }
        if (p.has("openedSafeCells")) {
            mySafeCells = p.get("openedSafeCells").getAsInt();
        }
        if (p.has("totalActions")) {
            myActions = p.get("totalActions").getAsInt();
        }
        infoPanel.updateSelfStats(mySafeCells, myFlags, myActions);
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

        // Update opponent mini-board
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
        if (gameClock != null) gameClock.stop();
        boardPanel.setInteractive(false);
        btnForfeit.setEnabled(false);
        lblStatus.setText("Trạng thái: KẾT THÚC");

        String reason = p.has("reason") ? p.get("reason").getAsString() : "Trận đấu kết thúc.";
        int delta = p.has("scoreDelta") ? p.get("scoreDelta").getAsInt() : 0;

        activeResultDialog = new ResultDialog(this, connection, gameId,
                titleOutcome, reason, delta, mySafeCells, myActions, myFlags,
                new ResultDialog.ResultActionListener() {
                    @Override
                    public void onPlayAgain() {
                        // User clicked play again, dialog remains open showing waiting status
                    }

                    @Override
                    public void onExit() {
                        dispose();
                        if (parentLobby != null) {
                            parentLobby.setVisible(true);
                        }
                    }
                });
        activeResultDialog.setVisible(true);
    }

    @Override
    public void dispose() {
        if (readyCountdownTimer != null) {
            readyCountdownTimer.stop();
        }
        if (gameClock != null) {
            gameClock.stop();
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
