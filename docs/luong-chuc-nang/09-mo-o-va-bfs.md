# Mở ô và mở lan BFS

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Click trái vào ô khi trận đang chơi.

## Luồng tổng quát

```text
OPEN_CELL → Board.openCell → SAFE/MINE/INVALID → cập nhật hoặc kết thúc
```

## Các bước và code

### Bước 1. Client gửi yêu cầu mở ô.

**Code:** [GameBoardPanel.java — dòng 92](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L92). Tìm `public void mousePressed(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
    public void mousePressed(MouseEvent e) {
        if (!interactive || opened[row][col]) return;

        if (SwingUtilities.isLeftMouseButton(e)) {
            // Click trái -> mở ô
            if (!flagged[row][col]) {
                pendingOpenCells.add(cellKey(row, col));
                connection.sendMessage(JsonProtocol.createOpenCell(gameId, row, col));
            }
        } else if (SwingUtilities.isRightMouseButton(e)) {
            // Click phải -> bật/tắt cờ
            if (flagged[row][col]) {
                connection.sendMessage(JsonProtocol.createRemoveFlag(gameId, row, col));
            } else {
                connection.sendMessage(JsonProtocol.createPlaceFlag(gameId, row, col));
            }
        }
    }
});
```

### Bước 2. Chuyển tọa độ tới session.

