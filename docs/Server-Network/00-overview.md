# Chương 00: Tổng Quan Về Server, Client & Lập Trình Mạng

Tài liệu này giải thích những khái niệm căn bản nhất của môn Lập trình Mạng dành cho người mới bắt đầu, gắn liền với cách áp dụng thực tế trong trò chơi **Minesweeper Online**.

---

## 1. Những Khái Niệm Cơ Bản

### 1.1. Server (Máy chủ) là gì?
* **Khái niệm đời thường**: Hãy tưởng tượng Server giống như **người trọng tài** hoặc **chủ quán cờ**. Quán mở cửa sẵn ở một địa chỉ cố định, ngồi đợi các đấu thủ đến chơi, ghi nhận bảng điểm, xáo cờ và phân xử thắng thua công minh.
* **Trong project này**: Server là chương trình [MainServer.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/MainServer.java). Nó chạy liên tục, mở sẵn một cổng mạng (`port 2209`), nắm giữ bản đồ mìn bí mật mà không người chơi nào được biết trước.

### 1.2. Client (Máy khách) là gì?
* **Khái niệm đời thường**: Client giống như **người chơi cờ**. Người chơi bước vào quán, xin đăng ký tên, nhìn quanh xem ai đang rảnh để mời thi đấu, khi vào bàn thì bấm mở ô cờ trên màn hình của mình.
* **Trong project này**: Client là ứng dụng giao diện đồ họa [MainClient.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/MainClient.java). Khi người dùng nhấn chuột mở ô cờ hoặc cắm cờ, Client sẽ gói yêu cầu đó lại và gửi tới Server.

### 1.3. Network (Mạng máy tính) là gì?
* Network là cầu nối vật lý hoặc không dây (dây mạng LAN, Wi-Fi, Internet) giúp các máy tính có thể truyền các chuỗi tín hiệu số (dãy byte 0 và 1) qua lại với nhau.

### 1.4. Socket là gì?
* **Khái niệm**: Socket là "ổ cắm điện" hoặc "đầu dây cáp mạng ảo" mà hệ điều hành cung cấp cho lập trình viên.
* Muốn gửi dữ liệu từ chương trình Java sang một máy tính khác:
  1. Bạn cắm một đầu dây (`Socket` ở Client).
  2. Server cắm một ổ cắm chờ sẵn (`ServerSocket` ở Server).
  3. Khi nối vào nhau thành công, ta có một đường ống hai chiều thông suốt: dữ liệu đẩy vào đầu này sẽ chảy ra đầu kia.

```text
[Ứng dụng Client]                             [Ứng dụng Server]
        |                                             |
   (Socket Client)   ====================>    (Socket tại Server)
        |           Đường truyền TCP ổn định          |
   [OutputStream]   ====================>       [InputStream]
   (Đẩy dữ liệu đi)                           (Hút dữ liệu vào)
```

---

## 2. So Sánh TCP và UDP: Dự Án Này Dùng Gì?

Trong lập trình mạng, hai giao thức truyền tải tầng vận chuyển phổ biến nhất là **TCP** và **UDP**.

| Tiêu chí | TCP (Transmission Control Protocol) | UDP (User Datagram Protocol) |
| :--- | :--- | :--- |
| **Bản chất** | Hướng kết nối (Connection-oriented) | Phi kết nối (Connectionless) |
| **Độ tin cậy** | **100% tin cậy**. Đảm bảo dữ liệu không bị mất gói, không đảo lộn thứ tự. | Có thể bị mất gói dữ liệu trên đường truyền mà không báo lại. |
| **Kiểm tra lỗi** | Tự động truyền lại gói tin nếu bị lỗi/mất. | Không tự động truyền lại. |
| **Ứng dụng** | Game chiến thuật, web, ngân hàng, chat. | Video call trực tiếp, game bắn súng FPS tốc độ cao. |

