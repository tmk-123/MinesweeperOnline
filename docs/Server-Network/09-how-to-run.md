# Chương 09: Hướng Dẫn Khởi Chạy & Kiểm Thử Hệ Thống

Chương này hướng dẫn từng bước từ chuẩn bị môi trường, biên dịch mã nguồn cho đến khởi chạy 1 Server và 2 Client để trải nghiệm trận đấu đối kháng thực tế.

---

## 1. Chuẩn Bị Môi Trường

1. **Java Development Kit (JDK)**:
   * Yêu cầu: Java JDK 21 trở lên (khuyến nghị JDK 26).
   * Kiểm tra trong Terminal bằng lệnh:
     ```powershell
     java -version
     ```
2. **Apache Ant**:
   * Dự án đã tích hợp sẵn phiên bản Apache Ant di động tại thư mục:
     `tools/apache-ant-1.10.14/bin/ant.bat`
   * Bạn không cần phải cài đặt thêm bất kỳ công cụ nào khác.

---

## 2. Biên Dịch Dự Án (Build)

Mở cửa sổ Terminal tại thư mục gốc của project (`D:\Ki1_4\LTM\MinesweeperOnline`):

1. **Dọn dẹp thư mục build cũ**:
   ```powershell
   .\tools\apache-ant-1.10.14\bin\ant.bat clean
   ```
2. **Biên dịch toàn bộ mã nguồn**:
   ```powershell
   .\tools\apache-ant-1.10.14\bin\ant.bat compile
   ```
3. **Đóng gói file JAR hoàn chỉnh**:
   ```powershell
   .\tools\apache-ant-1.10.14\bin\ant.bat jar
   ```
   *File đóng gói sẽ được tạo tại: `dist/MinesweeperOnline.jar`.*

---

## 3. Khởi Chạy Server (BẮT BUỘC CHẠY ĐẦU TIÊN)

> [!IMPORTANT]
> **Quy tắc bất di bất dịch**: Bạn bắt buộc phải bật Server trước khi bật Client. Nếu Client mở lên mà không thấy Server đang lắng nghe ở cổng `2209`, Client sẽ báo lỗi kết nối ngay lập tức!

Mở **Terminal số 1** và chạy lệnh:
```powershell
.\tools\apache-ant-1.10.14\bin\ant.bat run-server
```
*(Hoặc chạy trực tiếp bằng lệnh `java`:)*
```powershell
java -cp "build/classes;lib/gson.jar;lib/mysql-connector-j.jar" minesweeperonline.server.MainServer
```

**Màn hình Server xuất hiện dòng thông báo thành công:**
```text
==================================================
      MINESWEEPER ONLINE - TCP SERVER             
==================================================
[MainServer] Server started successfully on port: 2209
[MainServer] Waiting for client connections...
```

---

## 4. Khởi Chạy 2 Client Để Thi Đấu Đối Kháng

### 4.1. Khởi chạy Client 1 (Người chơi 1 - Alice)
Mở **Terminal số 2** và chạy lệnh:
```powershell
.\tools\apache-ant-1.10.14\bin\ant.bat run-client
```
1. Cửa sổ Đăng nhập hiện lên.
2. Nhập tài khoản có sẵn:
   * **Tên đăng nhập**: `alice`
   * **Mật khẩu**: `123456`
3. Nhấn **Đăng nhập** -> Giao diện Sảnh chờ (`MainMenuFrame`) xuất hiện.

---

### 4.2. Khởi chạy Client 2 (Người chơi 2 - Bob)
Mở **Terminal số 3** và chạy lệnh:
```powershell
.\tools\apache-ant-1.10.14\bin\ant.bat run-client
```
1. Cửa sổ Đăng nhập thứ hai hiện lên.
2. Nhập tài khoản:
   * **Tên đăng nhập**: `bob`
   * **Mật khẩu**: `123456`
3. Nhấn **Đăng nhập** -> Giao diện Sảnh chờ thứ hai xuất hiện.

---

## 5. Các Bước Trải Nghiệm Trận Đấu

1. **Quan sát Sảnh chờ**:
   * Trên màn hình của `alice`, danh sách người chơi online sẽ xuất hiện người chơi `bob` với trạng thái `Rảnh (IDLE)`.
   * Trên màn hình của `bob`, danh sách cũng xuất hiện `alice`.
2. **Thách đấu**:
   * Trên máy của `alice`: Click chọn dòng của `bob`, sau đó nhấn nút **Thách đấu**.
   * Trên máy của `bob`: Một hộp thoại đếm ngược 30 giây [ChallengeDialog](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/ChallengeDialog.java) sẽ lập tức hiện lên hỏi: *"alice muốn thách đấu bạn! Bạn có đồng ý không?"*.
   * Bob nhấn nút **Chấp nhận**.
3. **Vào trận**:
   * Cả 2 Client cùng được chuyển vào bàn đấu [GameFrame](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/GameFrame.java).
   * Màn hình có hiệu ứng đếm ngược **5 giây READY** trước khi vào trận.
4. **Thi đấu**:
   * **Click chuột trái**: Gửi yêu cầu mở ô. Nếu ô an toàn là số 0, các ô lân cận tự động nở rộng bằng thuật toán BFS.
   * **Click chuột phải**: Cắm cờ 🚩 hoặc gỡ cờ.
   * **Quan sát Bàn cờ nhỏ (Mini-board)**: Ở góc phải, bạn sẽ thấy tiến trình mở ô của đối thủ được tô chấm xanh thời gian thực!
5. **Kết thúc trận**:
   * Hộp thoại [ResultDialog](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/client/ui/ResultDialog.java) hiện ra thông báo kết quả:
     * Người thắng được **+5 điểm**.
     * Người thua bị **-3 điểm** (nếu mở trúng mìn) hoặc **-5 điểm** (nếu bấm Đầu hàng).
   * Cả 2 có thể nhấn **Chơi lại** để tái đấu hoặc **Thoát ra sảnh** để quay lại sảnh chính.
6. **Xem Bảng xếp hạng & Lịch sử đấu**:
   * Nhấn nút **"🏆 Bảng xếp hạng"** trên sảnh chính để xem vị trí của mình (Huy chương vàng 🥇, bạc 🥈, đồng 🥉).
   * Nhấn nút **"📜 Lịch sử đấu"** để xem lại bảng chi tiết từng trận vừa diễn ra.

---

## 6. Chạy Kiểm Thử Tự Động (Automated Test Suite)

Dự án có sẵn bộ test tích hợp toàn diện từ Phase 1 đến Phase 11. Để kiểm tra toàn bộ luồng mạng mà không cần thao tác tay:
```powershell
java -cp "build/classes;lib/gson.jar;lib/mysql-connector-j.jar" -ea minesweeperonline.MinesweeperOnline
```
Hệ thống sẽ tự động khởi tạo Server ảo, kết nối các Client ảo, giả lập các pha mở ô, cắm cờ, bỏ cuộc, rớt mạng, truy vấn bảng xếp hạng và lịch sử đấu để đảm bảo code hoạt động hoàn hảo 100%.
