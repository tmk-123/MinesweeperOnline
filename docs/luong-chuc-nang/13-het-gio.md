# Hết thời gian thi đấu

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Đồng hồ server giảm về 0.

## Luồng tổng quát

```text
startPlayingTimer → handleTimeout → arbitrateTimeout → finishGame → kết quả
```

## Các bước và code

### Bước 1. Timer giảm thời gian định kỳ, tạm không trừ khi đang chờ kết nối lại.

**Code:** [GameSession.java — dòng 116](../../src/minesweeperonline/server/GameSession.java#L116). Tìm `private void startPlayingTimer()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void startPlayingTimer() {
    matchTimerTask = timerScheduler.scheduleAtFixedRate(() -> {
        synchronized (gameLock) {
            if (finished.get()) {
                stopTimers();
                return;
            }

            // Nếu đang tạm dừng chờ đối thủ kết nối lại thì không trừ thời gian ván đấu
            if (disconnectedUserId >= 0) {
                return;
            }

            remainingSeconds--;
            if (remainingSeconds <= 0) {
                handleTimeout();
            }
        }
    }, 1, 1, TimeUnit.SECONDS);
}

private void stopTimers() {
    if (matchTimerTask != null) {
        matchTimerTask.cancel(false);
```

### Bước 2. Phân xử và gửi hòa hoặc thắng/thua.

**Code:** [GameSession.java — dòng 347](../../src/minesweeperonline/server/GameSession.java#L347). Tìm `private void handleTimeout()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleTimeout() {
    int winner = GameRules.arbitrateTimeout(player1, player2);
    finishGame(winner, "TIMEOUT");

    if (winner == 0) {
        // Hòa (DRAW): mỗi người một message riêng vì danh sách mìn của hai bàn khác nhau
        for (Player p : new Player[]{player1, player2}) {
            Message drawMsg = new Message(MessageType.GAME_DRAW);
            drawMsg.setGameId(gameId);
            JsonObject pd = new JsonObject();
            pd.addProperty("reason", "Het gio thi dau! Ket qua hoa.");
            pd.addProperty("scoreDelta", ServerConfig.SCORE_DRAW);
            attachMines(pd, p.getUserId());
            drawMsg.setPayload(pd);
            (p == player1 ? handler1 : handler2).sendMessage(drawMsg);
        }
    } else {
        ClientHandler winHandler = (winner == player1.getUserId()) ? handler1 : handler2;
        ClientHandler loseHandler = (winner == player1.getUserId()) ? handler2 : handler1;
        int loser = (winner == player1.getUserId()) ? player2.getUserId() : player1.getUserId();

        Message winMsg = new Message(MessageType.GAME_WIN);
        winMsg.setGameId(gameId);
        JsonObject pw = new JsonObject();
        pw.addProperty("reason", "Het gio! Ban thang nho chi so vuot troi.");
        pw.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
        attachMines(pw, winner);
```

### Bước 3. So số ô an toàn, số thao tác, rồi số lần cắm cờ; bằng hết thì hòa.

**Code:** [GameRules.java — dòng 21](../../src/minesweeperonline/game/GameRules.java#L21). Tìm `public static int arbitrateTimeout(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
    public static int arbitrateTimeout(Player p1, Player p2) {
        // Tiêu chí 1: Mở được nhiều ô an toàn nhất
        if (p1.getOpenedSafeCells() > p2.getOpenedSafeCells()) {
            return p1.getUserId();
        } else if (p2.getOpenedSafeCells() > p1.getOpenedSafeCells()) {
            return p2.getUserId();
        }

        // Tiêu chí 2: Tổng số thao tác ít nhất
        if (p1.getTotalActions() < p2.getTotalActions()) {
            return p1.getUserId();
        } else if (p2.getTotalActions() < p1.getTotalActions()) {
            return p2.getUserId();
        }

        // Tiêu chí 3: Cắm được nhiều cờ nhất
        if (p1.getFlagsPlaced() > p2.getFlagsPlaced()) {
            return p1.getUserId();
        } else if (p2.getFlagsPlaced() > p1.getFlagsPlaced()) {
            return p2.getUserId();
        }

        // Tiêu chí 4: Bằng nhau hoàn toàn -> hòa (DRAW)
        return 0;
    }
}
```

### Bước 4. Client hiển thị kết quả phân xử.

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

Ưu tiên nhiều ô an toàn, ít thao tác, nhiều flagsPlaced. Tiêu chí cuối không phải số cờ đúng hay currentFlags.

## Trả lời khi bảo vệ

> Hết giờ, server dùng GameRules để chọn người thắng theo các chỉ số hoặc cho hòa.
