# 9. Chạy & kiểm thử

## 9.1. Chuẩn bị

- **JDK 26** (project biên dịch với `source/target = 26`). Kiểm tra: `java -version`.
- **Ant** có sẵn trong repo: `tools/apache-ant-1.10.14/bin/ant.bat`. Hoặc mở bằng NetBeans.
- **MySQL** (không bắt buộc): chạy [database/schema.sql](../../database/schema.sql). Thông tin kết nối nằm ở `ServerConfig` (`root`, mật khẩu rỗng, DB `minesweeper_online`). Không có MySQL thì server tự dùng fallback RAM.

## 9.2. Build

Chạy tại thư mục gốc project:

```powershell
.\tools\apache-ant-1.10.14\bin\ant.bat compile     # → build/classes
.\tools\apache-ant-1.10.14\bin\ant.bat jar         # → dist/MinesweeperOnline.jar (tùy chọn)
```

## 9.3. Chạy trên cùng một máy

Luôn **chạy server trước**.

```powershell
# Terminal 1 – server (port mặc định 2209)
.\tools\apache-ant-1.10.14\bin\ant.bat run-server

# Terminal 2, 3 – hai client
.\tools\apache-ant-1.10.14\bin\ant.bat run-client
```

Đăng nhập `alice` / `123456` và `bob` / `123456` (có sẵn trong `schema.sql` và trong fallback RAM).

## 9.4. Chạy qua mạng LAN (hai máy khác nhau)

Đây là kịch bản "mạng thật" nên demo cho môn Lập trình mạng.

1. **Máy server**: chạy server như trên, rồi xem IP LAN bằng `ipconfig` (ví dụ `192.168.1.10`).
2. **Mở firewall** cho cổng 2209 trên máy server (chạy PowerShell với quyền Administrator):
   ```powershell
   New-NetFirewallRule -DisplayName "Minesweeper 2209" -Direction Inbound -Protocol TCP -LocalPort 2209 -Action Allow
   ```
3. **Máy client**: truyền IP và port của server vào `MainClient` (`ant run-client` không nhận tham số, nên chạy `java` trực tiếp):
   ```powershell
   java -cp "build/classes;lib/gson.jar;lib/mysql-connector-j.jar" minesweeperonline.client.MainClient 192.168.1.10 2209
   ```
4. Muốn đổi port server: `java -cp "..." minesweeperonline.server.MainServer 3000`, và client dùng `... MainClient <ip> 3000`.

Lỗi thường gặp:

- `Connection refused`: server chưa chạy, hoặc sai port.
- Treo khoảng 5 giây rồi báo lỗi: sai IP, hoặc firewall đang chặn.

## 9.5. Kịch bản demo gợi ý

1. Hai client đăng nhập, thấy nhau trong danh sách với trạng thái `IDLE`.
2. A thách đấu B. Ở client thứ ba, trạng thái của A và B chuyển thành `CHALLENGING`.
3. B chấp nhận. Màn hình đếm READY 5 giây, rồi `GAME_START`.
4. Hai bên mở ô và cắm cờ. Bàn nhỏ của đối thủ cập nhật theo thời gian thực.
5. Một người đầu hàng hoặc trúng mìn. Màn hình kết quả hiện ra, có thể chơi lại hoặc thoát.
6. **Demo xử lý mạng**: đang trong trận thì tắt hẳn một client. Người còn lại nhận "Đối thủ mất kết nối", được +5, và nút "Chơi lại" bị khóa.
7. Mở Bảng xếp hạng và Lịch sử đấu để thấy điểm đã cập nhật.

Log phía server in ra mọi sự kiện (`[ClientHandler] Login successful…`, `[GameManager] Created GameSession…`). Nên để terminal server hiển thị trong lúc demo.

## 9.6. Bộ test tự động

Repo có sẵn các test tích hợp: tự bật server ở port riêng (2210…), mở client socket thật, rồi kiểm tra từng chức năng.

```powershell
.\tools\apache-ant-1.10.14\bin\ant.bat compile
java -ea -cp "build/classes;lib/gson.jar;lib/mysql-connector-j.jar" minesweeperonline.MinesweeperOnline
```

Bắt buộc có cờ `-ea` để các lệnh `assert` có hiệu lực.

> Nếu MySQL đang chạy và đã có database `minesweeper_online`, bộ test sẽ **ghi thật** vào DB: tạo các user như `testuser_…`, `alpha_…`, `hist_p1_…` và các trận đấu. Nếu muốn giữ DB sạch để demo, hãy chạy test khi chưa tạo DB (server tự dùng fallback RAM), hoặc xóa các user test sau khi chạy. Kết quả mong đợi là mỗi phần in một dòng `ALL ... PASSED`:

| File | Kiểm tra |
|---|---|
| `TestPhase1` | model + serialize/deserialize message |
| `TestPhase2` | server thật: login, online list, thách đấu, rớt mạng |
| `TestPhase3` | DAO + đăng ký/đăng nhập qua mạng |
| `TestPhase6` | luật chơi: rải mìn, vùng an toàn, BFS, phân xử hết giờ |
| `TestPhase8_9` | trọn một trận: GAME_START, đồng bộ cờ, đầu hàng, chơi lại, rớt mạng giữa trận |
| `TestLeaderboard`, `TestMatchHistory` | bảng xếp hạng, lịch sử qua mạng |
