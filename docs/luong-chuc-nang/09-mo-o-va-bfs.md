# Mở ô và mở lan BFS

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

Tiếp luồng 08: A mở (3,4) lần đầu trên bàn có vành mìn minh họa. Tâm có số 0, BFS mở đúng 9 ô vùng hàng 2–4, cột 3–5.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí                                                                                 | Hàm / dấu mốc                                              | Dữ liệu và việc thực hiện                                                                                                                                                |
| ------ | ---------------------------------------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1      | [GameBoardPanel.java:96](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L96)   | `public void mousePressed(`                                 | Ghi khóa 3*12+4=40 vào pendingOpenCells, gửi OPEN_CELL và chưa tự mở nút.                                                                                              |
| 2      | [ClientHandler.java:208](../../src/minesweeperonline/server/ClientHandler.java#L208)      | `private void handleOpenCell(`                              | Gọi session.handleOpenCell(101,3,4).                                                                                                                                          |
| 3      | [GameSession.java:154](../../src/minesweeperonline/server/GameSession.java#L154)          | `public void handleOpenCell(`                               | Tạo newlyOpened rỗng, gọi Board của A.openCell(3,4,newlyOpened).                                                                                                           |
| 4      | [Board.java:109](../../src/minesweeperonline/game/Board.java#L109)                        | `public OpenResult openCell(`                               | Sinh mìn như luồng 08, tăng totalActions 0→1; ô tâm có số 0 nên dùng BFS.                                                                                           |
| 5      | [Board.java:145](../../src/minesweeperonline/game/Board.java#L145)                        | `Queue<Cell> queue = new ArrayDeque<>();`                   | Mở tâm, tăng openedSafeCells, thêm vào newlyOpened và queue.                                                                                                             |
| 6      | [Board.java:151](../../src/minesweeperonline/game/Board.java#L151)                        | `while (!queue.isEmpty())`                                  | poll ô, xét 8 hàng xóm. Đánh dấu mở ngay để tránh đếm trùng; chỉ ô số 0 được thêm queue để lan tiếp. Ô có số được mở nhưng không lan từ nó. |
| 7      | [GameSession.java:227](../../src/minesweeperonline/server/GameSession.java#L227)          | `Message updateSelf = new Message(MessageType.CELL_OPENED)` | Nhận SAFE, chưa đủ 121 ô: gửi CELL_OPENED cho A và GAME_UPDATE cho B.                                                                                                   |
| 8      | [GameFrame.java:333](../../src/minesweeperonline/client/ui/GameFrame.java#L333)           | `private void onCellOpened(`                                | Duyệt openedCells, gọi boardPanel.revealCell cho từng ô, cập nhật thống kê.                                                                                            |
| 9      | [GameBoardPanel.java:127](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L127) | `public void revealCell(`                                   | Đặt opened=true, xóa trạng thái chờ/cờ, tô nền và hiện số nếu lớn hơn 0.                                                                                        |

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

Server → A, đủ 9 ô của bàn minh họa:

```json
{
  "type": "CELL_OPENED",
  "gameId": "a1b2c3d4",
  "payload": {
    "openedCells": [
      {
        "row": 3,
        "col": 4,
        "adjacentMines": 0,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 2,
        "col": 3,
        "adjacentMines": 5,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 2,
        "col": 4,
        "adjacentMines": 3,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 2,
        "col": 5,
        "adjacentMines": 5,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 3,
        "col": 3,
        "adjacentMines": 3,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 3,
        "col": 5,
        "adjacentMines": 3,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 4,
        "col": 3,
        "adjacentMines": 5,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 4,
        "col": 4,
        "adjacentMines": 3,
        "isMine": false,
        "isExploded": false
      },
      {
        "row": 4,
        "col": 5,
        "adjacentMines": 5,
        "isMine": false,
        "isExploded": false
      }
    ],
    "openedSafeCells": 9,
    "totalActions": 1
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
    "opponentTotalActions": 1,
    "openedCoords": [
      {
        "r": 3,
        "c": 4
      },
      {
        "r": 2,
        "c": 3
      },
      {
        "r": 2,
        "c": 4
      },
      {
        "r": 2,
        "c": 5
      },
      {
        "r": 3,
        "c": 3
      },
      {
        "r": 3,
        "c": 5
      },
      {
        "r": 4,
        "c": 3
      },
      {
        "r": 4,
        "c": 4
      },
      {
        "r": 4,
        "c": 5
      }
    ]
  }
}
```

A thấy 9 ô mở nhưng thao tác chỉ tăng 1 vì chỉ click một lần. B chỉ nhận tọa độ và tiến độ để vẽ bàn thu nhỏ, không nhận số mìn lân cận của A. Với kết quả ngẫu nhiên khác, số ô mở lan có thể khác. Mở một ô số 2 ở lượt sau thì chỉ mở đúng ô đó; mở trúng mìn chuyển sang luồng 12.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Client gửi yêu cầu mở ô.

**Code:** [GameBoardPanel.java — dòng 96](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L96). Tìm `public void mousePressed(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void mousePressed(MouseEvent e) {
    if (!interactive || opened[row][col]) return;

    if (SwingUtilities.isLeftMouseButton(e)) {
        // Click trái -> mở ô
        if (!flagged[row][col]) {
            // Chỉ ghi nhận yêu cầu đang chờ, chưa tự mở ô trước khi server trả lời.
            pendingOpenCells.add(cellKey(row, col));
            connection.sendMessage(JsonProtocol.createOpenCell(gameId, row, col));
        }
    } else if (SwingUtilities.isRightMouseButton(e)) {
        // Cờ cũng chỉ đổi hình sau FLAG_PLACED/FLAG_REMOVED để khớp trạng thái server.
        // Click phải -> bật/tắt cờ
        if (flagged[row][col]) {
            connection.sendMessage(JsonProtocol.createRemoveFlag(gameId, row, col));
        } else {
            connection.sendMessage(JsonProtocol.createPlaceFlag(gameId, row, col));
        }
    }
```

### Bước 2. Chuyển tọa độ tới session.

**Code:** [ClientHandler.java — dòng 208](../../src/minesweeperonline/server/ClientHandler.java#L208). Tìm `private void handleOpenCell(`.

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

**Code:** [GameSession.java — dòng 154](../../src/minesweeperonline/server/GameSession.java#L154). Tìm `public void handleOpenCell(`.

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
        Board.OpenResult result = targetBoard.openCell(row, col, newlyOpened); // trả về safe/mine/invalid

        if (result == Board.OpenResult.INVALID) {
            return;
        }

        actingPlayer.setOpenedSafeCells(targetBoard.getOpenedSafeCells());
```

### Bước 4. Kiểm tra tọa độ, ô đã mở và cờ; sinh mìn nếu đây là lần đầu.

**Code:** [Board.java — dòng 109](../../src/minesweeperonline/game/Board.java#L109). Tìm `public OpenResult openCell(`.

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

**Code:** [Board.java — dòng 129](../../src/minesweeperonline/game/Board.java#L129). Tìm `if (target.isMine())`.

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

**Code:** [Board.java — dòng 145](../../src/minesweeperonline/game/Board.java#L145). Tìm `Queue<Cell> queue = new ArrayDeque<>();`.

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
                    // Đánh dấu ngay khi tìm thấy để các ô lân cận khác không thêm lại và đếm trùng.
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
```

### Bước 7. Gửi ô vừa mở cho người thao tác; gửi GAME_UPDATE cho đối thủ ở phần sau.

**Code:** [GameSession.java — dòng 227](../../src/minesweeperonline/server/GameSession.java#L227). Tìm `Message updateSelf = new Message(MessageType.CELL_OPENED)`.

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

**Code:** [GameFrame.java — dòng 333](../../src/minesweeperonline/client/ui/GameFrame.java#L333). Tìm `private void onCellOpened(`.

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
