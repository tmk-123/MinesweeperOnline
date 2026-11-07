# Chơi lại

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Một hoặc cả hai người gửi PLAY_AGAIN sau kết thúc.

## Luồng tổng quát

```text
PLAY_AGAIN → playAgainRequests → đủ hai người → session mới → MATCH_CREATED
```

## Các bước và code

### Bước 1. Chuyển yêu cầu tới session cũ.

**Code:** [ClientHandler.java — dòng 587](../../src/minesweeperonline/server/ClientHandler.java#L587). Tìm `private void handlePlayAgain(`.

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

**Code:** [GameSession.java — dòng 392](../../src/minesweeperonline/server/GameSession.java#L392). Tìm `public void handlePlayAgain(`.

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

**Code:** [GameSession.java — dòng 401](../../src/minesweeperonline/server/GameSession.java#L401). Tìm `if (playAgainRequests.size() >= 2)`.

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

**Code:** [GameSession.java — dòng 433](../../src/minesweeperonline/server/GameSession.java#L433). Tìm `pReq.addProperty("status", "WAITING")`.

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

**Code:** [MainMenuFrame.java — dòng 313](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L313). Tìm `private void handleMatchCreated(`.

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
