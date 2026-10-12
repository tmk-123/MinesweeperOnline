# 3. Giao thức (Protocol)

Protocol là "ngôn ngữ chung" giữa client và server. Nó được định nghĩa trong `common/protocol/`:

| File | Vai trò |
|---|---|
| [MessageType.java](../../src/minesweeperonline/common/protocol/MessageType.java) | Danh sách **loại** message (enum) |
| [Message.java](../../src/minesweeperonline/common/protocol/Message.java) | **Phong bì** chung cho mọi message |
| [JsonProtocol.java](../../src/minesweeperonline/common/protocol/JsonProtocol.java) | Đóng gói/mở gói JSON (Gson) + các hàm tạo message sẵn (`createLogin`, `createOpenCell`, …) |

> Quy tắc dự án: **không tự ý thay đổi protocol**, tức là không thêm/đổi `MessageType` hay tên field trong payload khi chưa thống nhất với cả nhóm, vì client và server phải đổi cùng lúc.

## 3.1. Phong bì (envelope)

Mỗi message là **một dòng JSON** kết thúc bằng `\n`:

```json
{"type":"OPEN_CELL","gameId":"4d0b4a22","payload":{"row":3,"col":5}}
```

| Trường | Ý nghĩa |
|---|---|
| `type` | Loại message: bên nhận `switch` theo trường này |
| `requestId` | Mã yêu cầu (tùy chọn). Server chép lại vào một số phản hồi, nhưng **client hiện chưa gửi** nên thực tế trường này thường vắng mặt (Gson bỏ qua field `null`) |
| `gameId` | Mã trận (tùy chọn). Client có gửi kèm, nhưng **server không dựa vào nó**: server tìm trận qua `userId` của kết nối (`GameManager.getSessionByUserId`). Nhờ vậy client không thể thao tác vào trận của người khác |
| `payload` | Dữ liệu cụ thể (`JsonObject`), mỗi `type` có một bộ field riêng |

**Đóng/mở gói:**

```java
String line = JsonProtocol.serializeWithNewline(msg);  // gửi
Message msg = JsonProtocol.deserialize(line);          // nhận; trả null nếu JSON hỏng
```

Nếu `type` không có trong enum (gõ sai, phiên bản khác), Gson gán `type = null` và server **lặng lẽ bỏ qua** message đó.

## 3.2. Bảng message đầy đủ

Ký hiệu: **C→S** là client gửi server; **S→C** là server gửi client; "(đối thủ)" là server gửi cho người còn lại.

### Tài khoản

| Yêu cầu (C→S) | Payload | Phản hồi (S→C) | Payload phản hồi |
|---|---|---|---|
| `REGISTER` | `username`, `password` | `REGISTER_SUCCESS` / `REGISTER_FAILED` | `message` / `reason` |
| `LOGIN` | `username`, `password` | `LOGIN_SUCCESS` / `LOGIN_FAILED` | `userId`, `username`, `totalScore` / `reason` |
| `LOGOUT` | – | *(không phản hồi)* | Tài khoản rời trạng thái online. Kết nối TCP **vẫn giữ** để quay về màn hình đăng nhập |

### Sảnh chờ & thách đấu

| Yêu cầu (C→S) | Payload | Server gửi | Payload |
|---|---|---|---|
| `GET_ONLINE_PLAYERS` | – | `ONLINE_PLAYERS` → người hỏi | `players: [{userId, username, totalScore, status}]` (trừ chính mình) |
| `CHALLENGE` | `targetUserId`, `targetUsername` | `CHALLENGE_REQUEST` → người bị mời | `fromUserId`, `fromUsername` |
| | | hoặc `ERROR` → người mời | `errorMessage` (không online, đang bận, tự mời mình…) |
| `ACCEPT` | `challengerUserId` | `MATCH_CREATED` → **cả hai** | `gameId`, `opponentId`, `opponentUsername` |
| | | hoặc `ERROR` → người nhận lời | lời mời không còn hiệu lực / người mời đã rời mạng |
| `REJECT` | `challengerUserId` | `REJECT` → người mời | `rejectedByUserId`, `rejectedByUsername` |
| *(server, sau 30s không trả lời)* | | `REJECT` → người mời | như trên: lời mời hết hạn được báo giống một lời từ chối |

`status` của người chơi ∈ `IDLE`, `CHALLENGING`, `PLAYING` (hằng số trong `Player`).

### Trong trận

| Yêu cầu (C→S) | Payload | Server gửi → người thao tác | Server gửi → đối thủ |
|---|---|---|---|
| *(tự động sau 5s)* | | `GAME_START {durationSeconds}` | `GAME_START {durationSeconds}` |
| `OPEN_CELL` | `row`, `col` | `CELL_OPENED {openedCells:[{row,col,adjacentMines,isMine,isExploded}], openedSafeCells, totalActions}` | `GAME_UPDATE {opponentId, opponentOpenedSafeCells, opponentCurrentFlags, opponentTotalActions, openedCoords:[{r,c}]}` |
| `PLACE_FLAG` | `row`, `col` | `FLAG_PLACED {row, col, currentFlags, totalActions}` | `GAME_UPDATE {…, flagRow, flagCol, flagState:true}` |
| `REMOVE_FLAG` | `row`, `col` | `FLAG_REMOVED {row, col, currentFlags, totalActions}` | `GAME_UPDATE {…, flagRow, flagCol, flagState:false}` |
| `FORFEIT` | – | `GAME_LOSE` | `GAME_WIN` |

