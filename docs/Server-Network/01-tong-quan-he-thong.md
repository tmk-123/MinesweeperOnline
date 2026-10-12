# 1. Tổng quan hệ thống

## 1.1. Hệ thống làm gì?

Game Dò mìn **đối kháng 1–1 qua mạng**. Hai người chơi, mỗi người có **bàn cờ riêng** 12×12 (23 mìn, 121 ô an toàn), chơi **cùng lúc** trong 5 phút. Ai mở hết ô an toàn trước thì thắng. Ai mở trúng mìn thì thua ngay. Hết giờ thì so chỉ số. Mỗi người nhìn thấy tiến độ của đối thủ trên một bàn cờ thu nhỏ.

## 1.2. Mô hình: Client – Server, Server-authoritative

```text
          Client A ─┐                  ┌─ MySQL
                    ├── TCP ── Server ─┤
          Client B ─┘                  └─ (RAM fallback nếu MySQL tắt)
```

- **Client** (Java Swing): hiển thị, nhận click, gửi yêu cầu, vẽ lại theo phản hồi.
- **Server**: nơi **duy nhất** giữ trạng thái chính thức: vị trí mìn, ô nào đã mở, thời gian, kết quả, điểm.
- **Database**: chỉ server truy cập. Client không bao giờ kết nối thẳng vào DB.

**Vì sao server-authoritative?** Nếu client tự biết mìn hoặc tự tính thắng thua, người chơi có thể sửa client để gian lận. Ở đây client **không bao giờ nhận vị trí mìn chưa mở**. Nó chỉ nhận kết quả của ô vừa mở.

## 1.3. Cấu trúc package

```text
src/minesweeperonline/
├── server/      ← PHẦN CỦA BẠN: nhận kết nối, điều phối, quản lý trận
│   ├── MainServer.java      cổng vào: ServerSocket + accept()
│   ├── ClientHandler.java   1 kết nối = 1 handler = 1 thread
│   ├── ClientManager.java   danh bạ: ai đang kết nối / online
│   ├── GameManager.java     danh bạ: trận nào đang có, ai ở trận nào
│   ├── GameSession.java     1 trận đấu: 2 bàn cờ, đồng hồ, kết quả
│   └── ServerConfig.java    hằng số: port, DB, thời gian, điểm
├── common/      ← dùng chung client + server
│   ├── protocol/            Message (phong bì), MessageType (loại), JsonProtocol (đóng/mở gói)
│   └── model/               User, Player, LeaderboardEntry, MatchHistoryEntry, ...
├── game/        ← luật chơi thuần, không biết gì về mạng
│   ├── Board.java           mở ô (BFS), đặt/gỡ cờ, đếm thống kê
│   ├── Cell.java            1 ô
│   ├── MineGenerator.java   rải 23 mìn, né vùng 3×3 quanh cú click đầu
│   └── GameRules.java       phân xử khi hết giờ
├── database/    ← JDBC: UserDAO, MatchDAO, MatchPlayerDAO, DatabaseConnection
├── client/      ← PHẦN CLIENT
│   ├── MainClient.java      điểm khởi động client
│   ├── network/ServerConnection.java   tầng mạng phía client
│   └── ui/                  các cửa sổ Swing
└── Test*.java, MinesweeperOnline.java  ← bộ test tích hợp
```

**Nguyên tắc tách lớp:**

- `game/` không import gì của `server/` hay mạng. Có thể test luật chơi độc lập.
- `common/` là "hợp đồng" giữa hai bên. Sửa ở đây là sửa cả client lẫn server, và **không được tự ý đổi protocol** (quy tắc trong `AGENTS.md`).
- `server/` ghép mọi thứ lại: nhận message → gọi `game/` → ghi `database/` → gửi message.

## 1.4. Vòng đời của một người chơi (nhìn từ server)

```text
 kết nối TCP ──► [chưa đăng nhập] ──LOGIN ok──► IDLE (rảnh, ở sảnh)
                                                  │  CHALLENGE / nhận CHALLENGE_REQUEST
                                                  ▼
                                             CHALLENGING (lời mời đang PENDING)
                           REJECT / hết 30s / người kia rời │  ACCEPT
                                        ◄──────────────────┘  ▼
                                                           PLAYING
                                     (READY 5s → chơi 300s → kết thúc → phòng kết quả)
                                                  │  EXIT (hoặc rớt mạng)
                                                  ▼
                                                 IDLE
 LOGOUT (ở sảnh)            ──► về [chưa đăng nhập], socket vẫn mở
 ngắt kết nối bất kỳ lúc nào ──► cleanup(): hủy lời mời, xử thua nếu đang chơi, xóa khỏi danh bạ
```

Chi tiết từng mũi tên nằm ở [05 – Luồng chức năng](05-luong-chuc-nang.md) và [06 – Trạng thái & đa luồng](06-trang-thai-va-da-luong.md).

## 1.5. Một thao tác đi qua hệ thống như thế nào?

Ví dụ người chơi A click mở ô (3, 5):

```text
GameBoardPanel (A)  ── click ──► ServerConnection.sendMessage(OPEN_CELL {row:3,col:5})
        ...  TCP  ...
ClientHandler(A).run()  readLine() → JsonProtocol.deserialize → handleMessage → handleOpenCell
        ▼
GameSession.handleOpenCell(userIdA, 3, 5)      (khóa gameLock)
        ▼
Board.openCell(...)  → SAFE/MINE/INVALID + danh sách ô vừa mở (BFS)
        ▼
gửi CELL_OPENED  → A   (chi tiết từng ô: số mìn kề)
gửi GAME_UPDATE  → B   (chỉ tọa độ + số liệu, KHÔNG có số mìn)
        ▼
GameFrame (A/B) nhận qua listener → SwingUtilities.invokeLater → vẽ lại
```

Mọi chức năng khác đều theo cùng khuôn: **client gửi yêu cầu → server kiểm tra → xử lý → gửi phản hồi cho người yêu cầu và/hoặc thông báo cho người liên quan.**
