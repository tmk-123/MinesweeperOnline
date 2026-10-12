# 7. Xử lý lỗi & ngắt kết nối

## 7.1. Bảng tình huống

| Tình huống | Phát hiện ở | Hệ thống làm gì |
|---|---|---|
| Port 2209 đã bị chiếm (`BindException`) | `new ServerSocket(port)` | in `Could not listen on port …`, server dừng |
| Client mở khi server chưa chạy (`ConnectException`) | `ServerConnection.connect` | hộp thoại "Không thể kết nối… Thử lại?" |
| Server không phản hồi khi kết nối | `socket.connect(..., 5000)` | sau 5 giây báo lỗi như trên |
| Client gửi JSON hỏng | `JsonProtocol.deserialize` trả `null` | server gửi `ERROR "Invalid JSON message format"`, **giữ kết nối** |
| JSON đúng cú pháp nhưng sai nội dung (thiếu `row`, `limit:"abc"`…) | `RuntimeException` trong `handleXxx` | `catch` trong `run()`: log + gửi `ERROR "Yêu cầu không hợp lệ!"`, **giữ kết nối** |
| `type` lạ | Gson gán `type = null` | bỏ qua |
| Thao tác chơi không hợp lệ (ô đã mở, có cờ, ngoài bàn, quá 23 cờ, ngoài PLAYING) | `Board` / `GameSession` | bỏ qua im lặng, không tính thao tác |
| `ACCEPT`/`REJECT` không khớp lời mời đang chờ | `handleAccept` / `handleReject` | `ERROR "lời mời không còn hiệu lực"` / bỏ qua |
| Đăng nhập tài khoản đang online ở kết nối khác | `handleLogin` | `LOGIN_FAILED` |
| Client tắt app / rớt mạng | `readLine()` → `null` hoặc `IOException` | `cleanup()`, xem §7.2 |
| MySQL tắt / sai mật khẩu (`SQLException`) | mọi DAO | chuyển sang **fallback RAM**, server vẫn chạy |
| Lỗi giữa transaction ghi kết quả | `saveFullMatchTransaction` | `rollback` + ghi tạm vào RAM |

## 7.2. Ngắt kết nối: `cleanup()` làm gì tùy theo lúc rời đi

| Người rời mạng đang ở… | Kết quả cho người rời | Người còn lại nhận | Trạng thái sau đó |
|---|---|---|---|
| Chưa đăng nhập | – | – | – |
| Sảnh (`IDLE`) | biến mất khỏi danh sách online | – | – |
| Đang **mời** người khác | lời mời bị hủy | (người được mời) bấm Chấp nhận → `ERROR` | người được mời → `IDLE` |
| Đang **được mời** | lời mời bị hủy | (người mời) `ERROR "… đã rời đi, lời mời bị hủy"` | người mời → `IDLE` |
| Trong trận (READY/PLAYING) | **FORFEIT −5** | `OPPONENT_DISCONNECTED` (+5), rồi `EXIT` | người còn lại → `IDLE` ngay (dù vẫn đang xem màn hình kết quả), session bị xóa |
| Phòng kết quả | – | `EXIT` (nút "Chơi lại" bị khóa) | người còn lại → `IDLE`, session bị xóa |

Dù người dùng rời đi lúc nào, server luôn **dọn sạch**: không để lại người chơi ma trong danh sách online, lời mời treo mãi, hay trận đấu không ai kết thúc.

`LOGOUT` (bấm Đăng xuất) dọn dẹp giống hệt bảng trên, chỉ khác là **socket không bị đóng**.

## 7.3. Hết giờ trả lời lời mời (30 giây)

Có hai lớp bảo vệ:

1. **Client**: `ChallengeDialog` đếm ngược bằng `javax.swing.Timer`. Hết giờ thì tự gửi `REJECT`.
2. **Server** (lớp có hiệu lực cuối cùng): khi nhận `CHALLENGE`, server hẹn giờ `ServerConfig.CHALLENGE_TIMEOUT_SECONDS` trên thread `Challenge-Timer`. Hết hạn mà lời mời vẫn PENDING thì đưa cả hai về `IDLE` và gửi `REJECT` cho người mời. Như vậy người mời không bao giờ bị treo, kể cả khi client bên kia bị đơ.

## 7.4. Hết giờ trận (300 giây)

Thread đồng hồ của `GameSession` giảm `remainingSeconds` mỗi giây. Về 0 thì gọi `handleTimeout()` → `GameRules.arbitrateTimeout(p1, p2)` → `finishGame` → gửi `GAME_WIN`/`GAME_LOSE` hoặc `GAME_DRAW` cho cả hai.

Đồng hồ hiển thị ở client chạy độc lập, nên có thể lệch server vài trăm mili-giây. Kết quả **luôn theo server**.

## 7.5. MySQL không chạy: chế độ fallback

- Mọi DAO bọc truy vấn trong `try { ... } catch (SQLException e) { ...dùng RAM... }`.
- Có sẵn 3 tài khoản: `alice`, `bob`, `charlie` (mật khẩu `123456`).
- Đăng ký, đăng nhập, điểm, lịch sử, bảng xếp hạng đều hoạt động, nhưng **mất hết khi tắt server**.
- Log sẽ in `MySQL unavailable (...)`. Đây là dấu hiệu server đang ở chế độ fallback.

## 7.6. Những gì chưa xử lý

Xem danh sách đầy đủ ở [10 – Ôn tập, mục Giới hạn](10-on-tap-van-dap.md). Tóm tắt:

- Không có heartbeat: rút dây mạng mà không có gói nào đi qua thì server có thể không phát hiện được ngay.
