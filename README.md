# GAME DÒ MÌN THI ĐẤU ĐỐI KHÁNG ONLINE (MINESWEEPER ONLINE)

Bài tập lớn môn: **Lập trình Mạng**  
Nhóm sinh viên thực hiện: **Nhóm 7**

---

## 1. Công nghệ sử dụng
- **Ngôn ngữ**: Java JDK 26 (tương thích hoàn toàn JDK 21+).
- **Môi trường phát triển**: Apache NetBeans 31 / Apache Ant.
- **Mạng**: TCP Socket (`ServerSocket`, `Socket`, đa luồng `Thread` độc lập cho từng Client).
- **Kiến trúc**: Client – Server Authoritative (Server làm trọng tài, quản lý toàn bộ mìn, ô an toàn, thời gian và điểm số).
- **Giao thức**: JSON (chuẩn Newline-delimited `\n`).
- **Thư viện**:
  - `lib/gson.jar`: Google Gson 2.11.0 (mã hóa và giải mã JSON).
  - `lib/mysql-connector-j.jar`: MySQL Connector/J 9.6.0 (kết nối JDBC).
- **Giao diện**: Java Swing (Desktop GUI).
- **Cơ sở dữ liệu**: MySQL 8.0+.

---

## 2. Cấu trúc Project
```text
MinesweeperOnline/
│
├── src/
│   └── minesweeperonline/
│       │
│       ├── server/                        # Phía Server
│       │   ├── MainServer.java            # Khởi tạo ServerSocket port 2209, lắng nghe Client
│       │   ├── ClientHandler.java         # Xử lý kết nối TCP riêng biệt cho mỗi Client
│       │   ├── ClientManager.java         # Quản lý danh sách kết nối và user online
│       │   ├── GameManager.java           # Quản lý danh sách các trận đấu (GameSession)
│       │   ├── GameSession.java           # Quản lý vòng đời trận đấu, đếm giờ, phân định thắng thua
│       │   └── ServerConfig.java          # Cấu hình cổng, timeout, kết nối DB, hệ số điểm
│       │
│       ├── client/                        # Phía Client
│       │   ├── MainClient.java            # Khởi chạy Client và mở giao diện
│       │   ├── ui/                        # Giao diện người dùng Java Swing
│       │   │   ├── LoginFrame.java        # Cửa sổ đăng nhập
│       │   │   ├── RegisterFrame.java     # Cửa sổ đăng ký
│       │   │   ├── MainMenuFrame.java     # Sảnh chờ (hiển thị điểm, danh sách online)
│       │   │   ├── OnlinePlayersPanel.java# Bảng danh sách người chơi online & nút Thách đấu
│       │   │   ├── ChallengeDialog.java   # Hộp thoại đếm ngược 30s khi nhận lời mời thách đấu
│       │   │   ├── GameFrame.java         # Màn hình thi đấu chính thức 12x12
│       │   │   ├── GameBoardPanel.java    # Bàn cờ 12x12 (click trái mở ô, click phải cắm cờ)
│       │   │   ├── OpponentMiniBoardPanel.java # Bàn cờ đối thủ thu nhỏ hiển thị tiến độ thời gian thực
│       │   │   ├── PlayerInfoPanel.java   # Thanh thông tin tiến độ của bạn và đối thủ
│       │   │   ├── ResultDialog.java      # Hộp thoại kết quả (Thắng/Thua/Hòa/Chơi lại/Thoát)
│       │   │   ├── LeaderboardDialog.java # Hộp thoại Bảng xếp hạng cao thủ (Top điểm & Thắng)
│       │   │   └── MatchHistoryDialog.java# Hộp thoại Lịch sử trận đấu (Kết quả, đối thủ, điểm)
│       │   └── network/
│       │       └── ServerConnection.java  # Quản lý kết nối TCP Socket Client -> Server
│       │
│       ├── common/                        # Dùng chung giữa Client và Server
│       │   ├── protocol/
│       │   │   ├── Message.java           # Khung gói tin JSON {type, requestId, gameId, payload}
│       │   │   ├── MessageType.java       # Danh mục các loại thông điệp
│       │   │   └── JsonProtocol.java      # Tiện ích serialize/deserialize JSON kèm '\n'
│       │   └── model/
│       │       ├── User.java              # Thực thể User
│       │       ├── Player.java            # Thực thể Player trong sảnh & trận đấu
│       │       ├── LeaderboardEntry.java  # Thực thể bản ghi Bảng xếp hạng (Rank, Wins, Score)
│       │       ├── MatchHistoryEntry.java # Thực thể bản ghi Lịch sử trận đấu (Đối thủ, tỉ số, thời gian)
│       │       ├── BoardState.java        # Trạng thái bàn cờ (hỗ trợ che giấu mìn đối thủ)
│       │       ├── CellState.java         # Trạng thái ô cờ
│       │       └── GameState.java         # Trạng thái phiên đấu (READY, PLAYING, TIMEOUT, FINISHED)
│       │
│       ├── game/                          # Xử lý luật chơi Dò mìn (Game Logic)
│       │   ├── Board.java                 # Quản lý bàn cờ 12x12, thuật toán BFS mở rộng ô 0
│       │   ├── Cell.java                  # Ô cờ
│       │   ├── MineGenerator.java         # Sinh ngẫu nhiên 23 mìn, tính số lân cận
│       │   ├── GameRules.java             # Luật phân định khi hết giờ (TIMEOUT)
│       │   └── GameResult.java            # Quy chuẩn kết quả (VICTORY, DEFEAT, DRAW, FORFEIT)
│       │
│       └── database/                      # Tầng cơ sở dữ liệu
│           ├── DatabaseConnection.java    # Quản lý JDBC Connection
│           ├── UserDAO.java               # Đăng nhập, đăng ký, bảng xếp hạng
│           ├── MatchDAO.java              # Lưu thông tin trận đấu
│           └── MatchPlayerDAO.java        # Lưu kết quả 2 người chơi trong 1 Transaction duy nhất
│
├── lib/                                   # Thư viện JAR
│   ├── gson.jar
│   └── mysql-connector-j.jar
│
├── database/
│   └── schema.sql                         # Kịch bản DDL MySQL
│
├── build.xml                              # File cấu hình Apache Ant
└── README.md
```

