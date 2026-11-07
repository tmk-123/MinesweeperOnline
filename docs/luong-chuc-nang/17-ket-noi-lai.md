# Khôi phục sau kết nối lại

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Người chơi đăng nhập lại trong lúc session vẫn chờ.

## Luồng tổng quát

```text
LOGIN → activeSession → handlePlayerReconnect → GAME_RECONNECTED → restoreGameState
```

## Các bước và code

### Bước 1. Sau đăng nhập, tìm session chưa kết thúc.

**Code:** [ClientHandler.java — dòng 282](../../src/minesweeperonline/server/ClientHandler.java#L282). Tìm `GameSession activeSession = gameManager.getSessionByUserId`.

Trích đoạn bắt đầu tại vị trí trên:

```java
GameSession activeSession = gameManager.getSessionByUserId(dbUser.getId());
if (activeSession != null && !activeSession.isFinished()) {
    activeSession.handlePlayerReconnect(dbUser.getId(), this, this.player);
}
```

### Bước 2. Kiểm tra đúng người, hủy timer chờ và gắn handler mới.

**Code:** [GameSession.java — dòng 666](../../src/minesweeperonline/server/GameSession.java#L666). Tìm `public boolean handlePlayerReconnect(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public boolean handlePlayerReconnect(int userId, ClientHandler newHandler, Player newPlayer) {
    synchronized (gameLock) {
        if (finished.get() || disconnectedUserId != userId) return false;

        System.out.println("[GameSession] Player " + userId + " successfully reconnected to match " + gameId + "! Resuming match.");

        if (disconnectGraceTask != null) {
            disconnectGraceTask.cancel(false);
            disconnectGraceTask = null;
        }
        this.disconnectedUserId = -1;

        if (userId == player1.getUserId()) {
            this.handler1 = newHandler;
        } else {
            this.handler2 = newHandler;
        }

        // Báo đối thủ là người chơi đã kết nối lại
        ClientHandler oppHandler = getOpponentHandler(userId);
        if (oppHandler != null) {
            Message oppMsg = new Message(MessageType.OPPONENT_RECONNECTED);
            oppMsg.setGameId(gameId);
            JsonObject p = new JsonObject();
            p.addProperty("reconnectedUsername", newPlayer.getUsername());
```

### Bước 3. Đóng gói thời gian, thống kê, ô mở và cờ để khôi phục.

**Code:** [GameSession.java — dòng 697](../../src/minesweeperonline/server/GameSession.java#L697). Tìm `Message recMsg = new Message(MessageType.GAME_RECONNECTED)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
Message recMsg = new Message(MessageType.GAME_RECONNECTED);
recMsg.setGameId(gameId);
JsonObject p = new JsonObject();
p.addProperty("gameId", gameId);

Player opp = getOpponent(userId);
p.addProperty("opponentId", opp != null ? opp.getUserId() : 0);
p.addProperty("opponentUsername", opp != null ? opp.getUsername() : "Doi thu");
p.addProperty("remainingSeconds", remainingSeconds);

Board myBoard = getBoardForPlayer(userId);
Board oppBoard = (myBoard == board1) ? board2 : board1;

p.addProperty("mySafeCells", myBoard.getOpenedSafeCells());
p.addProperty("myFlags", myBoard.getCurrentFlags());
p.addProperty("myActions", myBoard.getTotalActions());

p.addProperty("oppSafeCells", oppBoard.getOpenedSafeCells());
p.addProperty("oppFlags", oppBoard.getCurrentFlags());
p.addProperty("oppActions", oppBoard.getTotalActions());

JsonArray myOpenedArr = new JsonArray();
for (int r = 0; r < 12; r++) {
    for (int c = 0; c < 12; c++) {
        Cell cell = myBoard.getCell(r, c);
        if (cell.isOpened()) {
            JsonObject o = new JsonObject();
            o.addProperty("row", r);
            o.addProperty("col", c);
            o.addProperty("adjacentMines", cell.getAdjacentMines());
            o.addProperty("isMine", cell.isMine());
            o.addProperty("isExploded", cell.isExploded());
            myOpenedArr.add(o);
        }
    }
```

### Bước 4. Client mở lại giao diện trận.

**Code:** [MainMenuFrame.java — dòng 342](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L342). Tìm `private void handleGameReconnected(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleGameReconnected(Message message) {
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

### Bước 5. Dựng lại các ô và cờ từ dữ liệu server.

**Code:** [GameFrame.java — dòng 688](../../src/minesweeperonline/client/ui/GameFrame.java#L688). Tìm `public void restoreGameState(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void restoreGameState(JsonObject p) {
    if (readyCountdownTimer != null) readyCountdownTimer.stop();

    // 1. Khôi phục đồng hồ
    if (p.has("remainingSeconds")) {
        this.remainingSeconds = p.get("remainingSeconds").getAsInt();
    }
    int mins = remainingSeconds / 60;
    int secs = remainingSeconds % 60;
    infoPanel.updateMatchTimer(String.format("%02d:%02d", mins, secs));

    if (gameClock != null) gameClock.stop();
    gameClock = new Timer(1000, e -> {
        remainingSeconds--;
        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        infoPanel.updateMatchTimer(String.format("%02d:%02d", m, s));
        if (remainingSeconds <= 5 && remainingSeconds > 0) {
            SoundManager.getInstance().playTick();
        }
        if (remainingSeconds <= 0) {
            gameClock.stop();
        }
    });
    gameClock.start();

    // 2. Khôi phục thống kê
    if (p.has("mySafeCells")) mySafeCells = p.get("mySafeCells").getAsInt();
    if (p.has("myFlags")) myFlags = p.get("myFlags").getAsInt();
    if (p.has("myActions")) myActions = p.get("myActions").getAsInt();
```

## Điểm cần nắm

Giữ nguyên Board trên server, không sinh mìn lại và không tạo trận mới.

## Trả lời khi bảo vệ

> Server nối handler mới vào session cũ rồi gửi trạng thái để dựng lại giao diện.
