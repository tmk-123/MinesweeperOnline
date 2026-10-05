# Chương 02: Phân Tích Chi Tiết Server

Chương này đi sâu vào toàn bộ mã nguồn của phía Server (package `minesweeperonline.server`), giải thích cặn kẽ từng dòng code quan trọng của từng class.

---

## 1. Class `MainServer.java` - Điểm Bắt Đầu Của Máy Chủ

* **Đường dẫn file**: `src/minesweeperonline/server/MainServer.java`
* **Vai trò**: Là điểm khởi đầu (Entry Point) của toàn bộ hệ thống máy chủ, quản lý vòng đời của cổng kết nối `ServerSocket`.

### 1.1. Chương trình bắt đầu chạy từ đâu?
Chương trình Server bắt đầu từ hàm `main`:

```java
public static void main(String[] args) {
    int port = ServerConfig.DEFAULT_PORT; // Lấy cổng mặc định 2209
    if (args.length > 0) {
        try {
            port = Integer.parseInt(args[0]); // Cho phép tùy chỉnh cổng qua tham số
        } catch (NumberFormatException e) {
            System.out.println("Invalid port parameter, using default: " + port);
        }
    }
    MainServer server = new MainServer(port);
    server.start(); // Kích hoạt máy chủ
}
```

### 1.2. Server mở port ở đâu?
Hành động mở port diễn ra tại phương thức `start()`:

```java
serverSocket = new ServerSocket(port);
```
* **`ServerSocket` là gì?**: Là class đặc biệt của Java dùng riêng cho máy chủ. Nhiệm vụ của nó là đăng ký với hệ điều hành (Windows) để "chiếm giữ" một số cổng (port).
* **`port` (cổng mạng) là gì?**: Cổng mạng là một con số định danh từ 1 đến 65535 trên máy tính. Nếu IP là địa chỉ của tòa nhà, thì port giống như số phòng. Port `2209` trong dự án này giúp hệ điều hành biết: "Bất kỳ gói tin mạng nào gõ cửa phòng 2209, hãy chuyển nó cho chương trình Minesweeper Server".

### 1.3. Server đón nhận Client như thế nào?
Sau khi mở port, Server bước vào vòng lặp chờ khách:

```java
while (running) {
    try {
        Socket clientSocket = serverSocket.accept(); // (1) Dừng luồng chờ kết nối
        System.out.println("[MainServer] Accepted new connection from: " +
                clientSocket.getRemoteSocketAddress());

        ClientHandler handler = new ClientHandler(clientSocket, clientManager, gameManager); // (2) Tạo người phục vụ
        Thread clientThread = new Thread(handler, "Client-" + clientSocket.getPort());       // (3) Cấp luồng riêng
        clientThread.start();                                                                // (4) Chạy ngầm

    } catch (IOException e) { ... }
}
```
* **Dòng (1) `serverSocket.accept()`**: Đây là một **Blocking Call** (hàm chặn luồng). Server sẽ đứng yên tại dòng này, không tiêu tốn CPU, kiên nhẫn đợi cho đến khi có một Client gọi lệnh kết nối.
* Ngay khi Client kết nối, `accept()` trả về một đối tượng `Socket clientSocket` đại diện cho đường truyền với Client đó.
* **Dòng (2), (3), (4)**: `MainServer` không trực tiếp làm việc với client này mà bọc nó vào `ClientHandler`, sau đó khởi chạy một `Thread` độc lập để xử lý song song. Nhờ vậy, hàng trăm người chơi có thể kết nối cùng lúc mà không ai phải xếp hàng chờ đợi!

---

## 2. Class `ClientHandler.java` - Người Phục Vụ Riêng Cho Từng Client

* **Đường dẫn file**: `src/minesweeperonline/server/ClientHandler.java`
* **Vai trò**: Đại diện cho 1 kết nối mạng của 1 người chơi, chịu trách nhiệm đọc dữ liệu đến, xử lý yêu cầu và trả về kết quả.

### 2.1. Server thiết lập luồng đọc/ghi mạng ở đâu?
Trong phương thức `run()` của `ClientHandler`:

```java
reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
```
* `socket.getInputStream()`: Nhận dòng byte thô chạy từ dây mạng vào.
* `InputStreamReader(..., UTF_8)`: Chuyển byte thành ký tự chữ có dấu (tiếng Việt).
* `BufferedReader`: Gom các ký tự thành bộ đệm để đọc được nguyên một dòng văn bản bằng hàm `readLine()`.
* `PrintWriter(..., true)`: Hỗ trợ ghi chuỗi văn bản ra mạng, tham số `true` bật chế độ **Auto-Flush** (tự động đẩy dữ liệu đi ngay lập tức không bị nghẽn trong RAM).

### 2.2. Vòng lặp đọc dữ liệu từ Client
```java
String line;
while (running && (line = reader.readLine()) != null) {
    line = line.trim();
    if (line.isEmpty()) continue;

    Message message = JsonProtocol.deserialize(line);
    if (message != null) {
        handleMessage(message); // Phân loại và xử lý request
    }
}
```
* Hàm `reader.readLine()` sẽ chặn (block) cho đến khi Client gửi đủ một chuỗi kết thúc bằng ký tự xuống dòng `\n`.
* `JsonProtocol.deserialize(line)`: Biến chuỗi JSON thành đối tượng Java `Message`.

