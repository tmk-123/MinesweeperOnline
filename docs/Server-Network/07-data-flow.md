# Chương 07: Luồng Dữ Liệu Chi Tiết (Data Flow)

Chương này mô tả từng bước đi của dữ liệu qua các class và hàm thực tế trong các kịch bản hoạt động chính của hệ thống.

---

## 1. Kịch Bản 1: Luồng Đăng Nhập (Login Flow)

Khi người chơi điền tên đăng nhập và mật khẩu tại `LoginFrame`:

```text
[LoginFrame]                 [ServerConnection]             [ClientHandler]             [UserDAO]
     |                              |                              |                        |
     | 1. Bấm nút "Đăng nhập"       |                              |                        |
     |----------------------------->|                              |                        |
     |                              | 2. Gửi LOGIN (JSON + '\n')   |                        |
     |                              |----------------------------->|                        |
     |                              |                              | 3. userDAO.authenticate|
     |                              |                              |----------------------->|
     |                              |                              |<-----------------------|
     |                              |                              |    (User hợp lệ)       |
     |                              |                              | 4. Đăng ký ClientManager
     |                              | 5. Trả về LOGIN_SUCCESS      |                        |
     |                              |<-----------------------------|                        |
     | 6. Kích hoạt Listener        |                              |                        |
     |<-----------------------------|                              |                        |
     | 7. Mở MainMenuFrame          |                              |                        |
```

### Chi tiết các bước trong code:
1. **Bước 1**: `LoginFrame.java` gọi `connection.sendMessage(JsonProtocol.createLogin(username, password))`.
2. **Bước 2**: `ServerConnection.java` đẩy chuỗi JSON qua Socket `writer.print(json)`.
3. **Bước 3**: `ClientHandler.java` nhận chuỗi qua `reader.readLine()`, gọi hàm `handleLogin(message)`.
4. **Bước 4**: `ClientHandler` gọi `userDAO.authenticate(username, password)`.
5. **Bước 5**: `ClientHandler` đăng ký người dùng với `clientManager.registerUser(userId, this)` và gửi lại gói tin `LOGIN_SUCCESS`.
6. **Bước 6 & 7**: `ServerConnection` nhận `LOGIN_SUCCESS`, kích hoạt `LoginFrame` đóng lại và khởi tạo mở sảnh chính `MainMenuFrame`.

---

## 2. Kịch Bản 2: Thách Đấu & Khởi Tạo Trận Đấu (Challenge Flow)

Alice (Client 1) chọn Bob (Client 2) trên danh sách và bấm "Thách đấu":

```text
[Alice Client]               [Server (ClientHandler 1 & 2)]            [Bob Client]
      |                                    |                                 |
      | 1. Gửi CHALLENGE                   |                                 |
      |----------------------------------->|                                 |
      |                                    | 2. Gửi CHALLENGE_REQUEST (30s)  |
      |                                    |-------------------------------->|
      |                                    |                                 | (Bob thấy ChallengeDialog)
      |                                    | 3. Gửi ACCEPT                   |
      |                                    |<--------------------------------|
      |                                    |                                 |
      |                                    | 4. GameManager.createSession()  |
      |                                    |    (Sinh 23 mìn ngẫu nhiên)     |
      | 5. Nhận MATCH_CREATED              | 5. Nhận MATCH_CREATED           |
      |<-----------------------------------|-------------------------------->|
      | (Mở GameFrame: Đếm ngược 5s READY) | (Mở GameFrame: Đếm ngược 5s)    |
      |                                    |                                 |
      | 6. Nhận GAME_START                 | 6. Nhận GAME_START              |
      |<-----------------------------------|-------------------------------->|
      | (Bắt đầu bấm mở ô - PLAYING)       | (Bắt đầu bấm mở ô - PLAYING)    |
```

