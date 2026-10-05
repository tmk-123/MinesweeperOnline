# Chương 01: Kiến Trúc Hệ Thống (Architecture)

Chương này mô tả sơ đồ kiến trúc tổng thể của dự án **Minesweeper Online**, chỉ rõ từng tầng (layer), từng module và cách các đối tượng Java phối hợp với nhau qua mạng TCP Socket.

---

## 1. Sơ Đồ Kiến Trúc Tổng Thể

Hệ thống được chia làm 4 khối rõ rệt:

```text
========================================================================================
                                CLIENT (ỨNG DỤNG MÁY KHÁCH)
  [Giao diện Swing UI]
    - LoginFrame / RegisterFrame (Đăng nhập / Đăng ký)
    - MainMenuFrame (Sảnh chờ online, Bảng xếp hạng, Lịch sử đấu)
    - GameFrame (Bàn cờ chính 12x12 + Bàn cờ nhỏ đối thủ)
            ^
            | (gọi hàm / lắng nghe Event)
            v
  [Tầng mạng Client: ServerConnection.java]
    - Quản lý Socket kết nối tới Server (127.0.0.1:2209)
    - Chạy Thread riêng biệt đọc luồng byte từ Socket: BufferedReader.readLine()
    - Gửi thông điệp: PrintWriter.print(json + "\n")
========================================================================================
                                     ||
                                     ||  TCP Socket (Cổng 2209)
                                     ||  JSON (Newline-delimited '\n')
                                     \/
========================================================================================
                                SERVER (ỨNG DỤNG MÁY CHỦ)
  [Cổng đón khách: MainServer.java]
    - ServerSocket(2209) chạy vòng lặp vô tận: serverSocket.accept()
    - Mỗi khi có 1 Client kết nối, tạo 1 luồng ClientHandler độc lập
            |
            +---> [ClientManager.java]: Quản lý danh sách kết nối & User đang Online
            |
            +---> [GameManager.java]: Quản lý các phòng đấu GameSession
            |
            +---> [ClientHandler.java]: Người phục vụ riêng cho từng Client
                    - Đọc request JSON từ Client
                    - Chuyển tiếp hành động vào GameSession
                    - Gửi phản hồi JSON về Client
            |
            +---> [GameSession.java]: Trọng tài cho từng cặp đấu (P1 vs P2)
                    - 2 Bàn cờ Board 12x12 độc lập (MineGenerator sinh 23 mìn)
                    - Thuật toán BFS loang ô an toàn 0
                    - Đồng hồ đếm ngược 5s chuẩn bị và 300s ván đấu
                    - Trọng tài phân xử Thắng/Thua/Hòa/Bỏ cuộc
========================================================================================
                                     ||
                                     ||  JDBC Connection (MySQL)
                                     ||  (Tự động chuyển Fallback RAM nếu mất MySQL)
                                     \/
========================================================================================
                                CƠ SỞ DỮ LIỆU (DATABASE)
  [Tầng DAO: Data Access Objects]
    - DatabaseConnection.java: Cấp phát kết nối JDBC
    - UserDAO.java: Xác thực tài khoản, tính điểm, Bảng xếp hạng
    - MatchDAO.java & MatchPlayerDAO.java: Lưu kết quả trận đấu trong 1 TRANSACTION
  [Bảng MySQL]: `User`, `Match`, `MatchPlayer`
========================================================================================
```

---

## 2. Giải Thích Chi Tiết Từng Khối Kiến Trúc

### 2.1. Phía Client (Máy khách)
1. **Tầng Giao Diện (UI Layer - Package `client.ui`)**:
   * Xây dựng hoàn toàn bằng **Java Swing**.
   * Chỉ quan tâm đến việc nhận tương tác từ người dùng (nhấp chuột, gõ phím) và hiển thị dữ liệu lên màn hình.
   * **Không bao giờ tự xử lý logic game**: Khi bấm vào ô ( hàng 2, cột 3 ), Client UI không tự đoán xem ô đó là mìn hay số mấy, mà chuyển lệnh xuống `ServerConnection`.
