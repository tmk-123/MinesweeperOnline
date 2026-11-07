# Đăng nhập

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Người dùng nhập thông tin và chọn đăng nhập.

## Luồng tổng quát

```text
LoginFrame → LOGIN → authenticate → registerUser → LOGIN_SUCCESS
```

## Các bước và code

### Bước 1. Client kiểm tra dữ liệu và gửi LOGIN.

**Code:** [LoginFrame.java — dòng 132](../../src/minesweeperonline/client/ui/LoginFrame.java#L132). Tìm `private void performLogin()`.

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

**Code:** [ClientHandler.java — dòng 237](../../src/minesweeperonline/server/ClientHandler.java#L237). Tìm `private void handleLogin(`.

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

**Code:** [ClientHandler.java — dòng 272](../../src/minesweeperonline/server/ClientHandler.java#L272). Tìm `this.user = dbUser;`.

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

private void handleRegister(Message message) {
    JsonObject payload = message.getPayload();
```

### Bước 5. Client nhận thông báo thành công hoặc thất bại.

**Code:** [LoginFrame.java — dòng 174](../../src/minesweeperonline/client/ui/LoginFrame.java#L174). Tìm `private void setupNetworkListener()`.

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
