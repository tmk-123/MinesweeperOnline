# Chương 04: Bản Chất Network Trong Code Thực Tế

Chương này bóc tách từng khái niệm cốt lõi của Lập trình Mạng và chỉ rõ dòng code tương ứng trong project **Minesweeper Online**.

---

## 1. Bản Đồ Các Khái Niệm Mạng Trong Project

```text
+---------------------------------------------------------------------------------------+
|                                    MÔI TRƯỜNG MẠNG                                    |
|                                                                                       |
|  Địa chỉ IP: 127.0.0.1 (ServerConfig.DEFAULT_HOST)                                    |
|  Cổng dịch vụ: 2209     (ServerConfig.DEFAULT_PORT)                                    |
|  Giao thức: TCP (Tin cậy, hướng kết nối, đảm bảo thứ tự byte)                         |
+---------------------------------------------------------------------------------------+
                                            |
                                            v
+---------------------------------------------------------------------------------------+
|                                    ĐƯỜNG ỐNG SOCKET                                   |
|                                                                                       |
|   CLIENT (Socket)                                             SERVER (ClientHandler)  |
|          |                                                               |            |
|          +--- OutputStream ----> [Dữ liệu gửi đi (JSON)] ---> InputStream+            |
|          |    (PrintWriter)                                  (BufferedReader)         |
|          |                                                               |            |
|          +<-- InputStream <----- [Dữ liệu trả về (JSON)] <--- OutputStream+           |
|               (BufferedReader)                               (PrintWriter)            |
+---------------------------------------------------------------------------------------+
```

---

## 2. Chi Tiết Từng Khái Niệm Kèm Đoạn Code Thực Tế

### 2.1. IP Address (Địa chỉ IP)
* **Bản chất**: Là mã số định danh của một máy tính trong mạng IP (tương tự như số nhà).
* **Trong code**: `ServerConfig.DEFAULT_HOST = "127.0.0.1"`.
  * `127.0.0.1`: Địa chỉ IP cục bộ trỏ về chính máy mình. Khi kiểm thử trên cùng 1 máy tính, cả Client và Server đều dùng IP này.

### 2.2. Port (Cổng kết nối)
* **Bản chất**: Mỗi ứng dụng mạng trên một máy tính phải có một Port riêng để hệ điều hành biết gói tin nào thuộc về ứng dụng nào.
* **Trong code**: `ServerConfig.DEFAULT_PORT = 2209`.
  * Tại sao chọn `2209`? Các cổng từ 0 đến 1023 là cổng hệ thống dành riêng (Well-known ports như 80 cho HTTP, 443 cho HTTPS, 22 cho SSH). Cổng `2209` nằm trong dải cổng tự do (>1024), tránh bị xung đột với các phần mềm khác.
  * Cổng `3306` (`ServerConfig.DB_PORT`): Cổng mặc định của hệ quản trị cơ sở dữ liệu MySQL.

### 2.3. Socket & ServerSocket
* **`ServerSocket`**: Chỉ dùng ở phía **Server** ([MainServer.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/MainServer.java#L31)):
  ```java
  serverSocket = new ServerSocket(port);
  ```
  Nhiệm vụ duy nhất: Lắng nghe ở cổng 2209 và chấp nhận kết nối từ Client.
* **`Socket`**: Đại diện cho một đường dây liên lạc cụ thể giữa 2 tiến trình.
  * Phía Client ([ServerConnection.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/network/ServerConnection.java#L48)):
    ```java
    socket = new Socket();
    socket.connect(new InetSocketAddress(host, port), 5000);
    ```
  * Phía Server ([MainServer.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/MainServer.java#L40)):
    ```java
    Socket clientSocket = serverSocket.accept();
    ```

### 2.4. Timeout (Thời gian chờ tối đa)
* **Bản chất**: Nếu Server không hoạt động hoặc mạng bị treo, Client không thể ngồi chờ mãi mãi.
* **Trong code**:
  ```java
  socket.connect(new InetSocketAddress(host, port), 5000);
  ```
  Con số `5000` đại diện cho **5000 mili-giây (5 giây)**. Nếu quá 5 giây mà không bắt tay thành công với Server, Java sẽ ném ra lỗi để hiển thị hộp thoại cảnh báo người dùng.

### 2.5. InputStream & OutputStream
* **Bản chất**: Mọi dữ liệu đi qua mạng TCP về bản chất là một **dòng byte liên tục (Stream of bytes)**.
  * `InputStream`: Chiều dữ liệu đổ VÀO chương trình (nhận dữ liệu).
  * `OutputStream`: Chiều dữ liệu đẩy RA khỏi chương trình (gửi dữ liệu).
* Vì `InputStream` và `OutputStream` mặc định chỉ đọc/ghi từng byte số (`byte[]`), rất khó để gửi chuỗi văn bản JSON tiếng Việt. Vì vậy, project bọc chúng qua các lớp chuyển đổi:
  * `InputStreamReader` và `OutputStreamWriter` chỉ định bảng mã `StandardCharsets.UTF_8`.

### 2.6. BufferedReader & PrintWriter
* **`BufferedReader`**:
  ```java
  reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
  ```
  Cung cấp hàm `readLine()` cực kỳ mạnh mẽ: nó tự động gom các byte nhận được từ mạng cho đến khi gặp ký tự xuống dòng `\n` thì trả về nguyên 1 chuỗi `String`.
* **`PrintWriter`**:
  ```java
  writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
  ```
  * Cho phép in chuỗi String ra mạng giống hệt như lệnh `System.out.println()`.
  * Tham số `true` (Auto-Flush): Sau khi in xong, nó tự động xả sạch bộ nhớ đệm (buffer), tống chuỗi ký tự đi ngay trên đường dây cáp mạng mà không cần lập trình viên phải gọi thêm lệnh `flush()` thủ công.

### 2.7. Tại Sao Dự Án Không Dùng UDP (DatagramSocket / DatagramPacket)?
* UDP gửi các gói tin độc lập (DatagramPacket), không có kết nối cố định, không đảm bảo gói tin có tới nơi hay không.
* Trong Minesweeper Online, nếu người chơi mở trúng ô mìn hoặc hết giờ thi đấu, gói tin kết quả **bắt buộc phải tới nơi 100%**. Nếu dùng UDP mà gói tin bị thất lạc trên mạng Wi-Fi chập chờn, trạng thái bàn cờ 2 bên sẽ bị lệch vĩnh viễn. Do đó, TCP là lựa chọn chuẩn xác duy nhất!
