# Đăng nhập

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

an_demo đăng nhập bằng 123456. Giả sử database trả User có ID=101, totalScore=60.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [LoginFrame.java:134](../../src/minesweeperonline/client/ui/LoginFrame.java#L134) | `private void performLogin()` | Gọi createLogin rồi sendMessage, khóa nút lúc chờ phản hồi. |
| 2 | [ClientHandler.java:244](../../src/minesweeperonline/server/ClientHandler.java#L244) | `private void handleLogin(` | Lấy username/password, gọi userDAO.authenticate(...). |
| 3 | [UserDAO.java:36](../../src/minesweeperonline/database/UserDAO.java#L36) | `public User authenticate(` | SELECT theo tên và mật khẩu; trả User hoặc null. |
| 4 | [ClientHandler.java:281](../../src/minesweeperonline/server/ClientHandler.java#L281) | `clientManager.registerUser(dbUser.getId(), this)` | Gắn User, tạo Player và ghi 101→handler vào onlineUsers. |
| 5 | [JsonProtocol.java:98](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L98) | `public static Message createLoginSuccess(` | Dựng phản hồi có userId,username,totalScore. |
| 6 | [LoginFrame.java:178](../../src/minesweeperonline/client/ui/LoginFrame.java#L178) | `private void setupNetworkListener()` | Nhận LOGIN_SUCCESS, tạo User phía client và mở MainMenuFrame. |
| 7 | [ClientHandler.java:291](../../src/minesweeperonline/server/ClientHandler.java#L291) | `activeSession.handlePlayerReconnect(` | Sau LOGIN_SUCCESS, thử khôi phục trận dở nếu tài khoản còn session chưa kết thúc. |

## Message và kết quả trong tình huống này

Client → server:

```json
{
  "type": "LOGIN",
  "payload": {
    "username": "an_demo",
    "password": "123456"
  }
}
```

Server → client:

```json
{
  "type": "LOGIN_SUCCESS",
  "payload": {
    "userId": 101,
    "username": "an_demo",
    "totalScore": 60
  }
}
```

Sai mật khẩu hoặc tài khoản đang đăng nhập ở một handler khác còn hoạt động sẽ trả LOGIN_FAILED. Server lấy ID từ kết quả xác thực, không tin một userId do client tự khai.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Client kiểm tra dữ liệu và gửi LOGIN.

**Code:** [LoginFrame.java — dòng 134](../../src/minesweeperonline/client/ui/LoginFrame.java#L134). Tìm `private void performLogin()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void performLogin() {
    String username = txtUsername.getText().trim();
    String password = new String(txtPassword.getPassword()).trim();

    if (username.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this,
                "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!",
                "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        return;
    }

    btnLogin.setEnabled(false);
    btnLogin.setText("Đang đăng nhập...");

    Message loginMsg = JsonProtocol.createLogin(username, password);
    connection.sendMessage(loginMsg);
}

/**
```

### Bước 2. Server xác thực và kiểm tra tài khoản đang dùng trên kết nối khác.

**Code:** [ClientHandler.java — dòng 244](../../src/minesweeperonline/server/ClientHandler.java#L244). Tìm `private void handleLogin(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleLogin(Message message) {
    JsonObject payload = message.getPayload();
    String username = payload.has("username") ? payload.get("username").getAsString() : "";
    String password = payload.has("password") ? payload.get("password").getAsString() : "";

    if (username.isEmpty() || password.isEmpty()) {
        sendMessage(JsonProtocol.createLoginFailed("Username va password khong duoc de trong!"));
        return;
    }

    User dbUser = userDAO.authenticate(username, password);
    if (dbUser == null) {
        sendMessage(JsonProtocol.createLoginFailed("Sai ten dang nhap hoac mat khau!"));
        return;
    }

    ClientHandler existing = clientManager.getClientByUserId(dbUser.getId());
    if (existing != null && existing != this) {
        if (!existing.isRunning()) {
            clientManager.unregisterUser(dbUser.getId());
        } else {
            sendMessage(JsonProtocol.createLoginFailed("Tai khoan dang duoc dang nhap o mot thiet bi khac!"));
            return;
        }
    }

    // Client đăng xuất không gửi LOGOUT: cùng một kết nối đăng nhập lại,
```

### Bước 3. DAO truy vấn thông tin tài khoản, trả User hoặc null.

**Code:** [UserDAO.java — dòng 36](../../src/minesweeperonline/database/UserDAO.java#L36). Tìm `public User authenticate(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public User authenticate(String username, String password) {
    String sql = "SELECT id, username, password, totalScore, createdAt FROM `User` WHERE username = ? AND password = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, username);
        ps.setString(2, password);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null; // Sai username hoặc password trong MySQL
    } catch (SQLException e) {
        // MySQL lỗi/không hoạt động -> fallback sang bộ nhớ trong
        System.err.println("[UserDAO] MySQL unavailable (" + e.getMessage() + "), using fallback in-memory store.");
        User u = fallbackUsers.get(username);
        if (u != null && u.getPassword().equals(password)) {
            return u;
        }
        return null;
    }
}
```

### Bước 4. Gắn user/player vào handler, đăng ký online và thử khôi phục trận đang dở.

**Code:** [ClientHandler.java — dòng 279](../../src/minesweeperonline/server/ClientHandler.java#L279). Tìm `this.user = dbUser;`.

Trích đoạn bắt đầu tại vị trí trên:

```java
    this.user = dbUser;
    this.player = new Player(dbUser.getId(), dbUser.getUsername(), dbUser.getTotalScore());
    clientManager.registerUser(dbUser.getId(), this);

    Message res = JsonProtocol.createLoginSuccess(user.getId(), user.getUsername(), user.getTotalScore());
    res.setRequestId(message.getRequestId());
    sendMessage(res);
    System.out.println("[ClientHandler] Login successful: " + username + " (id=" + user.getId() + ")");

    // Kiểm tra xem người dùng có trận đấu đang dở dang (bị rớt mạng) cần khôi phục không
    GameSession activeSession = gameManager.getSessionByUserId(dbUser.getId());
    if (activeSession != null && !activeSession.isFinished()) {
        activeSession.handlePlayerReconnect(dbUser.getId(), this, this.player);
    }
}

/** Kiểm tra thông tin và tên trùng, tạo tài khoản qua UserDAO rồi trả kết quả đăng ký cho client. */
private void handleRegister(Message message) {
```

### Bước 5. Client nhận thông báo thành công hoặc thất bại.

**Code:** [LoginFrame.java — dòng 178](../../src/minesweeperonline/client/ui/LoginFrame.java#L178). Tìm `private void setupNetworkListener()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void setupNetworkListener() {
    loginListener = message -> {
        SwingUtilities.invokeLater(() -> {
            if (message.getType() == MessageType.LOGIN_SUCCESS) {
                int userId = message.getPayload().get("userId").getAsInt();
                String username = message.getPayload().get("username").getAsString();
                int totalScore = message.getPayload().get("totalScore").getAsInt();

                User loggedUser = new User(userId, username, totalScore);

                // Mở MainMenuFrame và đóng LoginFrame
                MainMenuFrame mainMenu = new MainMenuFrame(connection, loggedUser);
                mainMenu.setVisible(true);
                this.dispose();

            } else if (message.getType() == MessageType.LOGIN_FAILED) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                String reason = message.getPayload().has("reason") ?
                        message.getPayload().get("reason").getAsString() : "Đăng nhập thất bại!";
                JOptionPane.showMessageDialog(this, reason, "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
```

## Điểm cần nắm

Các yêu cầu chơi lấy danh tính từ user đã gắn với handler. DAO cũng có nhánh fallback khi SQL lỗi.

## Trả lời khi bảo vệ

> Đăng nhập thành công thì server gắn tài khoản vào kết nối và đăng ký trạng thái online.
