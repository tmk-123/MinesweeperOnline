package minesweeperonline.common.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import minesweeperonline.common.model.MatchHistoryEntry;

import java.util.List;

/**
 * Xử lý serialize/deserialize JSON và cung cấp các hàm tiện ích tạo message
 * cho giao tiếp mạng của MinesweeperOnline.
 */
public class JsonProtocol {

    private static final Gson gson = new GsonBuilder().create();

    public static Gson getGson() {
        return gson;
    }

    /**
     * Serialize một Message thành chuỗi JSON trên một dòng, không có ký tự xuống dòng.
     */
    public static String serialize(Message message) {
        if (message == null) {
            return "{}";
        }
        return gson.toJson(message);
    }

    /**
     * Serialize một Message và thêm ký tự phân tách dòng chuẩn của protocol (\n).
     */
    public static String serializeWithNewline(Message message) {
        return serialize(message) + "\n";
    }

    /**
     * Deserialize chuỗi JSON thành đối tượng Message.
     * Trả về null nếu parse thất bại.
     */
    public static Message deserialize(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json.trim(), Message.class);
        } catch (JsonParseException e) {
            System.err.println("JsonProtocol parse error: " + e.getMessage() + " | payload: " + json);
            return null;
        }
    }

    // ==========================================
    // Các hàm factory tiện ích tạo message
    // ==========================================

    public static Message createRegister(String username, String password) {
        Message msg = new Message(MessageType.REGISTER);
        JsonObject p = new JsonObject();
        p.addProperty("username", username);
        p.addProperty("password", password);
        msg.setPayload(p);
        return msg;
    }

    public static Message createRegisterSuccess(String message) {
        Message msg = new Message(MessageType.REGISTER_SUCCESS);
        JsonObject p = new JsonObject();
        p.addProperty("message", message);
        msg.setPayload(p);
        return msg;
    }

    public static Message createRegisterFailed(String reason) {
        Message msg = new Message(MessageType.REGISTER_FAILED);
        JsonObject p = new JsonObject();
        p.addProperty("reason", reason);
        msg.setPayload(p);
        return msg;
    }

    public static Message createLogin(String username, String password) {
        Message msg = new Message(MessageType.LOGIN);
        JsonObject p = new JsonObject();
        p.addProperty("username", username);
        p.addProperty("password", password);
        msg.setPayload(p);
        return msg;
    }

    public static Message createLoginSuccess(int userId, String username, int totalScore) {
        Message msg = new Message(MessageType.LOGIN_SUCCESS);
        JsonObject p = new JsonObject();
        p.addProperty("userId", userId);
        p.addProperty("username", username);
        p.addProperty("totalScore", totalScore);
        msg.setPayload(p);
        return msg;
    }

    public static Message createLoginFailed(String reason) {
        Message msg = new Message(MessageType.LOGIN_FAILED);
        JsonObject p = new JsonObject();
        p.addProperty("reason", reason);
        msg.setPayload(p);
        return msg;
    }

    public static Message createLogout() {
        return new Message(MessageType.LOGOUT);
    }

    public static Message createGetOnlinePlayers() {
        return new Message(MessageType.GET_ONLINE_PLAYERS);
    }

    public static Message createChallenge(int targetUserId, String targetUsername) {
        Message msg = new Message(MessageType.CHALLENGE);
        JsonObject p = new JsonObject();
        p.addProperty("targetUserId", targetUserId);
        p.addProperty("targetUsername", targetUsername);
        msg.setPayload(p);
        return msg;
    }

    public static Message createChallengeRequest(int fromUserId, String fromUsername) {
        Message msg = new Message(MessageType.CHALLENGE_REQUEST);
        JsonObject p = new JsonObject();
        p.addProperty("fromUserId", fromUserId);
        p.addProperty("fromUsername", fromUsername);
        msg.setPayload(p);
        return msg;
    }

    public static Message createAccept(int challengerUserId) {
        Message msg = new Message(MessageType.ACCEPT);
        JsonObject p = new JsonObject();
        p.addProperty("challengerUserId", challengerUserId);
        msg.setPayload(p);
        return msg;
    }

    public static Message createReject(int challengerUserId) {
        Message msg = new Message(MessageType.REJECT);
        JsonObject p = new JsonObject();
        p.addProperty("challengerUserId", challengerUserId);
        msg.setPayload(p);
        return msg;
    }

    public static Message createQuickMatch() {
        return new Message(MessageType.QUICK_MATCH);
    }

    public static Message createCancelQuickMatch() {
        return new Message(MessageType.CANCEL_QUICK_MATCH);
    }

    public static Message createOpenCell(String gameId, int row, int col) {
        Message msg = new Message(MessageType.OPEN_CELL);
        msg.setGameId(gameId);
        JsonObject p = new JsonObject();
        p.addProperty("row", row);
        p.addProperty("col", col);
        msg.setPayload(p);
        return msg;
    }

    public static Message createPlaceFlag(String gameId, int row, int col) {
        Message msg = new Message(MessageType.PLACE_FLAG);
        msg.setGameId(gameId);
        JsonObject p = new JsonObject();
        p.addProperty("row", row);
        p.addProperty("col", col);
        msg.setPayload(p);
        return msg;
    }

    public static Message createRemoveFlag(String gameId, int row, int col) {
        Message msg = new Message(MessageType.REMOVE_FLAG);
        msg.setGameId(gameId);
        JsonObject p = new JsonObject();
        p.addProperty("row", row);
        p.addProperty("col", col);
        msg.setPayload(p);
        return msg;
    }

    public static Message createForfeit(String gameId) {
        Message msg = new Message(MessageType.FORFEIT);
        msg.setGameId(gameId);
        return msg;
    }

    public static Message createPlayAgain(String gameId) {
        Message msg = new Message(MessageType.PLAY_AGAIN);
        msg.setGameId(gameId);
        return msg;
    }

    public static Message createExit(String gameId) {
        Message msg = new Message(MessageType.EXIT);
        msg.setGameId(gameId);
        return msg;
    }

    public static Message createGetLeaderboard() {
        return new Message(MessageType.GET_LEADERBOARD);
    }

    public static Message createGetMatchHistory(int limit) {
        Message msg = new Message(MessageType.GET_MATCH_HISTORY);
        JsonObject p = new JsonObject();
        p.addProperty("limit", limit);
        msg.setPayload(p);
        return msg;
    }

    public static Message createGetMatchHistory() {
        return createGetMatchHistory(20);
    }

    public static Message createMatchHistory(List<MatchHistoryEntry> list) {
        Message msg = new Message(MessageType.MATCH_HISTORY);
        JsonObject p = new JsonObject();
        p.add("history", gson.toJsonTree(list));
        msg.setPayload(p);
        return msg;
    }

    public static Message createError(String requestId, String errorMessage) {
        Message msg = new Message(MessageType.ERROR);
        msg.setRequestId(requestId);
        JsonObject p = new JsonObject();
        p.addProperty("errorMessage", errorMessage);
        msg.setPayload(p);
        return msg;
    }
}
