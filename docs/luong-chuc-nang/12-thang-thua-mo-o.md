# Thắng hoặc thua khi mở ô

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

Một ván khác với luồng BFS: A đã mở lần đầu ở (0,0), mìn sinh ra có một quả ở (3,4). A bấm (3,4) ở lượt sau. Trước khi chốt kết quả, A có 60 điểm, B có 40.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [GameBoardPanel.java:96](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L96) | `public void mousePressed(` | Gửi OPEN_CELL với row=3,col=4, giữ click trong pendingOpenCells. |
| 2 | [ClientHandler.java:208](../../src/minesweeperonline/server/ClientHandler.java#L208) | `private void handleOpenCell(` | Chuyển tới session.handleOpenCell(101,3,4). |
| 3 | [GameSession.java:154](../../src/minesweeperonline/server/GameSession.java#L154) | `public void handleOpenCell(` | Gọi Board.openCell rồi nhận OpenResult.MINE. |
| 4 | [Board.java:129](../../src/minesweeperonline/game/Board.java#L129) | `if (target.isMine())` | Đặt exploded=true,opened=true; tăng thao tác, không tăng ô an toàn và trả MINE. |
| 5 | [GameSession.java:180](../../src/minesweeperonline/server/GameSession.java#L180) | `finishGame(opponentPlayer.getUserId(), "CLICKED_MINE")` | Chốt B (102) thắng, dừng timer và lưu trận. A 60→57, B 40→45. |
| 6 | [GameSession.java:521](../../src/minesweeperonline/server/GameSession.java#L521) | `public void attachMines(` | Lấy danh sách mìn của từng bàn để gắn vào gói gửi riêng cho chủ bàn đó. |
| 7 | [GameFrame.java:422](../../src/minesweeperonline/client/ui/GameFrame.java#L422) | `private void onGameEnd(` | Khóa bàn, dừng timer, hiện ResultDialog; đối chiếu click chờ để vẽ ô nổ của A. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "OPEN_CELL",
  "gameId": "a1b2c3d4",
  "payload": {
    "row": 3,
    "col": 4
  }
}
```

Phần cố định server gửi A; gói thực tế còn có payload.mines đầy đủ:

```json
{
  "type": "GAME_LOSE",
  "gameId": "a1b2c3d4",
  "payload": {
    "reason": "Ban da mo phai min tai (3, 4)!",
    "scoreDelta": -3
  }
}
```

`mines` có dạng `[{"r":3,"c":4}, ...]` với đủ 23 tọa độ nếu bàn đã sinh mìn. Dấu ... ở câu này là phần mô tả, không phải nội dung JSON thật. B nhận GAME_WIN, scoreDelta=5 và danh sách mìn **bàn B** nếu đã sinh.

Nhánh MINE gửi gói kết thúc, không gửi CELL_OPENED trước. Nếu A mở đủ 121 ô an toàn thì người thắng là A, reason lưu CLEARED_ALL_SAFE_CELLS. Lần mở đầu ở (3,4) không thể trúng mìn vì vùng 3×3 được bảo vệ.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Đánh dấu ô mìn nổ và trả MINE.

**Code:** [Board.java — dòng 129](../../src/minesweeperonline/game/Board.java#L129). Tìm `if (target.isMine())`.

Trích đoạn bắt đầu tại vị trí trên:

```java
if (target.isMine()) {
    target.setExploded(true);
    target.setOpened(true);
    if (openedList != null) openedList.add(target);
    return OpenResult.MINE;
}
```

### Bước 2. Cho đối thủ thắng và gửi thông báo kết thúc tới hai bên.

**Code:** [GameSession.java — dòng 178](../../src/minesweeperonline/server/GameSession.java#L178). Tìm `if (result == Board.OpenResult.MINE)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
if (result == Board.OpenResult.MINE) {
    // Mở trúng mìn -> thua ngay lập tức
    finishGame(opponentPlayer.getUserId(), "CLICKED_MINE");

    // Báo cho người thua
    Message loseMsg = new Message(MessageType.GAME_LOSE);
    loseMsg.setGameId(gameId);
    JsonObject pLose = new JsonObject();
    pLose.addProperty("reason", "Ban da mo phai min tai (" + row + ", " + col + ")!");
    pLose.addProperty("scoreDelta", ServerConfig.SCORE_DEFEAT);
    attachMines(pLose, actingPlayer.getUserId());
    loseMsg.setPayload(pLose);
    actingHandler.sendMessage(loseMsg);

    // Báo cho người thắng
    Message winMsg = new Message(MessageType.GAME_WIN);
    winMsg.setGameId(gameId);
    JsonObject pWin = new JsonObject();
    pWin.addProperty("reason", "Doi thu [" + actingPlayer.getUsername() + "] da mo phai min!");
    pWin.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
    attachMines(pWin, opponentPlayer.getUserId());
    winMsg.setPayload(pWin);
    opponentHandler.sendMessage(winMsg);

} else if (result == Board.OpenResult.SAFE) {
```

### Bước 3. Người mở hết ô an toàn thắng trận.

**Code:** [GameSession.java — dòng 204](../../src/minesweeperonline/server/GameSession.java#L204). Tìm `if (targetBoard.isAllSafeCellsOpened())`.

Trích đoạn bắt đầu tại vị trí trên:

```java
if (targetBoard.isAllSafeCellsOpened()) {
    finishGame(actingPlayer.getUserId(), "CLEARED_ALL_SAFE_CELLS");

    Message winMsg = new Message(MessageType.GAME_WIN);
    winMsg.setGameId(gameId);
    JsonObject pWin = new JsonObject();
    pWin.addProperty("reason", "Ban da xuat sac mo het tat ca 121 o an toan!");
    pWin.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
    attachMines(pWin, actingPlayer.getUserId());
    winMsg.setPayload(pWin);
    actingHandler.sendMessage(winMsg);

    Message loseMsg = new Message(MessageType.GAME_LOSE);
    loseMsg.setGameId(gameId);
    JsonObject pLose = new JsonObject();
    pLose.addProperty("reason", "Doi thu da hoan thanh mo toan bo 121 o an toan truoc!");
    pLose.addProperty("scoreDelta", ServerConfig.SCORE_DEFEAT);
    attachMines(pLose, opponentPlayer.getUserId());
    loseMsg.setPayload(pLose);
    opponentHandler.sendMessage(loseMsg);

} else {
    // Gửi các ô vừa mở cho người chơi thực hiện thao tác
    Message updateSelf = new Message(MessageType.CELL_OPENED);
    updateSelf.setGameId(gameId);
```

### Bước 4. Chỉ nhánh đổi được AtomicBoolean mới chốt và lưu kết quả.

**Code:** [GameSession.java — dòng 556](../../src/minesweeperonline/server/GameSession.java#L556). Tìm `public boolean finishGame(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public boolean finishGame(int winnerUserId, String reason) {
    if (finished.compareAndSet(false, true)) {
        stopTimers();
        synchronized (gameLock) {
            this.state = GameState.FINISHED;
            this.endedAt = System.currentTimeMillis();
            this.winnerUserId = winnerUserId;
            this.endReason = reason;
        }
        saveMatchToDatabase(winnerUserId, reason);
        return true;
    }
    return false;
```

### Bước 5. Client xử lý kết quả và danh sách mìn kèm theo.

**Code:** [GameFrame.java — dòng 422](../../src/minesweeperonline/client/ui/GameFrame.java#L422). Tìm `private void onGameEnd(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void onGameEnd(String titleOutcome, JsonObject p, boolean isWin) {
    if (gameEnded) return;
    gameEnded = true;
    hideDisconnectOverlay();
    if (readyCountdownTimer != null) readyCountdownTimer.stop();
    if (gameClock != null) gameClock.stop();
    if (opponentDisconnectTimer != null) opponentDisconnectTimer.stop();
    boardPanel.setInteractive(false);
    btnForfeit.setEnabled(false);
    setGameStatus("Trạng thái: KẾT THÚC", UITheme.TEXT_MUTED);

    String reason = p.has("reason") ? p.get("reason").getAsString() : "Trận đấu kết thúc.";
    if (!isWin && isMineClickLoss(reason)) {
        playExplosionOnce();
    } else if (isWin) {
        SoundManager.getInstance().playVictory();
    } else {
        SoundManager.getInstance().playDefeat();
    }

    // Khi thua do click trúng mìn, GAME_LOSE không kèm CELL_OPENED.
    // Đối chiếu click đang chờ với danh sách mìn để giữ hiệu ứng nổ ở đúng ô.
    if (p.has("mines") && p.get("mines").isJsonArray()) {
        List<int[]> mines = new ArrayList<>();
        for (JsonElement el : p.getAsJsonArray("mines")) {
            JsonObject o = el.getAsJsonObject();
```

## Điểm cần nắm

Mỗi người có bàn riêng nên danh sách mìn kết thúc cũng theo từng bàn.

## Trả lời khi bảo vệ

> Trúng mìn thua ngay; mở hết ô an toàn thì thắng. Server chốt, lưu và thông báo kết quả.
