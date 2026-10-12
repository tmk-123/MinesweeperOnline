# 10. Ôn tập vấn đáp & giới hạn hiện tại

## 10.1. Câu hỏi thường gặp

**1. Vì sao dùng TCP mà không dùng UDP?**
Mỗi message (mở ô, thắng/thua) phải đến đủ và đúng thứ tự. TCP lo việc gửi lại và sắp xếp thứ tự. Nếu dùng UDP, ta phải tự làm những việc đó. Game theo thao tác, không cần độ trễ cực thấp như game bắn súng, nên TCP phù hợp hơn.

**2. TCP là dòng byte, làm sao biết một message kết thúc ở đâu?**
Dự án dùng newline-delimited JSON: mỗi message là JSON trên một dòng, cộng thêm `\n`. Bên nhận đọc bằng `BufferedReader.readLine()`. (`JsonProtocol.serializeWithNewline`)

**3. Server phục vụ nhiều client cùng lúc thế nào?**
Thread-per-client. `MainServer` gọi `accept()` trong vòng lặp; mỗi socket mới được giao cho `new Thread(new ClientHandler(...))`. Mỗi handler chặn ở `readLine()` của riêng client mình.
*Nhược điểm:* mỗi kết nối tốn một thread. *Cải tiến:* thread pool (`Executors.newFixedThreadPool`) hoặc Java NIO (`Selector`, non-blocking).

**4. Server gửi tin cho đúng một người cụ thể bằng cách nào?**
`ClientManager.onlineUsers` ánh xạ `userId → ClientHandler`. Khi muốn gửi cho B: `clientManager.getClientByUserId(B).sendMessage(msg)`. Trong trận, `GameSession` giữ sẵn `handler1` và `handler2`.

**5. Vì sao `sendMessage` phải `synchronized`?**
Nhiều thread có thể cùng ghi vào một socket: thread của chính client, thread của đối thủ, thread đồng hồ trận. Nếu không khóa, hai chuỗi JSON có thể xen vào nhau và làm hỏng framing.

**6. Client nhận dữ liệu mà giao diện không bị đơ là nhờ đâu?**
Một thread nền (`Client-Network-Listener`) chặn ở `readLine()`. Mỗi khi có message, nó gọi listener, và listener dùng `SwingUtilities.invokeLater` để cập nhật giao diện trên EDT.

**7. Client mất kết nối giữa trận thì sao?**
`readLine()` trả `null` hoặc ném `IOException`, rồi `cleanup()` chạy: người rời đi bị xử FORFEIT (−5), đối thủ nhận `OPPONENT_DISCONNECTED` (+5) và `EXIT`, session bị xóa. Xem [07](07-xu-ly-loi-va-ngat-ket-noi.md).

**8. Làm sao chống gian lận?**
Server-authoritative: mìn chỉ nằm ở server, và chỉ được rải khi có click đầu tiên. Client chỉ nhận kết quả của ô vừa mở. Đối thủ chỉ nhận tọa độ, không nhận số. Server tìm trận theo `userId` của kết nối chứ không theo `gameId` client gửi lên, nên không ai thao tác được vào bàn của người khác. Server kiểm tra lại mọi thao tác dù client đã chặn trước.

**9. Hai người thao tác cùng lúc thì xử lý ra sao?**
Mọi thao tác trong trận chạy trong `synchronized (gameLock)`, nên được xử lý **lần lượt theo thứ tự server nhận**. Việc kết thúc trận dùng `AtomicBoolean.compareAndSet`, nên dù trúng mìn, hết giờ và rớt mạng xảy ra sát nhau, trận cũng chỉ kết thúc và ghi DB một lần.

**10. Hai người cùng thách đấu một người thì sao?**
Bước kiểm tra `IDLE` và đặt `CHALLENGING` nằm trong `synchronized (clientManager)`. Người đến trước được chấp nhận; người sau nhận `ERROR "đang bận"`.

**11. Đồng hồ trận do ai quản lý?**
Server (`GameSession.timerScheduler`, tick mỗi giây). Client chỉ có đồng hồ hiển thị. Hết giờ là do server quyết định. Hạn 30 giây của lời mời cũng do server đếm (`Challenge-Timer`).

**11b. Đăng xuất khác gì đóng app?**
Đăng xuất gửi `LOGOUT`: server hủy lời mời, gỡ tài khoản khỏi danh sách online, nhưng **giữ socket** để client quay về màn hình đăng nhập. Đóng app thì socket đóng: `readLine()` trả `null` và `cleanup()` dọn dẹp toàn bộ.

**12. Vì sao dùng JSON (Gson) thay cho `ObjectOutputStream`?**
JSON là văn bản nên dễ đọc khi debug, không phụ thuộc phiên bản class Java, và client viết bằng ngôn ngữ khác vẫn hiểu được. `ObjectOutputStream` gọn hơn nhưng gắn chặt với Java và `serialVersionUID`, đồng thời có rủi ro bảo mật khi deserialize dữ liệu không tin cậy.

**13. Kết quả trận được ghi thế nào cho an toàn?**
Ghi trong một transaction JDBC (`setAutoCommit(false)` … `commit()`/`rollback()`) gồm: một dòng `Match`, hai dòng `MatchPlayer` và cập nhật điểm hai `User`. Các thao tác này hoặc cùng thành công, hoặc không thao tác nào được áp dụng.

**14. Muốn chơi giữa hai máy thì làm gì?**
Chạy server ở máy A, mở firewall cổng 2209, rồi ở máy B chạy `MainClient <IP máy A> 2209`. Xem [09](09-chay-va-kiem-thu.md).

## 10.2. Giới hạn còn tồn tại (chưa sửa)

| # | Giới hạn | Ảnh hưởng | Hướng sửa |
|---|---|---|---|
| 1 | Không có heartbeat / `setSoTimeout` | Rút dây mạng "êm" thì server có thể không biết ngay | Gửi ping định kỳ (cần thêm message), hoặc đặt read timeout |
| 2 | Mật khẩu lưu dạng thô (nhóm quyết định giữ nguyên) | Lộ DB là lộ mật khẩu | Băm (SHA-256 + salt, hoặc BCrypt) |
| 3 | Ghi socket trong lúc giữ `gameLock` | Một client treo có thể làm khựng trận | Gửi tin sau khi nhả khóa, hoặc dùng hàng đợi gửi riêng |
| 4 | Danh sách online dùng polling 4s | Tốn băng thông, trễ tối đa 4s | Server chủ động đẩy khi có thay đổi (`broadcast`) |
| 5 | DB chỉ lưu điểm thay đổi, không lưu "điểm trước/sau trận" như đặc tả §2.7 | Thiếu một chi tiết so với đặc tả | Thêm cột `scoreBefore`/`scoreAfter` vào `MatchPlayer` |
| 6 | Số thread không giới hạn | Nhiều kết nối thì tốn tài nguyên | Thread pool |
