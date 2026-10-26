package minesweeperonline.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import minesweeperonline.common.model.LeaderboardEntry;
import minesweeperonline.common.model.MatchHistoryEntry;
import minesweeperonline.common.model.Player;
import minesweeperonline.common.model.User;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;
import minesweeperonline.database.MatchPlayerDAO;
import minesweeperonline.database.UserDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Xử lý giao tiếp với một TCP client.
 * Mỗi kết nối client chạy trên một Thread riêng.
 */
public class ClientHandler implements Runnable {

    // Hủy các lời mời không được trả lời sau CHALLENGE_TIMEOUT_SECONDS
    private static final ScheduledExecutorService CHALLENGE_TIMER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Challenge-Timer");
                t.setDaemon(true);
                return t;
            });
    private static final AtomicLong CHALLENGE_SEQ = new AtomicLong();

    private final Socket socket;
    private final ClientManager clientManager;
    private final GameManager gameManager;
    private final MatchmakingManager matchmakingManager;

    private BufferedReader reader;
    private PrintWriter writer;

    // Được thread khác đọc (sảnh, game session); khi logout thì ghi trong synchronized (clientManager)
    private volatile User user;
    private volatile Player player;
    private volatile boolean running = true;

    // Trạng thái lời mời thách đấu đang chờ (-1 = không có). Được bảo vệ bởi synchronized (clientManager).
    private int pendingTargetId = -1;      // user mà client này đã thách đấu
    private int pendingChallengerId = -1;  // user đã thách đấu client này
    private long pendingChallengeToken;    // định danh lời mời mà client này đã gửi

    private final UserDAO userDAO = new UserDAO();

    public ClientHandler(Socket socket, ClientManager clientManager, GameManager gameManager, MatchmakingManager matchmakingManager) {
        this.socket = socket;
        this.clientManager = clientManager;
        this.gameManager = gameManager;
        this.matchmakingManager = matchmakingManager;
    }

    public ClientHandler(Socket socket, ClientManager clientManager, GameManager gameManager) {
        this(socket, clientManager, gameManager, null);
    }

    public boolean isRunning() {
        return running && socket != null && !socket.isClosed();
    }

    @Override
    public void run() {
        try {
            this.reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            clientManager.addClient(this);

            System.out.println("[ClientHandler] Connected: " + socket.getRemoteSocketAddress());

            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                Message message = JsonProtocol.deserialize(line);
                if (message != null) {
                    // Payload sai định dạng (vd: thiếu "row") không được làm chết kết nối
                    try {
                        handleMessage(message);
                    } catch (RuntimeException e) {
                        System.err.println("[ClientHandler] Error handling " + message.getType() + ": " + e);
                        sendMessage(JsonProtocol.createError(message.getRequestId(), "Yeu cau khong hop le!"));
                    }
                } else {
                    sendMessage(JsonProtocol.createError("", "Invalid JSON message format"));
                }
            }

        } catch (IOException e) {
            System.out.println("[ClientHandler] Connection reset/closed: " + socket.getRemoteSocketAddress());
        } finally {
            cleanup();
        }
    }

    /**
     * Phân loại và xử lý một message protocol nhận được.
     */
    public void handleMessage(Message message) {
        if (message == null || message.getType() == null) {
            return;
        }

        MessageType type = message.getType();

        switch (type) {
            case LOGIN:
                handleLogin(message);
                break;

            case REGISTER:
                handleRegister(message);
                break;

            case LOGOUT:
                handleLogout(message);
                break;

            case GET_ONLINE_PLAYERS:
                handleGetOnlinePlayers(message);
                break;

            case CHALLENGE:
                handleChallenge(message);
                break;

            case ACCEPT:
                handleAccept(message);
                break;

            case REJECT:
                handleReject(message);
                break;

            case FORFEIT:
                handleForfeit(message);
                break;

            case OPEN_CELL:
                handleOpenCell(message);
                break;

            case PLACE_FLAG:
                handlePlaceFlag(message);
                break;

            case REMOVE_FLAG:
                handleRemoveFlag(message);
                break;

            case PLAY_AGAIN:
                handlePlayAgain(message);
                break;

            case GET_LEADERBOARD:
                handleGetLeaderboard(message);
                break;

            case GET_MATCH_HISTORY:
                handleGetMatchHistory(message);
                break;

            case EXIT:
                handleExit(message);
                break;

            case QUICK_MATCH:
                handleQuickMatch(message);
                break;

            case CANCEL_QUICK_MATCH:
                handleCancelQuickMatch(message);
                break;

            default:
                System.out.println("[ClientHandler] Received message type: " + type + " from " +
                        (user != null ? user.getUsername() : "unauthenticated"));
                break;
        }
    }

    private void handleOpenCell(Message message) {
        if (user == null) return;
        GameSession session = gameManager.getSessionByUserId(user.getId());
        if (session != null) {
            JsonObject p = message.getPayload();
            int row = p.get("row").getAsInt();
            int col = p.get("col").getAsInt();
            session.handleOpenCell(user.getId(), row, col);
        }
    }

    private void handlePlaceFlag(Message message) {
        if (user == null) return;
        GameSession session = gameManager.getSessionByUserId(user.getId());
        if (session != null) {
            JsonObject p = message.getPayload();
            int row = p.get("row").getAsInt();
            int col = p.get("col").getAsInt();
            session.handlePlaceFlag(user.getId(), row, col);
        }
    }

    private void handleRemoveFlag(Message message) {
        if (user == null) return;
        GameSession session = gameManager.getSessionByUserId(user.getId());
        if (session != null) {
            JsonObject p = message.getPayload();
            int row = p.get("row").getAsInt();
            int col = p.get("col").getAsInt();
            session.handleRemoveFlag(user.getId(), row, col);
        }
    }

    private void handleLogin(Message message) {
        JsonObject payload = message.getPayload();
        String username = payload.has("username") ? payload.get("username").getAsString() : "";
        String password = payload.has("password") ? payload.get("password").getAsString() : "";

        if (username.isEmpty() || password.isEmpty()) {
            sendMessage(JsonProtocol.createLoginFailed("Username va password khong duoc de trong!"));
            return;
        }

        User dbUser = userDAO.authenticate(username, password);
        if (dbUser == null) {
            sendMessage(JsonProtocol.createLoginFailed("Sai ten dang nhap hoac mat khau!"));
            return;
        }

        ClientHandler existing = clientManager.getClientByUserId(dbUser.getId());
        if (existing != null && existing != this) {
            sendMessage(JsonProtocol.createLoginFailed("Tai khoan dang duoc dang nhap o mot thiet bi khac!"));
            return;
        }

        // Client đăng xuất không gửi LOGOUT: cùng một kết nối đăng nhập lại,
        // nên phải giải phóng tài khoản cũ đang gắn với kết nối này.
        if (this.user != null) {
            cancelPendingChallenge();
            if (this.user.getId() != dbUser.getId()) {
                clientManager.unregisterUser(this.user.getId());
            }
        }

        this.user = dbUser;
        this.player = new Player(dbUser.getId(), dbUser.getUsername(), dbUser.getTotalScore());
        clientManager.registerUser(dbUser.getId(), this);

        Message res = JsonProtocol.createLoginSuccess(user.getId(), user.getUsername(), user.getTotalScore());
        res.setRequestId(message.getRequestId());
        sendMessage(res);
        System.out.println("[ClientHandler] Login successful: " + username + " (id=" + user.getId() + ")");
    }

    private void handleRegister(Message message) {
        JsonObject payload = message.getPayload();
        String username = payload.has("username") ? payload.get("username").getAsString() : "";
        String password = payload.has("password") ? payload.get("password").getAsString() : "";

        if (username.isEmpty() || password.isEmpty()) {
            sendMessage(JsonProtocol.createRegisterFailed("Username va password khong duoc de trong!"));
            return;
        }

        if (userDAO.existsByUsername(username)) {
            sendMessage(JsonProtocol.createRegisterFailed("Ten dang nhap da ton tai!"));
            return;
        }

        boolean registered = userDAO.register(username, password);
        if (registered) {
            Message res = JsonProtocol.createRegisterSuccess("Dang ky tai khoan thanh cong!");
            res.setRequestId(message.getRequestId());
            sendMessage(res);
            System.out.println("[ClientHandler] User registered: " + username);
        } else {
            sendMessage(JsonProtocol.createRegisterFailed("Khong the tao tai khoan, vui long thu lai!"));
        }
    }

    private void handleGetOnlinePlayers(Message message) {
        int currentUserId = (user != null) ? user.getId() : -1;
        List<Player> onlineList = clientManager.getOnlinePlayers(currentUserId);

        Message res = new Message(MessageType.ONLINE_PLAYERS);
        res.setRequestId(message.getRequestId());
        JsonObject payload = new JsonObject();
        JsonArray array = new JsonArray();

        for (Player p : onlineList) {
            JsonObject item = new JsonObject();
            item.addProperty("userId", p.getUserId());
            item.addProperty("username", p.getUsername());
            item.addProperty("totalScore", p.getTotalScore());
            item.addProperty("status", p.getStatus());
            array.add(item);
        }

        payload.add("players", array);
        res.setPayload(payload);
        sendMessage(res);
    }

    private void handleGetLeaderboard(Message message) {
        List<LeaderboardEntry> list = userDAO.getLeaderboard(10);
        Message res = new Message(MessageType.LEADERBOARD);
        res.setRequestId(message.getRequestId());
        JsonObject payload = new JsonObject();
        JsonArray array = new JsonArray();

        for (LeaderboardEntry e : list) {
            JsonObject item = new JsonObject();
            item.addProperty("rank", e.getRank());
            item.addProperty("userId", e.getUserId());
            item.addProperty("username", e.getUsername());
            item.addProperty("totalScore", e.getTotalScore());
            item.addProperty("wins", e.getWins());
            item.addProperty("totalMatches", e.getTotalMatches());
            item.addProperty("winRate", e.getWinRate());
            array.add(item);
        }

        payload.add("leaderboard", array);
        res.setPayload(payload);
        sendMessage(res);
    }

    private void handleGetMatchHistory(Message message) {
        if (user == null) return;
        int limit = 20;
        if (message.getPayload() != null && message.getPayload().has("limit")) {
            limit = message.getPayload().get("limit").getAsInt();
        }
        limit = Math.max(1, Math.min(limit, 100));
        MatchPlayerDAO matchPlayerDAO = new MatchPlayerDAO();
        List<MatchHistoryEntry> list = matchPlayerDAO.getMatchHistory(user.getId(), limit);

        Message res = new Message(MessageType.MATCH_HISTORY);
        res.setRequestId(message.getRequestId());
        JsonObject payload = new JsonObject();
        payload.add("history", JsonProtocol.getGson().toJsonTree(list));
        res.setPayload(payload);
        sendMessage(res);
    }

    private void handleQuickMatch(Message message) {
        if (user == null || player == null) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Vui lòng đăng nhập trước khi tìm trận!"));
            return;
        }
        if (matchmakingManager != null) {
            matchmakingManager.joinQueue(this);
        }
    }

    private void handleCancelQuickMatch(Message message) {
        if (matchmakingManager != null) {
            matchmakingManager.leaveQueue(this);
        }
    }

    private void handleChallenge(Message message) {
        if (user == null || player == null) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Ban chua dang nhap!"));
            return;
        }

        JsonObject payload = message.getPayload();
        int targetUserId = payload.has("targetUserId") ? payload.get("targetUserId").getAsInt() : -1;

        if (targetUserId == user.getId()) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Khong the tu thach dau chinh minh!"));
            return;
        }

        ClientHandler targetHandler;
        // Kiểm tra rồi cập nhật trạng thái của hai người chơi phải là atomic: hai người
        // cùng lúc thách đấu một người thì không được cả hai cùng thành công.
        synchronized (clientManager) {
            if (!Player.STATUS_IDLE.equals(player.getStatus())) {
                sendMessage(JsonProtocol.createError(message.getRequestId(), "Ban dang ban, khong the thach dau!"));
                return;
            }

            targetHandler = clientManager.getClientByUserId(targetUserId);
            if (targetHandler == null || targetHandler.getPlayer() == null) {
                sendMessage(JsonProtocol.createError(message.getRequestId(), "Nguoi choi khong online!"));
                return;
            }

            if (!Player.STATUS_IDLE.equals(targetHandler.getPlayer().getStatus())) {
                sendMessage(JsonProtocol.createError(message.getRequestId(), "Nguoi choi dang ban hoac dang thi dau!"));
                return;
            }

            // Cả hai người chơi chuyển sang CHALLENGING trong lúc lời mời đang PENDING
            player.setStatus(Player.STATUS_CHALLENGING);
            targetHandler.getPlayer().setStatus(Player.STATUS_CHALLENGING);
            this.pendingTargetId = targetUserId;
            targetHandler.pendingChallengerId = user.getId();
            this.pendingChallengeToken = CHALLENGE_SEQ.incrementAndGet();
        }

        // Hạn chót phía server: lời mời tự hết hạn kể cả khi người được mời không trả lời
        long token = pendingChallengeToken;
        CHALLENGE_TIMER.schedule(() -> expireChallenge(token),
                ServerConfig.CHALLENGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Chuyển tiếp CHALLENGE_REQUEST tới người được mời
        Message challengeReq = JsonProtocol.createChallengeRequest(user.getId(), user.getUsername());
        targetHandler.sendMessage(challengeReq);
    }

    /**
     * Chạy trên thread Challenge-Timer: nếu lời mời ứng với token vẫn còn
     * PENDING thì hủy nó, giống như người được mời gửi REJECT.
     */
    private void expireChallenge(long token) {
        Message rejMsg;
        synchronized (clientManager) {
            if (pendingTargetId < 0 || pendingChallengeToken != token) {
                return; // đã được chấp nhận / từ chối / hủy
            }
            ClientHandler target = clientManager.getClientByUserId(pendingTargetId);
            if (target != null && target.pendingChallengerId == user.getId()) {
                target.pendingChallengerId = -1;
                target.getPlayer().setStatus(Player.STATUS_IDLE);
            }
            rejMsg = new Message(MessageType.REJECT);
            JsonObject p = new JsonObject();
            p.addProperty("rejectedByUserId", pendingTargetId);
            p.addProperty("rejectedByUsername", target != null ? target.getUser().getUsername() : "Nguoi choi");
            rejMsg.setPayload(p);

            pendingTargetId = -1;
            player.setStatus(Player.STATUS_IDLE);
            System.out.println("[ClientHandler] Challenge expired after " +
                    ServerConfig.CHALLENGE_TIMEOUT_SECONDS + "s: " + user.getUsername());
        }
        sendMessage(rejMsg);
    }

    private void handleAccept(Message message) {
        if (user == null || player == null) return;

        JsonObject payload = message.getPayload();
        int challengerUserId = payload.has("challengerUserId") ? payload.get("challengerUserId").getAsInt() : -1;

        ClientHandler challengerHandler;
        GameSession session;
        synchronized (clientManager) {
            // Chỉ chấp nhận lời mời thực sự đang chờ user này
            if (challengerUserId < 0 || pendingChallengerId != challengerUserId) {
                sendMessage(JsonProtocol.createError(message.getRequestId(), "Loi moi thach dau khong con hieu luc!"));
                return;
            }
            pendingChallengerId = -1;

            challengerHandler = clientManager.getClientByUserId(challengerUserId);
            if (challengerHandler == null || challengerHandler.getPlayer() == null
                    || challengerHandler.pendingTargetId != user.getId()) {
                sendMessage(JsonProtocol.createError(message.getRequestId(), "Nguoi thach dau da roi mang!"));
                player.setStatus(Player.STATUS_IDLE);
                return;
            }
            challengerHandler.pendingTargetId = -1;

            // Tạo GameSession mới (chuyển cả hai người chơi sang PLAYING)
            session = gameManager.createSession(
                    challengerHandler.getPlayer(), challengerHandler,
                    this.player, this
            );
        }

        // Báo cho cả hai người chơi: MATCH_CREATED
        Message matchCreatedP1 = new Message(MessageType.MATCH_CREATED);
        JsonObject p1Data = new JsonObject();
        p1Data.addProperty("gameId", session.getGameId());
        p1Data.addProperty("opponentId", this.user.getId());
        p1Data.addProperty("opponentUsername", this.user.getUsername());
        matchCreatedP1.setPayload(p1Data);
        challengerHandler.sendMessage(matchCreatedP1);

        Message matchCreatedP2 = new Message(MessageType.MATCH_CREATED);
        JsonObject p2Data = new JsonObject();
        p2Data.addProperty("gameId", session.getGameId());
        p2Data.addProperty("opponentId", session.getPlayer1().getUserId());
        p2Data.addProperty("opponentUsername", session.getPlayer1().getUsername());
        matchCreatedP2.setPayload(p2Data);
        this.sendMessage(matchCreatedP2);
    }

    private void handleReject(Message message) {
        if (user == null || player == null) return;
        JsonObject payload = message.getPayload();
        int challengerUserId = payload.has("challengerUserId") ? payload.get("challengerUserId").getAsInt() : -1;

        ClientHandler challengerHandler;
        synchronized (clientManager) {
            // Bỏ qua REJECT cũ (lời mời đã được chấp nhận/hủy)
            if (challengerUserId < 0 || pendingChallengerId != challengerUserId) {
                return;
            }
            pendingChallengerId = -1;
            player.setStatus(Player.STATUS_IDLE);

            challengerHandler = clientManager.getClientByUserId(challengerUserId);
            if (challengerHandler != null && challengerHandler.pendingTargetId == user.getId()) {
                challengerHandler.pendingTargetId = -1;
                challengerHandler.getPlayer().setStatus(Player.STATUS_IDLE);
            } else {
                challengerHandler = null;
            }
        }

        if (challengerHandler != null) {
            Message rejMsg = new Message(MessageType.REJECT);
            JsonObject p = new JsonObject();
            p.addProperty("rejectedByUserId", user.getId());
            p.addProperty("rejectedByUsername", user.getUsername());
            rejMsg.setPayload(p);
            challengerHandler.sendMessage(rejMsg);
        }
    }

    private void handleForfeit(Message message) {
        if (user == null) return;
        GameSession session = gameManager.getSessionByUserId(user.getId());
        if (session != null && !session.isFinished()) {
            Player opp = session.getOpponent(user.getId());
            int winnerId = (opp != null) ? opp.getUserId() : 0;
            if (session.finishGame(winnerId, "FORFEIT")) {
                ClientHandler oppHandler = session.getOpponentHandler(user.getId());
                if (oppHandler != null) {
                    Message winMsg = new Message(MessageType.GAME_WIN);
                    JsonObject p = new JsonObject();
                    p.addProperty("reason", "Doi thu da dau hang!");
                    p.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                    session.attachMines(p, winnerId);
                    winMsg.setPayload(p);
                    oppHandler.sendMessage(winMsg);
                }
                Message loseMsg = new Message(MessageType.GAME_LOSE);
                JsonObject p = new JsonObject();
                p.addProperty("reason", "Ban da dau hang!");
                p.addProperty("scoreDelta", ServerConfig.SCORE_FORFEIT);
                session.attachMines(p, user.getId());
                loseMsg.setPayload(p);
                sendMessage(loseMsg);
            }
        }
    }

    private void handlePlayAgain(Message message) {
        if (user == null) return;
        GameSession session = gameManager.getSessionByUserId(user.getId());
        if (session != null) {
            session.handlePlayAgain(user.getId(), gameManager);
        }
    }

    private void handleExit(Message message) {
        if (user != null) {
            GameSession session = gameManager.getSessionByUserId(user.getId());
            if (session != null) {
                // Rời phòng trước khi trận kết thúc thì tính là FORFEIT
                if (!session.isFinished()) {
                    handleForfeit(message);
                }
                session.handleExit(user.getId(), gameManager);
            }
            if (player != null) {
                player.setStatus(Player.STATUS_IDLE);
            }
        }
    }

    /**
     * LOGOUT: kết nối vẫn giữ (quay về màn hình đăng nhập) nhưng tài khoản chuyển
     * sang offline. Hủy lời mời đang chờ (nếu có) và rời trận giống như EXIT.
     */
    private void handleLogout(Message message) {
        if (user == null) return;
        cancelPendingChallenge();
        handleExit(message);

        // Giữ lock của sảnh để không thread nào thấy handler này online mà player lại null
        synchronized (clientManager) {
            clientManager.unregisterUser(user.getId());
            System.out.println("[ClientHandler] Logout: " + user.getUsername());
            user = null;
            player = null;
        }
    }

    /**
     * Gửi một message protocol tới client này theo cách thread-safe.
     */
    public synchronized void sendMessage(Message message) {
        if (writer != null && !socket.isClosed()) {
            String json = JsonProtocol.serializeWithNewline(message);
            writer.print(json);
            writer.flush();
        }
    }

    public synchronized void close() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Bỏ qua lỗi khi đóng
        }
    }

    /**
     * Hủy lời mời PENDING mà client này tham gia (đã gửi hoặc đã nhận)
     * và đưa phía bên kia về IDLE. Người thách đấu đang chờ sẽ được thông báo.
     */
    private void cancelPendingChallenge() {
        if (matchmakingManager != null) {
            matchmakingManager.leaveQueue(this);
        }
        if (user == null) return;
        ClientHandler waitingChallenger = null;

        synchronized (clientManager) {
            if (pendingTargetId >= 0) {
                ClientHandler target = clientManager.getClientByUserId(pendingTargetId);
                if (target != null && target.pendingChallengerId == user.getId()) {
                    target.pendingChallengerId = -1;
                    target.getPlayer().setStatus(Player.STATUS_IDLE);
                }
                pendingTargetId = -1;
            }
            if (pendingChallengerId >= 0) {
                ClientHandler challenger = clientManager.getClientByUserId(pendingChallengerId);
                if (challenger != null && challenger.pendingTargetId == user.getId()) {
                    challenger.pendingTargetId = -1;
                    challenger.getPlayer().setStatus(Player.STATUS_IDLE);
                    waitingChallenger = challenger;
                }
                pendingChallengerId = -1;
            }
            if (player != null && Player.STATUS_CHALLENGING.equals(player.getStatus())) {
                player.setStatus(Player.STATUS_IDLE);
            }
        }

        if (waitingChallenger != null) {
            waitingChallenger.sendMessage(JsonProtocol.createError(null,
                    "Nguoi choi [" + user.getUsername() + "] da roi di, loi moi thach dau bi huy!"));
        }
    }

    /**
     * Dọn dẹp sạch trạng thái của client khi mất kết nối.
     */
    private void cleanup() {
        running = false;
        if (matchmakingManager != null) {
            matchmakingManager.leaveQueue(this);
        }
        clientManager.removeClient(this);

        if (user != null) {
            cancelPendingChallenge();

            GameSession session = gameManager.getSessionByUserId(user.getId());
            if (session != null) {
                // Mất kết nối giữa trận = FORFEIT
                if (!session.isFinished()) {
                    Player opponent = session.getOpponent(user.getId());
                    int winnerId = (opponent != null) ? opponent.getUserId() : 0;
                    if (session.finishGame(winnerId, "FORFEIT_DISCONNECT")) {
                        ClientHandler oppHandler = session.getOpponentHandler(user.getId());
                        if (oppHandler != null) {
                            Message dcMsg = new Message(MessageType.OPPONENT_DISCONNECTED);
                            JsonObject p = new JsonObject();
                            p.addProperty("reason", "Doi thu mat ket noi!");
                            p.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                            session.attachMines(p, winnerId);
                            dcMsg.setPayload(p);
                            oppHandler.sendMessage(dcMsg);
                        }
                    }
                }
                // Rời phòng: báo cho đối thủ (vô hiệu nút PLAY AGAIN),
                // đưa cả hai người chơi về IDLE và hủy session
                session.handleExit(user.getId(), gameManager);
            }
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Bỏ qua
        }
        System.out.println("[ClientHandler] Cleanup complete for: " +
                (user != null ? user.getUsername() : socket.getRemoteSocketAddress()));
    }

    public User getUser() {
        return user;
    }

    public Player getPlayer() {
        return player;
    }

    public Socket getSocket() {
        return socket;
    }
}