### Chi tiết các bước trong code:
1. **Bước 1**: Alice gửi gói tin `CHALLENGE` kèm `targetUserId = 2` (Bob).
2. **Bước 2**: Server tìm thấy `ClientHandler` của Bob trong `ClientManager`, gửi gói tin `CHALLENGE_REQUEST` tới Bob. Giao diện của Bob tự động bật hộp thoại [ChallengeDialog.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/ChallengeDialog.java) đếm ngược 30 giây.
3. **Bước 3**: Bob bấm nút "Chấp nhận", gửi gói tin `ACCEPT` lên Server.
4. **Bước 4**: Server gọi `gameManager.createSession(alice, bob)`:
   * Tạo 2 bàn cờ 12x12 riêng biệt.
   * `MineGenerator` rải ngẫu nhiên 23 quả mìn vào mỗi bàn cờ.
5. **Bước 5**: Server phát gói tin `MATCH_CREATED` cho cả 2 máy. Cả Alice và Bob cùng mở [GameFrame.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/GameFrame.java). Cả 2 máy thấy hiệu ứng đếm ngược 5 giây chuẩn bị.
6. **Bước 6**: Hết 5 giây, `GameSession` phát lệnh `GAME_START`. Trận đấu chính thức bước vào trạng thái `PLAYING`.

---

## 3. Kịch Bản 3: Đánh Cờ Thời Gian Thực & Đồng Bộ Bàn Cờ Nhỏ

Alice click chuột trái vào ô hàng 2, cột 4:

```text
[Alice GameBoardPanel]          [Server GameSession]         [Bob OpponentMiniBoardPanel]
          |                               |                                |
          | 1. Gửi OPEN_CELL(row=2, col=4)|                                |
          |------------------------------>|                                |
          |                               | 2. Kiểm tra Board 1:           |
          |                               |    - Không có mìn              |
          |                               |    - Chạy thuật toán BFS       |
          |                               |      mở rộng các ô xung quanh  |
          | 3. Nhận CELL_OPENED           |                                |
          |<------------------------------|                                |
          | (Lật mở các ô trên bàn chính) | 4. Gửi GAME_UPDATE             |
          |                               |------------------------------->|
          |                               |                                | 5. Cập nhật các chấm xanh
          |                               |                                |    trên bàn cờ thu nhỏ
```

### Tính năng chống gian lận (Anti-cheat) tại bước 4:
* Gói tin `CELL_OPENED` gửi cho Alice có đầy đủ thông tin: số mìn xung quanh (`adjacentMines`) để vẽ số 1, 2, 3 lên bàn cờ.
* Nhưng gói tin `GAME_UPDATE` gửi cho Bob **chỉ chứa tọa độ ô đã mở**, hoàn toàn không gửi mìn! Bob chỉ nhìn thấy trên bàn cờ nhỏ [OpponentMiniBoardPanel.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/OpponentMiniBoardPanel.java) rằng Alice vừa mở thêm ô an toàn, tạo cảm giác thi đấu rượt đuổi nghẹt thở mà không thể soi mìn của nhau.

---

## 4. Kịch Bản 4: Kết Thúc Trận Đấu & Lưu Giao Dịch Database

Khi Bob không may click trúng ô mìn:

```text
[Bob Client]                  [GameSession (Server)]               [Alice Client]
     |                                   |                                |
     | 1. Click mở ô mìn                |                                |
     |---------------------------------->|                                |
     |                                   | 2. Phát hiện mìn nổ!           |
     |                                   |    winnerId = Alice (Id=1)     |
     |                                   | 3. MatchPlayerDAO:             |
     |                                   |    Chạy 1 TRANSACTION:         |
     |                                   |    - Insert Match              |
     |                                   |    - Insert MatchPlayer 1 & 2  |
     |                                   |    - Update Score Alice: +5    |
     |                                   |    - Update Score Bob:   -3    |
     | 4. Nhận GAME_LOSE (delta = -3)    | 4. Nhận GAME_WIN (delta = +5)  |
     |<----------------------------------|------------------------------->|
     | (Hiện ResultDialog: THẤT BẠI)     |                                | (Hiện ResultDialog: CHIẾN THẮNG)
```

Toàn bộ quá trình tính điểm và ghi nhận vào cơ sở dữ liệu được thực hiện tự động và đồng bộ, đảm bảo tính toàn vẹn dữ liệu 100%.
