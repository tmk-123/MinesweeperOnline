# Đăng ký tài khoản

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

Người dùng nhập an_demo, mật khẩu 123456 và xác nhận 123456. Giả sử tên này chưa tồn tại.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [RegisterFrame.java:161](../../src/minesweeperonline/client/ui/RegisterFrame.java#L161) | `private void performRegister()` | Đọc dữ liệu; chặn rỗng hoặc mật khẩu xác nhận không khớp. Gọi createRegister rồi sendMessage. |
| 2 | [JsonProtocol.java:60](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L60) | `public static Message createRegister(` | Payload chỉ có username/password; không gửi mật khẩu xác nhận. |
| 3 | [ClientHandler.java:296](../../src/minesweeperonline/server/ClientHandler.java#L296) | `private void handleRegister(` | Đọc payload, kiểm tra rỗng và gọi userDAO.existsByUsername("an_demo"). |
| 4 | [UserDAO.java:81](../../src/minesweeperonline/database/UserDAO.java#L81) | `public boolean register(` | INSERT User bằng PreparedStatement, tổng điểm ban đầu bằng 0. |
| 5 | [JsonProtocol.java:70](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L70) | `public static Message createRegisterSuccess(` | Tạo REGISTER_SUCCESS với payload.message; handler gửi lại client. |
| 6 | [RegisterFrame.java:198](../../src/minesweeperonline/client/ui/RegisterFrame.java#L198) | `private void setupNetworkListener()` | Nhận phản hồi trên EDT, thành công thì điền sẵn thông tin vào màn hình đăng nhập. |

## Message và kết quả trong tình huống này

Client → server:

```json
{
  "type": "REGISTER",
  "payload": {
    "username": "an_demo",
    "password": "123456"
  }
}
```

Server → client:

```json
{
  "type": "REGISTER_SUCCESS",
  "payload": {
    "message": "Dang ky tai khoan thanh cong!"
  }
}
```

Tên trùng trả REGISTER_FAILED với payload.reason="Ten dang nhap da ton tai!". Đăng ký thành công chưa phải đăng nhập. Nếu MySQL lỗi, DAO có nhánh lưu dự phòng trong RAM, dữ liệu không bền qua lần tắt server.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Client kiểm tra dữ liệu và gửi REGISTER.

**Code:** [RegisterFrame.java — dòng 161](../../src/minesweeperonline/client/ui/RegisterFrame.java#L161). Tìm `private void performRegister()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void performRegister() {
    String username = txtUsername.getText().trim();
    String password = new String(txtPassword.getPassword()).trim();
    String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();

    if (username.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this,
                "Vui lòng nhập tên đăng nhập và mật khẩu!",
                "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        return;
    }

    if (!password.equals(confirmPassword)) {
        JOptionPane.showMessageDialog(this,
                "Mật khẩu xác nhận không khớp!",
                "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        return;
    }

    this.lastRegisteredUsername = username;
    this.lastRegisteredPassword = password;

    btnRegister.setEnabled(false);
    btnRegister.setText("Đang đăng ký...");
```

### Bước 2. Server kiểm tra dữ liệu rỗng và trùng tên rồi gọi DAO.

**Code:** [ClientHandler.java — dòng 296](../../src/minesweeperonline/server/ClientHandler.java#L296). Tìm `private void handleRegister(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleRegister(Message message) {
    JsonObject payload = message.getPayload();
    String username = payload.has("username") ? payload.get("username").getAsString() : "";
    String password = payload.has("password") ? payload.get("password").getAsString() : "";

    if (username.isEmpty() || password.isEmpty()) {
        sendMessage(JsonProtocol.createRegisterFailed("Username va password khong duoc de trong!"));
        return;
    }

    if (userDAO.existsByUsername(username)) {
        sendMessage(JsonProtocol.createRegisterFailed("Ten dang nhap da ton tai!"));
        return;
    }

    boolean registered = userDAO.register(username, password);
    if (registered) {
        Message res = JsonProtocol.createRegisterSuccess("Dang ky tai khoan thanh cong!");
        res.setRequestId(message.getRequestId());
        sendMessage(res);
        System.out.println("[ClientHandler] User registered: " + username);
    } else {
        sendMessage(JsonProtocol.createRegisterFailed("Khong the tao tai khoan, vui long thu lai!"));
    }
}
```

### Bước 3. DAO thêm tài khoản bằng PreparedStatement.

**Code:** [UserDAO.java — dòng 81](../../src/minesweeperonline/database/UserDAO.java#L81). Tìm `public boolean register(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public boolean register(String username, String password) {
    if (existsByUsername(username)) {
        return false;
    }

    String sql = "INSERT INTO `User` (username, password, totalScore) VALUES (?, ?, 0)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        ps.setString(1, username);
        ps.setString(2, password);

        int affected = ps.executeUpdate();
        if (affected > 0) {
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    fallbackUsers.put(username, new User(id, username, password, 0, ""));
                }
            }
            return true;
        }
        return false;
    } catch (SQLException e) {
        System.err.println("[UserDAO] MySQL unavailable (" + e.getMessage() + "), registering into fallback store.");
        int id = fallbackIdGen.incrementAndGet();
        fallbackUsers.put(username, new User(id, username, password, 0, ""));
        return true;
    }
}
```

### Bước 4. Client nhận kết quả đăng ký và hiển thị thông báo.

**Code:** [RegisterFrame.java — dòng 198](../../src/minesweeperonline/client/ui/RegisterFrame.java#L198). Tìm `private void setupNetworkListener()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void setupNetworkListener() {
    registerListener = message -> {
        SwingUtilities.invokeLater(() -> {
            if (message.getType() == MessageType.REGISTER_SUCCESS) {
                JOptionPane.showMessageDialog(this,
                        "Đăng ký tài khoản thành công! Bạn có thể đăng nhập ngay.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
                if (loginFrame != null) {
                    loginFrame.setCredentials(lastRegisteredUsername, lastRegisteredPassword);
                    loginFrame.setVisible(true);
                }

            } else if (message.getType() == MessageType.REGISTER_FAILED) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký tài khoản");

                String reason = message.getPayload().has("reason") ?
                        message.getPayload().get("reason").getAsString() : "Đăng ký thất bại!";
                JOptionPane.showMessageDialog(this, reason, "Lỗi đăng ký", JOptionPane.ERROR_MESSAGE);
            }
        });
```

## Điểm cần nắm

DAO có fallback trong bộ nhớ khi SQLException. Dữ liệu đó không bền như database.

## Trả lời khi bảo vệ

> Server kiểm tra tên tài khoản rồi gọi DAO tạo tài khoản và trả kết quả.
