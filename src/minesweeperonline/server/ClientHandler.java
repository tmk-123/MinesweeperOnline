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

/**
 * Handles communication with an individual TCP Client.
 * Runs on a dedicated Thread per client connection.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientManager clientManager;
    private final GameManager gameManager;

    private BufferedReader reader;
    private PrintWriter writer;

    private User user;
    private Player player;
    private volatile boolean running = true;

    private final UserDAO userDAO = new UserDAO();

    public ClientHandler(Socket socket, ClientManager clientManager, GameManager gameManager) {
        this.socket = socket;
        this.clientManager = clientManager;
        this.gameManager = gameManager;
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
                    handleMessage(message);
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
     * Dispatches and processes an incoming protocol message.
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

        if (clientManager.isUserOnline(dbUser.getId())) {
            sendMessage(JsonProtocol.createLoginFailed("Tai khoan dang duoc dang nhap o mot thiet bi khac!"));
            return;
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
        List<LeaderboardEntry> list = userDAO.getLeaderboard(50);
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
        MatchPlayerDAO matchPlayerDAO = new MatchPlayerDAO();
        List<MatchHistoryEntry> list = matchPlayerDAO.getMatchHistory(user.getId(), limit);

        Message res = new Message(MessageType.MATCH_HISTORY);
        res.setRequestId(message.getRequestId());
        JsonObject payload = new JsonObject();
        payload.add("history", JsonProtocol.getGson().toJsonTree(list));
        res.setPayload(payload);
        sendMessage(res);
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

        ClientHandler targetHandler = clientManager.getClientByUserId(targetUserId);
        if (targetHandler == null || targetHandler.getPlayer() == null) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Nguoi choi khong online!"));
            return;
        }

        if (!Player.STATUS_IDLE.equals(targetHandler.getPlayer().getStatus())) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Nguoi choi dang ban hoac dang thi dau!"));
            return;
        }

        // Mark challenger as challenging
        player.setStatus(Player.STATUS_CHALLENGING);
        targetHandler.getPlayer().setStatus(Player.STATUS_CHALLENGING);

        // Forward CHALLENGE_REQUEST to target
        Message challengeReq = JsonProtocol.createChallengeRequest(user.getId(), user.getUsername());
        targetHandler.sendMessage(challengeReq);
    }

    private void handleAccept(Message message) {
        if (user == null || player == null) return;

        JsonObject payload = message.getPayload();
        int challengerUserId = payload.has("challengerUserId") ? payload.get("challengerUserId").getAsInt() : -1;

        ClientHandler challengerHandler = clientManager.getClientByUserId(challengerUserId);
        if (challengerHandler == null || challengerHandler.getPlayer() == null) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Nguoi thach dau da roi mang!"));
            player.setStatus(Player.STATUS_IDLE);
            return;
        }

        // Create new GameSession
        GameSession session = gameManager.createSession(
                challengerHandler.getPlayer(), challengerHandler,
                this.player, this
        );

        // Notify both players: MATCH_CREATED
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
        p2Data.addProperty("opponentId", challengerHandler.getUser().getId());
        p2Data.addProperty("opponentUsername", challengerHandler.getUser().getUsername());
        matchCreatedP2.setPayload(p2Data);
        this.sendMessage(matchCreatedP2);
    }

    private void handleReject(Message message) {
        if (user == null || player == null) return;
        JsonObject payload = message.getPayload();
        int challengerUserId = payload.has("challengerUserId") ? payload.get("challengerUserId").getAsInt() : -1;

        player.setStatus(Player.STATUS_IDLE);

        ClientHandler challengerHandler = clientManager.getClientByUserId(challengerUserId);
        if (challengerHandler != null && challengerHandler.getPlayer() != null) {
            challengerHandler.getPlayer().setStatus(Player.STATUS_IDLE);
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
                    winMsg.setPayload(p);
                    oppHandler.sendMessage(winMsg);
                }
                Message loseMsg = new Message(MessageType.GAME_LOSE);
                JsonObject p = new JsonObject();
                p.addProperty("reason", "Ban da dau hang!");
                p.addProperty("scoreDelta", ServerConfig.SCORE_FORFEIT);
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
                session.handleExit(user.getId(), gameManager);
            }
            if (player != null) {
                player.setStatus(Player.STATUS_IDLE);
            }
        }
    }

    /**
     * Sends a protocol message to this client in thread-safe manner.
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
            // Ignored on close
        }
    }

    /**
     * Cleanly disposes of client state upon disconnection.
     */
    private void cleanup() {
        running = false;
        clientManager.removeClient(this);

        // If in game, notify opponent of disconnect (FORFEIT)
        if (user != null) {
            GameSession session = gameManager.getSessionByUserId(user.getId());
            if (session != null && !session.isFinished()) {
                Player opponent = session.getOpponent(user.getId());
                int winnerId = (opponent != null) ? opponent.getUserId() : 0;
                boolean ended = session.finishGame(winnerId, "FORFEIT_DISCONNECT");
                if (ended) {
                    ClientHandler oppHandler = session.getOpponentHandler(user.getId());
                    if (oppHandler != null) {
                        Message dcMsg = new Message(MessageType.OPPONENT_DISCONNECTED);
                        JsonObject p = new JsonObject();
                        p.addProperty("reason", "Doi thu mat ket noi!");
                        p.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                        dcMsg.setPayload(p);
                        oppHandler.sendMessage(dcMsg);
                    }
                    gameManager.removeSession(session.getGameId());
                }
            }
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignored
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