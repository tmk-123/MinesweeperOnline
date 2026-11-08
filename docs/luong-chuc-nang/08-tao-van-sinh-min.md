# Bắt đầu ván và sinh mìn ở lần mở đầu

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A/B vừa vào trận a1b2c3d4. Sau 5 giây READY, A mở (3,4) lần đầu. Tọa độ bắt đầu từ 0: đây là hàng thứ 4, cột thứ 5 trên màn hình.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [GameManager.java:19](../../src/minesweeperonline/server/GameManager.java#L19) | `public GameSession createSession(` | Gọi new GameSession(A,handlerA,B,handlerB), lưu session và ánh xạ userId. |
| 2 | [GameSession.java:62](../../src/minesweeperonline/server/GameSession.java#L62) | `public GameSession(` | Tạo board1/board2 bằng new Board(), đặt READY và gọi startReadyCountdown(). |
| 3 | [Board.java:52](../../src/minesweeperonline/game/Board.java#L52) | `public final void initialize()` | Đặt minesGenerated=false, chưa mở/chưa có mìn; không gọi generateMines lúc tạo bàn. |
| 4 | [GameSession.java:87](../../src/minesweeperonline/server/GameSession.java#L87) | `private void startReadyCountdown()` | Sau 5 giây đặt PLAYING, gửi GAME_START có durationSeconds=300 rồi chạy đồng hồ server. |
| 5 | [GameBoardPanel.java:96](../../src/minesweeperonline/client/ui/GameBoardPanel.java#L96) | `public void mousePressed(` | Click trái (3,4), gọi createOpenCell("a1b2c3d4",3,4) và sendMessage qua TCP. |
| 6 | [ClientHandler.java:208](../../src/minesweeperonline/server/ClientHandler.java#L208) | `private void handleOpenCell(` | Tra session của userId=101, đọc row=3/col=4 rồi gọi session.handleOpenCell(101,3,4). |
| 7 | [GameSession.java:154](../../src/minesweeperonline/server/GameSession.java#L154) | `public void handleOpenCell(` | Chọn Board của A, gọi targetBoard.openCell(3,4,newlyOpened). |
| 8 | [Board.java:109](../../src/minesweeperonline/game/Board.java#L109) | `public OpenResult openCell(` | Kiểm tra tọa độ/cờ/đã mở, thấy minesGenerated=false nên gọi generateMines(grid,12,12,23,3,4). |
| 9 | [MineGenerator.java:29](../../src/minesweeperonline/game/MineGenerator.java#L29) | `public static void generateMines(` | Xóa dữ liệu mìn cũ, tạo candidates, loại vùng an toàn, xáo trộn rồi đặt mìn ở 23 vị trí đầu. |
| 10 | [MineGenerator.java:80](../../src/minesweeperonline/game/MineGenerator.java#L80) | `public static boolean isInSafeZone(` | Loại mọi ô hàng 2–4, cột 3–5: cách tâm (3,4) không quá 1 theo mỗi chiều. |
| 11 | [MineGenerator.java:90](../../src/minesweeperonline/game/MineGenerator.java#L90) | `public static void calculateAdjacentMines(` | Đếm tối đa 8 hàng xóm cho mỗi ô an toàn, ghi adjacentMines. |
| 12 | [Board.java:122](../../src/minesweeperonline/game/Board.java#L122) | `this.minesGenerated = true` | Quay lại Board.openCell, đặt cờ đã sinh mìn rồi tiếp tục mở tâm và BFS. |

## Message và kết quả trong tình huống này

Server → hai client khi hết READY:

```json
{
  "type": "GAME_START",
  "gameId": "a1b2c3d4",
  "payload": {
    "durationSeconds": 300
  }
}
```

A → server khi bấm lần đầu:

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

**Một kết quả xáo trộn minh họa có thể xảy ra:** 16 mìn trên vành hình vuông hàng 1–5, cột 2–6; 7 mìn còn lại ở (10,0) đến (10,6). Vùng hàng 2–4, cột 3–5 không có mìn. Tâm (3,4) có số 0; các ô sát tâm có số 3 hoặc 5. Không phải mọi ván đều có bố trí này. [Luồng 09 có JSON đủ 9 ô mở](09-mo-o-va-bfs.md).

Lần mở tiếp theo minesGenerated=true nên **không sinh mìn lại**. B có Board riêng: nếu B chưa mở ô thì bàn B chưa sinh mìn dù A đã mở. Cắm cờ không sinh mìn. Click vào ô đã mở/cắm cờ không được tính là lần mở hợp lệ đầu tiên.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Tạo session và lưu ánh xạ userId sang gameId.

**Code:** [GameManager.java — dòng 19](../../src/minesweeperonline/server/GameManager.java#L19). Tìm `public GameSession createSession(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public GameSession createSession(Player p1, ClientHandler h1, Player p2, ClientHandler h2) {
    GameSession session = new GameSession(p1, h1, p2, h2);
    sessions.put(session.getGameId(), session);
    userToGameMap.put(p1.getUserId(), session.getGameId());
    userToGameMap.put(p2.getUserId(), session.getGameId());
    System.out.println("[GameManager] Created GameSession: " + session.getGameId() +
            " for [" + p1.getUsername() + "] vs [" + p2.getUsername() + "]");
    return session;
}
```

### Bước 2. Tạo hai Board riêng, đặt READY và bắt đầu đếm ngược.

**Code:** [GameSession.java — dòng 62](../../src/minesweeperonline/server/GameSession.java#L62). Tìm `public GameSession(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public GameSession(Player player1, ClientHandler handler1, Player player2, ClientHandler handler2) {
    this.gameId = UUID.randomUUID().toString().substring(0, 8);
    this.player1 = player1;
    this.handler1 = handler1;
    this.board1 = new Board();

    this.player2 = player2;
    this.handler2 = handler2;
    this.board2 = new Board();

    this.player1.setStatus(Player.STATUS_PLAYING);
    this.player2.setStatus(Player.STATUS_PLAYING);
    this.player1.resetMatchStats();
    this.player2.resetMatchStats();

    this.state = GameState.READY;
    this.startedAt = System.currentTimeMillis();
    this.remainingSeconds = ServerConfig.MATCH_DURATION_SECONDS;

    startReadyCountdown();
}
```

### Bước 3. Tạo lưới và từng Cell rồi gọi initialize.

**Code:** [Board.java — dòng 38](../../src/minesweeperonline/game/Board.java#L38). Tìm `public Board()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public Board() {
    this.grid = new Cell[ROWS][COLS];
    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {
            this.grid[r][c] = new Cell(r, c);
        }
    }
    initialize();
}
```

### Bước 4. Reset trạng thái, đặt minesGenerated=false; chưa sinh mìn.

**Code:** [Board.java — dòng 52](../../src/minesweeperonline/game/Board.java#L52). Tìm `public final void initialize()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public final void initialize() {
    this.openedSafeCells = 0;
    this.currentFlags = 0;
    this.flagsPlaced = 0;
    this.flagsRemoved = 0;
    this.totalActions = 0;
    this.minesGenerated = false;

    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {
            this.grid[r][c].reset();
        }
    }
```

### Bước 5. Hết READY thì chuyển PLAYING, gửi GAME_START và chạy timer trận.

**Code:** [GameSession.java — dòng 87](../../src/minesweeperonline/server/GameSession.java#L87). Tìm `private void startReadyCountdown()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void startReadyCountdown() {
    System.out.println("[GameSession] Match " + gameId + " created. Starting " +
            ServerConfig.READY_COUNTDOWN_SECONDS + "s READY countdown...");
    timerScheduler.schedule(() -> {
        try {
            synchronized (gameLock) {
                if (finished.get()) return;
                this.state = GameState.PLAYING;
                this.startedAt = System.currentTimeMillis();
            }

            // Gửi GAME_START cho cả hai người chơi
            Message startMsg = new Message(MessageType.GAME_START);
            startMsg.setGameId(gameId);
            JsonObject p = new JsonObject();
            p.addProperty("durationSeconds", ServerConfig.MATCH_DURATION_SECONDS);
            startMsg.setPayload(p);

            System.out.println("[GameSession] Broadcasted GAME_START for match: " + gameId);
            handler1.sendMessage(startMsg);
            handler2.sendMessage(startMsg);

            startPlayingTimer();
        } catch (Exception e) {
            System.err.println("[GameSession] Error starting game: " + e.getMessage());
        }
    }, ServerConfig.READY_COUNTDOWN_SECONDS, TimeUnit.SECONDS);
}
```

### Bước 6. Client xử lý sự kiện bắt đầu, cho phép chơi theo logic giao diện.

**Code:** [GameFrame.java — dòng 308](../../src/minesweeperonline/client/ui/GameFrame.java#L308). Tìm `private void onGameStart()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void onGameStart() {
    if (readyCountdownTimer != null) {
        readyCountdownTimer.stop();
    }
    setGameStatus("Trạng thái: ĐANG THI ĐẤU", UITheme.COLOR_SUCCESS);
    boardPanel.setInteractive(true);

    remainingSeconds = 300;
    if (gameClock != null) gameClock.stop();
    gameClock = new Timer(1000, e -> {
        remainingSeconds--;
        int mins = remainingSeconds / 60;
        int secs = remainingSeconds % 60;
        infoPanel.updateMatchTimer(String.format("%02d:%02d", mins, secs));
        if (remainingSeconds <= 5 && remainingSeconds > 0) {
            SoundManager.getInstance().playTick();
        }
        if (remainingSeconds <= 0) {
            gameClock.stop();
        }
    });
```

### Bước 7. Click trái gửi OPEN_CELL với tọa độ ô.

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

### Bước 8. Server lấy tọa độ và tìm session theo user của kết nối.

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

### Bước 9. Kiểm tra trạng thái trận và chọn Board của người đang thao tác.

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
```

### Bước 10. Đây là chỗ thực sự sinh mìn: truyền 6 tham số, sau đó đặt minesGenerated=true.

**Code:** [Board.java — dòng 120](../../src/minesweeperonline/game/Board.java#L120). Tìm `if (!this.minesGenerated)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
if (!this.minesGenerated) {
    MineGenerator.generateMines(grid, ROWS, COLS, TOTAL_MINES, r, c);
    this.minesGenerated = true;
}
```

### Bước 11. Kiểm tra số lượng, làm sạch dữ liệu mìn và gom các ô ngoài vùng an toàn.

**Code:** [MineGenerator.java — dòng 29](../../src/minesweeperonline/game/MineGenerator.java#L29). Tìm `public static void generateMines(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static void generateMines(Cell[][] grid, int rows, int cols, int mineCount, int safeRow, int safeCol) {

    // kiểm tra nếu thay đổi kích thước bàn cờ
    int totalCells = rows * cols;
    if (mineCount >= totalCells) {
        throw new IllegalArgumentException("Mine count must be strictly less than total cells.");
    }

    // Xóa mìn và số mìn xung quanh hiện có, giữ nguyên trạng thái cờ và ô đã mở
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            grid[r][c].setMine(false);
            grid[r][c].setAdjacentMines(0);
        }
    }

    // ô đó và 8 ô xung quanh an toàn
    List<int[]> candidates = new ArrayList<>();
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            if (!isInSafeZone(r, c, safeRow, safeCol)) {
                candidates.add(new int[]{r, c});
            }
```

### Bước 12. Xáo trộn, chọn 23 vị trí đầu làm mìn và tính số lân cận.

**Code:** [MineGenerator.java — dòng 60](../../src/minesweeperonline/game/MineGenerator.java#L60). Tìm `Collections.shuffle(candidates, random)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
Collections.shuffle(candidates, random);
for (int i = 0; i < mineCount; i++) {
    int[] pos = candidates.get(i);
    grid[pos[0]][pos[1]].setMine(true);
}

// Tính số mìn xung quanh cho tất cả các ô
calculateAdjacentMines(grid, rows, cols);
```

### Bước 13. Sau khi sinh mìn, ô đầu có số 0 nên Board tiếp tục mở lan BFS.

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
```

### Bước 14. Client nhận danh sách ô đã mở để cập nhật giao diện.

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

Tạo ván chưa sinh mìn. Mỗi người tự kích hoạt sinh mìn trên bàn riêng. Cắm cờ trước lần mở không sinh mìn. Bước xóa dữ liệu mìn là phòng trường hợp lưới có dữ liệu cũ; trong lần sinh đầu của Board thì lưới đã sạch. Chừa vùng đầu không bảo đảm toàn bàn giải được mà không đoán.

## Trả lời khi bảo vệ

> Khi tạo ván chỉ tạo hai bàn trống. Đến lần mở ô hợp lệ đầu tiên, server đặt 23 mìn ngoài vùng 3 × 3 quanh ô đó, tính số và mở lan.
