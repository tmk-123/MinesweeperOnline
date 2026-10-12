# Tài liệu Server & Network — Minesweeper Online

Bộ tài liệu này giải thích phần **Server** và **Network** của dự án: từ bức tranh tổng thể, đến từng class, rồi từng chức năng chạy qua những dòng code nào.

> Mọi mô tả ở đây được đối chiếu trực tiếp với mã nguồn hiện tại. Nếu sửa code làm thay đổi hành vi, hãy sửa lại tài liệu tương ứng.

---

## Lộ trình đọc (từ khái quát → cụ thể)

| # | Tài liệu | Trả lời câu hỏi |
|---|---|---|
| 1 | [Tổng quan hệ thống](01-tong-quan-he-thong.md) | Hệ thống gồm những khối nào, ai nói chuyện với ai, package nào làm gì? |
| 2 | [Kiến thức mạng nền tảng](02-kien-thuc-mang-nen-tang.md) | TCP, Socket, stream, blocking, framing được dùng trong code như thế nào? |
| 3 | [Giao thức (Protocol)](03-giao-thuc.md) | Hai bên gửi cho nhau cái gì? Danh sách đầy đủ message + payload thật. |
| 4 | [Kiến trúc Server](04-kien-truc-server.md) | Từng class phía server làm gì, sống bao lâu, giữ dữ liệu gì? |
| 5 | [Luồng từng chức năng](05-luong-chuc-nang.md) | Đăng nhập, thách đấu, mở ô, kết thúc trận, chơi lại… chạy qua hàm nào? |
| 6 | [Trạng thái & đa luồng](06-trang-thai-va-da-luong.md) | Có những thread nào? Trạng thái người chơi/trận đấu chuyển ra sao? Khóa ở đâu? |
| 7 | [Xử lý lỗi & ngắt kết nối](07-xu-ly-loi-va-ngat-ket-noi.md) | Rớt mạng, gửi rác, MySQL tắt… thì hệ thống phản ứng thế nào? |
| 8 | [Tầng mạng phía Client](08-tang-mang-client.md) | Client kết nối, nhận tin, cập nhật giao diện ra sao? |
| 9 | [Chạy & kiểm thử](09-chay-va-kiem-thu.md) | Build, chạy server + 2 client (cùng máy hoặc qua LAN), chạy test. |
| 10 | [Ôn tập vấn đáp](10-on-tap-van-dap.md) | Câu hỏi thường gặp khi bảo vệ + các giới hạn còn tồn tại. |

---

## Bản đồ một trang

```text
 ┌──────────── CLIENT (mỗi người chơi 1 tiến trình) ────────────┐
 │  Swing UI (LoginFrame, MainMenuFrame, GameFrame, ...)         │
 │        │ gọi sendMessage()          ▲ listener + invokeLater   │
 │        ▼                            │                          │
 │  ServerConnection  ── 1 Socket ── thread "Client-Network-Listener"
 └────────────────────────────┬─────────────────────────────────┘
                              │ TCP, cổng 2209
                              │ mỗi message = 1 dòng JSON + '\n'
 ┌────────────────────────────▼─────────────────────────────────┐
 │ SERVER                                                        │
 │  MainServer  ── accept() ──► new Thread(ClientHandler) / client│
 │                                                               │
 │  ClientHandler  (đọc dòng → Message → switch theo type)       │
 │     ├─ ClientManager : ai đang online (userId → handler)      │
 │     ├─ GameManager   : ai đang ở trận nào (userId → gameId)   │
 │     └─ GameSession   : 1 trận = 2 Board + đồng hồ + luật       │
 │                           └─ game/ (Board, MineGenerator, GameRules)
 │  database/ (UserDAO, MatchPlayerDAO) ── JDBC ──► MySQL         │
 └───────────────────────────────────────────────────────────────┘
```

**Ba ý quan trọng nhất cần nhớ:**

1. **Server-authoritative**: mìn, kết quả mở ô, thời gian, thắng/thua, điểm đều do server quyết định. Client chỉ gửi yêu cầu và vẽ lại.
2. **Thread-per-client**: mỗi client có một `ClientHandler` chạy trên thread riêng, chặn ở `readLine()`.
3. **Framing bằng `\n`**: TCP là dòng byte liên tục, nên mỗi message là đúng một dòng JSON và bên nhận đọc bằng `readLine()`.

---

## Tài liệu khác trong thư mục `docs/`

- `mo_ta.txt` / `mo_ta.docx`: đặc tả yêu cầu của đề bài (luật chơi, điểm, trạng thái). Khi phân vân "đúng hay sai", hãy đối chiếu với file này.
- `kien_truc.txt` / `kien_truc.docx`: tài liệu kiến trúc chung của nhóm.
