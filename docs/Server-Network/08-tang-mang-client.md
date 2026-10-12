# 8. Tầng mạng phía Client

Phần giao diện (Swing) thuộc nhóm client. Tài liệu này chỉ đi vào **chỗ client chạm vào mạng**, tức là chỗ nối với phần server và network.

## 8.1. Khởi động: `MainClient`

[MainClient.java](../../src/minesweeperonline/client/MainClient.java)

```text
main(args)  host = args[0] ?: 127.0.0.1 ; port = args[1] ?: 2209
  └─ SwingUtilities.invokeLater:
        connection.connect(host, port)
          thành công → new LoginFrame(connection)
          thất bại   → hộp thoại "Thử lại?" (Yes → thử lại, No → thoát)
```

**Cả chương trình client chỉ có một `ServerConnection`, tức một socket.** Mọi màn hình (Login, Lobby, Game…) dùng chung đối tượng này.

## 8.2. `ServerConnection`: lớp mạng của client

[ServerConnection.java](../../src/minesweeperonline/client/network/ServerConnection.java)

| Hàm | Việc |
|---|---|
| `connect(host, port)` | `new Socket()` + `connect(..., 5000)` → tạo reader/writer UTF-8 → khởi động thread nền `Client-Network-Listener` (daemon) |
| `sendMessage(msg)` | `synchronized`: `print(json + "\n")` + `flush()` |
| `listenForIncomingMessages()` | vòng lặp `readLine()` → `deserialize` → gọi **mọi** `MessageListener` đã đăng ký |
| `disconnect(reason)` | đóng socket, gọi `ConnectionListener.onDisconnected` |
| `addMessageListener` / `removeMessageListener` | đăng ký/hủy người nghe (danh sách `CopyOnWriteArrayList`) |

### Mẫu Observer (listener)

`ServerConnection` không biết màn hình nào cần message nào. Nó **phát cho tất cả** listener, mỗi màn hình tự lọc theo `type`:

```java
lobbyListener = message -> SwingUtilities.invokeLater(() -> handleServerMessage(message));
connection.addMessageListener(lobbyListener);
...
switch (message.getType()) { case ONLINE_PLAYERS: ...; case CHALLENGE_REQUEST: ...; }
```

| Màn hình | Nghe các type |
|---|---|
| `LoginFrame` | `LOGIN_SUCCESS`, `LOGIN_FAILED` |
| `RegisterFrame` | `REGISTER_SUCCESS`, `REGISTER_FAILED` |
| `MainMenuFrame` (sảnh) | `ONLINE_PLAYERS`, `CHALLENGE_REQUEST`, `MATCH_CREATED`, `REJECT`, `LEADERBOARD`, `MATCH_HISTORY`, `ERROR` |
| `GameFrame` (trận) | `GAME_START`, `CELL_OPENED`, `FLAG_PLACED`, `FLAG_REMOVED`, `GAME_UPDATE`, `GAME_WIN/LOSE/DRAW`, `OPPONENT_DISCONNECTED`, `PLAY_AGAIN`, `EXIT` |

**Quy tắc bắt buộc:** màn hình nào đăng ký listener thì phải **gỡ listener khi `dispose()`**. Nếu không, cửa sổ đã đóng vẫn tiếp tục phản ứng với message. Ví dụ: sau khi đăng xuất rồi đăng nhập lại sẽ mở **hai** sảnh, hoặc lời mời thách đấu bật lên hai lần. Cả 4 màn hình trên đều đã gỡ listener trong `dispose()`.

### Vì sao phải `invokeLater`?

Listener được gọi trên **thread mạng**, nhưng Swing chỉ an toàn khi chạy trên **EDT**. `SwingUtilities.invokeLater` xếp việc vào hàng đợi của EDT. Nhờ vậy, thứ tự các message nhận được cũng chính là thứ tự chúng được xử lý trên giao diện.

## 8.3. Client gửi gì, ở đâu

| Hành động người dùng | File | Message |
|---|---|---|
| Bấm Đăng nhập / Đăng ký | `LoginFrame`, `RegisterFrame` | `LOGIN`, `REGISTER` |
| Bấm Đăng xuất | `MainMenuFrame` | `LOGOUT` (socket giữ nguyên, quay lại `LoginFrame`) |
| Sảnh tự làm mới mỗi 4s | `MainMenuFrame` → `OnlinePlayersPanel.refreshOnlinePlayers` | `GET_ONLINE_PLAYERS` |
| Bấm Thách đấu | `OnlinePlayersPanel` | `CHALLENGE` |
| Chấp nhận / Từ chối / hết 30s | `ChallengeDialog` | `ACCEPT` / `REJECT` |
| Click trái / phải trên ô | `GameBoardPanel` | `OPEN_CELL` / `PLACE_FLAG` / `REMOVE_FLAG` |
| Bấm Đầu hàng | `GameFrame` | `FORFEIT` |
| Chơi lại / Thoát ra sảnh | `ResultDialog` | `PLAY_AGAIN` / `EXIT` |
| Mở bảng xếp hạng / lịch sử | `LeaderboardDialog`, `MatchHistoryDialog` | `GET_LEADERBOARD`, `GET_MATCH_HISTORY` |

`GameBoardPanel` chặn sẵn vài thao tác vô nghĩa (click trái lên ô có cờ, click lên ô đã mở) để đỡ tốn băng thông. Nhưng **server vẫn kiểm tra lại**, vì không bao giờ được tin client.

## 8.4. Đồng bộ màn hình hai bên

- **Bàn chính** của mình: vẽ theo `CELL_OPENED` / `FLAG_*`. Client **không tự đoán** ô là số mấy.
- **Bàn nhỏ** của đối thủ (`OpponentMiniBoardPanel`): vẽ theo `GAME_UPDATE.openedCoords` (tô ô đã mở) và `flagRow/flagCol/flagState`.
- **Chỉ số** (ô đã mở, cờ, thao tác) của cả hai bên: lấy từ số liệu server gửi kèm.
