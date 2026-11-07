# Bắt đầu ván và sinh mìn ở lần mở đầu

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Tạo trận mới; người chơi mở ô hợp lệ đầu tiên sau đếm ngược.

## Luồng tổng quát

```text
createSession → new Board → READY → GAME_START → OPEN_CELL → Board.openCell → generateMines
```

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

**Code:** [GameSession.java — dòng 59](../../src/minesweeperonline/server/GameSession.java#L59). Tìm `public GameSession(`.

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

**Code:** [Board.java — dòng 37](../../src/minesweeperonline/game/Board.java#L37). Tìm `public Board()`.

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

**Code:** [Board.java — dòng 51](../../src/minesweeperonline/game/Board.java#L51). Tìm `public final void initialize()`.

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

**Code:** [GameSession.java — dòng 84](../../src/minesweeperonline/server/GameSession.java#L84). Tìm `private void startReadyCountdown()`.

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

**Code:** [GameFrame.java — dòng 299](../../src/minesweeperonline/client/ui/GameFrame.java#L299). Tìm `private void onGameStart()`.

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

### Bước 8. Server lấy tọa độ và tìm session theo user của kết nối.

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

### Bước 9. Kiểm tra trạng thái trận và chọn Board của người đang thao tác.

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
```

### Bước 10. Đây là chỗ thực sự sinh mìn: truyền 6 tham số, sau đó đặt minesGenerated=true.

**Code:** [Board.java — dòng 117](../../src/minesweeperonline/game/Board.java#L117). Tìm `if (!this.minesGenerated)`.

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

    // Gom các ô ứng viên nằm ngoài vùng an toàn (ô an toàn + 8 ô xung quanh)
    List<int[]> candidates = new ArrayList<>();
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            if (!isInSafeZone(r, c, safeRow, safeCol)) {
                candidates.add(new int[]{r, c});
            }
        }
    }
```

### Bước 12. Xáo trộn, chọn 23 vị trí đầu làm mìn và tính số lân cận.

**Code:** [MineGenerator.java — dòng 58](../../src/minesweeperonline/game/MineGenerator.java#L58). Tìm `Collections.shuffle(candidates, random)`.

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

```

### Bước 14. Client nhận danh sách ô đã mở để cập nhật giao diện.

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

Tạo ván chưa sinh mìn. Mỗi người tự kích hoạt sinh mìn trên bàn riêng. Cắm cờ trước lần mở không sinh mìn. Bước xóa dữ liệu mìn là phòng trường hợp lưới có dữ liệu cũ; trong lần sinh đầu của Board thì lưới đã sạch. Chừa vùng đầu không bảo đảm toàn bàn giải được mà không đoán.

## Trả lời khi bảo vệ

> Khi tạo ván chỉ tạo hai bàn trống. Đến lần mở ô hợp lệ đầu tiên, server đặt 23 mìn ngoài vùng 3 × 3 quanh ô đó, tính số và mở lan.
