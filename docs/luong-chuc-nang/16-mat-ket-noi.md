# Mất kết nối và chờ 15 giây

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Luồng đọc đóng hoặc có lỗi I/O.

## Luồng tổng quát

```text
run → cleanup → handlePlayerDisconnect → chờ hoặc xử thua hết hạn
```

## Các bước và code

### Bước 1. Vòng đọc kết thúc dẫn tới cleanup trong finally.

**Code:** [ClientHandler.java — dòng 91](../../src/minesweeperonline/server/ClientHandler.java#L91). Tìm `while (running && (line = reader.readLine())`.

Trích đoạn bắt đầu tại vị trí trên:

```java
        while (running && (line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            Message message = JsonProtocol.deserialize(line);
            if (message != null) {
                // Payload sai định dạng (vd: thiếu "row") không được làm chết kết nối
                try {
                    handleMessage(message);
                } catch (RuntimeException e) {
                    System.err.println("[ClientHandler] Error handling " + message.getType() + ": " + e);
                    sendMessage(JsonProtocol.createError(message.getRequestId(), "Yeu cau khong hop le!"));
                }
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
```

### Bước 2. Dọn kết nối, queue/lời mời và báo session nếu trận đang chơi.

**Code:** [ClientHandler.java — dòng 694](../../src/minesweeperonline/server/ClientHandler.java#L694). Tìm `private void cleanup()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void cleanup() {
    running = false;
    if (matchmakingManager != null) {
        matchmakingManager.leaveQueue(this);
    }
    clientManager.removeClient(this);

    if (user != null) {
        cancelPendingChallenge();

        GameSession session = gameManager.getSessionByUserId(user.getId());
        if (session != null) {
            if (!session.isFinished()) {
                // Mất kết nối giữa trận: cho phép 15 giây kết nối lại trước khi xử FORFEIT
                session.handlePlayerDisconnect(user.getId(), gameManager);
            } else {
                // Rời phòng khi trận đã kết thúc
                session.handleExit(user.getId(), gameManager);
            }
        }
    }

    try {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
```

### Bước 3. Ghi nhận người mất kết nối, báo đối thủ và đặt thời gian chờ.

**Code:** [GameSession.java — dòng 611](../../src/minesweeperonline/server/GameSession.java#L611). Tìm `public void handlePlayerDisconnect(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void handlePlayerDisconnect(int userId, GameManager gameManager) {
    synchronized (gameLock) {
        if (finished.get() || (state != GameState.READY && state != GameState.PLAYING)) return;
        this.disconnectedUserId = userId;

        System.out.println("[GameSession] Player " + userId + " disconnected. Pausing match " + gameId +
                " for 15s reconnection window...");

        ClientHandler oppHandler = getOpponentHandler(userId);
        if (oppHandler != null) {
            Message msg = new Message(MessageType.OPPONENT_DISCONNECTED);
            msg.setGameId(gameId);
            JsonObject p = new JsonObject();
            p.addProperty("status", "WAITING_RECONNECT");
            p.addProperty("countdownSeconds", 15);
            p.addProperty("disconnectedUsername", getPlayer(userId) != null ? getPlayer(userId).getUsername() : "Doi thu");
            p.addProperty("message", "Đối thủ bị mất kết nối! Đang chờ kết nối lại (15s)...");
            msg.setPayload(p);
            oppHandler.sendMessage(msg);
        }

        if (disconnectGraceTask != null) {
            disconnectGraceTask.cancel(false);
        }

        disconnectGraceTask = timerScheduler.schedule(() -> {
            synchronized (gameLock) {
                if (finished.get() || disconnectedUserId != userId) return;

                System.out.println("[GameSession] 15s reconnection window expired for player " + userId + " in match " + gameId);
                Player opponent = getOpponent(userId);
                int winnerId = (opponent != null) ? opponent.getUserId() : 0;
                if (finishGame(winnerId, "FORFEIT_DISCONNECT_TIMEOUT")) {
                    ClientHandler handler = getOpponentHandler(userId);
                    if (handler != null) {
                        Message forfeitMsg = new Message(MessageType.OPPONENT_DISCONNECTED);
                        forfeitMsg.setGameId(gameId);
                        JsonObject p = new JsonObject();
                        p.addProperty("status", "FORFEIT");
                        p.addProperty("reason", "Đối thủ không kết nối lại sau 15 giây!");
                        p.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                        attachMines(p, winnerId);
                        forfeitMsg.setPayload(p);
                        handler.sendMessage(forfeitMsg);
                    }
```

### Bước 4. Quá hạn thì đối thủ thắng; sau xử lý xóa session.

**Code:** [GameSession.java — dòng 643](../../src/minesweeperonline/server/GameSession.java#L643). Tìm `if (finishGame(winnerId, "FORFEIT_DISCONNECT_TIMEOUT"))`.

Trích đoạn bắt đầu tại vị trí trên:

```java
                if (finishGame(winnerId, "FORFEIT_DISCONNECT_TIMEOUT")) {
                    ClientHandler handler = getOpponentHandler(userId);
                    if (handler != null) {
                        Message forfeitMsg = new Message(MessageType.OPPONENT_DISCONNECTED);
                        forfeitMsg.setGameId(gameId);
                        JsonObject p = new JsonObject();
                        p.addProperty("status", "FORFEIT");
                        p.addProperty("reason", "Đối thủ không kết nối lại sau 15 giây!");
                        p.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                        attachMines(p, winnerId);
                        forfeitMsg.setPayload(p);
                        handler.sendMessage(forfeitMsg);
                    }
                }
                gameManager.removeSession(gameId);
            }
        }, 15, TimeUnit.SECONDS);
    }
}

/**
```

### Bước 5. Client hiển thị thời gian chờ.

**Code:** [GameFrame.java — dòng 522](../../src/minesweeperonline/client/ui/GameFrame.java#L522). Tìm `private void startOpponentDisconnectCountdown(int seconds,`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void startOpponentDisconnectCountdown(int seconds, String dcName) {
    opponentDisconnectSeconds = seconds;
    setGameStatus("Trạng thái: ĐỐI THỦ MẤT KẾT NỐI (Chờ " + opponentDisconnectSeconds + "s...)", UITheme.COLOR_DANGER);

    // 1. Khóa click trên bàn cờ
    boardPanel.setInteractive(false);

    // 2. Tạm dừng đồng hồ ván đấu
    if (gameClock != null) {
        gameClock.stop();
    }

    // 3. Kích hoạt Overlay che mờ toàn màn hình và khóa click
    if (disconnectOverlay != null) {
        disconnectOverlay.update(opponentDisconnectSeconds, dcName);
        disconnectOverlay.setVisible(true);
    }

    if (opponentDisconnectTimer != null) opponentDisconnectTimer.stop();
    opponentDisconnectTimer = new Timer(1000, e -> {
        opponentDisconnectSeconds--;
        if (opponentDisconnectSeconds > 0) {
            setGameStatus("Trạng thái: ĐỐI THỦ MẤT KẾT NỐI (Chờ " + opponentDisconnectSeconds + "s...)", UITheme.COLOR_DANGER);
            if (disconnectOverlay != null) {
                disconnectOverlay.update(opponentDisconnectSeconds, dcName);
```

## Điểm cần nắm

TCP không bảo đảm phát hiện ngay mọi dạng mất mạng. 15 giây tính sau khi server xử lý sự kiện ngắt kết nối.

## Trả lời khi bảo vệ

> Server cho thời gian quay lại, quá hạn thì người bị ngắt kết nối thua.
