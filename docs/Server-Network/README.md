# Tài Liệu Server & Network - Minesweeper Online

Chào mừng bạn đến với bộ tài liệu kỹ thuật chi tiết về tầng **Server & Mạng (Networking)** của dự án Game Dò Mìn Thi Đấu Đối Kháng Trực Tuyến (*Minesweeper Online*).

Bộ tài liệu này được biên soạn dành riêng cho người mới học Lập trình Mạng, giải thích trực quan từ những khái niệm nền tảng nhất cho đến cách code thực tế trong từng file Java của dự án.

---

## Danh Sách Bài Học / Mục Lục

| STT | Tên Tài Liệu | Nội Dung Chính |
| :---: | :--- | :--- |
| **00** | [00. Tổng quan mạng](00-overview.md) | Khái niệm Server, Client, Network, Socket, TCP/UDP, mô hình dự án |
| **01** | [01. Kiến trúc hệ thống](01-architecture.md) | Sơ đồ kiến trúc tổng thể, mô hình Server-Authoritative, kết nối DB |
| **02** | [02. Phân tích Server](02-server.md) | Phân tích sâu `MainServer`, `ClientHandler`, `ClientManager`, `GameSession` |
| **03** | [03. Phân tích Client](03-client.md) | Phân tích `MainClient`, `ServerConnection`, luồng lắng nghe bất đồng bộ |
| **04** | [04. Bản chất Network trong Code](04-network.md) | Giải thích `ServerSocket`, `Socket`, `InputStream`, `BufferedReader`, `PrintWriter` |
| **05** | [05. Giao thức Protocol & Message](05-protocol.md) | Cấu trúc gói tin JSON, cơ chế Newline-delimited (`\n`), danh mục `MessageType` |
| **06** | [06. Giải thích chi tiết từng File](06-files.md) | Bảng tổng hợp vai trò, ai gọi, gọi ai, dữ liệu đi qua cho từng file |
| **07** | [07. Luồng dữ liệu (Data Flow)](07-data-flow.md) | Truy vết luồng dữ liệu 7 kịch bản: Đăng nhập, Thách đấu, Đánh cờ, Kết thúc |
| **08** | [08. Xử lý lỗi & Ngoại lệ](08-error-handling.md) | Xử lý đứt mạng, đối thủ bỏ cuộc, timeout 5 phút, MySQL offline fallback |
| **09** | [09. Hướng dẫn chạy chương trình](09-how-to-run.md) | Cách build và chạy Server cùng 2 Client để chơi đối kháng thực tế |

---

## Kim Chỉ Nam Khi Đọc Tài Liệu

Khi tiếp cận một project Lập trình Mạng viết bằng Java, hãy luôn tự đặt ra 5 câu hỏi cốt lõi:
1. **Chương trình bắt đầu chạy từ đâu?** (Hàm `main` ở đâu?)
2. **Ai là người chủ động mở kết nối? Ai là người chờ lắng nghe?** (`Socket` vs `ServerSocket`)
3. **Hai bên nói chuyện với nhau bằng ngôn ngữ gì?** (Giao thức JSON kết thúc bằng `\n`)
4. **Khi một Client gửi tin nhắn, Server nhận và phân phát như thế nào?** (`ClientHandler` -> `GameSession` -> đối thủ)
5. **Điều gì xảy ra nếu mạng bị đứt hoặc lỗi phát sinh?** (Cơ chế Forfeit & In-memory Fallback)

Toàn bộ 10 chương tài liệu tiếp theo sẽ lần lượt giải đáp cặn kẽ 5 câu hỏi trên!
