# 2. Kiến thức mạng nền tảng (gắn với code)

Mỗi mục bên dưới gồm hai phần: **khái niệm** và **nó nằm ở đâu trong code**.

## 2.1. IP và Port

- **IP** xác định *máy*. `127.0.0.1` (localhost) nghĩa là chính máy đang chạy.
- **Port** xác định *chương trình* trên máy đó. Server lắng nghe ở **2209**, MySQL ở **3306**.
- Trong code: `ServerConfig.DEFAULT_HOST = "127.0.0.1"`, `ServerConfig.DEFAULT_PORT = 2209`.
- Server có thể đổi port qua tham số: `MainServer 3000`. Client nhận `host port` qua tham số: `MainClient 192.168.1.10 2209`. Đây là cách chơi giữa **hai máy khác nhau** trong cùng mạng LAN.

## 2.2. TCP hay UDP? Dự án dùng TCP

| | TCP | UDP |
|---|---|---|
| Kết nối | Có (bắt tay 3 bước) | Không |
| Thứ tự, không mất dữ liệu | Có, khi kết nối còn sống | Không đảm bảo |
| Ranh giới gói tin | **Không có**: chỉ là dòng byte | Có (mỗi datagram là một gói) |

Game dò mìn cần mọi thông báo (mở ô, thắng/thua) **đến đủ và đúng thứ tự**, nên dùng TCP: `java.net.ServerSocket` ở server và `java.net.Socket` ở client.

> Lưu ý: TCP đảm bảo thứ tự và không mất dữ liệu **khi kết nối còn sống**. Khi mạng đứt, phía kia nhận lỗi hoặc EOF. Cách code xử lý chuyện này nằm ở [07](07-xu-ly-loi-va-ngat-ket-noi.md).

## 2.3. ServerSocket và Socket

```java
// Server – MainServer.start()
serverSocket = new ServerSocket(port);        // chiếm port 2209, bắt đầu lắng nghe
Socket clientSocket = serverSocket.accept();  // CHẶN tới khi có client kết nối

// Client – ServerConnection.connect()
socket = new Socket();
socket.connect(new InetSocketAddress(host, port), 5000);  // timeout kết nối 5 giây
```

- `ServerSocket` chỉ dùng để **đón** kết nối. Mỗi lần `accept()` thành công, nó trả về một `Socket` **mới** dành riêng cho client đó.
- Sau khi kết nối, hai bên **ngang hàng**: mỗi bên có một `InputStream` (nhận) và một `OutputStream` (gửi).

## 2.4. Stream → Reader/Writer → dòng văn bản

Socket chỉ cho ta byte. Code bọc nhiều lớp để làm việc với chuỗi:

```java
reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
```

| Lớp | Việc |
|---|---|
| `InputStream` / `OutputStream` | byte thô |
| `InputStreamReader` / `OutputStreamWriter` + `UTF_8` | byte ↔ ký tự (giữ đúng tiếng Việt). **Hai bên phải cùng bảng mã.** |
| `BufferedReader.readLine()` | gom ký tự tới khi gặp `\n`, trả về một dòng |
| `PrintWriter.print()` + `flush()` | ghi chuỗi, rồi đẩy khỏi bộ đệm |

> Tham số `true` của `PrintWriter` (auto-flush) **chỉ có tác dụng với `println/printf/format`**. Code dùng `print(json)`, nên phải gọi `flush()` tường minh. Code đã làm đúng điều này trong cả hai hàm `sendMessage`.

## 2.5. Framing: vì sao mỗi message phải kết thúc bằng `\n`

TCP không giữ ranh giới message. Nếu gửi liền hai message:

```text
{"type":"A"}{"type":"B"}
```

bên nhận có thể đọc ra `{"type":"A"}{"ty` rồi mới tới `pe":"B"}`. Cách giải là **quy ước ranh giới**. Dự án dùng **newline-delimited JSON**:

- Bên gửi: `JsonProtocol.serializeWithNewline(msg)` = JSON trên **một dòng** + `"\n"`. Gson mặc định không xuống dòng bên trong JSON, nên mỗi message chiếm đúng một dòng.
- Bên nhận: `reader.readLine()` trả về **đúng một message**.

Các cách framing khác (để trả lời nếu bị hỏi): gửi trước độ dài (`DataOutputStream.writeInt(len)` + nội dung), hoặc dùng `ObjectOutputStream` (Java serialization).

## 2.6. Blocking I/O và Thread-per-client

`accept()` và `readLine()` đều **chặn** (blocking): thread đứng chờ cho tới khi có dữ liệu. Vì vậy:

- `MainServer` có một thread chỉ để `accept()`. Mỗi client mới được giao cho `new Thread(new ClientHandler(...))`.
- Mỗi `ClientHandler` chặn ở `readLine()` của **riêng** client đó, nên client này im lặng không làm ảnh hưởng client khác.
- Phía client cũng có một thread nền (`Client-Network-Listener`) chặn ở `readLine()`, để giao diện Swing không bị đơ.

Ưu điểm của mô hình này là dễ hiểu và đủ dùng cho vài chục người chơi. Nhược điểm là mỗi kết nối tốn một thread. Hướng mở rộng: dùng thread pool (`ExecutorService`) hoặc NIO (`Selector`), xem [10 – Ôn tập](10-on-tap-van-dap.md).

## 2.7. Gửi từ nhiều thread cùng lúc → phải đồng bộ

Một socket có thể bị **nhiều thread cùng ghi**. Ví dụ thread của A gửi `CHALLENGE_REQUEST` cho B, đúng lúc thread của B gửi phản hồi cho chính B, và thread đồng hồ của trận cũng gửi `GAME_WIN`. Nếu không khóa, hai chuỗi JSON có thể xen vào nhau và làm hỏng framing. Vì vậy:

```java
public synchronized void sendMessage(Message message) { ... print + flush ... }
```

Chỉ một thread được ghi vào một socket tại một thời điểm. Client cũng làm tương tự trong `ServerConnection.sendMessage`.

## 2.8. Phát hiện ngắt kết nối

| Tình huống | `readLine()` |
|---|---|
| Bên kia đóng socket bình thường (FIN) | trả về `null` → thoát vòng lặp |
| Mất mạng đột ngột / tắt tiến trình (RST) | ném `IOException` (`SocketException: Connection reset`) |
| Rút dây mạng mà không có gói nào đi qua (half-open) | **chờ mãi**: dự án chưa có heartbeat hay `setSoTimeout` (xem giới hạn ở [10](10-on-tap-van-dap.md)) |

Cả hai trường hợp đầu đều dẫn vào khối `finally { cleanup(); }` của `ClientHandler.run()`.
