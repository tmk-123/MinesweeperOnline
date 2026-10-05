# Chương 03: Phân Tích Chi Tiết Client

Chương này giải thích cách hoạt động của ứng dụng máy khách (Client), từ lúc khởi động, thiết lập kết nối Socket tới Server, cho đến cách gửi lệnh và nhận phản hồi bất đồng bộ.

---

## 1. Điểm Khởi Chạy Của Client: `MainClient.java`

* **Đường dẫn file**: `src/minesweeperonline/client/MainClient.java`
* **Vai trò**: Là điểm xuất phát (Entry Point) của ứng dụng giao diện phía người chơi.

```java
public static void main(String[] args) {
    MainClient app = new MainClient();
    app.launch();
}
```

Khi phương thức `launch()` được gọi, nó ủy thác việc mở giao diện cho luồng sự kiện của Swing (`SwingUtilities.invokeLater`) để tránh hiện tượng treo giao diện người dùng (UI Freezing):

```java
private void connectAndOpenLogin() {
    boolean connected = connection.connect(ServerConfig.DEFAULT_HOST, ServerConfig.DEFAULT_PORT);
    if (connected) {
        LoginFrame loginFrame = new LoginFrame(connection);
        loginFrame.setVisible(true);
    } else {
        // Hiển thị hộp thoại báo lỗi nếu Server chưa bật!
        int option = JOptionPane.showConfirmDialog(null,
                "Không thể kết nối đến Máy chủ (127.0.0.1:" + ServerConfig.DEFAULT_PORT + ")!\n" +
                "Vui lòng đảm bảo MainServer đã được khởi động.\n\nBạn có muốn thử kết nối lại không?",
                "Lỗi kết nối Server", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        ...
    }
}
```

---

## 2. Client Biết Địa Chỉ Server Bằng Cách Nào?

Client muốn gọi điện cho Server thì phải biết: **Số điện thoại (IP)** và **Số máy lẻ (Port)**.

```text
[Client] --------------------------------------------> [Server]
               IP:   127.0.0.1 (Localhost)
               Port: 2209
```

* **Địa chỉ IP (`host`)**: Được khai báo trong [ServerConfig.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/ServerConfig.java):
  ```java
  public static final String DEFAULT_HOST = "127.0.0.1";
  ```
  `127.0.0.1` là địa chỉ vòng lặp cục bộ (Loopback Address hay `localhost`). Nó thông báo cho hệ điều hành rằng Server đang chạy ngay trên chính chiếc máy tính này. Nếu chơi qua mạng LAN, người dùng chỉ cần đổi IP này thành IP của máy chủ (ví dụ `192.168.1.15`).
* **Cổng mạng (`port`)**: Được khai báo là `2209`.

---

## 3. Tạo Socket Và Kết Nối: `ServerConnection.java`

* **Đường dẫn file**: `src/minesweeperonline/client/network/ServerConnection.java`
* **Vai trò**: Đây là trái tim mạng của Client. Toàn bộ việc tạo Socket, kết nối, gửi dữ liệu đi và lắng nghe dữ liệu về đều nằm tập trung tại đây.

### 3.1. Client tạo Socket ở đâu?
Hành động tạo Socket diễn ra trong phương thức `connect(String host, int port)`:

```java
socket = new Socket();
// Kết nối với thời gian chờ tối đa 5 giây (Connection Timeout)
socket.connect(new InetSocketAddress(host, port), 5000);

reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
```
* `socket.connect(..., 5000)`: Client sẽ cố gắng gõ cửa Server trong vòng 5 giây. Nếu sau 5 giây mà Server không phản hồi (chưa bật Server), nó sẽ ném ra lỗi `SocketTimeoutException` hoặc `ConnectException` thay vì bị treo vô tận.

### 3.2. Luồng Lắng Nghe Bất Đồng Bộ (Background Listener Thread)
Sau khi kết nối thành công, Client **bắt buộc phải tạo một Thread riêng** để ngồi đợi tin nhắn từ Server:

```java
// Bắt đầu luồng nhận tin nhắn chạy ngầm
listenerThread = new Thread(this::listenForIncomingMessages, "Client-Network-Listener");
listenerThread.setDaemon(true); // Thread phụ thuộc, tự tắt khi ứng dụng tắt
listenerThread.start();
```

Tại sao phải dùng Thread riêng?
* Trong ứng dụng đồ họa Java Swing, có một luồng chính gọi là **EDT (Event Dispatch Thread)** chuyên vẽ các nút bấm, ô cờ.
* Nếu để luồng chính này gọi `reader.readLine()`, giao diện sẽ **bị đơ hoàn toàn (Not Responding)** vì phải chờ mạng.
* Do đó, `ServerConnection` dùng một Worker Thread riêng biệt để đọc dữ liệu:

```java
private void listenForIncomingMessages() {
    try {
        String line;
        // Liên tục đón nhận từng dòng JSON do Server bắn về
        while (connected && (line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Message message = JsonProtocol.deserialize(line);
            if (message != null) {
                notifyMessageReceived(message); // Báo tin cho giao diện UI
            }
        }
    } catch (IOException e) { ... }
}
```

---

## 4. Client Gửi Request Lên Server Như Thế Nào?

Bất cứ khi nào người chơi thao tác trên giao diện (bấm nút Đăng nhập, gửi thách đấu, mở ô cờ), Client UI chỉ cần gọi hàm:

```java
public synchronized void sendMessage(Message message) {
    if (!connected || writer == null) {
        System.err.println("[ServerConnection] Cannot send message: Not connected to server!");
        return;
    }

    try {
        String json = JsonProtocol.serializeWithNewline(message);
        writer.print(json); // In chuỗi JSON kèm '\n' vào ống mạng
        writer.flush();     // Ép gửi đi ngay
    } catch (Exception e) {
        disconnect("Error sending data");
    }
}
```

Ví dụ khi người chơi click chuột trái mở ô hàng 3 cột 5 trên bàn cờ:
```java
// Trong GameBoardPanel.java
connection.sendMessage(JsonProtocol.createOpenCell(gameId, 3, 5));
```

---

## 5. Client Đóng Socket Như Thế Nào?

Khi người dùng nhấn nút Thoát hoặc tắt cửa sổ:

```java
public synchronized void disconnect(String reason) {
    connected = false;
    try {
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Đóng socket giải phóng tài nguyên
        }
    } catch (IOException e) { ... }

    notifyDisconnected(reason); // Báo cho UI biết đã ngắt mạng
}
```
Lệnh `socket.close()` sẽ tự động đóng cả `InputStream` và `OutputStream`, báo cho Server biết Client này đã rời cuộc chơi.
