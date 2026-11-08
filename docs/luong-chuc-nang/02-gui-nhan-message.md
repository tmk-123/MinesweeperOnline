# Gửi, nhận và phân loại message

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

Client gửi yêu cầu mở ô (3,4), trận a1b2c3d4. Ở đây chỉ theo dõi cách message đi qua TCP, chưa xét luật mở ô.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [JsonProtocol.java:176](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L176) | `public static Message createOpenCell(` | Nhận ("a1b2c3d4",3,4), dựng Message OPEN_CELL với row=3, col=4. |
| 2 | [ServerConnection.java:108](../../src/minesweeperonline/client/network/ServerConnection.java#L108) | `public synchronized void sendMessage(` | Gọi serializeWithNewline, writer.print và flush để gửi qua socket. |
| 3 | [JsonProtocol.java:36](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L36) | `public static String serializeWithNewline(` | Gọi serialize rồi nối ký tự xuống dòng để phân tách message. |
| 4 | [ClientHandler.java:82](../../src/minesweeperonline/server/ClientHandler.java#L82) | `public void run()` | readLine đọc một dòng; deserialize tạo Message Java; gọi handleMessage(message). |
| 5 | [ClientHandler.java:124](../../src/minesweeperonline/server/ClientHandler.java#L124) | `public void handleMessage(` | switch chọn OPEN_CELL và gọi handleOpenCell(message). |
| 6 | [ClientHandler.java:651](../../src/minesweeperonline/server/ClientHandler.java#L651) | `public synchronized void sendMessage(` | Khi có kết quả, server cũng ghi JSON một dòng vào socket của người nhận. |
| 7 | [ServerConnection.java:157](../../src/minesweeperonline/client/network/ServerConnection.java#L157) | `private void listenForIncomingMessages()` | Đọc phản hồi, giải mã rồi phát cho các MessageListener. |
| 8 | [GameFrame.java:191](../../src/minesweeperonline/client/ui/GameFrame.java#L191) | `private void setupNetworkListener()` | Chuyển xử lý phản hồi sang EDT bằng SwingUtilities.invokeLater. |

## Message và kết quả trong tình huống này

Client → server:

```json
{
  "type": "OPEN_CELL",
  "gameId": "a1b2c3d4",
  "payload": {
    "row": 3,
    "col": 4
  }
}
```

TCP là luồng byte, không tự chia message. readLine đọc được một gói nhờ dấu kết thúc dòng. Client không gọi trực tiếp hàm Java trên server: chỉ truyền dữ liệu. JSON sai cú pháp có thể giải mã thành null; lỗi payload trong lúc xử lý được ClientHandler bắt RuntimeException và trả ERROR.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Mã hóa message thành JSON có ký tự xuống dòng.

**Code:** [JsonProtocol.java — dòng 36](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L36). Tìm `public static String serializeWithNewline(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static String serializeWithNewline(Message message) {
    return serialize(message) + "\n";
}

/**
```

### Bước 2. Client ghi vào writer và flush.

**Code:** [ServerConnection.java — dòng 115](../../src/minesweeperonline/client/network/ServerConnection.java#L115). Tìm `String json = JsonProtocol.serializeWithNewline`.

Trích đoạn bắt đầu tại vị trí trên:

```java
    String json = JsonProtocol.serializeWithNewline(message);
    writer.print(json);
    writer.flush();
} catch (Exception e) {
    System.err.println("[ServerConnection] Error sending message: " + e.getMessage());
```

### Bước 3. Server đọc một dòng, deserialize và xử lý lỗi.

**Code:** [ClientHandler.java — dòng 94](../../src/minesweeperonline/server/ClientHandler.java#L94). Tìm `while (running && (line = reader.readLine())`.

Trích đoạn bắt đầu tại vị trí trên:

```java
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
        }
    } else {
        sendMessage(JsonProtocol.createError("", "Invalid JSON message format"));
    }
}
```

### Bước 4. Switch theo type để chuyển đến handler tương ứng.

**Code:** [ClientHandler.java — dòng 124](../../src/minesweeperonline/server/ClientHandler.java#L124). Tìm `public void handleMessage(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void handleMessage(Message message) {
    if (message == null || message.getType() == null) {
        return;
    }

    MessageType type = message.getType();

    switch (type) {
        case LOGIN:
            handleLogin(message);
            break;

        case REGISTER:
            handleRegister(message);
            break;

        case LOGOUT:
            handleLogout(message);
            break;
```

### Bước 5. Server gửi phản hồi qua socket; synchronized tuần tự hóa việc gửi trên handler.

**Code:** [ClientHandler.java — dòng 651](../../src/minesweeperonline/server/ClientHandler.java#L651). Tìm `public synchronized void sendMessage(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public synchronized void sendMessage(Message message) {
    if (writer != null && !socket.isClosed()) {
        String json = JsonProtocol.serializeWithNewline(message);
        writer.print(json);
        writer.flush();
    }
}
```

## Điểm cần nắm

Một lần gửi không tương ứng cố định một gói TCP. flush không xác nhận bên nhận đã xử lý xong.

## Trả lời khi bảo vệ

> Message là JSON kết thúc bằng xuống dòng; bên nhận dùng readLine rồi phân loại theo type.
