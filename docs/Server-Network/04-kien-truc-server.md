# 4. Kiến trúc Server

## 4.1. Các đối tượng và quan hệ

```text
MainServer (1 cái, sống suốt chương trình)
 ├── ServerSocket(2209)
 ├── ClientManager (1)  ── onlineUsers: userId → ClientHandler
 ├── GameManager   (1)  ── sessions:    gameId → GameSession
 │                       └ userToGameMap: userId → gameId
 └── mỗi kết nối: ClientHandler (+1 Thread)  ── sống tới khi socket đóng
                    ├─ socket, reader, writer
                    ├─ user   (User từ DB, null nếu chưa đăng nhập)
                    └─ player (Player: điểm, status, thống kê trận)

GameSession (mỗi trận 1 cái, sống từ ACCEPT tới khi EXIT/rời mạng)
 ├── player1/handler1/board1
 ├── player2/handler2/board2
 ├── state: READY → PLAYING → FINISHED
 └── timerScheduler (1 thread riêng): đếm READY 5s, rồi tick mỗi giây 300s
```

**`ClientManager`** và **`GameManager`** là "danh bạ" dùng chung giữa mọi thread. Vì vậy chúng dùng `ConcurrentHashMap` và `CopyOnWriteArrayList`, các cấu trúc an toàn khi nhiều thread cùng đọc/ghi.

## 4.2. `MainServer`: cổng vào

[MainServer.java](../../src/minesweeperonline/server/MainServer.java)

```java
serverSocket = new ServerSocket(port);
while (running) {
    Socket clientSocket = serverSocket.accept();                  // chặn chờ client
    ClientHandler handler = new ClientHandler(clientSocket, clientManager, gameManager);
    new Thread(handler, "Client-" + clientSocket.getPort()).start();  // giao cho thread mới
}
```

- `main(args)`: port mặc định 2209, có thể truyền qua `args[0]`.
- `stop()`: đóng `ServerSocket`, làm `accept()` ném `IOException` và thoát vòng lặp. Hàm này được đăng ký làm *shutdown hook*, nên Ctrl+C cũng đóng server gọn gàng.
- `MainServer` **không xử lý nghiệp vụ**. Nó chỉ đón khách rồi giao cho `ClientHandler`.

## 4.3. `ClientHandler`: người phục vụ riêng cho một client

[ClientHandler.java](../../src/minesweeperonline/server/ClientHandler.java)

### Vòng đời: `run()`

```text
mở reader/writer (UTF-8) → clientManager.addClient(this)
loop: line = readLine()          ← chặn
      bỏ dòng trống
      msg = JsonProtocol.deserialize(line)
      msg == null  → gửi ERROR "Invalid JSON"
      else         → try { handleMessage(msg) } catch (RuntimeException) { gửi ERROR, giữ kết nối }
EOF / IOException → finally cleanup()
```

Khối `try/catch` quanh `handleMessage` giúp một message sai kiểu (ví dụ thiếu `row`) **không làm rớt kết nối** của người chơi.

### Bộ phân phối: `handleMessage()`

Một `switch (message.getType())` gọi các hàm `handleXxx`. Bảng tổng hợp:

| Nhóm | Hàm | Việc chính |
|---|---|---|
| Tài khoản | `handleRegister` | kiểm tra rỗng → trùng tên → `userDAO.register` |
| | `handleLogin` | kiểm tra rỗng → `userDAO.authenticate` → chặn đăng nhập trùng ở kết nối khác → gán `user`/`player` → `clientManager.registerUser` |
| | `handleLogout` | hủy lời mời đang chờ → rời trận như `EXIT` (nếu có) → `unregisterUser` → `user = player = null` (socket vẫn mở) |
| Sảnh | `handleGetOnlinePlayers` | lấy `clientManager.getOnlinePlayers(trừ mình)` |
| | `handleChallenge` / `handleAccept` / `handleReject` | quản lý lời mời PENDING (xem [05 §3](05-luong-chuc-nang.md)) |
| | `expireChallenge` | chạy trên thread `Challenge-Timer`: sau 30s mà lời mời vẫn PENDING thì hủy và gửi `REJECT` cho người mời |
| Trong trận | `handleOpenCell` / `handlePlaceFlag` / `handleRemoveFlag` | lấy `row`, `col` → tìm `GameSession` theo `userId` → **chuyển cho GameSession** |
| | `handleForfeit` | `session.finishGame(đối thủ, "FORFEIT")` → gửi `GAME_WIN`/`GAME_LOSE` |
| Sau trận | `handlePlayAgain` / `handleExit` | chuyển cho `GameSession` |
| Thống kê | `handleGetLeaderboard` / `handleGetMatchHistory` | gọi DAO, đóng gói JSON |