2. **Tầng Mạng Client (Network Layer - `ServerConnection.java`)**:
   * Đóng vai trò là "người phát ngôn" của Client.
   * Chứa một tiến trình ngầm (Daemon Thread) chuyên ngồi trực chờ dữ liệu gửi về từ Server qua `BufferedReader`. Khi có gói tin mới, nó phát tín hiệu (trigger listener) để cập nhật giao diện Swing một cách mượt mà.

### 2.2. Phía Server (Máy chủ)
Mô hình Server được tổ chức theo chuẩn **Thread-per-Client Pattern** (Mỗi khách hàng một luồng riêng):
1. **`MainServer` (Người gác cổng)**:
   * Mở cổng mạng `port 2209`.
   * Luôn trong tư thế chờ: `Socket clientSocket = serverSocket.accept();`.
   * Ngay khi có khách cắm cáp vào, `MainServer` không ngồi nói chuyện với khách đó mà giao khách cho một nhân viên mới (`ClientHandler`) phục vụ trong luồng riêng (`new Thread(handler).start()`). `MainServer` lại quay về chờ vị khách tiếp theo.
2. **`ClientHandler` (Người phục vụ riêng)**:
   * Chạy trên luồng riêng của vị khách đó.
   * Lắng nghe từng gói tin JSON mà vị khách gửi lên, phân loại lệnh qua lệnh `switch (message.getType())` (đăng nhập, thách đấu, mở ô, cắm cờ...).
3. **`ClientManager` (Sổ theo dõi khách)**:
   * Sử dụng cấu trúc dữ liệu đa luồng an toàn (`ConcurrentHashMap`, `CopyOnWriteArrayList`).
   * Theo dõi xem ai đang online, ai đang bận chơi, ai vừa đăng xuất để cập nhật bảng danh sách người chơi online.
4. **`GameManager` & `GameSession` (Ban trọng tài trận đấu)**:
   * Khi 2 người chơi chấp nhận thách đấu, `GameManager` tạo một thực thể `GameSession`.
   * `GameSession` kiểm soát vòng đời của trận đấu:
     * Giai đoạn `READY` (đếm ngược 5 giây).
     * Giai đoạn `PLAYING` (tối đa 300 giây - 5 phút).
     * Giai đoạn `FINISHED` (phân xử điểm số, lưu kết quả).

### 2.3. Tầng Dùng Chung (Common Layer)
* Nằm tại package `minesweeperonline.common`.
* Cả Server và Client đều import package này để cùng hiểu chung:
  * Khung gói tin `Message`: Cả 2 bên đều đóng gói và mở gói dữ liệu theo cùng một form chuẩn.
  * Danh mục `MessageType`: Bảng từ điển các loại lệnh (LOGIN, OPEN_CELL, GAME_WIN,...).
  * Thực thể dữ liệu `User`, `Player`, `LeaderboardEntry`, `MatchHistoryEntry`.

### 2.4. Tầng Cơ Sở Dữ Liệu (Database Layer)
* Dự án hỗ trợ kết nối trực tiếp đến **MySQL Server** qua JDBC (`lib/mysql-connector-j.jar`).
* Toàn bộ thao tác ghi điểm trận đấu được bảo vệ bằng **ACID Transaction** (Một giao dịch duy nhất trong `MatchPlayerDAO.java`): Lưu thông tin trận -> Lưu thống kê Player 1 -> Lưu thống kê Player 2 -> Cập nhật điểm 2 người. Nếu bất kỳ bước nào lỗi, toàn bộ giao dịch sẽ tự động `rollback` lại!
* **Cơ chế Fallback thông minh (In-Memory Fallback)**: Nếu người dùng chưa bật MySQL hoặc sai mật khẩu root, ứng dụng tự động kích hoạt bộ nhớ đệm RAM để lưu tạm user, lịch sử đấu và bảng xếp hạng mà không bị văng crash chương trình.
