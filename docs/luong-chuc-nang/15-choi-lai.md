# Chơi lại

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

Trận a1b2c3d4 đã FINISHED. A bấm Chơi lại trước; B đồng ý sau.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [ResultDialog.java:130](../../src/minesweeperonline/client/ui/ResultDialog.java#L130) | `JsonProtocol.createPlayAgain(` | Nút chơi lại gửi PLAY_AGAIN mang mã trận hiện tại. |
| 2 | [ClientHandler.java:605](../../src/minesweeperonline/server/ClientHandler.java#L605) | `private void handlePlayAgain(` | Tra session theo userId, gọi session.handlePlayAgain(userId,gameManager). |
| 3 | [GameSession.java:396](../../src/minesweeperonline/server/GameSession.java#L396) | `public void handlePlayAgain(` | Chỉ xử lý khi finished=true; thêm ID vào Set playAgainRequests, yêu cầu lặp của A không tính thành hai người. |
| 4 | [GameSession.java:437](../../src/minesweeperonline/server/GameSession.java#L437) | `pReq.addProperty("status", "WAITING")` | Nếu mới có A: gửi WAITING cho A, REQUESTED kèm danh tính A cho B. |
| 5 | [GameManager.java:64](../../src/minesweeperonline/server/GameManager.java#L64) | `public void removeSession(` | Khi có đủ hai ID, xóa session cũ cùng ánh xạ cũ. |
| 6 | [GameManager.java:19](../../src/minesweeperonline/server/GameManager.java#L19) | `public GameSession createSession(` | Tạo mã trận mới, hai Board mới, reset thống kê và bắt đầu READY. |
| 7 | [MainMenuFrame.java:305](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L305) | `private void handleMatchCreated(` | Nhận MATCH_CREATED của ván mới, thay GameFrame cũ bằng cửa sổ mới. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "PLAY_AGAIN",
  "gameId": "a1b2c3d4",
  "payload": {}
}
```

Server → A khi mới có một người yêu cầu:

```json
{
  "type": "PLAY_AGAIN",
  "payload": {
    "status": "WAITING",
    "message": "Đang chờ đối thủ đồng ý chơi lại..."
  }
}
```

Server → B:

```json
{
  "type": "PLAY_AGAIN",
  "payload": {
    "status": "REQUESTED",
    "requestedByUserId": 101,
    "requestedByUsername": "an_demo",
    "message": "Đối thủ [an_demo] muốn tái đấu ván mới!"
  }
}
```

Khi B đồng ý, server gửi MATCH_CREATED có **gameId mới** cho từng người. Không dùng lại bàn cũ, không sinh mìn ngay: phải chờ lần mở đầu của ván mới. Nếu đối thủ đã EXIT và session bị xóa thì một yêu cầu chơi lại không tạo được ván.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Chuyển yêu cầu tới session cũ.

**Code:** [ClientHandler.java — dòng 605](../../src/minesweeperonline/server/ClientHandler.java#L605). Tìm `private void handlePlayAgain(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handlePlayAgain(Message message) {
    if (user == null) return;
    GameSession session = gameManager.getSessionByUserId(user.getId());
    if (session != null) {
        session.handlePlayAgain(user.getId(), gameManager);
    }
```

### Bước 2. Chỉ nhận khi đã kết thúc; thêm userId vào Set.

**Code:** [GameSession.java — dòng 396](../../src/minesweeperonline/server/GameSession.java#L396). Tìm `public void handlePlayAgain(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void handlePlayAgain(int userId, GameManager gameManager) {
    synchronized (gameLock) {
        if (!finished.get()) return;

        playAgainRequests.add(userId);
        Player acting = getPlayer(userId);
        ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
        ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

        if (playAgainRequests.size() >= 2) {
            // Cả hai đều muốn chơi lại -> bắt đầu tái đấu!
            gameManager.removeSession(this.gameId);
```

### Bước 3. Đủ hai người thì xóa session cũ, tạo session mới và gửi MATCH_CREATED.

**Code:** [GameSession.java — dòng 405](../../src/minesweeperonline/server/GameSession.java#L405). Tìm `if (playAgainRequests.size() >= 2)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
if (playAgainRequests.size() >= 2) {
    // Cả hai đều muốn chơi lại -> bắt đầu tái đấu!
    gameManager.removeSession(this.gameId);

    GameSession newSession = gameManager.createSession(
            player1, handler1,
            player2, handler2
    );

    // Gửi MATCH_CREATED cho cả hai người chơi
    Message p1Msg = new Message(MessageType.MATCH_CREATED);
    JsonObject p1 = new JsonObject();
    p1.addProperty("gameId", newSession.getGameId());
    p1.addProperty("opponentId", player2.getUserId());
    p1.addProperty("opponentUsername", player2.getUsername());
    p1Msg.setPayload(p1);
    handler1.sendMessage(p1Msg);

    Message p2Msg = new Message(MessageType.MATCH_CREATED);
    JsonObject p2 = new JsonObject();
    p2.addProperty("gameId", newSession.getGameId());
    p2.addProperty("opponentId", player1.getUserId());
    p2.addProperty("opponentUsername", player1.getUsername());
    p2Msg.setPayload(p2);
    handler2.sendMessage(p2Msg);

    System.out.println("[GameSession] Rematch created: " + newSession.getGameId() +
            " between [" + player1.getUsername() + "] and [" + player2.getUsername() + "]");
} else {
    // Mới có 1 người yêu cầu -> gửi WAITING cho người đó và REQUESTED cho đối thủ
```

### Bước 4. Mới một người thì báo WAITING cho họ và REQUESTED cho đối thủ.

**Code:** [GameSession.java — dòng 437](../../src/minesweeperonline/server/GameSession.java#L437). Tìm `pReq.addProperty("status", "WAITING")`.

Trích đoạn bắt đầu tại vị trí trên:

```java
            pReq.addProperty("status", "WAITING");
            pReq.addProperty("message", "Đang chờ đối thủ đồng ý chơi lại...");
            reqMsg.setPayload(pReq);
            actingHandler.sendMessage(reqMsg);

            Message oppMsg = new Message(MessageType.PLAY_AGAIN);
            JsonObject pOpp = new JsonObject();
            pOpp.addProperty("status", "REQUESTED");
            pOpp.addProperty("requestedByUserId", userId);
            pOpp.addProperty("requestedByUsername", acting.getUsername());
            pOpp.addProperty("message", "Đối thủ [" + acting.getUsername() + "] muốn tái đấu ván mới!");
            oppMsg.setPayload(pOpp);
            opponentHandler.sendMessage(oppMsg);
        }
    }
}
```

### Bước 5. Client mở giao diện trận mới.

**Code:** [MainMenuFrame.java — dòng 305](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L305). Tìm `private void handleMatchCreated(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleMatchCreated(Message message) {
    JsonObject payload = message.getPayload();
    String gameId = payload.get("gameId").getAsString();
    String opponentName = payload.has("opponentUsername") ?
            payload.get("opponentUsername").getAsString() : "Đối thủ";

    onlinePanel.resetChallengeState();
    if (isSearchingMatch) {
        isSearchingMatch = false;
        btnQuickMatch.setText("Tìm trận nhanh");
        btnQuickMatch.setBackground(UITheme.COLOR_PRIMARY);
    }

    int targetState = lastGameExtendedState;
    Rectangle targetBounds = lastGameBounds;

    if (currentGameFrame != null && currentGameFrame.isDisplayable()) {
        targetState = currentGameFrame.getCurrentExtendedState();
        targetBounds = currentGameFrame.getNormalBounds();
        lastGameExtendedState = targetState;
        lastGameBounds = targetBounds;
        currentGameFrame.dispose();
    }

    currentGameFrame = new GameFrame(connection, currentUser, opponentName, gameId, this, targetState, targetBounds);
```

## Điểm cần nắm

Session mới có hai Board mới, chưa sinh mìn. Set tránh đếm lặp yêu cầu của một user.

## Trả lời khi bảo vệ

> Cả hai đồng ý thì server tạo trận mới và đếm ngược lại, không dùng lại bàn cũ.