`ClientHandler` là **tầng điều phối**. Luật chơi nằm ở `GameSession` và `game/`, truy cập dữ liệu nằm ở `database/`.

### Gửi tin: `sendMessage()`

`synchronized` nên mỗi lúc chỉ một thread được ghi vào socket này (lý do ở [02 §2.7](02-kien-thuc-mang-nen-tang.md)). Các thread khác gửi tin cho client này bằng cách gọi `handler.sendMessage(...)` của nó. Ví dụ `GameSession` gửi `GAME_UPDATE` cho đối thủ theo cách này.

### Dọn dẹp: `cleanup()`

Chạy khi kết nối kết thúc, vì bất kỳ lý do gì:

1. `clientManager.removeClient(this)`: không còn online.
2. `cancelPendingChallenge()`: hủy lời mời đang chờ, trả người kia về `IDLE`. Nếu người kia là người mời đang chờ thì báo `ERROR` cho họ.
3. Nếu đang ở trong một `GameSession`:
   - trận chưa xong: xử **thua do bỏ cuộc** (`FORFEIT_DISCONNECT`) và gửi `OPPONENT_DISCONNECTED` cho đối thủ;
   - sau đó (cả khi trận đã xong): `session.handleExit(...)` gửi `EXIT` cho đối thủ, đưa cả hai về `IDLE` và xóa session.
4. Đóng socket.

## 4.4. `ClientManager`: danh bạ kết nối

[ClientManager.java](../../src/minesweeperonline/server/ClientManager.java)

| Cấu trúc | Ý nghĩa |
|---|---|
| `allClients: CopyOnWriteArrayList<ClientHandler>` | mọi kết nối, kể cả chưa đăng nhập |
| `onlineUsers: ConcurrentHashMap<Integer, ClientHandler>` | người đã đăng nhập: `userId → handler`. Đây là cách server **tìm đúng socket để gửi cho một người cụ thể** |

Hàm hay dùng: `registerUser`, `unregisterUser`, `getClientByUserId`, `getOnlinePlayers(excludeId)`, `removeClient`.
`broadcast`/`broadcastExcept` có sẵn nhưng hiện **chưa dùng**: danh sách online được client tự hỏi lại mỗi 4 giây (polling).

`ClientManager` còn được dùng làm **đối tượng khóa** cho các thao tác thách đấu (`synchronized (clientManager)`), xem [06](06-trang-thai-va-da-luong.md).

## 4.5. `GameManager`: danh bạ trận đấu

[GameManager.java](../../src/minesweeperonline/server/GameManager.java)

| Hàm | Việc |
|---|---|
| `createSession(p1, h1, p2, h2)` | tạo `GameSession`, ghi `sessions[gameId]` và `userToGameMap[p1]`, `userToGameMap[p2]` |
| `getSessionByUserId(userId)` | từ người chơi suy ra trận của họ. **Mọi thao tác trong trận đều tra theo cách này** |
| `removeSession(gameId)` | xóa trận; chỉ gỡ ánh xạ của người chơi **còn trỏ vào đúng trận này** (`remove(key, value)`) |

## 4.6. `GameSession`: trọng tài của một trận

[GameSession.java](../../src/minesweeperonline/server/GameSession.java)

### Trạng thái và đồng hồ

```text
constructor ─► READY ──(5s, timerScheduler.schedule)──► PLAYING ──► FINISHED
                 │                                         │
                 └─ gửi GAME_START khi chuyển sang PLAYING ─┘ tick mỗi 1s: remainingSeconds--
                                                              về 0 → handleTimeout()
```

- `gameId` = 8 ký tự đầu của một UUID ngẫu nhiên.
- Hai bàn `board1`, `board2` **độc lập**: mìn của mỗi bàn chỉ được rải khi người đó **click lần đầu** (đảm bảo ô đầu tiên và 8 ô xung quanh không có mìn).
- Đồng hồ chính thức nằm ở server (`remainingSeconds`). Client chỉ chạy một đồng hồ hiển thị riêng, bắt đầu từ lúc nhận `GAME_START`.

### Xử lý thao tác: `handleOpenCell`, `handlePlaceFlag`, `handleRemoveFlag`

