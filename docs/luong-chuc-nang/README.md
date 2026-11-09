# Luồng chức năng và sự kiện

Mỗi file giải thích một chức năng qua tình huống cụ thể: thao tác của người dùng → hàm nào gọi hàm nào và ở file nào → dữ liệu truyền vào → JSON gửi/nhận → thay đổi trên server và giao diện → trích đoạn code để đối chiếu.

**Cách mở code:** bấm liên kết trong Markdown Preview. Nếu trình xem không nhảy theo `#L`, dùng Go to Line theo dòng được ghi hoặc tìm đoạn code/tên hàm. Các snippet là trích đoạn của mã hiện tại, không phải hàm đầy đủ. Khi mã đổi số dòng, chạy `python tools/update_flow_references.py` từ thư mục gốc dự án để cập nhật tham chiếu.

## Quy ước cho các ví dụ

- A là `an_demo`, userId `101`; B là `binh_demo`, userId `102`. Đây là tên và ID minh họa, tài liệu không tạo tài khoản thật.
- Mã trận ví dụ là `a1b2c3d4`. Khi chơi lại, server tạo mã mới. Hai người giữ hai bàn cờ riêng trên server.
- Tọa độ bắt đầu từ 0: `(3,4)` là hàng thứ 4, cột thứ 5 trên giao diện. `row/col` dùng trong yêu cầu và ô chi tiết; `r/c` dùng ở danh sách tọa độ thu gọn theo đúng code.
- JSON được trình bày nhiều dòng cho dễ đọc. Gói thực tế gửi bằng UTF-8, mỗi message trên một dòng và kết thúc bằng `\n`. `requestId` hoặc `gameId` null bị Gson bỏ qua; không phải mọi phản hồi đều đặt hai trường này.
- Thứ tự bảng gọi hàm mô tả luồng xử lý giữa các phần, không có nghĩa client gọi trực tiếp hàm Java ở server. Qua ranh giới client/server luôn là dữ liệu trên TCP.
- Các tình huống là độc lập nếu không ghi “tiếp luồng”. Bố trí mìn minh họa chỉ là một kết quả có thể có của thuật toán ngẫu nhiên, không được gán cố định vào trò chơi.
- Điểm hiện tại: thắng `+5`, thua `-3`, bỏ cuộc `-5`, hòa `+1`; tổng điểm không âm. Lịch sử giữ mức điểm đã lưu ở thời điểm kết thúc trận.

## Nên đọc lần theo như thế nào?

Để hiểu sinh mìn, đọc [08](08-tao-van-sinh-min.md) rồi [09](09-mo-o-va-bfs.md): A bấm `(3,4)` lần đầu, vùng 3×3 được bảo vệ, server đặt 23 mìn và mở lan 9 ô trong bàn ví dụ. Tiếp [10](10-cam-co.md) và [11](11-go-co.md) để theo dõi cắm/gỡ cờ tại `(3,7)` và cách các bộ đếm tăng.

Để hiểu thua do mìn, đọc [12](12-thang-thua-mo-o.md): đó là ván khác, lần mở đầu ở `(0,0)`, sau đó bấm trúng mìn tại `(3,4)`. Hai tình huống không mâu thuẫn vì ô bấm đầu luôn được bảo vệ. Đọc tiếp [19](19-luu-ket-qua-diem.md), [20](20-bang-xep-hang.md), [21](21-lich-su-tran.md) để theo dõi kết quả từ server tới database và giao diện.

[Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

- [Khởi động và kết nối TCP](01-ket-noi-tcp.md)
- [Gửi, nhận và phân loại message](02-gui-nhan-message.md)
- [Đăng ký tài khoản](03-dang-ky.md)
- [Đăng nhập](04-dang-nhap.md)
- [Danh sách người chơi online](05-danh-sach-online.md)
- [Mời thách đấu, chấp nhận, từ chối và hết hạn](06-moi-thach-dau.md)
- [Ghép trận nhanh và hủy tìm](07-ghep-tran-nhanh.md)
- [Bắt đầu ván và sinh mìn ở lần mở đầu](08-tao-van-sinh-min.md)
- [Mở ô và mở lan BFS](09-mo-o-va-bfs.md)
- [Cắm cờ](10-cam-co.md)
- [Gỡ cờ](11-go-co.md)
- [Thắng hoặc thua khi mở ô](12-thang-thua-mo-o.md)
- [Hết thời gian thi đấu](13-het-gio.md)
- [Đầu hàng](14-dau-hang.md)
- [Chơi lại](15-choi-lai.md)
- [Mất kết nối và chờ 15 giây](16-mat-ket-noi.md)
- [Khôi phục sau kết nối lại](17-ket-noi-lai.md)
- [Rời phòng và đăng xuất](18-roi-phong-dang-xuat.md)
- [Lưu kết quả và cập nhật điểm](19-luu-ket-qua-diem.md)
- [Bảng xếp hạng](20-bang-xep-hang.md)
- [Lịch sử trận đấu](21-lich-su-tran.md)
