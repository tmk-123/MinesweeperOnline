# Ghép trận nhanh và hủy tìm

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A bấm Tìm trận nhanh khi hàng đợi rỗng; một lúc sau B cũng bấm tìm.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [MainMenuFrame.java:176](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L176) | `private void toggleQuickMatch()` | A gửi QUICK_MATCH và đổi chữ nút sang đang tìm. |
| 2 | [ClientHandler.java:391](../../src/minesweeperonline/server/ClientHandler.java#L391) | `private void handleQuickMatch(` | Kiểm tra đăng nhập, gọi matchmakingManager.joinQueue(this). |
| 3 | [MatchmakingManager.java:31](../../src/minesweeperonline/server/MatchmakingManager.java#L31) | `public synchronized void joinQueue(` | Lượt A: chưa có đối thủ nên đặt QUEUED và thêm A. Lượt B: poll A, kiểm tra kết nối và tài khoản còn hợp lệ. |
| 4 | [GameManager.java:19](../../src/minesweeperonline/server/GameManager.java#L19) | `public GameSession createSession(` | Tạo session cho A/B, hai bàn riêng và ánh xạ hai userId tới mã trận. |
| 5 | [MainMenuFrame.java:305](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L305) | `private void handleMatchCreated(` | Hai phía nhận mã trận/tên đối thủ và mở GameFrame. |
| 6 | [MatchmakingManager.java:103](../../src/minesweeperonline/server/MatchmakingManager.java#L103) | `public synchronized void leaveQueue(` | Nếu hủy trước khi ghép, CANCEL_QUICK_MATCH dẫn đến xóa khỏi queue và trả Player về IDLE. |

## Message và kết quả trong tình huống này

Client → server:

```json
{
  "type": "QUICK_MATCH",
  "payload": {}
}
```

Sau khi B vào, server → A:

```json
{
  "type": "MATCH_CREATED",
  "payload": {
    "gameId": "a1b2c3d4",
    "opponentId": 102,
    "opponentUsername": "binh_demo"
  }
}
```

Nếu hủy trong lúc còn chờ, client → server:

```json
{
  "type": "CANCEL_QUICK_MATCH",
  "payload": {}
}
```

Khi chưa có đối thủ, code không gửi gói xác nhận QUEUED riêng; client tự hiện trạng thái chờ. joinQueue/leaveQueue dùng synchronized để các thread client không sửa hàng đợi cùng lúc. MATCH_CREATED của B chứa tên/ID của A.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Client gửi tìm hoặc hủy tìm trận.

**Code:** [MainMenuFrame.java — dòng 176](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L176). Tìm `private void toggleQuickMatch()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
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
```

### Bước 2. Handler chuyển yêu cầu cho MatchmakingManager.

**Code:** [ClientHandler.java — dòng 391](../../src/minesweeperonline/server/ClientHandler.java#L391). Tìm `private void handleQuickMatch(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleQuickMatch(Message message) {
    if (user == null || player == null) {
        sendMessage(JsonProtocol.createError(message.getRequestId(), "Vui lòng đăng nhập trước khi tìm trận!"));
        return;
    }
    if (matchmakingManager != null) {
        matchmakingManager.joinQueue(this);
    }
}
```

### Bước 3. Kiểm tra người chơi và tìm đối thủ còn hợp lệ.

**Code:** [MatchmakingManager.java — dòng 31](../../src/minesweeperonline/server/MatchmakingManager.java#L31). Tìm `public synchronized void joinQueue(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public synchronized void joinQueue(ClientHandler client) {
    if (client == null || client.getUser() == null || client.getPlayer() == null) {
        return;
    }

    int userId = client.getUser().getId();
    if (gameManager.isUserInGame(userId)) {
        client.sendMessage(JsonProtocol.createError(null, "Bạn đang trong một ván đấu khác!"));
        return;
    }

    // Bỏ khỏi hàng đợi nếu đã có từ trước để tránh trùng lặp
    queue.remove(client);

    // Tìm một đối thủ hợp lệ đang chờ trong hàng đợi
    ClientHandler opponent = null;
    while (!queue.isEmpty()) {
        ClientHandler candidate = queue.poll();
        if (candidate != null && candidate != client
                && candidate.isRunning()
                && candidate.getUser() != null
                && candidate.getPlayer() != null
                && clientManager.isUserOnline(candidate.getUser().getId())
                && !gameManager.isUserInGame(candidate.getUser().getId())) {
            opponent = candidate;
            break;
        }
    }

    if (opponent != null) {
```

### Bước 4. Có đối thủ thì tạo trận và gửi MATCH_CREATED; không có thì đưa vào queue.

**Code:** [MatchmakingManager.java — dòng 60](../../src/minesweeperonline/server/MatchmakingManager.java#L60). Tìm `if (opponent != null)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
if (opponent != null) {
    // Đã tìm thấy đối thủ -> Chuyển cả hai sang PLAYING và tạo ván đấu mới
    opponent.getPlayer().setStatus(Player.STATUS_PLAYING);
    client.getPlayer().setStatus(Player.STATUS_PLAYING);

    GameSession session = gameManager.createSession(
            opponent.getPlayer(), opponent,
            client.getPlayer(), client
    );

    System.out.println("[Matchmaking] Match found: [" + opponent.getUser().getUsername() +
            "] vs [" + client.getUser().getUsername() + "] -> GameId: " + session.getGameId());

    // Gửi MATCH_CREATED cho đối thủ (Player 1)
    Message matchCreatedP1 = new Message(MessageType.MATCH_CREATED);
    JsonObject p1Data = new JsonObject();
    p1Data.addProperty("gameId", session.getGameId());
    p1Data.addProperty("opponentId", client.getUser().getId());
    p1Data.addProperty("opponentUsername", client.getUser().getUsername());
    matchCreatedP1.setPayload(p1Data);
    opponent.sendMessage(matchCreatedP1);

    // Gửi MATCH_CREATED cho người vừa tìm (Player 2)
    Message matchCreatedP2 = new Message(MessageType.MATCH_CREATED);
    JsonObject p2Data = new JsonObject();
```

### Bước 5. Xóa khỏi hàng đợi và trả về IDLE khi phù hợp.

**Code:** [MatchmakingManager.java — dòng 103](../../src/minesweeperonline/server/MatchmakingManager.java#L103). Tìm `public synchronized void leaveQueue(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public synchronized void leaveQueue(ClientHandler client) {
    if (client == null) return;
    boolean removed = queue.remove(client);
    if (removed) {
        if (client.getPlayer() != null && Player.STATUS_QUEUED.equals(client.getPlayer().getStatus())) {
            client.getPlayer().setStatus(Player.STATUS_IDLE);
        }
        if (client.getUser() != null) {
            System.out.println("[Matchmaking] User left queue: " + client.getUser().getUsername() +
                    " | Total in queue: " + queue.size());
        }
    }
}
```

## Điểm cần nắm

Các thao tác queue được synchronized. Đối thủ phải còn kết nối, online và chưa ở trận khác.

## Trả lời khi bảo vệ

> Người đến trước chờ trong hàng đợi; có người hợp lệ tiếp theo thì server ghép vào một trận.