Cả ba đều chạy trong `synchronized (gameLock)` và có chung khuôn:

1. Nếu trận đã xong hoặc chưa ở `PLAYING` thì bỏ qua.
2. Lấy bàn cờ **của chính người gửi** (`getBoardForPlayer(userId)`).
3. Gọi `Board` (`openCell` / `placeFlag` / `removeFlag`). Nếu không hợp lệ thì bỏ qua.
4. Cập nhật thống kê vào `Player`.
5. Gửi kết quả chi tiết cho người thao tác và bản rút gọn (`GAME_UPDATE`) cho đối thủ.
6. Riêng `openCell`: nếu trúng mìn thì người này thua; nếu đủ 121 ô thì người này thắng.

### Kết thúc: `finishGame(winnerId, reason)`

- Dùng `AtomicBoolean finished.compareAndSet(false, true)`: **chỉ lần gọi đầu tiên có tác dụng**. Điều này quan trọng vì nhiều nguồn có thể cùng muốn kết thúc trận một lúc: đồng hồ hết giờ, người chơi trúng mìn, đầu hàng, rớt mạng.
- Dừng đồng hồ, đặt `FINISHED`, gọi `saveMatchToDatabase` (tính điểm, ghi DB, cập nhật điểm trong RAM).
- Người chơi **vẫn ở trạng thái `PLAYING`** trong phòng kết quả cho tới khi `EXIT` hoặc rời mạng (đúng đặc tả §2.10).

### Sau trận

- `handlePlayAgain`: ghi nhận phiếu vào `playAgainRequests`. Khi đủ 2 phiếu thì xóa session cũ, tạo session mới với **chính hai `ClientHandler` đó**, và gửi `MATCH_CREATED`.
- `handleExit`: báo `EXIT` cho đối thủ, đưa cả hai về `IDLE`, xóa session.

### Tính điểm (`saveMatchToDatabase`)

| Kết quả | Điểm | Ghi chú |
|---|---|---|
| VICTORY | +5 | |
| DEFEAT | −3 | trúng mìn, bị mở hết trước, thua khi hết giờ |
| FORFEIT | −5 | `reason` chứa "FORFEIT" (đầu hàng, rời trận, mất kết nối) |
| DRAW | +1 | hết giờ, bằng nhau mọi tiêu chí |

Điểm tổng không xuống dưới 0 (`GREATEST(0, totalScore + delta)` trong SQL, `Math.max(0, …)` trong RAM).

## 4.7. `ServerConfig`: hằng số

[ServerConfig.java](../../src/minesweeperonline/server/ServerConfig.java): port, thông tin MySQL (`root` / mật khẩu rỗng, DB `minesweeper_online`), thời gian (READY 5s, trận 300s), điểm số.
`CHALLENGE_TIMEOUT_SECONDS` (30) là hạn trả lời lời mời, do server đếm. Kích thước bàn cờ thực tế lấy từ hằng số trong `Board`.

> `DB_PASSWORD` là mật khẩu MySQL của **máy đang chạy server**. Không nên commit mật khẩu thật lên repo chung.

## 4.8. Tầng lưu trữ (database)

| Bảng | Nội dung |
|---|---|
| `User` | `id`, `username` (unique), `password`, `totalScore`, `createdAt` |
| `Match` | `id` (= gameId), `status`, `startedAt`, `endedAt`, `winnerId`, `result` (lý do kết thúc) |
| `MatchPlayer` | mỗi trận 2 dòng: `matchId`, `userId`, `result`, `openedSafeCells`, `totalActions`, `flagsPlaced`, `score` (delta) |

- Script tạo bảng: [database/schema.sql](../../database/schema.sql).
- **Ghi kết quả trong một transaction** (`MatchPlayerDAO.saveFullMatchTransaction`): insert `Match` → insert 2 `MatchPlayer` → update điểm 2 `User` → `commit`. Nếu lỗi thì `rollback`, không có chuyện điểm được cộng mà lịch sử thiếu.
- **Fallback RAM**: mọi DAO bắt `SQLException` và chuyển sang các `ConcurrentHashMap` tĩnh (có sẵn `alice`/`bob`/`charlie`, mật khẩu `123456`). Nhờ vậy demo được khi máy không có MySQL. Dữ liệu fallback **mất khi tắt server**.
- Mỗi truy vấn mở một kết nối JDBC mới (không có connection pool). Cách này đơn giản và đủ cho quy mô bài tập.
