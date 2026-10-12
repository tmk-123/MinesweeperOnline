# 5. Luồng từng chức năng

Mỗi chức năng được trình bày theo cùng một khung: **sơ đồ trao đổi → server kiểm tra gì → code ở đâu**.

Ký hiệu: `CH(A)` là `ClientHandler` của người chơi A (chạy trên thread của A).

---

## 1. Đăng ký

```text
RegisterFrame ──REGISTER{username,password}──► CH.handleRegister
                                               ├─ rỗng?            → REGISTER_FAILED
                                               ├─ existsByUsername → REGISTER_FAILED "đã tồn tại"
                                               └─ userDAO.register → REGISTER_SUCCESS
```

- Đăng ký **không đăng nhập luôn**. Người dùng quay lại màn hình đăng nhập.
- Mật khẩu đang lưu **dạng thô** (xem giới hạn ở [10](10-on-tap-van-dap.md)).

## 2. Đăng nhập

```text
LoginFrame ──LOGIN──► CH.handleLogin
                      ├─ rỗng?                                   → LOGIN_FAILED
                      ├─ userDAO.authenticate == null            → LOGIN_FAILED "sai mật khẩu"
                      ├─ tài khoản đang gắn với KẾT NỐI KHÁC     → LOGIN_FAILED "đăng nhập nơi khác"
                      ├─ kết nối này đã đăng nhập trước đó       → hủy lời mời cũ; nếu khác tài khoản thì gỡ tài khoản cũ
                      ├─ this.user = ..., this.player = new Player(...)  (status IDLE)
                      ├─ clientManager.registerUser(userId, this)
                      └─ LOGIN_SUCCESS {userId, username, totalScore} ──► LoginFrame mở MainMenuFrame
```

Một kết nối có thể đăng nhập nhiều lần liên tiếp (ví dụ đăng xuất rồi đăng nhập tài khoản khác), vì socket vẫn giữ nguyên trong suốt thời gian chạy client.

## 2b. Đăng xuất

```text
MainMenuFrame (nút Đăng xuất) ──LOGOUT──► CH.handleLogout
                                          ├─ cancelPendingChallenge()  (người đang mời mình nhận ERROR "đã rời đi")
                                          ├─ handleExit()              (nếu còn ở trong trận: xử như EXIT/FORFEIT)
                                          └─ [lock clientManager] unregisterUser ; user = player = null
client: đóng sảnh, mở LoginFrame (cùng socket)
```

- Server **không trả lời** `LOGOUT`. Socket vẫn mở, client quay lại màn hình đăng nhập.
- Bước gán `user = player = null` nằm trong khóa `clientManager`. Nhờ vậy thread khác (đang xử lý thách đấu) không bao giờ thấy một handler "đang online nhưng `player == null`".

## 3. Thách đấu

Đây là chức năng có nhiều trạng thái nhất. Server lưu lời mời đang chờ (**PENDING**) trong hai trường của `ClientHandler`:

- `pendingTargetId`: người **mình đã mời** (ở phía người mời).
- `pendingChallengerId`: người **đã mời mình** (ở phía người được mời).

Mọi thao tác đọc/ghi hai trường này và `status` đều nằm trong `synchronized (clientManager)`.

### 3.1. Gửi lời mời

```text
A: OnlinePlayersPanel ──CHALLENGE{targetUserId=B}──► CH(A).handleChallenge
   [lock clientManager]
     A chưa đăng nhập / tự mời mình         → ERROR
     A không IDLE                            → ERROR "Bạn đang bận"
     B không online                          → ERROR "không online"
     B không IDLE                            → ERROR "đang bận hoặc đang thi đấu"
     A.status = B.status = CHALLENGING
     A.pendingTargetId = B ; B.pendingChallengerId = A ; A.pendingChallengeToken = số mới
   [unlock]
   CHALLENGE_TIMER.schedule(expireChallenge(token), 30s)     ← hạn chót phía server
   CH(B).sendMessage(CHALLENGE_REQUEST{fromUserId=A}) ──► B: ChallengeDialog (đếm 30s)
```

**Vì sao cần khóa?** Nếu A và C cùng mời B đúng một lúc mà không khóa, cả hai có thể cùng thấy B đang `IDLE` và cùng gửi lời mời. Khóa biến bước "kiểm tra rồi đặt" thành **nguyên tử**, nên chỉ một người thắng và người kia nhận `ERROR`.

### 3.2. Chấp nhận

