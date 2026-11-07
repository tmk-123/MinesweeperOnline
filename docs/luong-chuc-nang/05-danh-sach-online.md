# Danh sách người chơi online

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Client yêu cầu danh sách ở sảnh.

## Luồng tổng quát

```text
GET_ONLINE_PLAYERS → ClientManager → ONLINE_PLAYERS → MainMenuFrame
```

## Các bước và code

### Bước 1. Tạo yêu cầu lấy danh sách.

**Code:** [JsonProtocol.java — dòng 116](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L116). Tìm `public static Message createGetOnlinePlayers()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static Message createGetOnlinePlayers() {
    return new Message(MessageType.GET_ONLINE_PLAYERS);
}
```

### Bước 2. Server đóng gói userId, tên, điểm và trạng thái.

**Code:** [ClientHandler.java — dòng 314](../../src/minesweeperonline/server/ClientHandler.java#L314). Tìm `private void handleGetOnlinePlayers(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
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
```

### Bước 3. Lấy các Player của tài khoản online.

**Code:** [ClientManager.java — dòng 82](../../src/minesweeperonline/server/ClientManager.java#L82). Tìm `public List<Player> getOnlinePlayers(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public List<Player> getOnlinePlayers(int excludeUserId) {
    List<Player> list = new ArrayList<>();
    for (ClientHandler handler : onlineUsers.values()) {
        Player p = handler.getPlayer(); // chỉ đọc một lần: có thể thành null khi LOGOUT
        if (p != null && p.getUserId() != excludeUserId) {
            list.add(p);
        }
    }
    return list;
}
```

### Bước 4. Client cập nhật danh sách người chơi.

**Code:** [MainMenuFrame.java — dòng 270](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L270). Tìm `private void handleOnlinePlayers(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
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
```

## Điểm cần nắm

Socket đang kết nối và tài khoản đã đăng nhập là hai khái niệm khác nhau.

## Trả lời khi bảo vệ

> Server gửi thông tin và trạng thái của những tài khoản online để sảnh hiển thị.
