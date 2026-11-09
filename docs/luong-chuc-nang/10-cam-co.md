# Cắm cờ

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A đã mở 9 ô bằng 1 thao tác. Ô (3,7) chưa mở/chưa có cờ; A click phải để cắm cờ.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí                                                                               | Hàm / dấu mốc                                       | Dữ liệu và việc thực hiện                                                                                                    |
| ------ | -------------------------------------------------------------------------------------- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------- |
| 1      | [GameBoardPanel.java:96](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L96) | `public void mousePressed(`                          | Nhánh chuột phải chọn createPlaceFlag nếu chưa có cờ, createRemoveFlag nếu đã có cờ; gửi tọa độ (3,7).            |
| 2      | [ClientHandler.java:220](../../src/minesweeperonline/server/ClientHandler.java#L220)    | `private void handlePlaceFlag(`                      | Tra session theo tài khoản và gọi session.handlePlaceFlag(101,3,7).                                                            |
| 3      | [GameSession.java:265](../../src/minesweeperonline/server/GameSession.java#L265)        | `public void handlePlaceFlag(`                       | Khóa gameLock, kiểm tra PLAYING rồi gọi Board.placeFlag(3,7).                                                                  |
| 4      | [Board.java:184](../../src/minesweeperonline/game/Board.java#L184)                      | `public boolean placeFlag(`                          | Kiểm tra tọa độ, ô chưa mở/chưa có cờ, currentFlags<23. Đặt flagged=true, tăng currentFlags,flagsPlaced,totalActions. |
| 5      | [GameSession.java:278](../../src/minesweeperonline/server/GameSession.java#L278)        | `Message res = new Message(MessageType.FLAG_PLACED)` | Gửi FLAG_PLACED cho A và GAME_UPDATE cho B.                                                                                      |
| 6      | [GameFrame.java:364](../../src/minesweeperonline/client/ui/GameFrame.java#L364)         | `private void onFlagPlaced(`                         | Gọi boardPanel.setCellFlagged(3,7,true) rồi cập nhật bộ đếm.                                                                |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "PLACE_FLAG",
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
  "type": "FLAG_PLACED",
  "gameId": "a1b2c3d4",
  "payload": {
    "row": 3,
    "col": 7,
    "currentFlags": 1,
    "totalActions": 2
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
    "opponentCurrentFlags": 1,
    "opponentTotalActions": 2,
    "flagRow": 3,
    "flagCol": 7,
    "flagState": true
  }
}
```

Cờ là đánh dấu của người chơi, server không đòi ô phải thực sự có mìn. Chỉ đổi hình sau khi server xác nhận. Board trả false nếu thao tác không hợp lệ: không có gói thành công và không tăng bộ đếm. Mỗi lần cắm/gỡ hợp lệ đều tính một thao tác.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Client gửi yêu cầu cắm cờ.

**Code:** [GameBoardPanel.java — dòng 112](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L112). Tìm `connection.sendMessage(JsonProtocol.createPlaceFlag`.

Trích đoạn bắt đầu tại vị trí trên:

```java
connection.sendMessage(JsonProtocol.createPlaceFlag(gameId, row, col));
```

### Bước 2. Chuyển yêu cầu tới session.

**Code:** [ClientHandler.java — dòng 220](../../src/minesweeperonline/server/ClientHandler.java#L220). Tìm `private void handlePlaceFlag(`.

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

**Code:** [GameSession.java — dòng 265](../../src/minesweeperonline/server/GameSession.java#L265). Tìm `public void handlePlaceFlag(`.

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

**Code:** [Board.java — dòng 184](../../src/minesweeperonline/game/Board.java#L184). Tìm `public boolean placeFlag(`.

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

**Code:** [GameFrame.java — dòng 364](../../src/minesweeperonline/client/ui/GameFrame.java#L364). Tìm `private void onFlagPlaced(`.

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
