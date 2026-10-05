# Chương 08: Xử Lý Lỗi & Ngoại Lệ (Error Handling)

Lập trình mạng không thể tránh khỏi các sự cố: rớt mạng, mất kết nối, người chơi tắt đột ngột, server chưa bật hoặc database bị tắt. Chương này phân tích chi tiết cách project giải quyết từng tình huống lỗi.

---

## 1. Bảng Tổng Hợp Các Ngoại Lệ (Exceptions) Trong Dự Án

| Tên Ngoại Lệ (Exception) | Nguyên Nhân Xảy Ra | Vị Trí Xảy Ra | Cách Dự Án Xử Lý |
| :--- | :--- | :--- | :--- |
| `java.net.BindException` | Cổng 2209 đã bị chiếm (do một Server cũ đang chạy ngầm hoặc phần mềm khác) | [MainServer.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/MainServer.java#L31) | Bắt lỗi trong `catch (IOException e)`, in thông báo: `Could not listen on port 2209` và dừng an toàn. |
| `java.net.ConnectException` | Client cố gắng kết nối nhưng Server chưa được bật | [ServerConnection.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/network/ServerConnection.java#L66) | Bắt lỗi, hiển thị hộp thoại `JOptionPane` hỏi người chơi: *"Không thể kết nối máy chủ! Bạn có muốn thử lại không?"*. |
| `java.net.SocketTimeoutException` | Quá thời gian chờ kết nối 5 giây mà Server không phản hồi | [ServerConnection.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/network/ServerConnection.java#L50) | Ngắt kết nối và gọi `disconnect("Connection failed: timeout")`. |
| `java.net.SocketException` (Connection reset) | Người chơi rút dây mạng, mất Wi-Fi hoặc tắt ứng dụng bằng Task Manager | `ClientHandler.java` và `ServerConnection.java` | Vòng lặp `readLine()` kết thúc, tự động kích hoạt hàm dọn dẹp `cleanup()`. |
| `com.google.gson.JsonSyntaxException` | Gói tin mạng bị rách hoặc có người cố tình gửi dữ liệu sai định dạng | [JsonProtocol.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/JsonProtocol.java#L43) | Bắt ngoại lệ, in log cảnh báo và trả về `null`, không làm sập ứng dụng. |
| `java.sql.SQLException` | MySQL chưa bật, sai mật khẩu root hoặc lỗi kết nối DB | [UserDAO.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/database/UserDAO.java) & [MatchPlayerDAO.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/database/MatchPlayerDAO.java) | **Kích hoạt chế độ In-Memory Fallback**: Lưu user và bảng điểm vào RAM, server vẫn tiếp tục phục vụ bình thường! |

---

## 2. Xử Lý Các Sự Cố Mạng Cụ Thể

### 2.1. Trường hợp: Server chưa chạy mà bật Client
* **Hiện tượng**: Người dùng mở Client trước khi mở Server.
* **Xử lý trong code** ([MainClient.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/MainClient.java#L37)):
  * Phương thức `connection.connect(...)` trả về `false`.
  * Client bật một hộp thoại thông báo màu đỏ:
    ```text
    "Không thể kết nối đến Máy chủ (127.0.0.1:2209)!
    Vui lòng đảm bảo MainServer đã được khởi động.
    Bạn có muốn thử kết nối lại không?"
    ```
  * Nếu chọn **Yes**: Thử kết nối lại.
  * Nếu chọn **No**: Thoát ứng dụng một cách êm đẹp (`System.exit(0)`).

---

### 2.2. Trường hợp: Client bị rớt mạng hoặc tắt app khi đang trong trận đấu
* **Hiện tượng**: Alice và Bob đang thi đấu, đột nhiên Bob rút dây mạng hoặc tắt cửa sổ GameFrame.
* **Xử lý trong code** ([ClientHandler.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/ClientHandler.java#L453)):
  1. Khi Bob ngắt kết nối, `reader.readLine()` ném ngoại lệ -> Chuyển vào hàm `cleanup()`.
  2. Server kiểm tra thấy Bob đang tham gia một `GameSession` chưa kết thúc.
  3. Server lập tức xử thua Bob vì lý do bỏ cuộc (`FORFEIT_DISCONNECT`), trừ 5 điểm của Bob.
  4. Server gửi ngay gói tin `OPPONENT_DISCONNECTED` tới Client của Alice kèm cộng 5 điểm chiến thắng cho Alice:
     ```json
     {
       "type": "OPPONENT_DISCONNECTED",
       "payload": {
         "reason": "Đối thủ mất kết nối!",
         "scoreDelta": 5
       }
     }
     ```
  5. Màn hình của Alice hiện hộp thoại chiến thắng, trận đấu kết thúc công bằng mà không bị treo vĩnh viễn.

---

### 2.3. Trường hợp: Hết giờ thách đấu (30 giây Timeout)
* **Hiện tượng**: Alice gửi thách đấu cho Bob, nhưng Bob đang bận đi lấy nước không bấm Chấp nhận hay Từ chối.
* **Xử lý trong code** ([ChallengeDialog.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/ChallengeDialog.java)):
  * Đồng hồ Swing Timer đếm ngược từ 30 về 0 giây.
  * Khi về 0 giây, Dialog tự động phát gói tin `REJECT` lên Server và tự đóng lại.
  * Alice nhận được thông báo đối thủ từ chối thách đấu và nút "Thách đấu" của Alice sáng trở lại.

---

### 2.4. Trường hợp: Hết 5 phút thi đấu (300 giây Match Timeout)
* **Hiện tượng**: Cả 2 người chơi quá cẩn thận, không ai mở trúng mìn và đồng hồ trận đấu chạm mốc 00:00.
* **Xử lý trong code** ([GameSession.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/GameSession.java) & [GameRules.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/game/GameRules.java)):
  * Server kích hoạt trọng tài phân xử theo thứ tự ưu tiên:
    1. Người mở được nhiều ô an toàn hơn sẽ THẮNG (`VICTORY` +5 điểm).
    2. Nếu số ô bằng nhau: Người có tổng số thao tác (actions) ít hơn sẽ THẮNG.
    3. Nếu vẫn bằng nhau: Người cắm nhiều cờ hợp lệ hơn sẽ THẮNG.
    4. Nếu vẫn bằng nhau tuyệt đối: Xử HÒA (`DRAW`, mỗi bên được thưởng +1 điểm an ủi).

---

### 2.5. Trường hợp: Cơ sở dữ liệu MySQL bị tắt hoặc lỗi kết nối
* **Cơ chế In-Memory Fallback (Dự phòng trong bộ nhớ RAM)**:
  * Trong các file [UserDAO.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/database/UserDAO.java) và [MatchPlayerDAO.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/database/MatchPlayerDAO.java), toàn bộ câu lệnh SQL đều nằm trong khối `try-catch (SQLException e)`.
  * Khi bắt được lỗi không kết nối được MySQL, hệ thống **không báo lỗi làm dừng chương trình**, mà tự động chuyển hướng đọc/ghi sang các biến bộ nhớ RAM:
    * `ConcurrentHashMap<String, User> fallbackUsers` (có sẵn 3 user mẫu: `alice`, `bob`, `charlie` mật khẩu `123456`).
    * `CopyOnWriteArrayList<FallbackMatchRecord> fallbackMatches` để lưu trữ lịch sử trận đấu tạm thời.
  * Nhờ cơ chế này, bạn có thể mang project đi chấm bài trên bất kỳ máy tính nào mà không bắt buộc máy đó phải cài sẵn MySQL Server!
