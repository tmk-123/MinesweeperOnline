# Gửi, nhận và phân loại message

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Một thao tác cần trao đổi qua mạng.

## Luồng tổng quát

```text
UI → JsonProtocol → ServerConnection → TCP → ClientHandler → handler chức năng
```

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

**Code:** [ClientHandler.java — dòng 91](../../src/minesweeperonline/server/ClientHandler.java#L91). Tìm `while (running && (line = reader.readLine())`.

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

**Code:** [ClientHandler.java — dòng 121](../../src/minesweeperonline/server/ClientHandler.java#L121). Tìm `public void handleMessage(`.

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

**Code:** [ClientHandler.java — dòng 632](../../src/minesweeperonline/server/ClientHandler.java#L632). Tìm `public synchronized void sendMessage(`.

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
