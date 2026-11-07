# Cắm cờ

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Click phải trên ô đóng chưa có cờ.

## Luồng tổng quát

```text
PLACE_FLAG → handlePlaceFlag → Board.placeFlag → FLAG_PLACED + GAME_UPDATE
```

## Các bước và code

### Bước 1. Client gửi yêu cầu cắm cờ.

**Code:** [GameBoardPanel.java — dòng 106](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L106). Tìm `connection.sendMessage(JsonProtocol.createPlaceFlag`.

Trích đoạn bắt đầu tại vị trí trên:

```java
connection.sendMessage(JsonProtocol.createPlaceFlag(gameId, row, col));
```

### Bước 2. Chuyển yêu cầu tới session.

**Code:** [ClientHandler.java — dòng 215](../../src/minesweeperonline/server/ClientHandler.java#L215). Tìm `private void handlePlaceFlag(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handlePlaceFlag(Message message) {
    if (user == null) return;
    GameSession session = gameManager.getSessionByUserId(user.getId());
    if (session != null) {
        JsonObject p = message.getPayload();
        int row = p.get("row").getAsInt();
        int col = p.get("col").getAsInt();
        session.handlePlaceFlag(user.getId(), row, col);
    }
}
```

### Bước 3. Kiểm tra trận, gọi Board và gửi xác nhận cùng cập nhật đối thủ.

**Code:** [GameSession.java — dòng 261](../../src/minesweeperonline/server/GameSession.java#L261). Tìm `public void handlePlaceFlag(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void handlePlaceFlag(int userId, int row, int col) {
    synchronized (gameLock) {
        if (finished.get() || state != GameState.PLAYING || disconnectedUserId >= 0) return;
        Board targetBoard = getBoardForPlayer(userId);
        Player actingPlayer = getPlayer(userId);
        ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
        ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

        if (targetBoard != null && targetBoard.placeFlag(row, col)) {
            actingPlayer.setCurrentFlags(targetBoard.getCurrentFlags());
            actingPlayer.setFlagsPlaced(targetBoard.getFlagsPlaced());
            actingPlayer.setTotalActions(targetBoard.getTotalActions());

            Message res = new Message(MessageType.FLAG_PLACED);
            res.setGameId(gameId);
            JsonObject p = new JsonObject();
            p.addProperty("row", row);
            p.addProperty("col", col);
            p.addProperty("currentFlags", targetBoard.getCurrentFlags());
            p.addProperty("totalActions", targetBoard.getTotalActions());
            res.setPayload(p);
            actingHandler.sendMessage(res);

            // Báo đối thủ số cờ hiện tại & vị trí cắm cờ trên bàn cờ thu nhỏ
            Message oppUpdate = new Message(MessageType.GAME_UPDATE);
            oppUpdate.setGameId(gameId);
            JsonObject op = new JsonObject();
            op.addProperty("opponentId", actingPlayer.getUserId());
            op.addProperty("opponentOpenedSafeCells", targetBoard.getOpenedSafeCells());
            op.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
            op.addProperty("opponentTotalActions", targetBoard.getTotalActions());
            op.addProperty("flagRow", row);
            op.addProperty("flagCol", col);
            op.addProperty("flagState", true);
```

### Bước 4. Kiểm tra ô và giới hạn cờ; tăng currentFlags, flagsPlaced, totalActions.

**Code:** [Board.java — dòng 180](../../src/minesweeperonline/game/Board.java#L180). Tìm `public boolean placeFlag(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public boolean placeFlag(int r, int c) {
    if (!isValidCoordinate(r, c)) return false;
    Cell cell = grid[r][c];
    if (cell.isOpened() || cell.isFlagged() || currentFlags >= TOTAL_MINES) {
        return false;
    }

    cell.setFlagged(true);
    this.currentFlags++;
    this.flagsPlaced++;
    this.totalActions++;
    return true;
}
```

### Bước 5. Client hiển thị cờ sau xác nhận.

**Code:** [GameFrame.java — dòng 353](../../src/minesweeperonline/client/ui/GameFrame.java#L353). Tìm `private void onFlagPlaced(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void onFlagPlaced(JsonObject p) {
    int r = p.get("row").getAsInt();
    int c = p.get("col").getAsInt();
    boardPanel.setCellFlagged(r, c, true);
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

Không kiểm tra ô có mìn thật khi cắm cờ. Có thể cắm trước lần mở đầu; sinh mìn giữ trạng thái cờ.

## Trả lời khi bảo vệ

> Server kiểm tra cắm cờ hợp lệ rồi cập nhật bàn và xác nhận cho client.