**Điểm chống gian lận:** `CELL_OPENED` (gửi cho chính người mở) có `adjacentMines` để vẽ số. `GAME_UPDATE` (gửi cho đối thủ) **chỉ có tọa độ** (`r`, `c`) và số liệu tổng, không có số mìn kề hay vị trí mìn.

Yêu cầu không hợp lệ (ô đã mở, ô đang cắm cờ, ngoài bàn, chưa tới `PLAYING`, trận đã xong, quá 23 cờ) bị **bỏ qua im lặng**: không phản hồi, không tính thao tác.

### Kết thúc trận

| Message (S→C) | Payload | Khi nào |
|---|---|---|
| `GAME_WIN` | `reason`, `scoreDelta` (+5) | đối thủ trúng mìn / mình mở hết ô / hết giờ mà mình hơn / đối thủ đầu hàng |
| `GAME_LOSE` | `reason`, `scoreDelta` (−3, hoặc −5 nếu đầu hàng) | ngược lại |
| `GAME_DRAW` | `reason`, `scoreDelta` (+1) | hết giờ và mọi chỉ số bằng nhau |
| `OPPONENT_DISCONNECTED` | `reason`, `scoreDelta` (+5) | đối thủ mất kết nối giữa trận |

Khi người chơi **mở trúng mìn** hoặc **mở ô an toàn cuối cùng**, server gửi thẳng `GAME_LOSE`/`GAME_WIN`, **không** gửi `CELL_OPENED` cho thao tác đó.

### Sau trận (phòng kết quả)

| Yêu cầu (C→S) | Server gửi |
|---|---|
| `PLAY_AGAIN` | lần 1: `PLAY_AGAIN {status:"WAITING", message}` → người bấm; `PLAY_AGAIN {status:"REQUESTED", requestedByUserId, requestedByUsername, message}` → đối thủ |
| | khi cả hai đã bấm: `MATCH_CREATED` → cả hai (trận mới, `gameId` mới) |
| `EXIT` | `EXIT {message}` → đối thủ (nút "Chơi lại" của họ bị khóa) |

### Thống kê

| Yêu cầu (C→S) | Payload | Phản hồi |
|---|---|---|
| `GET_LEADERBOARD` | – | `LEADERBOARD {leaderboard:[{rank, userId, username, totalScore, wins, totalMatches, winRate}]}` (top 50) |
| `GET_MATCH_HISTORY` | `limit` (server giới hạn 1..100, mặc định 20) | `MATCH_HISTORY {history:[{matchId, opponentName, result, scoreDelta, openedSafeCells, flagsPlaced, totalActions, durationSeconds, playedAt, outcomeReason}]}` |

### Lỗi chung

| Message (S→C) | Payload | Khi nào |
|---|---|---|
| `ERROR` | `errorMessage` | JSON hỏng, payload sai kiểu, yêu cầu bị từ chối ở sảnh, hoặc người mình đang mời đã rời đi |

`GAME_READY` có trong enum nhưng **không được dùng**: giai đoạn READY được báo ngầm qua `MATCH_CREATED`, và kết thúc bằng `GAME_START`.

## 3.3. Một phiên mẫu (đúng như trên dây mạng)

```text
A→S {"type":"LOGIN","payload":{"username":"alice","password":"123456"}}
S→A {"type":"LOGIN_SUCCESS","payload":{"userId":1,"username":"alice","totalScore":25}}
A→S {"type":"CHALLENGE","payload":{"targetUserId":2,"targetUsername":"bob"}}
S→B {"type":"CHALLENGE_REQUEST","payload":{"fromUserId":1,"fromUsername":"alice"}}
B→S {"type":"ACCEPT","payload":{"challengerUserId":1}}
S→A {"type":"MATCH_CREATED","payload":{"gameId":"4d0b4a22","opponentId":2,"opponentUsername":"bob"}}
S→B {"type":"MATCH_CREATED","payload":{"gameId":"4d0b4a22","opponentId":1,"opponentUsername":"alice"}}
      ... 5 giây ...
S→A,B {"type":"GAME_START","gameId":"4d0b4a22","payload":{"durationSeconds":300}}
A→S {"type":"OPEN_CELL","gameId":"4d0b4a22","payload":{"row":5,"col":5}}
S→A {"type":"CELL_OPENED","gameId":"4d0b4a22","payload":{"openedCells":[{"row":5,"col":5,"adjacentMines":0,"isMine":false,"isExploded":false}, ...],"openedSafeCells":31,"totalActions":1}}
S→B {"type":"GAME_UPDATE","gameId":"4d0b4a22","payload":{"opponentId":1,"opponentOpenedSafeCells":31,"opponentCurrentFlags":0,"opponentTotalActions":1,"openedCoords":[{"r":5,"c":5}, ...]}}
```

## 3.4. Thêm một loại message mới (quy trình, khi nhóm đã đồng ý)

1. Thêm hằng vào `MessageType`.
2. (Tùy chọn) thêm hàm `createXxx(...)` trong `JsonProtocol`.
3. Server: thêm `case` trong `ClientHandler.handleMessage` + hàm `handleXxx`.
4. Client: gửi bằng `connection.sendMessage(...)` và xử lý phản hồi trong listener của màn hình tương ứng.
5. Cập nhật bảng ở tài liệu này.
