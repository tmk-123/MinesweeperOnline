# Ôn tập vấn đáp — Minesweeper Online

Tài liệu đối chiếu với source ngày 02/11/2026. Mỗi liên kết dẫn đến file code trong project, kèm số dòng và tên hàm để tra cứu. Số dòng có thể thay đổi khi sửa code.

**Cách dùng:** mở bản Preview Markdown rồi bấm liên kết. Liên kết dùng đường dẫn tương đối, nên vẫn dùng được khi chuyển cả project sang máy khác. Đuôi `#L...` chỉ dòng code; nếu trình xem hoặc NetBeans chỉ mở file mà không nhảy đúng dòng, dùng chức năng Go to Line hoặc tìm tên hàm ghi cạnh liên kết.

## Tài liệu luồng theo từng chức năng

[Mở mục lục 21 luồng chức năng và sự kiện](luong-chuc-nang/README.md). Mỗi chức năng có một file riêng, kèm bước chạy, code và liên kết source.

- [Bắt đầu ván và sinh mìn](luong-chuc-nang/08-tao-van-sinh-min.md)
- [Mở ô và BFS](luong-chuc-nang/09-mo-o-va-bfs.md)
- [Mất kết nối](luong-chuc-nang/16-mat-ket-noi.md) và [khôi phục trận](luong-chuc-nang/17-ket-noi-lai.md)

## 1. Bản đồ source