### 2.3. Xử lý request ở đâu?
Tại phương thức `handleMessage(Message message)`, Server dùng `switch-case` để phân loại:

```java
switch (type) {
    case LOGIN:              handleLogin(message); break;
    case REGISTER:           handleRegister(message); break;
    case GET_ONLINE_PLAYERS: handleGetOnlinePlayers(message); break;
    case CHALLENGE:          handleChallenge(message); break;
    case ACCEPT:             handleAccept(message); break;
    case REJECT:             handleReject(message); break;
    case OPEN_CELL:          handleOpenCell(message); break;
    case PLACE_FLAG:         handlePlaceFlag(message); break;
    case REMOVE_FLAG:        handleRemoveFlag(message); break;
    case GET_LEADERBOARD:    handleGetLeaderboard(message); break;
    case GET_MATCH_HISTORY:  handleGetMatchHistory(message); break;
    ...
}
```

### 2.4. Server gửi Response về Client như thế nào?
Để đảm bảo an toàn đa luồng (tránh trường hợp 2 thread cùng ghi đè lên nhau vào 1 socket), phương thức gửi dữ liệu được khóa đồng bộ:

```java
public synchronized void sendMessage(Message message) {
    if (writer != null && !socket.isClosed()) {
        String json = JsonProtocol.serializeWithNewline(message);
        writer.print(json); // Đã kèm '\n' ở cuối
        writer.flush();     // Tống dữ liệu ra card mạng ngay lập tức
    }
}
```

### 2.5. Server đóng kết nối ở đâu?
Khi người chơi tắt ứng dụng hoặc mất mạng, `reader.readLine()` sẽ trả về `null` hoặc ném `IOException`. Khi đó, khối `finally` sẽ gọi hàm `cleanup()`:

```java
private void cleanup() {
    running = false;
    clientManager.removeClient(this); // Xóa khỏi danh sách online

    // Nếu đang trong trận mà thoát, xử thua người thoát và trao chiến thắng cho đối thủ!
    if (user != null) {
        GameSession session = gameManager.getSessionByUserId(user.getId());
        if (session != null && !session.isFinished()) {
            session.finishGame(opponent.getUserId(), "FORFEIT_DISCONNECT");
        }
    }

    if (socket != null && !socket.isClosed()) {
        socket.close(); // Đóng socket giải phóng cổng
    }
}
```

---

## 3. Class `ClientManager.java` - Quản Lý Danh Sách Online

* **Đường dẫn file**: `src/minesweeperonline/server/ClientManager.java`
* **Vai trò**: Sổ danh bạ lưu trữ tất cả các client đang kết nối và người dùng đã đăng nhập.
* **Cấu trúc dữ liệu sử dụng**:
  * `List<ClientHandler> allClients = new CopyOnWriteArrayList<>()`: Lưu toàn bộ socket đang nối vào server.
  * `ConcurrentHashMap<Integer, ClientHandler> onlineUsers = new ConcurrentHashMap<>()`: Ánh xạ từ `userId` sang `ClientHandler` tương ứng để gửi tin nhắn đích danh cho 1 người cụ thể.
* **Các phương thức quan trọng**:
  * `registerUser(int userId, ClientHandler client)`: Ghi nhận user đã đăng nhập.
  * `isUserOnline(int userId)`: Kiểm tra tài khoản có đang đăng nhập ở máy khác không (chống đăng nhập trùng lặp).
  * `broadcast(Message message)`: Gửi một thông báo tới toàn bộ người chơi trên server.

---

## 4. Class `GameManager.java` & `GameSession.java` - Quản Lý Bàn Cờ Đối Kháng

* **`GameManager.java`**: Quản lý tập hợp tất cả các ván đấu đang diễn ra. Cho phép tìm ván đấu dựa trên mã `gameId` hoặc `userId`.
* **`GameSession.java`**: Vòng đời của một trận đấu giữa 2 người chơi:
  1. **Sinh bàn cờ**: Khởi tạo 2 bàn cờ 12x12 độc lập với 23 mìn ngẫu nhiên bằng `MineGenerator`.
  2. **Giai đoạn READY (5 giây)**: Chạy một `ScheduledExecutorService` đếm ngược 5 giây chuẩn bị trước khi phát lệnh `GAME_START`.
  3. **Giai đoạn PLAYING (300 giây)**: Khi nhận tọa độ mở ô từ `ClientHandler`, gọi thuật toán loang BFS của `Board.java`. Nếu mở trúng mìn -> Lập tức phân xử thua.
  4. **Giai đoạn FINISHED**: Tính toán điểm số (+5, -3, -5, +1) và gọi `MatchPlayerDAO.saveFullMatchTransaction` để lưu kết quả vào MySQL bằng 1 Transaction an toàn.
