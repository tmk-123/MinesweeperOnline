package minesweeperonline.common.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Khung message chuẩn của protocol cho mọi message trao đổi qua mạng giữa Client và Server.
 * Định dạng:
 * {
 *   "type": "...",
 *   "requestId": "...",
 *   "gameId": "...",
 *   "payload": { ... }
 * }
 */
public class Message {

    private static final Gson gson = new Gson();

    private MessageType type;
    private String requestId;
    private String gameId;
    private JsonObject payload;

    public Message() {
        this.payload = new JsonObject();
    }

    public Message(MessageType type) {
        this.type = type;
        this.payload = new JsonObject();
    }

    public Message(MessageType type, JsonObject payload) {
        this.type = type;
        this.payload = (payload != null) ? payload : new JsonObject();
    }

    public Message(MessageType type, String requestId, String gameId, JsonObject payload) {
        this.type = type;
        this.requestId = requestId;
        this.gameId = gameId;
        this.payload = (payload != null) ? payload : new JsonObject();
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public JsonObject getPayload() {
        if (payload == null) {
            payload = new JsonObject();
        }
        return payload;
    }

    public void setPayload(JsonObject payload) {
        this.payload = payload;
    }

    /**
     * Hàm tiện ích chuyển một POJO bất kỳ thành payload dạng JsonObject.
     */
    public void setPayloadObject(Object object) {
        if (object == null) {
            this.payload = new JsonObject();
        } else if (object instanceof JsonObject) {
            this.payload = (JsonObject) object;
        } else {
            JsonElement tree = gson.toJsonTree(object);
            if (tree.isJsonObject()) {
                this.payload = tree.getAsJsonObject();
            } else {
                this.payload = new JsonObject();
                this.payload.add("data", tree);
            }
        }
    }

    /**
     * Hàm tiện ích      payload thành một class cụ thể.
     */
    public <T> T getPayloadAs(Class<T> clazz) {
        if (payload == null) {
            return null;
        }
        return gson.fromJson(payload, clazz);
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", requestId='" + requestId + '\'' +
                ", gameId='" + gameId + '\'' +
                ", payload=" + payload +
                '}';
    }
}
