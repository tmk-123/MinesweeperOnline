# Mất kết nối và chờ 15 giây

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A bị ngắt socket giữa trận, B còn kết nối. Giả sử trận còn 240 giây.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [ClientHandler.java:82](../../src/minesweeperonline/server/ClientHandler.java#L82) | `public void run()` | readLine trả null hoặc IOException; finally gọi cleanup(). |
| 2 | [ClientHandler.java:714](../../src/minesweeperonline/server/ClientHandler.java#L714) | `private void cleanup()` | Bỏ handler khỏi kết nối/online, hủy lời mời; nếu trận chưa xong gọi session.handlePlayerDisconnect(101,gameManager). |
| 3 | [GameSession.java:616](../../src/minesweeperonline/server/GameSession.java#L616) | `public void handlePlayerDisconnect(` | Đặt disconnectedUserId=101, báo B đang chờ và hẹn xử lý sau 15 giây. |
| 4 | [GameSession.java:119](../../src/minesweeperonline/server/GameSession.java#L119) | `private void startPlayingTimer()` | Tick trả về sớm khi disconnectedUserId>=0 nên không giảm thời gian trận trong lúc chờ. |
| 5 | [GameFrame.java:541](../../src/minesweeperonline/client/ui/GameFrame.java#L541) | `private void startOpponentDisconnectCountdown(int seconds, String dcName)` | B khóa bàn, dừng đồng hồ hiển thị và hiện lớp phủ đếm ngược. |
| 6 | [GameSession.java:648](../../src/minesweeperonline/server/GameSession.java#L648) | `finishGame(winnerId, "FORFEIT_DISCONNECT_TIMEOUT")` | Nếu sau 15 giây A chưa trở lại: B thắng +5, A FORFEIT -5, báo B rồi xóa session. |

## Message và kết quả trong tình huống này

Server → B khi phát hiện ngắt kết nối:

```json
{
  "type": "OPPONENT_DISCONNECTED",
  "gameId": "a1b2c3d4",
  "payload": {
    "status": "WAITING_RECONNECT",
    "countdownSeconds": 15,
    "disconnectedUsername": "an_demo",
    "message": "Đối thủ bị mất kết nối! Đang chờ kết nối lại (15s)..."
  }
}
```

Không có gói do A gửi để báo mình mất mạng: server phát hiện qua đọc socket. Hạn 15 giây bắt đầu khi server xử lý được sự kiện, không bảo đảm trùng lúc rút dây mạng. Quá hạn, B nhận OPPONENT_DISCONNECTED với status=FORFEIT, reason và scoreDelta=5, kèm mìn bàn B nếu đã sinh. A quay lại kịp thì theo luồng 17.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Vòng đọc kết thúc dẫn tới cleanup trong finally.

**Code:** [ClientHandler.java — dòng 94](../../src/minesweeperonline/server/ClientHandler.java#L94). Tìm `while (running && (line = reader.readLine())`.

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

**Code:** [ClientHandler.java — dòng 714](../../src/minesweeperonline/server/ClientHandler.java#L714). Tìm `private void cleanup()`.

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

**Code:** [GameSession.java — dòng 616](../../src/minesweeperonline/server/GameSession.java#L616). Tìm `public void handlePlayerDisconnect(`.

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

**Code:** [GameSession.java — dòng 648](../../src/minesweeperonline/server/GameSession.java#L648). Tìm `if (finishGame(winnerId, "FORFEIT_DISCONNECT_TIMEOUT"))`.

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

**Code:** [GameFrame.java — dòng 541](../../src/minesweeperonline/client/ui/GameFrame.java#L541). Tìm `private void startOpponentDisconnectCountdown(int seconds,`.

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
