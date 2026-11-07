# Gỡ cờ

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Click phải vào ô có cờ.

## Luồng tổng quát

```text
REMOVE_FLAG → handleRemoveFlag → Board.removeFlag → FLAG_REMOVED + GAME_UPDATE
```

## Các bước và code

### Bước 1. Client gửi yêu cầu gỡ cờ.

**Code:** [GameBoardPanel.java — dòng 104](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L104). Tìm `connection.sendMessage(JsonProtocol.createRemoveFlag`.

Trích đoạn bắt đầu tại vị trí trên:

```java
connection.sendMessage(JsonProtocol.createRemoveFlag(gameId, row, col));
```

### Bước 2. Chuyển yêu cầu tới session.

**Code:** [ClientHandler.java — dòng 226](../../src/minesweeperonline/server/ClientHandler.java#L226). Tìm `private void handleRemoveFlag(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleRemoveFlag(Message message) {
    if (user == null) return;
    GameSession session = gameManager.getSessionByUserId(user.getId());
    if (session != null) {
        JsonObject p = message.getPayload();
        int row = p.get("row").getAsInt();
        int col = p.get("col").getAsInt();
        session.handleRemoveFlag(user.getId(), row, col);
    }
}
```

### Bước 3. Server xử lý và gửi xác nhận nếu gỡ thành công.

**Code:** [GameSession.java — dòng 304](../../src/minesweeperonline/server/GameSession.java#L304). Tìm `public void handleRemoveFlag(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void handleRemoveFlag(int userId, int row, int col) {
    synchronized (gameLock) {
        if (finished.get() || state != GameState.PLAYING || disconnectedUserId >= 0) return;
        Board targetBoard = getBoardForPlayer(userId);
        Player actingPlayer = getPlayer(userId);
        ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
        ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

        if (targetBoard != null && targetBoard.removeFlag(row, col)) {
            actingPlayer.setCurrentFlags(targetBoard.getCurrentFlags());
            actingPlayer.setFlagsRemoved(targetBoard.getFlagsRemoved());
            actingPlayer.setTotalActions(targetBoard.getTotalActions());

            Message res = new Message(MessageType.FLAG_REMOVED);
            res.setGameId(gameId);
            JsonObject p = new JsonObject();
            p.addProperty("row", row);
            p.addProperty("col", col);
            p.addProperty("currentFlags", targetBoard.getCurrentFlags());
            p.addProperty("totalActions", targetBoard.getTotalActions());
            res.setPayload(p);
            actingHandler.sendMessage(res);

            // Báo đối thủ số cờ hiện tại & vị trí gỡ cờ trên bàn cờ thu nhỏ
            Message oppUpdate = new Message(MessageType.GAME_UPDATE);
            oppUpdate.setGameId(gameId);
            JsonObject op = new JsonObject();
            op.addProperty("opponentId", actingPlayer.getUserId());
            op.addProperty("opponentOpenedSafeCells", targetBoard.getOpenedSafeCells());
            op.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
            op.addProperty("opponentTotalActions", targetBoard.getTotalActions());
            op.addProperty("flagRow", row);
            op.addProperty("flagCol", col);
            op.addProperty("flagState", false);
```

### Bước 4. Chỉ gỡ ô có cờ; giảm currentFlags, tăng flagsRemoved và totalActions.

**Code:** [Board.java — dòng 197](../../src/minesweeperonline/game/Board.java#L197). Tìm `public boolean removeFlag(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public boolean removeFlag(int r, int c) {
    if (!isValidCoordinate(r, c)) return false;
    Cell cell = grid[r][c];
    if (!cell.isFlagged()) {
        return false;
    }

    cell.setFlagged(false);
    if (this.currentFlags > 0) {
        this.currentFlags--;
    }
    this.flagsRemoved++;
    this.totalActions++;
    return true;
}
```

### Bước 5. Client cập nhật ô và số cờ.

**Code:** [GameFrame.java — dòng 367](../../src/minesweeperonline/client/ui/GameFrame.java#L367). Tìm `private void onFlagRemoved(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void onFlagRemoved(JsonObject p) {
    int r = p.get("row").getAsInt();
    int c = p.get("col").getAsInt();
    boardPanel.setCellFlagged(r, c, false);
    if (p.has("currentFlags")) {
        myFlags = p.get("currentFlags").getAsInt();
    }
    if (p.has("totalActions")) {
        myActions = p.get("totalActions").getAsInt();
    }
    infoPanel.updateSelfStats(mySafeCells, myFlags, myActions);
    SoundManager.getInstance().playFlag();
```

## Điểm cần nắm

Gỡ cờ không giảm flagsPlaced vì đó là tổng số lần cắm, khác số cờ đang có currentFlags.

## Trả lời khi bảo vệ

> Server gỡ cờ, giảm số cờ hiện tại và tăng thống kê thao tác rồi báo client.
