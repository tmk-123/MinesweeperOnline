# Ghép trận nhanh và hủy tìm

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Người chơi bật hoặc tắt tìm trận nhanh.

## Luồng tổng quát

```text
QUICK_MATCH → joinQueue → chờ hoặc createSession → MATCH_CREATED
```

## Các bước và code

### Bước 1. Client gửi tìm hoặc hủy tìm trận.

**Code:** [MainMenuFrame.java — dòng 191](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L191). Tìm `private void toggleQuickMatch()`.

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

**Code:** [ClientHandler.java — dòng 379](../../src/minesweeperonline/server/ClientHandler.java#L379). Tìm `private void handleQuickMatch(`.

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

**Code:** [MatchmakingManager.java — dòng 30](../../src/minesweeperonline/server/MatchmakingManager.java#L30). Tìm `public synchronized void joinQueue(`.

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

**Code:** [MatchmakingManager.java — dòng 59](../../src/minesweeperonline/server/MatchmakingManager.java#L59). Tìm `if (opponent != null)`.

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

**Code:** [MatchmakingManager.java — dòng 102](../../src/minesweeperonline/server/MatchmakingManager.java#L102). Tìm `public synchronized void leaveQueue(`.

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
