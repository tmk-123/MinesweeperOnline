# Đầu hàng

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A đang đấu với B, bấm Đầu hàng rồi xác nhận. A có 60 điểm và B có 40 điểm.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [GameFrame.java:164](../../src/minesweeperonline/client/ui/GameFrame.java#L164) | `btnForfeit.addActionListener(` | Sau khi xác nhận, gửi createForfeit(gameId) qua TCP. |
| 2 | [ClientHandler.java:576](../../src/minesweeperonline/server/ClientHandler.java#L576) | `private void handleForfeit(` | Tìm session theo tài khoản A; nếu chưa kết thúc thì lấy B làm người thắng. |
| 3 | [GameSession.java:556](../../src/minesweeperonline/server/GameSession.java#L556) | `public boolean finishGame(` | Nhận winner=102,reason=FORFEIT; compareAndSet ngăn chốt/lưu hai lần. |
| 4 | [GameSession.java:479](../../src/minesweeperonline/server/GameSession.java#L479) | `public void saveMatchToDatabase(` | A FORFEIT -5, B VICTORY +5: điểm thành 55 và 45. |
| 5 | [ClientHandler.java:595](../../src/minesweeperonline/server/ClientHandler.java#L595) | `p.addProperty("reason", "Ban da dau hang!")` | Gửi GAME_LOSE cho A và GAME_WIN cho B; nhánh này không đặt gameId ngoài payload của phản hồi. |
| 6 | [GameFrame.java:422](../../src/minesweeperonline/client/ui/GameFrame.java#L422) | `private void onGameEnd(` | Dừng ván, khóa thao tác và hiển thị kết quả. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "FORFEIT",
  "gameId": "a1b2c3d4",
  "payload": {}
}
```

Phần cố định server gửi A; có thêm mines nếu bàn đã sinh:

```json
{
  "type": "GAME_LOSE",
  "payload": {
    "reason": "Ban da dau hang!",
    "scoreDelta": -5
  }
}
```

Đầu hàng -5 khác thua vì mìn/hết giờ -3. Điểm tổng chặn ở 0: người chỉ có 2 điểm bị đầu hàng còn 0, nhưng lịch sử vẫn ghi mức thay đổi theo luật là -5. Yêu cầu đầu hàng sau khi trận kết thúc không ghi thêm một trận.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Tạo message yêu cầu đầu hàng.

**Code:** [JsonProtocol.java — dòng 209](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L209). Tìm `public static Message createForfeit(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static Message createForfeit(String gameId) {
    Message msg = new Message(MessageType.FORFEIT);
    msg.setGameId(gameId);
    return msg;
}
```

### Bước 2. Lấy session và cho đối thủ thắng; chỉ báo kết quả khi chốt thành công.

**Code:** [ClientHandler.java — dòng 576](../../src/minesweeperonline/server/ClientHandler.java#L576). Tìm `private void handleForfeit(`.

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

### Bước 4. Client xử lý thông báo kết thúc.

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
```

## Điểm cần nắm

Điểm người đầu hàng theo SCORE_FORFEIT, người thắng theo SCORE_VICTORY.

## Trả lời khi bảo vệ

> Server xử thua người đầu hàng, cho đối thủ thắng và lưu kết quả theo lý do FORFEIT.