```text
B ──ACCEPT{challengerUserId=A}──► CH(B).handleAccept
   [lock clientManager]
     B.pendingChallengerId != A                → ERROR "lời mời không còn hiệu lực"
     A đã offline / A không còn mời B          → ERROR "người thách đấu đã rời mạng", B → IDLE
     xóa pending hai phía
     session = gameManager.createSession(A, CH(A), B, CH(B))   // cả hai → PLAYING, READY 5s bắt đầu
   [unlock]
   MATCH_CREATED → A (đối thủ = B)
   MATCH_CREATED → B (đối thủ = A)
```

Bước kiểm tra `pendingChallengerId` ngăn client gửi `ACCEPT` "giả" để ép người khác vào trận khi họ chưa hề mời.

### 3.3. Từ chối / hết 30 giây

```text
B ──REJECT{challengerUserId=A}──► CH(B).handleReject
   [lock] nếu không còn pending với A → bỏ qua (REJECT trễ)
          xóa pending hai phía, A.status = B.status = IDLE
   [unlock]
   REJECT{rejectedByUsername=B} → A   (client A hiện "đã từ chối", bật lại nút)
```

**Hết 30 giây không trả lời:** có hai cơ chế, cơ chế nào đến trước thì có hiệu lực:

- `ChallengeDialog` phía B tự gửi `REJECT`.
- **Server** (thread `Challenge-Timer`) chạy `expireChallenge(token)`: nếu lời mời **đó** vẫn PENDING thì đưa cả hai về `IDLE` và gửi `REJECT` cho A.

Server cần đồng hồ riêng vì không được tin client: nếu client B bị treo hoặc bị sửa, A vẫn được giải phóng sau 30 giây. `token` giúp phân biệt các lời mời. Ví dụ: A mời B, B từ chối, A mời lại. Khi đồng hồ của **lời mời cũ** chạy tới hạn, nó thấy token không khớp và bỏ qua, không hủy nhầm lời mời mới.

### 3.4. Một bên rời mạng hoặc đăng xuất khi lời mời đang chờ

`cleanup()` (khi rời mạng) và `handleLogout()` (khi đăng xuất) đều gọi `cancelPendingChallenge()`:

- **Người mời rời đi**: người được mời về `IDLE`. Nếu sau đó họ bấm "Chấp nhận" thì nhận `ERROR "lời mời không còn hiệu lực"`.
- **Người được mời rời đi**: người mời về `IDLE` và nhận `ERROR "… đã rời đi, lời mời bị hủy"`, nút "Thách đấu" bật lại.

---

## 4. Vào trận: READY → PLAYING

```text
createSession ──► GameSession(...)  state = READY
                  timerScheduler.schedule(5s):
                       state = PLAYING
                       GAME_START{durationSeconds:300} → A, B
                       startPlayingTimer(): mỗi 1s remainingSeconds--, về 0 → handleTimeout()
Client: MATCH_CREATED → mở GameFrame (đếm READY hiển thị, bàn cờ khóa)
        GAME_START    → mở khóa bàn cờ, chạy đồng hồ hiển thị 05:00
```

Thao tác gửi lên trong giai đoạn READY bị bỏ qua (`state != PLAYING`).

## 5. Mở ô

```text
A click trái ──OPEN_CELL{row,col}──► CH(A).handleOpenCell → session.handleOpenCell(A,row,col)
[lock gameLock]
  board = board của A
  result = board.openCell(row, col, newlyOpened)
     INVALID (ngoài bàn / đã mở / đang có cờ)        → bỏ qua
     lần mở đầu tiên: MineGenerator rải 23 mìn tránh vùng 3×3 quanh (row,col)
     totalActions++ (1 lần cho mỗi click hợp lệ)
     MINE  → finishGame(B, "CLICKED_MINE") ; GAME_LOSE → A ; GAME_WIN → B
     SAFE, ô số (1..8) → mở đúng ô đó
     SAFE, ô 0        → BFS loang: mở mọi ô 0 liên thông + viền ô số
        nếu openedSafeCells == 121 → finishGame(A, "CLEARED_ALL_SAFE_CELLS") ; GAME_WIN → A ; GAME_LOSE → B
        ngược lại: CELL_OPENED{openedCells, openedSafeCells, totalActions} → A
                   GAME_UPDATE{opponentOpenedSafeCells, ..., openedCoords:[{r,c}]} → B
[unlock]
```

**BFS trong `Board.openCell`:** dùng hàng đợi `ArrayDeque`. Lấy một ô 0 ra, xét 8 ô quanh nó: ô nào chưa mở, không có cờ và không phải mìn thì mở; nếu ô đó cũng là 0 thì đưa vào hàng đợi. Các ô mở tự động **được tính vào `openedSafeCells`** nhưng **không tăng `totalActions`** (đúng đặc tả §2.4.2).

