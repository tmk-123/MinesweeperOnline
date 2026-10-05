# Chương 06: Giải Thích Chi Tiết Từng File Server & Network

Chương này cung cấp bảng tổng hợp và phân tích chuyên sâu cho từng file mã nguồn thuộc tầng Server & Network trong project.

---

## 1. Bảng Tổng Hợp Các File

| Tên File | Vai Trò Chính | Được Gọi Bởi (Caller) | Gọi Đến (Callee) |
| :--- | :--- | :--- | :--- |
| [MainServer.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/MainServer.java) | Khởi động Socket Server, lắng nghe port 2209 | Hệ điều hành / Người dùng chạy lệnh `run-server` | `ServerSocket`, `ClientHandler`, `ClientManager`, `GameManager` |
| [ClientHandler.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/ClientHandler.java) | Xử lý kết nối và thông điệp riêng cho 1 Client | `MainServer` (khi có kết nối mới) | `BufferedReader`, `PrintWriter`, `JsonProtocol`, `UserDAO`, `GameSession` |
| [ClientManager.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/ClientManager.java) | Quản lý danh bạ Client kết nối và User Online | `MainServer`, `ClientHandler` | `CopyOnWriteArrayList`, `ConcurrentHashMap`, `Player` |
| [GameManager.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/GameManager.java) | Quản lý danh sách các trận đấu đang diễn ra | `MainServer`, `ClientHandler` | `GameSession`, `ConcurrentHashMap` |
| [GameSession.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/GameSession.java) | Trọng tài ván đấu 2 người, đếm giờ, phân định thắng thua | `GameManager`, `ClientHandler` | `Board`, `MineGenerator`, `MatchPlayerDAO`, `ScheduledExecutorService` |
| [ServerConfig.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/ServerConfig.java) | Hằng số cấu hình hệ thống (Port, DB, Điểm số, Timer) | Toàn bộ các class Server & Client | Không phụ thuộc class nào |
| [ServerConnection.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/network/ServerConnection.java) | Quản lý Socket phía Client, lắng nghe bất đồng bộ | `MainClient`, các màn hình UI Swing | `Socket`, `BufferedReader`, `PrintWriter`, `JsonProtocol` |
| [MainClient.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/MainClient.java) | Điểm khởi đầu ứng dụng Client, mở màn hình đăng nhập | Hệ điều hành / Người dùng chạy lệnh `run-client` | `ServerConnection`, `LoginFrame`, `ServerConfig` |
| [Message.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/Message.java) | Khung phong bì gói tin mạng tiêu chuẩn `{type, payload,...}` | `JsonProtocol`, `ClientHandler`, `ServerConnection` | `MessageType`, `JsonObject` (Gson) |
| [MessageType.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/MessageType.java) | Enum danh mục các loại thông điệp mạng | `Message`, `ClientHandler`, các màn hình UI | Không phụ thuộc class nào |
| [JsonProtocol.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/JsonProtocol.java) | Tiện ích serialize/deserialize JSON kèm `\n` | `ClientHandler`, `ServerConnection`, các class test | Google Gson, `Message`, `MessageType` |
| [DatabaseConnection.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/database/DatabaseConnection.java) | Cung cấp kết nối JDBC tới MySQL Server | `UserDAO`, `MatchDAO`, `MatchPlayerDAO` | `DriverManager`, `ServerConfig` |
| [UserDAO.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/database/UserDAO.java) | Xác thực đăng nhập, đăng ký, tính điểm, Bảng xếp hạng | `ClientHandler` | `DatabaseConnection`, `User`, `LeaderboardEntry` |
| [MatchPlayerDAO.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/database/MatchPlayerDAO.java) | Ghi nhận kết quả trận đấu trong 1 TRANSACTION, Lịch sử đấu | `GameSession`, `ClientHandler` | `DatabaseConnection`, `MatchDAO`, `MatchHistoryEntry` |

---

## 2. Phân Tích Chuyên Sâu Từng File (6 Câu Hỏi)

### 2.1. `MainServer.java`
1. **Dùng để làm gì?**: Mở cổng 2209 bằng `ServerSocket`, chấp nhận các kết nối mạng từ Client và tạo luồng xử lý riêng cho từng Client.
2. **Tại sao cần?**: Nếu không có nó, máy tính không mở cổng mạng, các Client không có điểm đích để kết nối vào.
3. **Nếu xóa file này?**: Không thể khởi động được Server của trò chơi.
4. **Được file nào gọi?**: Người dùng chạy thông qua hàm `main(String[] args)` hoặc lệnh Ant `ant run-server`.
5. **Gọi đến file nào?**: Gọi `ServerConfig`, `ClientManager`, `GameManager`, `ClientHandler`.
6. **Dữ liệu đi qua như thế nào?**: Không xử lý nội dung dữ liệu JSON. File này chỉ đón nhận `Socket` thô từ hệ điều hành và trao `Socket` đó cho `ClientHandler`.

---

