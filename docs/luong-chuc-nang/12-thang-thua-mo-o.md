# Thắng hoặc thua khi mở ô

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Mở trúng mìn hoặc mở đủ 121 ô an toàn.

## Luồng tổng quát

```text
Board → GameSession → finishGame → GAME_WIN/GAME_LOSE → onGameEnd
```

## Các bước và code

### Bước 1. Đánh dấu ô mìn nổ và trả MINE.

**Code:** [Board.java — dòng 126](../../src/minesweeperonline/game/Board.java#L126). Tìm `if (target.isMine())`.

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

**Code:** [GameSession.java — dòng 174](../../src/minesweeperonline/server/GameSession.java#L174). Tìm `if (result == Board.OpenResult.MINE)`.

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

**Code:** [GameSession.java — dòng 200](../../src/minesweeperonline/server/GameSession.java#L200). Tìm `if (targetBoard.isAllSafeCellsOpened())`.

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

**Code:** [GameSession.java — dòng 551](../../src/minesweeperonline/server/GameSession.java#L551). Tìm `public boolean finishGame(`.

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

**Code:** [GameFrame.java — dòng 408](../../src/minesweeperonline/client/ui/GameFrame.java#L408). Tìm `private void onGameEnd(`.

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
