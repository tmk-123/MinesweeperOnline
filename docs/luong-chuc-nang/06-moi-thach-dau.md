# Mời thách đấu, chấp nhận, từ chối và hết hạn

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Người chơi chọn đối thủ để gửi lời mời.

## Luồng tổng quát

```text
CHALLENGE → CHALLENGE_REQUEST → ACCEPT/REJECT → MATCH_CREATED hoặc hủy
```

## Các bước và code

### Bước 1. Server kiểm tra người mời, đối thủ và trạng thái.

**Code:** [ClientHandler.java — dòng 395](../../src/minesweeperonline/server/ClientHandler.java#L395). Tìm `private void handleChallenge(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleChallenge(Message message) {
    if (user == null || player == null) {
        sendMessage(JsonProtocol.createError(message.getRequestId(), "Ban chua dang nhap!"));
        return;
    }

    JsonObject payload = message.getPayload();
    int targetUserId = payload.has("targetUserId") ? payload.get("targetUserId").getAsInt() : -1;

    if (targetUserId == user.getId()) {
        sendMessage(JsonProtocol.createError(message.getRequestId(), "Khong the tu thach dau chinh minh!"));
        return;
    }

    ClientHandler targetHandler;
    // Kiểm tra rồi cập nhật trạng thái của hai người chơi phải là atomic: hai người
    // cùng lúc thách đấu một người thì không được cả hai cùng thành công.
    synchronized (clientManager) {
        if (!Player.STATUS_IDLE.equals(player.getStatus())) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Ban dang ban, khong the thach dau!"));
            return;
        }

        targetHandler = clientManager.getClientByUserId(targetUserId);
```

### Bước 2. Lập lịch hết hạn và gửi lời mời tới đối thủ.

**Code:** [ClientHandler.java — dòng 438](../../src/minesweeperonline/server/ClientHandler.java#L438). Tìm `long token = pendingChallengeToken;`.

Trích đoạn bắt đầu tại vị trí trên:

```java
    long token = pendingChallengeToken;
    CHALLENGE_TIMER.schedule(() -> expireChallenge(token),
            ServerConfig.CHALLENGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    // Chuyển tiếp CHALLENGE_REQUEST tới người được mời
    Message challengeReq = JsonProtocol.createChallengeRequest(user.getId(), user.getUsername());
    targetHandler.sendMessage(challengeReq);
}

/**
```

### Bước 3. Client hiển thị lời mời.

**Code:** [MainMenuFrame.java — dòng 292](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L292). Tìm `private void handleChallengeRequest(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleChallengeRequest(Message message) {
    JsonObject payload = message.getPayload();
    int fromUserId = payload.get("fromUserId").getAsInt();
    String fromUsername = payload.get("fromUsername").getAsString();

    ChallengeDialog dialog = new ChallengeDialog(this, connection, fromUserId, fromUsername);
    dialog.setVisible(true);
}

private GameFrame currentGameFrame;
// Trạng thái và kích thước cửa sổ trận đấu (mặc định mở đầu phóng to toàn màn hình)
private int lastGameExtendedState = Frame.MAXIMIZED_BOTH;
```

### Bước 4. Xác minh lời mời còn hợp lệ rồi tạo trận.

**Code:** [ClientHandler.java — dòng 476](../../src/minesweeperonline/server/ClientHandler.java#L476). Tìm `private void handleAccept(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleAccept(Message message) {
    if (user == null || player == null) return;

    JsonObject payload = message.getPayload();
    int challengerUserId = payload.has("challengerUserId") ? payload.get("challengerUserId").getAsInt() : -1;

    ClientHandler challengerHandler;
    GameSession session;
    synchronized (clientManager) {
        // Chỉ chấp nhận lời mời thực sự đang chờ user này
        if (challengerUserId < 0 || pendingChallengerId != challengerUserId) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Loi moi thach dau khong con hieu luc!"));
            return;
        }
        pendingChallengerId = -1;

        challengerHandler = clientManager.getClientByUserId(challengerUserId);
        if (challengerHandler == null || challengerHandler.getPlayer() == null
                || challengerHandler.pendingTargetId != user.getId()) {
            sendMessage(JsonProtocol.createError(message.getRequestId(), "Nguoi thach dau da roi mang!"));
            player.setStatus(Player.STATUS_IDLE);
            return;
        }
        challengerHandler.pendingTargetId = -1;

        // Tạo GameSession mới (chuyển cả hai người chơi sang PLAYING)
        session = gameManager.createSession(
                challengerHandler.getPlayer(), challengerHandler,
                this.player, this
        );
    }

    // Báo cho cả hai người chơi: MATCH_CREATED
    Message matchCreatedP1 = new Message(MessageType.MATCH_CREATED);
    JsonObject p1Data = new JsonObject();
```

### Bước 5. Từ chối và dọn trạng thái lời mời.

**Code:** [ClientHandler.java — dòng 526](../../src/minesweeperonline/server/ClientHandler.java#L526). Tìm `private void handleReject(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleReject(Message message) {
    if (user == null || player == null) return;
    JsonObject payload = message.getPayload();
    int challengerUserId = payload.has("challengerUserId") ? payload.get("challengerUserId").getAsInt() : -1;

    ClientHandler challengerHandler;
    synchronized (clientManager) {
        // Bỏ qua REJECT cũ (lời mời đã được chấp nhận/hủy)
        if (challengerUserId < 0 || pendingChallengerId != challengerUserId) {
            return;
        }
        pendingChallengerId = -1;
        player.setStatus(Player.STATUS_IDLE);

        challengerHandler = clientManager.getClientByUserId(challengerUserId);
        if (challengerHandler != null && challengerHandler.pendingTargetId == user.getId()) {
            challengerHandler.pendingTargetId = -1;
            challengerHandler.getPlayer().setStatus(Player.STATUS_IDLE);
        } else {
            challengerHandler = null;
```

### Bước 6. Timer hủy lời mời vẫn ứng với token đang chờ.

**Code:** [ClientHandler.java — dòng 451](../../src/minesweeperonline/server/ClientHandler.java#L451). Tìm `private void expireChallenge(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void expireChallenge(long token) {
    Message rejMsg;
    synchronized (clientManager) {
        if (pendingTargetId < 0 || pendingChallengeToken != token) {
            return; // đã được chấp nhận / từ chối / hủy
        }
        ClientHandler target = clientManager.getClientByUserId(pendingTargetId);
        if (target != null && target.pendingChallengerId == user.getId()) {
            target.pendingChallengerId = -1;
            target.getPlayer().setStatus(Player.STATUS_IDLE);
        }
        rejMsg = new Message(MessageType.REJECT);
        JsonObject p = new JsonObject();
        p.addProperty("rejectedByUserId", pendingTargetId);
        p.addProperty("rejectedByUsername", target != null ? target.getUser().getUsername() : "Nguoi choi");
        rejMsg.setPayload(p);

        pendingTargetId = -1;
        player.setStatus(Player.STATUS_IDLE);
        System.out.println("[ClientHandler] Challenge expired after " +
```

## Điểm cần nắm

Token tránh timer của lời mời cũ hủy nhầm lời mời mới. Dữ liệu pending được bảo vệ qua khóa clientManager.

## Trả lời khi bảo vệ

> Server quản lý lời mời; đồng ý đúng lời mời thì tạo trận, từ chối hoặc hết hạn thì hủy.
