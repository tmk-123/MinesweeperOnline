# Gỡ cờ

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

Tiếp luồng 10: ô (3,7) có cờ, totalActions=2. A click phải lần nữa để gỡ.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [GameBoardPanel.java:96](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L96) | `public void mousePressed(` | Nhánh chuột phải chọn createPlaceFlag nếu chưa có cờ, createRemoveFlag nếu đã có cờ; gửi tọa độ (3,7). |
| 2 | [ClientHandler.java:232](../../src/minesweeperonline/server/ClientHandler.java#L232) | `private void handleRemoveFlag(` | Tra session theo tài khoản và gọi session.handleRemoveFlag(101,3,7). |
| 3 | [GameSession.java:308](../../src/minesweeperonline/server/GameSession.java#L308) | `public void handleRemoveFlag(` | Khóa gameLock, kiểm tra PLAYING rồi gọi Board.removeFlag(3,7). |
| 4 | [Board.java:201](../../src/minesweeperonline/game/Board.java#L201) | `public boolean removeFlag(` | Kiểm tra ô có cờ; đặt flagged=false, giảm currentFlags, tăng flagsRemoved và totalActions. Không giảm flagsPlaced. |
| 5 | [GameSession.java:321](../../src/minesweeperonline/server/GameSession.java#L321) | `Message res = new Message(MessageType.FLAG_REMOVED)` | Gửi FLAG_REMOVED cho A và GAME_UPDATE cho B. |
| 6 | [GameFrame.java:379](../../src/minesweeperonline/client/ui/GameFrame.java#L379) | `private void onFlagRemoved(` | Gọi boardPanel.setCellFlagged(3,7,false) rồi cập nhật bộ đếm. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "REMOVE_FLAG",
  "gameId": "a1b2c3d4",
  "payload": {
    "row": 3,
    "col": 7
  }
}
```

Server → A:

```json
{
  "type": "FLAG_REMOVED",
  "gameId": "a1b2c3d4",
  "payload": {
    "row": 3,
    "col": 7,
    "currentFlags": 0,
    "totalActions": 3
  }
}
```

Server → B:

```json
{
  "type": "GAME_UPDATE",
  "gameId": "a1b2c3d4",
  "payload": {
    "opponentId": 101,
    "opponentOpenedSafeCells": 9,
    "opponentCurrentFlags": 0,
    "opponentTotalActions": 3,
    "flagRow": 3,
    "flagCol": 7,
    "flagState": false
  }
}
```

Cờ là đánh dấu của người chơi, server không đòi ô phải thực sự có mìn. Chỉ đổi hình sau khi server xác nhận. Board trả false nếu thao tác không hợp lệ: không có gói thành công và không tăng bộ đếm. Mỗi lần cắm/gỡ hợp lệ đều tính một thao tác.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Client gửi yêu cầu gỡ cờ.

**Code:** [GameBoardPanel.java — dòng 110](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L110). Tìm `connection.sendMessage(JsonProtocol.createRemoveFlag`.

Trích đoạn bắt đầu tại vị trí trên:

```java
connection.sendMessage(JsonProtocol.createRemoveFlag(gameId, row, col));
```

### Bước 2. Chuyển yêu cầu tới session.

**Code:** [ClientHandler.java — dòng 232](../../src/minesweeperonline/server/ClientHandler.java#L232). Tìm `private void handleRemoveFlag(`.

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

**Code:** [GameSession.java — dòng 308](../../src/minesweeperonline/server/GameSession.java#L308). Tìm `public void handleRemoveFlag(`.

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

**Code:** [Board.java — dòng 201](../../src/minesweeperonline/game/Board.java#L201). Tìm `public boolean removeFlag(`.

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

**Code:** [GameFrame.java — dòng 379](../../src/minesweeperonline/client/ui/GameFrame.java#L379). Tìm `private void onFlagRemoved(`.

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