### Tại sao Minesweeper Online chọn TCP?
Game Dò mìn là game đối kháng theo lượt/thời gian thực mang tính logic tuyệt đối:
* Nếu người chơi mở phải ô mìn, gói tin thông báo kết quả **bắt buộc phải đến đích chính xác**.
* Nếu dùng UDP và bị rơi mất gói tin thông báo thua cuộc, ván đấu sẽ bị sai lệch trạng thái vĩnh viễn.
* Do đó, project sử dụng **Java TCP Socket** thuần túy: lớp `java.net.ServerSocket` cho máy chủ và `java.net.Socket` cho máy khách.

---

## 3. Dự Án Sử Dụng Mô Hình Nào?

Project này xây dựng theo mô hình **Client – Server Authoritative** (Server làm trọng tài tối cao):

```text
               +----------------------------------+
               |        SERVER TRỌNG TÀI          |
               | - Nắm vị trí 23 quả mìn ngẫu nhiên|
               | - Tính toán ô an toàn (BFS)      |
               | - Đồng hồ đếm ngược 300s & 5s     |
               | - Ghi nhận điểm số vào Database  |
               +-----------------+----------------+
                                 ^
         Gửi hành động mở ô/cờ    |    Đồng bộ tiến độ bàn cờ
         (TCP Socket JSON)       |    (TCP Socket JSON)
                                 v
        +------------------------+------------------------+
        |                                                 |
        v                                                 v
+-----------------------+                         +-----------------------+
|   CLIENT 1 (Alice)    |                         |    CLIENT 2 (Bob)     |
| - Nhìn thấy bàn cờ 12x12|                       | - Nhìn thấy bàn cờ 12x12|
| - Click chuột mở ô    |                         | - Click chuột mở ô    |
| - Bàn cờ nhỏ xem Bob  |                         | - Bàn cờ nhỏ xem Alice|
+-----------------------+                         +-----------------------+
```

### Điểm đặc biệt của Server-Authoritative:
1. **Chống gian lận (Anti-cheat)**: Client **hoàn toàn không biết vị trí 23 quả mìn**. Khi bắt đầu trận đấu, Server không hề gửi mìn xuống máy Client. Chỉ khi người chơi click vào một ô, Client gửi tọa độ lên Server, Server kiểm tra ô đó an toàn hay có mìn rồi mới phản hồi kết quả về!
2. **Minh bạch**: Cả hai người chơi đều bình đẳng, máy chủ làm trọng tài độc lập kiểm soát thời gian đếm ngược.

---

## 4. Server Nằm Ở Đâu? Client Nằm Ở Đâu?

* **Server nằm ở package**: `minesweeperonline.server`
  * Khởi động từ file [MainServer.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/MainServer.java).
* **Client nằm ở package**: `minesweeperonline.client`
  * Khởi động từ file [MainClient.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/MainClient.java).
* **Thành phần dùng chung nằm ở package**: `minesweeperonline.common`
  * Định nghĩa gói tin [Message.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/Message.java) và các giao thức [JsonProtocol.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/JsonProtocol.java).

---

## 5. Hai Bên Giao Tiếp Với Nhau Như Thế Nào?

Hai bên trao đổi thông điệp qua luồng tuần tự như sau:

```text
    CLIENT                                       SERVER
      |                                            |
      | -------- 1. Yêu cầu kết nối TCP ---------> |
      | <------- 2. Server chấp nhận (Accept) -----|
      |                                            |
      | -------- 3. Gửi Request (JSON + '\n') ----> |
      |                                            | (Server đọc chuỗi,
      |                                            |  giải mã JSON,
      |                                            |  thực thi logic)
      | <------- 4. Trả Response (JSON + '\n') --- |
      |                                            |
```

Tất cả các thông điệp trao đổi đều là chuỗi văn bản JSON chuẩn, mỗi thông điệp được ngắt dòng bằng ký tự `\n` để hai bên không bao giờ bị dính hoặc sót gói tin.
Chi tiết về kiến trúc các file và cấu trúc các gói tin này sẽ được trình bày ở các chương kế tiếp.