### 2.2. `ClientHandler.java`
1. **Dùng để làm gì?**: Chạy trên một luồng riêng (`Thread`), đọc từng dòng JSON từ Client gửi lên, phân tích cú pháp và gọi hàm xử lý tương ứng.
2. **Tại sao cần?**: Để Server phục vụ được hàng chục người chơi đồng thời mà không bị nghẽn (Thread-per-client model).
3. **Nếu xóa file này?**: Server chỉ kết nối được với Client đầu tiên rồi bị treo cứng, không thể giao tiếp với bất kỳ ai.
4. **Được file nào gọi?**: `MainServer.java` khởi tạo mỗi khi có `serverSocket.accept()`.
5. **Gọi đến file nào?**: `JsonProtocol`, `ClientManager`, `GameManager`, `GameSession`, `UserDAO`, `MatchPlayerDAO`.
6. **Dữ liệu đi qua như thế nào?**:
   * Chiều vào: Nhận dòng byte từ `socket.getInputStream()` -> `BufferedReader.readLine()` -> chuỗi JSON -> `JsonProtocol.deserialize()` -> đối tượng `Message`.
   * Chiều ra: Nhận đối tượng `Message` -> `JsonProtocol.serializeWithNewline()` -> ghi vào `PrintWriter` -> `socket.getOutputStream()`.

---

### 2.3. `ServerConnection.java`
1. **Dùng để làm gì?**: Đóng vai trò là cầu nối mạng duy nhất bên phía Client. Quản lý việc kết nối, ngắt kết nối, gửi tin nhắn đi và lắng nghe tin nhắn về.
2. **Tại sao cần?**: Tách biệt hoàn toàn phần mạng (Networking) ra khỏi phần giao diện người dùng (Swing UI), giúp code sạch sẽ và tránh bị treo giao diện (UI Freezing).
3. **Nếu xóa file này?**: Client trở thành một ứng dụng Offline đơn độc, các nút bấm trên giao diện không thể gửi dữ liệu ra ngoài.
4. **Được file nào gọi?**: `MainClient.java` khởi tạo một đối tượng duy nhất (Singleton-like) và truyền cho tất cả các Frame (`LoginFrame`, `MainMenuFrame`, `GameFrame`).
5. **Gọi đến file nào?**: `JsonProtocol`, `Message`, `Socket`, `BufferedReader`, `PrintWriter`.
6. **Dữ liệu đi qua như thế nào?**: Chạy vòng lặp ngầm `while (connected) reader.readLine()` để đón nhận gói tin từ Server, sau đó phát sự kiện tới các `MessageListener` của giao diện Swing thông qua `SwingUtilities.invokeLater`.

---

### 2.4. `JsonProtocol.java`
1. **Dùng để làm gì?**: Cung cấp các hàm tĩnh tiện ích chuyển đổi qua lại giữa Java Object và chuỗi JSON (sử dụng Google Gson), đồng thời ép chuẩn xuống dòng `\n`.
2. **Tại sao cần?**: Đảm bảo cả Client và Server dùng chung một chuẩn mã hóa và định dạng gói tin, tránh lỗi sai lệch giao thức.
3. **Nếu xóa file này?**: Cả Server và Client đều không thể đóng gói tin nhắn để gửi đi hoặc đọc tin nhắn nhận về.
4. **Được file nào gọi?**: `ClientHandler`, `ServerConnection`, `GameSession`, và tất cả các file Test.
5. **Gọi đến file nào?**: Thư viện `Gson` (`lib/gson.jar`), `Message`, `MessageType`.
6. **Dữ liệu đi qua như thế nào?**: Biến các biến dữ liệu Java (số nguyên, chuỗi, danh sách) thành chuỗi JSON nén trên 1 dòng kèm `\n` và ngược lại.

---

### 2.5. `GameSession.java`
1. **Dùng để làm gì?**: Làm trọng tài điều hành một ván đấu dò mìn đối kháng giữa 2 người chơi cụ thể.
2. **Tại sao cần?**: Đảm bảo luật chơi được thực thi công bằng ở phía Server: đếm giờ 300s, kiểm tra mìn, loang ô an toàn BFS, tính điểm thắng (+5)/thua (-3)/bỏ cuộc (-5).
3. **Nếu xóa file này?**: Trò chơi chỉ dừng lại ở sảnh chờ, không thể bước vào thi đấu đối kháng.
4. **Được file nào gọi?**: `GameManager.java` tạo ra khi có 2 người chơi chấp nhận lời thách đấu của nhau.
5. **Gọi đến file nào?**: `Board`, `Cell`, `MineGenerator`, `GameRules`, `MatchPlayerDAO`, `ClientHandler`.
6. **Dữ liệu đi qua như thế nào?**: Nhận tọa độ click `(row, col)` từ `ClientHandler` -> Kiểm tra với `Board` -> Phát gói tin `CELL_OPENED` cho người chơi và `GAME_UPDATE` cho đối thủ -> Khi ván đấu kết thúc, đẩy dữ liệu vào `MatchPlayerDAO.saveFullMatchTransaction`.
