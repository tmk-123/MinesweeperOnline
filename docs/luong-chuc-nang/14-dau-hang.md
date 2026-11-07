# Đầu hàng

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Người chơi gửi FORFEIT.

## Luồng tổng quát

```text
FORFEIT → handleForfeit → finishGame → GAME_WIN/GAME_LOSE
```

## Các bước và code

### Bước 1. Tạo message yêu cầu đầu hàng.

**Code:** [JsonProtocol.java — dòng 192](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L192). Tìm `public static Message createForfeit(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static Message createForfeit(String gameId) {
    Message msg = new Message(MessageType.FORFEIT);
    msg.setGameId(gameId);
    return msg;
}
```

### Bước 2. Lấy session và cho đối thủ thắng; chỉ báo kết quả khi chốt thành công.

**Code:** [ClientHandler.java — dòng 559](../../src/minesweeperonline/server/ClientHandler.java#L559). Tìm `private void handleForfeit(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleForfeit(Message message) {
    if (user == null) return;
    GameSession session = gameManager.getSessionByUserId(user.getId());
    if (session != null && !session.isFinished()) {
        Player opp = session.getOpponent(user.getId());
        int winnerId = (opp != null) ? opp.getUserId() : 0;
        if (session.finishGame(winnerId, "FORFEIT")) {
            ClientHandler oppHandler = session.getOpponentHandler(user.getId());
            if (oppHandler != null) {
                Message winMsg = new Message(MessageType.GAME_WIN);
                JsonObject p = new JsonObject();
                p.addProperty("reason", "Doi thu da dau hang!");
                p.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                session.attachMines(p, winnerId);
                winMsg.setPayload(p);
                oppHandler.sendMessage(winMsg);
            }
            Message loseMsg = new Message(MessageType.GAME_LOSE);
            JsonObject p = new JsonObject();
            p.addProperty("reason", "Ban da dau hang!");
            p.addProperty("scoreDelta", ServerConfig.SCORE_FORFEIT);
            session.attachMines(p, user.getId());
            loseMsg.setPayload(p);
            sendMessage(loseMsg);
        }
    }
}
```

### Bước 3. Dừng timer và lưu kết quả.

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

### Bước 4. Client xử lý thông báo kết thúc.

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
```

## Điểm cần nắm

Điểm người đầu hàng theo SCORE_FORFEIT, người thắng theo SCORE_VICTORY.

## Trả lời khi bảo vệ

> Server xử thua người đầu hàng, cho đối thủ thắng và lưu kết quả theo lý do FORFEIT.
