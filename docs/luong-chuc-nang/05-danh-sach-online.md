# Danh sách người chơi online

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A là an_demo (101), B là binh_demo (102). Cả hai đã đăng nhập; B đang rảnh. A mở sảnh hoặc bấm Làm mới.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [MainMenuFrame.java:48](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L48) | `autoRefreshTimer = new Timer(` | Khi sảnh hiện, mỗi 4 giây gọi làm mới online và bảng xếp hạng; nút Làm mới cũng gọi hai tác vụ. |
| 2 | [OnlinePlayersPanel.java:205](../../src/minesweeperonline/client/ui/OnlinePlayersPanel.java#L205) | `public void refreshOnlinePlayers()` | Gửi createGetOnlinePlayers() qua connection dùng chung. |
| 3 | [ClientHandler.java:323](../../src/minesweeperonline/server/ClientHandler.java#L323) | `private void handleGetOnlinePlayers(` | Lấy ID 101 từ tài khoản gắn handler, gọi getOnlinePlayers(101). |
| 4 | [ClientManager.java:89](../../src/minesweeperonline/server/ClientManager.java#L89) | `public List<Player> getOnlinePlayers(` | Duyệt onlineUsers, loại chính A và lấy Player từ các handler còn lại. |
| 5 | [MainMenuFrame.java:259](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L259) | `private void handleOnlinePlayers(` | Chuyển payload.players thành List<Player>, gọi onlinePanel.updatePlayers(players). |
| 6 | [OnlinePlayersPanel.java:210](../../src/minesweeperonline/client/ui/OnlinePlayersPanel.java#L210) | `public void updatePlayers(` | Thay các dòng trong bảng bằng dữ liệu mới. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "GET_ONLINE_PLAYERS",
  "payload": {}
}
```

Server → A:

```json
{
  "type": "ONLINE_PLAYERS",
  "payload": {
    "players": [
      {
        "userId": 102,
        "username": "binh_demo",
        "totalScore": 40,
        "status": "IDLE"
      }
    ]
  }
}
```

Nếu B thoát, lượt tải sau không còn B. A không hiện ở bảng của chính mình. Tài khoản chỉ có trong MySQL nhưng chưa đăng nhập không được tính là online. IDLE là trạng thái rảnh để nhận lời mời.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Tạo yêu cầu lấy danh sách.

**Code:** [JsonProtocol.java — dòng 123](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L123). Tìm `public static Message createGetOnlinePlayers()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static Message createGetOnlinePlayers() {
    return new Message(MessageType.GET_ONLINE_PLAYERS);
}
```

### Bước 2. Server đóng gói userId, tên, điểm và trạng thái.

**Code:** [ClientHandler.java — dòng 323](../../src/minesweeperonline/server/ClientHandler.java#L323). Tìm `private void handleGetOnlinePlayers(`.

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

/** Lấy 10 người đứng đầu qua UserDAO rồi gửi điểm, số trận và tỷ lệ thắng trong LEADERBOARD. */
private void handleGetLeaderboard(Message message) {
```

### Bước 3. Lấy các Player của tài khoản online.

**Code:** [ClientManager.java — dòng 89](../../src/minesweeperonline/server/ClientManager.java#L89). Tìm `public List<Player> getOnlinePlayers(`.

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

**Code:** [MainMenuFrame.java — dòng 259](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L259). Tìm `private void handleOnlinePlayers(`.

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
