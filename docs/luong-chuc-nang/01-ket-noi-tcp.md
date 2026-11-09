# Khởi động và kết nối TCP

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

Khởi động server tại 127.0.0.1:2209, sau đó chạy một client trên cùng máy.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [MainServer.java:90](../../src/minesweeperonline/server/MainServer.java#L90) | `public static void main(` | Đọc cổng, gọi new MainServer(2209).start(). |
| 2 | [MainServer.java:29](../../src/minesweeperonline/server/MainServer.java#L29) | `public void start()` | new ServerSocket(port) mở cổng; accept() chờ kết nối TCP. |
| 3 | [MainClient.java:27](../../src/minesweeperonline/client/MainClient.java#L27) | `public void launch()` | Khởi tạo giao diện rồi gọi connectAndOpenLogin(). |
| 4 | [MainClient.java:41](../../src/minesweeperonline/client/MainClient.java#L41) | `private void connectAndOpenLogin()` | Gọi connection.connect("127.0.0.1",2209). |
| 5 | [ServerConnection.java:67](../../src/minesweeperonline/client/network/ServerConnection.java#L67) | `public synchronized boolean connect(` | Tạo Socket, kết nối với hạn chờ 5 giây, tạo reader/writer UTF-8 và chạy luồng nhận. |
| 6 | [MainServer.java:44](../../src/minesweeperonline/server/MainServer.java#L44) | `Socket clientSocket = serverSocket.accept()` | Trả Socket mới, tạo ClientHandler và chạy trên thread riêng. |
| 7 | [ClientHandler.java:82](../../src/minesweeperonline/server/ClientHandler.java#L82) | `public void run()` | Tạo reader/writer, addClient(this) rồi chờ reader.readLine(). |

## Message và kết quả trong tình huống này

Bước này **chưa có JSON nghiệp vụ**. Kết nối TCP thành công rồi client mới mở LoginFrame. Kết nối chưa đưa tài khoản vào danh sách online: phải LOGIN thành công. Nếu server chưa chạy hoặc sai địa chỉ, client báo lỗi và cho phép thử lại.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Server tạo ServerSocket và chờ client.

**Code:** [MainServer.java — dòng 29](../../src/minesweeperonline/server/MainServer.java#L29). Tìm `public void start()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void start() {
    System.out.println("==================================================");
    System.out.println("      MINESWEEPER ONLINE - TCP SERVER             ");
    System.out.println("==================================================");

    try {
        serverSocket = new ServerSocket(port);
        System.out.println("[MainServer] Server started successfully on port: " + port);
        System.out.println("[MainServer] Waiting for client connections...");

        // Hook để tắt server an toàn khi JVM dừng
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[MainServer] Accepted new connection from: " +
                        clientSocket.getRemoteSocketAddress());
```

### Bước 2. Mỗi socket mới được giao cho một ClientHandler chạy trên thread riêng.

**Code:** [MainServer.java — dòng 44](../../src/minesweeperonline/server/MainServer.java#L44). Tìm `Socket clientSocket = serverSocket.accept()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
Socket clientSocket = serverSocket.accept();
System.out.println("[MainServer] Accepted new connection from: " +
        clientSocket.getRemoteSocketAddress());

ClientHandler handler = new ClientHandler(clientSocket, clientManager, gameManager, matchmakingManager);
Thread clientThread = new Thread(handler, "Client-" + clientSocket.getPort());
clientThread.start();
```

### Bước 3. Client tạo kết nối tới địa chỉ và cổng server.

**Code:** [ServerConnection.java — dòng 73](../../src/minesweeperonline/client/network/ServerConnection.java#L73). Tìm `new Socket(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
socket = new Socket();
// Kết nối với timeout 5 giây
socket.connect(new InetSocketAddress(host, port), 5000);

reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

connected = true;

// Khởi động thread nền nhận dữ liệu (readLine() chặn nên không chạy trên thread giao diện)
```

### Bước 4. Server tạo reader/writer UTF-8 và đọc dữ liệu theo dòng.

**Code:** [ClientHandler.java — dòng 82](../../src/minesweeperonline/server/ClientHandler.java#L82). Tìm `public void run()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void run() {
    try {
        this.reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        clientManager.addClient(this);

        System.out.println("[ClientHandler] Connected: " + socket.getRemoteSocketAddress());

        String line;
        while (running && (line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            Message message = JsonProtocol.deserialize(line);
            if (message != null) {
                // Payload sai định dạng (vd: thiếu "row") không được làm chết kết nối
                try {
                    handleMessage(message);
                } catch (RuntimeException e) {
                    System.err.println("[ClientHandler] Error handling " + message.getType() + ": " + e);
                    sendMessage(JsonProtocol.createError(message.getRequestId(), "Yeu cau khong hop le!"));
```

### Bước 5. Client dùng thread nền nhận phản hồi để giao diện không bị chặn.

**Code:** [ServerConnection.java — dòng 83](../../src/minesweeperonline/client/network/ServerConnection.java#L83). Tìm `listenerThread = new Thread`.

Trích đoạn bắt đầu tại vị trí trên:

```java
listenerThread = new Thread(this::listenForIncomingMessages, "Client-Network-Listener");
// Daemon: đóng cửa sổ cuối cùng là chương trình thoát, không phải chờ thread này
listenerThread.setDaemon(true);
listenerThread.start();
```

## Điểm cần nắm

TCP truyền luồng byte. Mỗi kết nối có handler riêng; ranh giới message do ứng dụng quy định.

## Trả lời khi bảo vệ

> Server lắng nghe bằng ServerSocket; mỗi client có socket và thread phục vụ riêng.
