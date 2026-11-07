# Rời phòng và đăng xuất

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Client gửi EXIT hoặc LOGOUT.

## Luồng tổng quát

```text
EXIT → có thể FORFEIT → handleExit; LOGOUT → EXIT → unregisterUser
```

## Các bước và code

### Bước 1. Nếu trận chưa kết thúc thì xử đầu hàng trước khi rời.

**Code:** [ClientHandler.java — dòng 595](../../src/minesweeperonline/server/ClientHandler.java#L595). Tìm `private void handleExit(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleExit(Message message) {
    if (user != null) {
        GameSession session = gameManager.getSessionByUserId(user.getId());
        if (session != null) {
            // Rời phòng trước khi trận kết thúc thì tính là FORFEIT
            if (!session.isFinished()) {
                handleForfeit(message);
            }
            session.handleExit(user.getId(), gameManager);
        }
        if (player != null) {
            player.setStatus(Player.STATUS_IDLE);
        }
    }
}
```

### Bước 2. Báo đối thủ, trả trạng thái và xóa session.

**Code:** [GameSession.java — dòng 453](../../src/minesweeperonline/server/GameSession.java#L453). Tìm `public void handleExit(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void handleExit(int userId, GameManager gameManager) {
    synchronized (gameLock) {
        playAgainRequests.remove(userId);
        ClientHandler opponentHandler = getOpponentHandler(userId);

        if (opponentHandler != null) {
            Message exitMsg = new Message(MessageType.EXIT);
            JsonObject p = new JsonObject();
            p.addProperty("message", "Đối thủ đã rời phòng ra sảnh.");
            exitMsg.setPayload(p);
            opponentHandler.sendMessage(exitMsg);
        }

        player1.setStatus(Player.STATUS_IDLE);
        player2.setStatus(Player.STATUS_IDLE);
        gameManager.removeSession(this.gameId);
    }
}

/**
 * Ghi kết quả trận đấu và điểm số vào database bằng transaction.
 */
```

### Bước 3. Hủy lời mời, xử lý rời trận rồi bỏ liên kết tài khoản.

**Code:** [ClientHandler.java — dòng 615](../../src/minesweeperonline/server/ClientHandler.java#L615). Tìm `private void handleLogout(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleLogout(Message message) {
    if (user == null) return;
    cancelPendingChallenge();
    handleExit(message);

    // Giữ lock của sảnh để không thread nào thấy handler này online mà player lại null
    synchronized (clientManager) {
        clientManager.unregisterUser(user.getId());
        System.out.println("[ClientHandler] Logout: " + user.getUsername());
        user = null;
        player = null;
    }
}

```

## Điểm cần nắm

LOGOUT giữ socket để đăng nhập lại. Đóng socket đi qua luồng mất kết nối riêng.

## Trả lời khi bảo vệ

> EXIT rời trận; LOGOUT còn xóa tài khoản khỏi kết nối nhưng giữ kết nối TCP.