**Code:** [ClientHandler.java — dòng 204](../../src/minesweeperonline/server/ClientHandler.java#L204). Tìm `private void handleOpenCell(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleOpenCell(Message message) {
    if (user == null) return;
    GameSession session = gameManager.getSessionByUserId(user.getId());
    if (session != null) {
        JsonObject p = message.getPayload();
        int row = p.get("row").getAsInt();
        int col = p.get("col").getAsInt();
        session.handleOpenCell(user.getId(), row, col);
    }
}
```

### Bước 3. Khóa gameLock và kiểm tra trạng thái trước khi gọi Board.

**Code:** [GameSession.java — dòng 150](../../src/minesweeperonline/server/GameSession.java#L150). Tìm `public void handleOpenCell(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void handleOpenCell(int userId, int row, int col) {
    synchronized (gameLock) {
        if (finished.get() || state != GameState.PLAYING || disconnectedUserId >= 0) {
            return;
        }

        Board targetBoard = getBoardForPlayer(userId);
        Player actingPlayer = getPlayer(userId);
        Player opponentPlayer = getOpponent(userId);
        ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
        ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

        if (targetBoard == null || actingPlayer == null) return;

        List<Cell> newlyOpened = new ArrayList<>();
        Board.OpenResult result = targetBoard.openCell(row, col, newlyOpened);

        if (result == Board.OpenResult.INVALID) {
            return;
        }

        actingPlayer.setOpenedSafeCells(targetBoard.getOpenedSafeCells());
```

### Bước 4. Kiểm tra tọa độ, ô đã mở và cờ; sinh mìn nếu đây là lần đầu.

**Code:** [Board.java — dòng 106](../../src/minesweeperonline/game/Board.java#L106). Tìm `public OpenResult openCell(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public OpenResult openCell(int r, int c, List<Cell> openedList) {
    if (!isValidCoordinate(r, c)) {
        return OpenResult.INVALID;
    }

    Cell target = grid[r][c];
    if (target.isOpened() || target.isFlagged()) {
        return OpenResult.INVALID;
    }

    // Sinh mìn ở lần click đầu tiên: đảm bảo ô được chọn có 0 mìn xung quanh
    if (!this.minesGenerated) {
        MineGenerator.generateMines(grid, ROWS, COLS, TOTAL_MINES, r, c);
        this.minesGenerated = true;
    }
```

### Bước 5. Trúng mìn trả MINE; ô có số chỉ mở riêng ô đó.

**Code:** [Board.java — dòng 126](../../src/minesweeperonline/game/Board.java#L126). Tìm `if (target.isMine())`.

Trích đoạn bắt đầu tại vị trí trên:

```java
if (target.isMine()) {
    target.setExploded(true);
    target.setOpened(true);
    if (openedList != null) openedList.add(target);
    return OpenResult.MINE;
}

// Trường hợp 2: Mở ô có số (adjacentMines > 0) -> chỉ mở đúng ô đó
if (target.getAdjacentMines() > 0) {
    target.setOpened(true);
    this.openedSafeCells++;
    if (openedList != null) openedList.add(target);
    return OpenResult.SAFE;
}
```

### Bước 6. BFS mở vùng ô 0 và các ô số ở rìa; đánh dấu đã mở để tránh lặp.

**Code:** [Board.java — dòng 142](../../src/minesweeperonline/game/Board.java#L142). Tìm `Queue<Cell> queue = new ArrayDeque<>();`.

Trích đoạn bắt đầu tại vị trí trên:

```java
    Queue<Cell> queue = new ArrayDeque<>();
    target.setOpened(true);
    this.openedSafeCells++;
    if (openedList != null) openedList.add(target);
    queue.add(target);

    while (!queue.isEmpty()) {
        Cell curr = queue.poll();

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = curr.getRow() + dr;
                int nc = curr.getCol() + dc;

                if (isValidCoordinate(nr, nc)) {
                    Cell neighbor = grid[nr][nc];
                    if (!neighbor.isOpened() && !neighbor.isFlagged() && !neighbor.isMine()) {
                        neighbor.setOpened(true);
                        this.openedSafeCells++;
                        if (openedList != null) openedList.add(neighbor);

                        // Chỉ đưa vào hàng đợi để lan tiếp với các ô có giá trị 0
                        if (neighbor.getAdjacentMines() == 0) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    return OpenResult.SAFE;
}
```

### Bước 7. Gửi ô vừa mở cho người thao tác; gửi GAME_UPDATE cho đối thủ ở phần sau.

**Code:** [GameSession.java — dòng 223](../../src/minesweeperonline/server/GameSession.java#L223). Tìm `Message updateSelf = new Message(MessageType.CELL_OPENED)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
Message updateSelf = new Message(MessageType.CELL_OPENED);
updateSelf.setGameId(gameId);
JsonObject pSelf = new JsonObject();
pSelf.add("openedCells", serializeOpenedCells(newlyOpened));
pSelf.addProperty("openedSafeCells", targetBoard.getOpenedSafeCells());
pSelf.addProperty("totalActions", targetBoard.getTotalActions());
updateSelf.setPayload(pSelf);
actingHandler.sendMessage(updateSelf);

// Gửi cập nhật tiến độ cho đối thủ (không lộ vị trí mìn)
Message updateOpp = new Message(MessageType.GAME_UPDATE);
updateOpp.setGameId(gameId);
JsonObject pOpp = new JsonObject();
pOpp.addProperty("opponentId", actingPlayer.getUserId());
pOpp.addProperty("opponentOpenedSafeCells", targetBoard.getOpenedSafeCells());
pOpp.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
pOpp.addProperty("opponentTotalActions", targetBoard.getTotalActions());

// Đồng bộ bàn cờ thu nhỏ: gửi tọa độ (row, col) các ô vừa mở
JsonArray openedCoords = new JsonArray();
for (Cell c : newlyOpened) {
    JsonObject coord = new JsonObject();
    coord.addProperty("r", c.getRow());
    coord.addProperty("c", c.getCol());
    openedCoords.add(coord);
}
pOpp.add("openedCoords", openedCoords);

updateOpp.setPayload(pOpp);
opponentHandler.sendMessage(updateOpp);
```

### Bước 8. Client đọc kết quả và gọi revealCell.

**Code:** [GameFrame.java — dòng 323](../../src/minesweeperonline/client/ui/GameFrame.java#L323). Tìm `private void onCellOpened(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void onCellOpened(JsonObject p) {
    boolean exploded = false;
    if (p.has("openedCells")) {
        JsonArray arr = p.getAsJsonArray("openedCells");
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            int r = o.get("row").getAsInt();
            int c = o.get("col").getAsInt();
            int adj = o.get("adjacentMines").getAsInt();
            boolean isMine = o.get("isMine").getAsBoolean();
            boolean isExp = o.get("isExploded").getAsBoolean();
            boardPanel.revealCell(r, c, adj, isMine, isExp);
            if (isExp) exploded = true;
        }
    }
    if (p.has("openedSafeCells")) {
        mySafeCells = p.get("openedSafeCells").getAsInt();
    }
    if (p.has("totalActions")) {
        myActions = p.get("totalActions").getAsInt();
    }
    infoPanel.updateSelfStats(mySafeCells, myFlags, myActions);

    if (exploded) {
        playExplosionOnce();
    } else {
```

## Điểm cần nắm

Các lần mở sau không sinh lại mìn. Nhánh thắng/thua xem file 12. Ô số ở rìa mở lan được hiển thị nhưng không tiếp tục lan từ ô đó.

## Trả lời khi bảo vệ

> Board xác định các ô được mở, server trả kết quả; mở lan dùng BFS với Queue.