| Phần | Vai trò | File |
|---|---|---|
| Client | Khởi động ứng dụng người chơi | [MainClient.java — dòng 63](../src/minesweeperonline/client/MainClient.java#L63) |
| Client | Kết nối TCP, gửi và nhận message | [ServerConnection.java](../src/minesweeperonline/client/network/ServerConnection.java) |
| Client | Nhận click trên bàn cờ và hiển thị ô | [GameBoardPanel.java](../src/minesweeperonline/client/ui/GameBoardPanel.java) |
| Server | Lắng nghe kết nối | [MainServer.java](../src/minesweeperonline/server/MainServer.java) |
| Server | Xử lý message của một client | [ClientHandler.java](../src/minesweeperonline/server/ClientHandler.java) |
| Server | Quản lý các trận | [GameManager.java](../src/minesweeperonline/server/GameManager.java) |
| Server | Điều phối một trận giữa hai người | [GameSession.java](../src/minesweeperonline/server/GameSession.java) |
| Game | Quản lý lưới và luật mở ô, cắm cờ | [Board.java](../src/minesweeperonline/game/Board.java) |
| Game | Đặt mìn và tính số mìn lân cận | [MineGenerator.java](../src/minesweeperonline/game/MineGenerator.java) |
| Common | Cấu trúc và mã hóa message | [Message.java](../src/minesweeperonline/common/protocol/Message.java), [JsonProtocol.java](../src/minesweeperonline/common/protocol/JsonProtocol.java) |
| Database | Cấu trúc cơ sở dữ liệu | [schema.sql](../database/schema.sql) |

## 2. Câu hỏi về bàn cờ và thuật toán

### 1. Code tạo bàn cờ ở đâu?

**Trả lời:** Constructor `Board()` tạo mảng hai chiều `Cell[ROWS][COLS]`, khởi tạo từng ô rồi gọi `initialize()` để đặt trạng thái ban đầu. Lúc này chưa có mìn.

**Mở code:** [Board.java — dòng 37, Board()](../src/minesweeperonline/game/Board.java#L37), [dòng 51, initialize()](../src/minesweeperonline/game/Board.java#L51).

### 2. Kích thước bàn và số mìn nằm ở đâu?

**Trả lời:** `Board` khai báo 12 hàng, 12 cột, 144 ô, 23 mìn và 121 ô an toàn. Logic sinh mìn của `Board` dùng trực tiếp các hằng số này. `ServerConfig` cũng có các hằng số cấu hình bàn cờ, nên cần phân biệt nơi khai báo với nơi thực sự được dùng.

**Mở code:** [Board.java — dòng 17](../src/minesweeperonline/game/Board.java#L17), [ServerConfig.java — dòng 26](../src/minesweeperonline/server/ServerConfig.java#L26).

### 3. Khi nào sinh mìn? Vì sao không sinh ngay khi tạo Board?

**Trả lời:** Sinh mìn khi có lần mở ô hợp lệ đầu tiên. Khi đó server biết tọa độ click để chừa vùng an toàn. Biến `minesGenerated` ngăn việc sinh lại mìn trong các lần mở tiếp theo.

**Mở code:** [Board.java — dòng 106, openCell(); nhánh sinh mìn ở dòng 106](../src/minesweeperonline/game/Board.java#L106).

### 4. Code sinh mìn ngẫu nhiên và không trùng ở đâu?

**Trả lời:** `generateMines()` gom các tọa độ hợp lệ vào `candidates`, xáo trộn bằng `Collections.shuffle(candidates, random)` rồi chọn 23 phần tử đầu. Mỗi tọa độ xuất hiện một lần, nên mìn không trùng vị trí.

**Mở code:** [MineGenerator.java — dòng 29, generateMines()](../src/minesweeperonline/game/MineGenerator.java#L29), [dòng 58, xáo trộn và đặt mìn](../src/minesweeperonline/game/MineGenerator.java#L58).

```java
Collections.shuffle(candidates, random);
for (int i = 0; i < mineCount; i++) {
    int[] pos = candidates.get(i);
    grid[pos[0]][pos[1]].setMine(true);
}
```

### 5. Bảo đảm lần mở đầu tiên an toàn thế nào?

**Trả lời:** Không đặt mìn tại ô đầu tiên và các ô lân cận trong vùng 3 × 3. Điều kiện là chênh lệch hàng và cột đều không quá 1. Ở cạnh hoặc góc, chỉ các ô nằm trong bàn mới được xét. Vì vậy ô đầu tiên có số mìn xung quanh bằng 0.

**Mở code:** [MineGenerator.java — dòng 43, lọc ứng viên](../src/minesweeperonline/game/MineGenerator.java#L43), [dòng 78, isInSafeZone()](../src/minesweeperonline/game/MineGenerator.java#L78).

### 6. Các số trên bàn cờ được tính ở đâu?

**Trả lời:** `calculateAdjacentMines()` duyệt từng ô không có mìn, dùng hai vòng lặp `dr`, `dc` từ -1 đến 1 để xét 8 ô lân cận. Bỏ qua chính ô đang xét, kiểm tra biên và đếm mìn, sau đó lưu vào `adjacentMines`.

**Mở code:** [MineGenerator.java — dòng 88, calculateAdjacentMines()](../src/minesweeperonline/game/MineGenerator.java#L88).

### 7. Vì sao click một ô lại mở cả vùng? Dùng thuật toán gì?

**Trả lời:** Ô có số 0 được mở lan bằng BFS. Hàng đợi chứa các ô trống cần xét tiếp. Các ô số ở rìa vùng được mở nhưng không tiếp tục lan từ chúng. Đánh dấu ô đã mở giúp tránh xử lý lặp.

**Mở code:** [Board.java — dòng 141, Queue và vòng lặp BFS](../src/minesweeperonline/game/Board.java#L141).

### 8. Nếu click ngoài bàn, ô đã mở hoặc ô đang cắm cờ thì sao?

**Trả lời:** `openCell()` kiểm tra tọa độ và trạng thái ô trước khi sinh mìn hoặc mở ô. Yêu cầu không hợp lệ trả về `OpenResult.INVALID`.

**Mở code:** [Board.java — dòng 106](../src/minesweeperonline/game/Board.java#L106).

### 9. Code kiểm tra trúng mìn và thắng ở đâu?

**Trả lời:** `Board.openCell()` trả về `MINE` khi mở trúng mìn. `GameSession` xử lý kết quả, xác định người thắng và gửi thông báo. Khi mở hết 121 ô an toàn, `isAllSafeCellsOpened()` xác nhận đã hoàn thành bàn.

**Mở code:** [Board.java — dòng 125, trúng mìn](../src/minesweeperonline/game/Board.java#L125), [dòng 213, kiểm tra mở hết ô](../src/minesweeperonline/game/Board.java#L213), [GameSession.java — dòng 150, xử lý thắng/thua](../src/minesweeperonline/server/GameSession.java#L150).

### 10. Hai người dùng cùng một bàn cờ không?

**Trả lời:** Mỗi người có một `Board` riêng là `board1`, `board2`. Mỗi bàn sinh mìn theo lần mở đầu của người tương ứng; code không sao chép cùng bản đồ mìn cho cả hai.

**Mở code:** [GameSession.java — dòng 63, tạo các Board](../src/minesweeperonline/server/GameSession.java#L63), [dòng 589, getBoardForPlayer()](../src/minesweeperonline/server/GameSession.java#L589).

## 3. Câu hỏi “chức năng này code ở đâu?”

| Câu hỏi | Trả lời ngắn | Mở code |
|---|---|---|
| Client nhận click chuột ở đâu? | `mousePressed()` phân biệt click trái mở ô, click phải bật/tắt cờ rồi gửi message. | [GameBoardPanel.java:92](../src/minesweeperonline/client/ui/GameBoardPanel.java#L92) |
| Server nhận yêu cầu mở ô ở đâu? | `handleOpenCell()` lấy `row`, `col`, tìm session theo user rồi chuyển tiếp. | [ClientHandler.java:204](../src/minesweeperonline/server/ClientHandler.java#L204) |
| Server chọn bàn và xử lý mở ô ở đâu? | `GameSession.handleOpenCell()` chọn Board, gọi `openCell()` rồi gửi kết quả. | [GameSession.java:150](../src/minesweeperonline/server/GameSession.java#L150) |
| Code cắm/gỡ cờ ở đâu? | ClientHandler chuyển yêu cầu; Board kiểm tra và thay đổi trạng thái cờ. | [ClientHandler.java:215](../src/minesweeperonline/server/ClientHandler.java#L215), [Board.java:180](../src/minesweeperonline/game/Board.java#L180), [Board.java:197](../src/minesweeperonline/game/Board.java#L197) |
| Code hiển thị ô sau khi server xác nhận? | `revealCell()` cập nhật giao diện ô. | [GameBoardPanel.java:121](../src/minesweeperonline/client/ui/GameBoardPanel.java#L121) |
| Đăng nhập ở đâu? | `handleLogin()` đọc dữ liệu, gọi DAO xác thực. | [ClientHandler.java:237](../src/minesweeperonline/server/ClientHandler.java#L237), [UserDAO.java:36](../src/minesweeperonline/database/UserDAO.java#L36) |
| Đăng ký ở đâu? | `handleRegister()` xử lý yêu cầu đăng ký. | [ClientHandler.java:288](../src/minesweeperonline/server/ClientHandler.java#L288) |
| Lấy danh sách xếp hạng ở đâu? | `handleGetLeaderboard()` xử lý yêu cầu. | [ClientHandler.java:337](../src/minesweeperonline/server/ClientHandler.java#L337) |
| Lịch sử trận đấu ở đâu? | `handleGetMatchHistory()` xử lý yêu cầu. | [ClientHandler.java:361](../src/minesweeperonline/server/ClientHandler.java#L361) |
| Ghép trận nhanh ở đâu? | Handler chuyển yêu cầu tới MatchmakingManager. | [ClientHandler.java:379](../src/minesweeperonline/server/ClientHandler.java#L379), [MatchmakingManager.java](../src/minesweeperonline/server/MatchmakingManager.java) |
| Gửi lời mời và chấp nhận ở đâu? | `handleChallenge()`, `handleAccept()`; chấp nhận hợp lệ sẽ tạo session. | [ClientHandler.java:395](../src/minesweeperonline/server/ClientHandler.java#L395), [ClientHandler.java:476](../src/minesweeperonline/server/ClientHandler.java#L476) |
| Tạo trận mới ở đâu? | `GameManager.createSession()`. | [GameManager.java:19](../src/minesweeperonline/server/GameManager.java#L19) |
| Đầu hàng ở đâu? | `handleForfeit()` xác định đối thủ thắng và gửi kết quả. | [ClientHandler.java:559](../src/minesweeperonline/server/ClientHandler.java#L559) |
| Chơi lại ở đâu? | `GameSession.handlePlayAgain()` xử lý và tạo session mới khi đủ điều kiện. | [GameSession.java:392](../src/minesweeperonline/server/GameSession.java#L392) |
| Đăng xuất ở đâu? | `handleLogout()` dọn trạng thái tài khoản, giữ kết nối để quay lại đăng nhập. | [ClientHandler.java:615](../src/minesweeperonline/server/ClientHandler.java#L615) |
| Mất kết nối ở đâu? | `cleanup()` gọi xử lý ngắt kết nối của session nếu trận còn chạy. | [ClientHandler.java:694](../src/minesweeperonline/server/ClientHandler.java#L694), [GameSession.java:611](../src/minesweeperonline/server/GameSession.java#L611) |
| Lưu kết quả ở đâu? | `saveMatchToDatabase()` gọi DAO lưu bằng transaction. | [GameSession.java:475](../src/minesweeperonline/server/GameSession.java#L475), [MatchPlayerDAO.java:91](../src/minesweeperonline/database/MatchPlayerDAO.java#L91) |

## 4. Câu hỏi trọng tâm Lập trình Mạng

### 11. Vì sao chọn TCP?

**Trả lời:** Các yêu cầu như mở ô, cắm cờ và kết quả trận cần được truyền đúng thứ tự và tin cậy. TCP cung cấp luồng byte có thứ tự; ứng dụng vẫn phải tự quy định ranh giới message, kiểm tra dữ liệu và xử lý mất kết nối. TCP có chi phí kết nối và có thể tăng độ trễ khi phải truyền lại dữ liệu.

**Mở code:** [MainServer.java — dòng 33, ServerSocket](../src/minesweeperonline/server/MainServer.java#L33), [ServerConnection.java](../src/minesweeperonline/client/network/ServerConnection.java).

### 12. Server phục vụ nhiều client thế nào?

**Trả lời:** Vòng lặp `accept()` nhận từng kết nối. Với mỗi socket mới, server tạo một `ClientHandler` và một thread riêng, nên việc đọc dữ liệu chặn của một client không giữ vòng lặp nhận kết nối.

**Mở code:** [MainServer.java — dòng 42–48](../src/minesweeperonline/server/MainServer.java#L42).

### 13. Message có cấu trúc gì? Phân loại ở đâu?

**Trả lời:** Message gồm `type`, `requestId`, `gameId`, `payload`. `ClientHandler.handleMessage()` dùng `switch` theo `MessageType` để chuyển đến hàm xử lý tương ứng.

**Mở code:** [Message.java — dòng 22](../src/minesweeperonline/common/protocol/Message.java#L22), [MessageType.java](../src/minesweeperonline/common/protocol/MessageType.java), [ClientHandler.java — dòng 121](../src/minesweeperonline/server/ClientHandler.java#L121).

### 14. TCP không có ranh giới message, vậy tách message bằng cách nào?

**Trả lời:** Mỗi message được serialize thành JSON và thêm ký tự xuống dòng. Bên nhận dùng `readLine()` để đọc một dòng hoàn chỉnh. Một lần đọc từ TCP không mặc định tương ứng một message; chính quy ước xuống dòng tạo ranh giới ở tầng ứng dụng.

**Mở code:** [JsonProtocol.java — dòng 36, serializeWithNewline()](../src/minesweeperonline/common/protocol/JsonProtocol.java#L36), [ClientHandler.java — dòng 91, nhận ở server](../src/minesweeperonline/server/ClientHandler.java#L91), [ServerConnection.java — dòng 161, nhận ở client](../src/minesweeperonline/client/network/ServerConnection.java#L161).

### 15. Vì sao cần flush()? Vì sao sendMessage() có synchronized?

**Trả lời:** `flush()` đẩy dữ liệu trong bộ đệm writer xuống luồng bên dưới. `synchronized` bảo vệ việc gửi qua cùng một handler để nhiều thread không ghi chồng chéo message. `flush()` không có nghĩa bên nhận đã xử lý xong yêu cầu.

**Mở code:** [ClientHandler.java — dòng 632, sendMessage()](../src/minesweeperonline/server/ClientHandler.java#L632).

### 16. Tại sao client nhận dữ liệu trên thread nền?

**Trả lời:** `readLine()` có thể chặn khi chưa có message. Đặt vòng nhận trên thread riêng giúp giao diện không bị đứng trong thời gian chờ mạng.

**Mở code:** [ServerConnection.java — dòng 83, tạo listenerThread](../src/minesweeperonline/client/network/ServerConnection.java#L83), [dòng 157, vòng nhận](../src/minesweeperonline/client/network/ServerConnection.java#L157).

### 17. JSON sai hoặc thiếu trường thì sao?

**Trả lời:** Nếu deserialize không thành message thì server gửi lỗi định dạng. Nếu handler phát sinh `RuntimeException`, chẳng hạn thiếu trường cần đọc, vòng xử lý bắt lỗi và gửi phản hồi lỗi thay vì để ngoại lệ đó kết thúc toàn bộ vòng nhận.

**Mở code:** [ClientHandler.java — dòng 97–107](../src/minesweeperonline/server/ClientHandler.java#L97).

### 18. Vì sao server giữ luật chơi và vị trí mìn?

**Trả lời:** Client gửi hành động; server kiểm tra, cập nhật Board và quyết định kết quả. Như vậy client không tự tuyên bố mở được ô hay thắng trận. Khi chơi, cập nhật mở ô chứa các ô vừa mở; danh sách toàn bộ mìn được gắn vào thông báo kết thúc trận ở các luồng gọi tương ứng.

**Mở code:** [GameSession.java — dòng 150, xử lý hành động](../src/minesweeperonline/server/GameSession.java#L150), [dòng 222, CELL_OPENED](../src/minesweeperonline/server/GameSession.java#L222), [dòng 518, attachMines()](../src/minesweeperonline/server/GameSession.java#L518).

**Câu hỏi sâu:** Bản thân `attachMines()` không kiểm tra `finished`; việc chỉ gọi lúc kết thúc phụ thuộc các nơi gọi hàm. Cần phân biệt quy ước sử dụng với điều kiện bảo vệ bên trong hàm.

### 19. Hai người cùng thao tác thì bảo vệ trạng thái thế nào?

**Trả lời:** Các thao tác trong `GameSession`, như mở ô, được bảo vệ bằng `synchronized (gameLock)`. Tại một thời điểm chỉ một thread giữ khóa đó để thực hiện đoạn xử lý tương ứng.

**Mở code:** [GameSession.java — dòng 151](../src/minesweeperonline/server/GameSession.java#L151), [dòng 261, cắm cờ](../src/minesweeperonline/server/GameSession.java#L261).

### 20. Tránh kết thúc và lưu trận hai lần thế nào?

**Trả lời:** `finishGame()` dùng `finished.compareAndSet(false, true)`. Chỉ lời gọi đổi được trạng thái từ false sang true mới chạy nhánh dừng timer, cập nhật kết quả và gọi lưu dữ liệu. Đây là bảo vệ một lần kết thúc trong bộ nhớ của session, không phải bảo đảm tự thử lại nếu ghi database thất bại.

**Mở code:** [GameSession.java — dòng 551, finishGame()](../src/minesweeperonline/server/GameSession.java#L551).

### 21. Transaction lưu trận để làm gì?

**Trả lời:** Nhóm các thay đổi dữ liệu của một trận vào một transaction. Nếu thành công thì `commit()`, nếu gặp lỗi thì `rollback()` để tránh lưu một phần các thay đổi trong transaction.

**Mở code:** [MatchPlayerDAO.java — dòng 91, saveFullMatchTransaction()](../src/minesweeperonline/database/MatchPlayerDAO.java#L91), [dòng 126, commit](../src/minesweeperonline/database/MatchPlayerDAO.java#L126), [dòng 151, rollback](../src/minesweeperonline/database/MatchPlayerDAO.java#L151).

### 22. Server phát hiện mất kết nối và xử lý ra sao?

**Trả lời:** Khi đọc trả về null hoặc xảy ra lỗi I/O, vòng nhận kết thúc rồi chạy `cleanup()`. Với trận đang chơi, session xử lý trạng thái mất kết nối và thời gian chờ kết nối lại. Mất mạng im lặng có thể không được TCP phát hiện tức thì; không nên khẳng định mọi trường hợp đều được phát hiện ngay.

**Mở code:** [ClientHandler.java — dòng 91, vòng đọc](../src/minesweeperonline/server/ClientHandler.java#L91), [dòng 694, cleanup()](../src/minesweeperonline/server/ClientHandler.java#L694), [GameSession.java — dòng 611, handlePlayerDisconnect()](../src/minesweeperonline/server/GameSession.java#L611).

## 5. Luồng nên tập trình bày khi bảo vệ

**Ví dụ thầy hỏi: “Em chỉ toàn bộ luồng mở một ô cho thầy xem.”**

1. [GameBoardPanel.mousePressed() — dòng 92](../src/minesweeperonline/client/ui/GameBoardPanel.java#L92): nhận click, tạo yêu cầu mở ô.
2. [ServerConnection — dòng 115](../src/minesweeperonline/client/network/ServerConnection.java#L115): serialize message và ghi ra kết nối TCP.
3. [ClientHandler.run() — dòng 91](../src/minesweeperonline/server/ClientHandler.java#L91): đọc dòng JSON và deserialize.
4. [ClientHandler.handleMessage() — dòng 121](../src/minesweeperonline/server/ClientHandler.java#L121): phân loại `OPEN_CELL`.
5. [ClientHandler.handleOpenCell() — dòng 204](../src/minesweeperonline/server/ClientHandler.java#L204): lấy tọa độ, tìm session theo user đã đăng nhập.
6. [GameSession.handleOpenCell() — dòng 150](../src/minesweeperonline/server/GameSession.java#L150): kiểm tra trạng thái trận, chọn Board của người chơi.
7. [Board.openCell() — dòng 106](../src/minesweeperonline/game/Board.java#L106): kiểm tra ô; nếu lần đầu thì sinh mìn; mở ô hoặc mở lan BFS.
8. [GameSession — dòng 222](../src/minesweeperonline/server/GameSession.java#L222): với nhánh mở an toàn và trận còn tiếp diễn, tạo `CELL_OPENED` chứa các ô vừa mở. Nhánh thắng/thua gửi message kết thúc tương ứng.
9. [ServerConnection — dòng 161](../src/minesweeperonline/client/network/ServerConnection.java#L161): client đọc phản hồi và thông báo cho listener.
10. [GameBoardPanel.revealCell() — dòng 121](../src/minesweeperonline/client/ui/GameBoardPanel.java#L121): hàm cập nhật hiển thị ô, được phần giao diện gọi khi xử lý phản hồi.

**Mẫu trả lời ngắn:** “Client gửi tọa độ qua TCP. Server xác định người chơi từ kết nối đã đăng nhập, chọn bàn của người đó và áp dụng luật trong Board. Sau đó server gửi các ô vừa mở hoặc kết quả trận về để client cập nhật giao diện.”

## 6. Câu hỏi mở rộng để tự luyện

- Nếu muốn đổi số mìn, phải kiểm tra những hằng số và giao diện nào? Vì sao sửa một chỗ có thể chưa đủ?
- Bảo đảm lần click đầu an toàn có đồng nghĩa bàn luôn giải được mà không đoán không? **Không**; thuật toán hiện tại chỉ bảo đảm vùng đầu an toàn.
- Cắm cờ có làm thay đổi vị trí mìn không? **Không**; đó là trạng thái đánh dấu của ô.
- Tại sao server lấy session theo user đã đăng nhập thay vì tin hoàn toàn dữ liệu client gửi? Tìm lại `ClientHandler.handleOpenCell()`.
- Nếu ghi database thất bại sau khi session đã kết thúc thì có tự lưu lại không? Đọc `finishGame()` và DAO trước khi trả lời; phân biệt chống gọi lặp với phục hồi sau lỗi.
- Nếu số client tăng rất lớn, mô hình một thread mỗi kết nối có giới hạn gì về bộ nhớ và lập lịch?
- Có thể chỉ đổi TCP thành UDP mà giữ nguyên code không? **Không**; phải thay cách giao tiếp và tự giải quyết thêm các yêu cầu tin cậy, thứ tự, ranh giới dữ liệu.

Khi trả lời bất kỳ chức năng nào, tập nêu đủ: **file → hàm → nơi gọi → dữ liệu đầu vào → kiểm tra → kết quả gửi về**.
