# Rời phòng và đăng xuất

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

Sau khi trận kết thúc, A bấm về sảnh ở hộp thoại kết quả. Sau đó A bấm Đăng xuất.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [ResultDialog.java:135](../../src/minesweeperonline/client/ui/ResultDialog.java#L135) | `JsonProtocol.createExit(` | Gửi EXIT có mã trận; callback client đóng cửa sổ trận và hiện sảnh. |
| 2 | [ClientHandler.java:614](../../src/minesweeperonline/server/ClientHandler.java#L614) | `private void handleExit(` | Tìm session; nếu trận chưa kết thúc thì gọi handleForfeit trước, sau đó session.handleExit. |
| 3 | [GameSession.java:457](../../src/minesweeperonline/server/GameSession.java#L457) | `public void handleExit(` | Bỏ yêu cầu chơi lại của A, báo B đối thủ rời phòng, đặt hai Player IDLE và xóa session. |
| 4 | [MainMenuFrame.java:130](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L130) | `connection.sendMessage(JsonProtocol.createLogout())` | Khi đăng xuất, gửi LOGOUT, dừng timer sảnh và quay lại LoginFrame. |
| 5 | [ClientHandler.java:634](../../src/minesweeperonline/server/ClientHandler.java#L634) | `private void handleLogout(` | Hủy lời mời, handleExit để xử lý phiên còn lại; unregisterUser(101), đặt user/player=null. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "EXIT",
  "gameId": "a1b2c3d4",
  "payload": {}
}
```

Server → B:

```json
{
  "type": "EXIT",
  "payload": {
    "message": "Đối thủ đã rời phòng ra sảnh."
  }
}
```

A → server khi đăng xuất:

```json
{
  "type": "LOGOUT",
  "payload": {}
}
```

EXIT là rời phòng, không phải đăng xuất. LOGOUT bỏ tài khoản khỏi online nhưng **giữ socket** để đăng nhập lại. Không có LOGOUT_SUCCESS riêng trong protocol hiện tại. Rời trận chưa xong bị xử đầu hàng; rời phòng đã kết thúc không trừ điểm thêm.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Nếu trận chưa kết thúc thì xử đầu hàng trước khi rời.

**Code:** [ClientHandler.java — dòng 614](../../src/minesweeperonline/server/ClientHandler.java#L614). Tìm `private void handleExit(`.

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

**Code:** [GameSession.java — dòng 457](../../src/minesweeperonline/server/GameSession.java#L457). Tìm `public void handleExit(`.

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

**Code:** [ClientHandler.java — dòng 634](../../src/minesweeperonline/server/ClientHandler.java#L634). Tìm `private void handleLogout(`.

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