---

## 3. Cách thêm thư viện vào dự án
Thư mục `lib/` đã chứa đầy đủ:
- `lib/gson.jar`
- `lib/mysql-connector-j.jar`

Các file cấu hình `build.xml`, `nbproject/project.properties`, `.classpath` đã được liên kết tự động tới 2 file JAR này.

---

## 4. Cấu hình MySQL
Thông số kết nối database được cấu hình tập trung tại file [ServerConfig.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/server/ServerConfig.java):
```java
public static final String DB_HOST = "localhost";
public static final int DB_PORT = 3306;
public static final String DB_NAME = "minesweeper_online";
public static final String DB_USER = "root";
public static final String DB_PASSWORD = ""; // Điền mật khẩu root của bạn tại đây
```
> **Ghi chú**: Nếu MySQL chưa bật hoặc sai mật khẩu, hệ thống sẽ tự động kích hoạt chế độ **Bộ nhớ dự phòng (In-Memory Fallback)** giúp máy chủ và ứng dụng vẫn hoạt động bình thường mà không bị crash.

---

## 5. Chạy file `schema.sql`
Mở MySQL Command Line hoặc MySQL Workbench và chạy file [database/schema.sql](file:///d:/Ki1_4/LTM/MinesweeperOnline/database/schema.sql):
```bash
mysql -u root -p < database/schema.sql
```
Lệnh trên sẽ tạo cơ sở dữ liệu `minesweeper_online` cùng 3 bảng: `User`, `Match`, `MatchPlayer` và chèn sẵn 3 tài khoản mẫu:
- `alice` / `123456`
- `bob` / `123456`
- `charlie` / `123456`

---

## 6. Build bằng Apache Ant
Mở Terminal tại thư mục gốc của project:
- Dọn dẹp thư mục build:
  ```powershell
  .\tools\apache-ant-1.10.14\bin\ant.bat clean
  ```
- Biên dịch source code:
  ```powershell
  .\tools\apache-ant-1.10.14\bin\ant.bat compile
  ```
- Đóng gói file JAR:
  ```powershell
  .\tools\apache-ant-1.10.14\bin\ant.bat jar
  ```

---

## 7. Khởi chạy Server
Chạy lệnh Ant:
```powershell
.\tools\apache-ant-1.10.14\bin\ant.bat run-server
```
Hoặc bằng lệnh `java`:
```powershell
java -cp "build/classes;lib/gson.jar;lib/mysql-connector-j.jar" minesweeperonline.server.MainServer
```

---

## 8. Khởi chạy Client
Chạy lệnh Ant:
```powershell
.\tools\apache-ant-1.10.14\bin\ant.bat run-client
```
Hoặc bằng lệnh `java`:
```powershell
java -cp "build/classes;lib/gson.jar;lib/mysql-connector-j.jar" minesweeperonline.client.MainClient
```

---

## 9. Chạy nhiều Client để thi đấu đối kháng
1. Mở Terminal 1: Chạy `run-server`.
2. Mở Terminal 2: Chạy `run-client` -> Đăng nhập tài khoản `alice` / `123456`.
3. Mở Terminal 3: Chạy `run-client` -> Đăng nhập tài khoản `bob` / `123456`.
4. Trên màn hình của `alice`, chọn `bob` và bấm **Thách đấu**.
5. Trên màn hình của `bob`, bấm **Chấp nhận**.
6. Cả 2 Client sẽ cùng được chuyển vào bàn đấu [GameFrame](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/GameFrame.java).

---

## 10. Giao thức Protocol (TCP Framing & JSON)
- Mỗi gói tin được gửi dưới dạng chuỗi JSON trên 1 dòng, kết thúc bằng ký tự xuống dòng `\n`.
- Phía nhận sử dụng `BufferedReader.readLine()` để bảo đảm đọc trọn vẹn đúng 1 message, tránh hiện tượng dính/gộp gói tin TCP.
- Cấu trúc tin nhắn:
  ```json
  {
    "type": "OPEN_CELL",
    "requestId": "REQ-123",
    "gameId": "abc12345",
    "payload": {
      "row": 3,
      "col": 5
    }
  }
  ```

---

## 11. Luồng hoạt động của trò chơi
1. **Đăng nhập / Đăng ký**: Client xác thực với Server qua `UserDAO`.
2. **Sảnh chờ**: Sau khi vào sảnh, Client định kỳ nhận danh sách người chơi online (`GET_ONLINE_PLAYERS`).
3. **Thách đấu**: Người chơi gửi lời mời (`CHALLENGE`). Đối thủ có 30 giây để chấp nhận hoặc từ chối (`ACCEPT`/`REJECT`).
4. **Vào trận**: Server khởi tạo `GameSession`, tạo 2 bàn cờ 12×12 độc lập với 23 mìn ngẫu nhiên.
5. **Giai đoạn READY (5 giây)**: Đếm ngược chuẩn bị, các ô chưa thể thao tác.
6. **Giai đoạn PLAYING (Tối đa 5 phút)**:
   - Click chuột trái: Gửi `OPEN_CELL`. Nếu ô là `0`, thuật toán BFS tự động mở rộng vùng an toàn lân cận.
   - Click chuột phải: Gửi `PLACE_FLAG` (cắm cờ, tối đa 23 cờ) hoặc `REMOVE_FLAG` (gỡ cờ).
   - Tiến độ mở ô và số cờ của đối thủ được cập nhật thời gian thực nhưng **vị trí mìn luôn được Server che giấu tuyệt đối**.
7. **Kết thúc trận đấu**:
   - Mở phải mìn: Thua ngay lập tức (`DEFEAT` -3 điểm), đối thủ thắng (`VICTORY` +5 điểm).
   - Mở đủ 121 ô an toàn: Thắng ngay lập tức (`VICTORY` +5 điểm).
   - Hết 5 phút (`TIMEOUT`): Server phân xử theo thứ tự: (1) Nhiều ô an toàn hơn -> (2) Ít thao tác hơn -> (3) Nhiều cờ hơn -> (4) Hòa (`DRAW` +1 điểm mỗi bên).
   - Thoát hoặc rớt mạng (`FORFEIT`): Xử thua người thoát (-5 điểm), trao chiến thắng cho đối thủ (+5 điểm).
   - Toàn bộ kết quả và cập nhật điểm số được lưu vào MySQL bằng **1 Transaction duy nhất**.
8. **Tái đấu (Play Again) & Trở về sảnh (Exit)**:
   - Khi trận kết thúc, hiển thị [ResultDialog](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/ResultDialog.java) thống kê chi tiết: nguyên nhân, điểm cộng/trừ, số ô an toàn đã mở, số thao tác hợp lệ, số cờ.
   - Nếu người chơi bấm **Chơi lại** (`PLAY_AGAIN`), Server ghi nhận và gửi thông báo mời tái đấu cho đối thủ.
   - Khi cả hai người chơi cùng đồng ý tái đấu, Server tự động khởi tạo một `GameSession` mới (mã trận mới, sinh 23 mìn ngẫu nhiên mới, reset thời gian và bước vào giai đoạn READY 5 giây).
   - Nếu một trong hai người chơi bấm **Thoát ra sảnh** (`EXIT`), phiên đấu kết thúc và cả hai người chơi được đưa trở lại màn hình sảnh chính (`MainMenuFrame`).

---

## 12. Bàn cờ đối thủ thu nhỏ (Mini-board)
- Bên cạnh bàn chơi chính 12×12, mỗi người chơi được quan sát thêm 1 bàn cờ thu nhỏ hiển thị tiến trình của đối thủ theo thời gian thực ([OpponentMiniBoardPanel](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/OpponentMiniBoardPanel.java)):
  - Màu xám/chấm xanh: các ô an toàn đối thủ đã mở.
  - Biểu tượng lá cờ đỏ: các vị trí đối thủ đã cắm cờ.
  - Màu lam nhạt: các ô còn ẩn.
- Server tuyệt đối **không gửi vị trí mìn** trong gói tin đồng bộ `GAME_UPDATE`, đảm bảo tính công bằng và bảo mật hoàn hảo.

---

## 13. Bảng xếp hạng người chơi (Leaderboard)
- Nhấn nút **"🏆 Bảng xếp hạng"** trên thanh tiêu đề của Sảnh chính ([MainMenuFrame](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/MainMenuFrame.java)).
- Bảng xếp hạng [LeaderboardDialog](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/LeaderboardDialog.java) hiển thị:
  - **Hạng**: Biểu tượng huy chương vàng 🥇, bạc 🥈, đồng 🥉 cho Top 3 người chơi dẫn đầu.
  - **Tên người chơi**: Tự động tô màu xanh lá nổi bật cho dòng của người chơi hiện tại.
  - **Điểm số**: Sắp xếp theo tổng điểm giảm dần (`totalScore DESC`).
  - **Số trận thắng & Tổng trận**: Sắp xếp phụ theo số trận thắng giảm dần (`wins DESC`).
  - **Tỉ lệ thắng**: Tính tự động phần trăm chiến thắng (`wins / totalMatches * 100%`).
  - Hỗ trợ nút **Làm mới** để nạp dữ liệu cập nhật thời gian thực từ máy chủ.

---

## 14. Lịch sử trận đấu (Match History)
- Nhấn nút **"📜 Lịch sử đấu"** trên thanh tiêu đề của Sảnh chính ([MainMenuFrame](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/MainMenuFrame.java)).
- Hộp thoại [MatchHistoryDialog](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/MatchHistoryDialog.java) hiển thị chi tiết các trận đấu đã tham gia:
  - **Thời gian**: Ngày và giờ diễn ra trận đấu (`dd/MM/yyyy HH:mm:ss`).
  - **Đối thủ**: Tên người chơi đối thủ trong trận.
  - **Kết quả**: Huy hiệu màu sắc trực quan (CHIẾN THẮNG xanh lá, THẤT BẠI đỏ, HÒA xanh dương, BỎ CUỘC cam).
  - **Điểm số**: Số điểm biến động (+5, -3, -5, +1).
  - **Thời lượng**: Thời gian thi đấu thực tế (ví dụ: `1m 05s` hoặc `45s`).
  - **Tiến độ**: Số ô an toàn đã mở (ví dụ: `35 / 121`), số cờ đã cắm và tổng số thao tác (`🚩 8 | ⚡ 45`).
  - **Ghi chú**: Lý do kết thúc trận (Mở trúng mìn, Hết 5 phút thi đấu, Đầu hàng giữa chừng, Mất kết nối, Mở sạch 121 ô an toàn).
  - **Thống kê tổng kết**: Tự động tính tổng số trận, số trận thắng/thua/hòa và phần trăm tỉ lệ thắng tổng thể.
  - Nút **Làm mới** truy vấn trực tiếp từ cơ sở dữ liệu MySQL (kèm fallback bộ nhớ RAM khi chạy thử nghiệm).