## 6. Cắm / gỡ cờ

```text
A click phải ──PLACE_FLAG / REMOVE_FLAG──► session.handlePlaceFlag / handleRemoveFlag
[lock gameLock]
  placeFlag hợp lệ khi: ô chưa mở, chưa có cờ, currentFlags < 23
     currentFlags++, flagsPlaced++, totalActions++
  removeFlag hợp lệ khi: ô đang có cờ
     currentFlags--, flagsRemoved++, totalActions++
  FLAG_PLACED/FLAG_REMOVED{row,col,currentFlags,totalActions} → A
  GAME_UPDATE{…, flagRow, flagCol, flagState} → B   (bàn nhỏ của B vẽ/xóa cờ)
[unlock]
```

Server **không** kiểm tra cờ có đặt đúng mìn hay không (đúng đặc tả §2.4.3).

## 7. Kết thúc trận: 4 con đường

| Nguồn | Chạy trên thread | Gọi | `reason` |
|---|---|---|---|
| Mở trúng mìn | thread của người click | `finishGame(đối thủ, …)` | `CLICKED_MINE` |
| Mở đủ 121 ô | thread của người click | `finishGame(người click, …)` | `CLEARED_ALL_SAFE_CELLS` |
| Hết 300 giây | thread `timerScheduler` của trận | `handleTimeout()` → `GameRules.arbitrateTimeout` | `TIMEOUT` |
| Đầu hàng (`FORFEIT`) / `EXIT` giữa trận | thread của người đầu hàng | `ClientHandler.handleForfeit` | `FORFEIT` |
| Mất kết nối giữa trận | thread của người rời mạng (trong `cleanup`) | `finishGame(đối thủ, …)` | `FORFEIT_DISCONNECT` |

**Phân xử khi hết giờ** (`GameRules.arbitrateTimeout`), xét lần lượt:

1. `openedSafeCells` nhiều hơn thì thắng.
2. Bằng nhau thì `totalActions` **ít hơn** thắng.
3. Bằng nhau thì `flagsPlaced` nhiều hơn thắng.
4. Vẫn bằng thì **DRAW**, cả hai nhận `GAME_DRAW`.

Vì `finishGame` dùng `compareAndSet`, nếu hai nguồn xảy ra gần như cùng lúc (ví dụ người chơi trúng mìn đúng giây cuối), chỉ **nguồn đầu tiên** được tính và kết quả chỉ được ghi DB một lần.

## 8. Phòng kết quả: chơi lại / thoát

```text
            ┌─ A: PLAY_AGAIN ──► WAITING → A ; REQUESTED → B (B thấy "đối thủ muốn chơi lại")
FINISHED ───┤   B: PLAY_AGAIN ──► đủ 2 phiếu: removeSession(cũ) ; createSession(mới) ; MATCH_CREATED → A, B
            │
            └─ A: EXIT ──► session.handleExit: EXIT → B ; A, B → IDLE ; removeSession
                           (B vẫn ở màn hình kết quả, nút "Chơi lại" bị khóa, B bấm "Thoát" để về sảnh)
```

- Trận mới khi chơi lại có `gameId` mới, hai bàn cờ mới (mìn rải lại ở click đầu) và đồng hồ mới.
- Nếu một người **rời mạng trong phòng kết quả**, `cleanup()` gọi `handleExit`, nên người còn lại cũng nhận `EXIT`.

## 9. Danh sách người chơi online

```text
MainMenuFrame (Timer 4s) ──GET_ONLINE_PLAYERS──► CH.handleGetOnlinePlayers
                         ◄──ONLINE_PLAYERS{players:[{userId, username, totalScore, status}]}──
```

Đây là cơ chế **polling** (client hỏi định kỳ), không phải server chủ động đẩy. Điểm `totalScore` trong danh sách được cập nhật ngay sau mỗi trận (server sửa `Player` trong RAM).

## 10. Bảng xếp hạng & lịch sử

- `GET_LEADERBOARD` → `UserDAO.getLeaderboard(50)`: `LEFT JOIN MatchPlayer`, sắp theo `totalScore DESC, wins DESC, username ASC`.
- `GET_MATCH_HISTORY{limit}` → `MatchPlayerDAO.getMatchHistory(userId, limit)`: lấy trận của **chính người hỏi** (server dùng `userId` của kết nối, không tin `userId` gửi lên), kèm tên đối thủ, mới nhất trước.
