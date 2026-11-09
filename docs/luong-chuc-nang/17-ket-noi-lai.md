# Khôi phục sau kết nối lại

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A đăng nhập lại trong hạn 15 giây bằng socket mới. B vẫn chờ. Bàn A có 9 ô mở như luồng BFS, không cờ, 1 thao tác; còn 240 giây.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [ServerConnection.java:67](../../src/minesweeperonline/client/network/ServerConnection.java#L67) | `public synchronized boolean connect(` | Mở TCP mới rồi gửi LOGIN bằng tài khoản cũ. |
| 2 | [ClientHandler.java:244](../../src/minesweeperonline/server/ClientHandler.java#L244) | `private void handleLogin(` | Xác thực, đăng ký handler mới, gửi LOGIN_SUCCESS rồi tra session chưa kết thúc của A. |
| 3 | [GameSession.java:671](../../src/minesweeperonline/server/GameSession.java#L671) | `public boolean handlePlayerReconnect(` | Chỉ nhận khi chưa finished và disconnectedUserId=101; hủy lịch hết hạn, đặt -1 và thay handler1/handler2 tương ứng. |
| 4 | [GameSession.java:692](../../src/minesweeperonline/server/GameSession.java#L692) | `Message oppMsg = new Message(MessageType.OPPONENT_RECONNECTED)` | B nhận thông báo A đã trở lại để bỏ lớp phủ và chạy tiếp đồng hồ. |
| 5 | [GameSession.java:702](../../src/minesweeperonline/server/GameSession.java#L702) | `Message recMsg = new Message(MessageType.GAME_RECONNECTED)` | Đọc Board đang giữ trên server, đóng gói đồng hồ, thống kê, ô mở/cờ và tọa độ tiến độ đối thủ. |
| 6 | [MainMenuFrame.java:335](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L335) | `private void handleGameReconnected(` | A tạo GameFrame theo mã trận cũ và gọi restoreGameState(payload). |
| 7 | [GameFrame.java:710](../../src/minesweeperonline/client/ui/GameFrame.java#L710) | `public void restoreGameState(` | Khôi phục đồng hồ, revealCell/setCellFlagged trên bàn mình và dựng bàn thu nhỏ đối thủ. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "LOGIN",
  "payload": {
    "username": "an_demo",
    "password": "123456"
  }
}
```

Sau LOGIN_SUCCESS, server → A:

```json
{
  "type": "GAME_RECONNECTED",
  "gameId": "a1b2c3d4",
  "payload": {
    "gameId": "a1b2c3d4",
    "opponentId": 102,
    "opponentUsername": "binh_demo",
    "remainingSeconds": 240,
    "mySafeCells": 9,
    "myFlags": 0,
    "myActions": 1,
    "oppSafeCells": 0,
    "oppFlags": 0,
    "oppActions": 0,
    "myOpenedCells": [
      {
        "row": 3,
        "col": 4,
        "adjacentMines": 0,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 2,
        "col": 3,
        "adjacentMines": 5,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 2,
        "col": 4,
        "adjacentMines": 3,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 2,
        "col": 5,
        "adjacentMines": 5,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 3,
        "col": 3,
        "adjacentMines": 3,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 3,
        "col": 5,
        "adjacentMines": 3,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 4,
        "col": 3,
        "adjacentMines": 5,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 4,
        "col": 4,
        "adjacentMines": 3,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 4,
        "col": 5,
        "adjacentMines": 5,
        "isMine": false,
        "isExploded": false
      }
    ],
    "myFlaggedCells": [],
    "oppOpenedCoords": [],
    "oppFlaggedCoords": []
  }
}
```

Không gửi vị trí mìn chưa lộ, không gọi generateMines lại. Server phải còn chạy và giữ session trong RAM; khởi động lại server làm mất ván dở. Nếu quá hạn/session bị xóa, LOGIN chỉ đưa A về sảnh. ServerConnection hiện không tự nối lại khi mất mạng: cần bước mở kết nối và đăng nhập lại.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Sau đăng nhập, tìm session chưa kết thúc.

**Code:** [ClientHandler.java — dòng 289](../../src/minesweeperonline/server/ClientHandler.java#L289). Tìm `GameSession activeSession = gameManager.getSessionByUserId`.

Trích đoạn bắt đầu tại vị trí trên:

```java
GameSession activeSession = gameManager.getSessionByUserId(dbUser.getId());
if (activeSession != null && !activeSession.isFinished()) {
    activeSession.handlePlayerReconnect(dbUser.getId(), this, this.player);
}
```

### Bước 2. Kiểm tra đúng người, hủy timer chờ và gắn handler mới.

**Code:** [GameSession.java — dòng 671](../../src/minesweeperonline/server/GameSession.java#L671). Tìm `public boolean handlePlayerReconnect(`.

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

**Code:** [GameSession.java — dòng 702](../../src/minesweeperonline/server/GameSession.java#L702). Tìm `Message recMsg = new Message(MessageType.GAME_RECONNECTED)`.

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

**Code:** [MainMenuFrame.java — dòng 335](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L335). Tìm `private void handleGameReconnected(`.

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

**Code:** [GameFrame.java — dòng 710](../../src/minesweeperonline/client/ui/GameFrame.java#L710). Tìm `public void restoreGameState(`.

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
