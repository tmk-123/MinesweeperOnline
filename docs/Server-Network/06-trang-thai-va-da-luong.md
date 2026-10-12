# 6. Trạng thái & đa luồng

## 6.1. Server có những thread nào?

| Thread | Số lượng | Làm gì |
|---|---|---|
| `main` | 1 | `MainServer.start()`: vòng lặp `accept()` |
| `Client-<port>` | 1 / kết nối | `ClientHandler.run()`: đọc và xử lý message của client đó |
| `pool-N-thread-1` | 1 / trận đang diễn ra | `GameSession.timerScheduler`: đếm READY 5s, tick 300s, gọi `handleTimeout()` |
| `Challenge-Timer` | 1 (dùng chung, daemon) | `ClientHandler.expireChallenge`: hủy lời mời quá 30s |
| shutdown hook | 1 | gọi `MainServer.stop()` khi tắt chương trình |

Ví dụ 2 người đang đấu: `main` + 2 `ClientHandler` + 1 timer = **4 thread**. Ba thread cuối **cùng chạm vào một `GameSession`**, nên cần đồng bộ.

## 6.2. Dữ liệu dùng chung và cách bảo vệ

| Dữ liệu | Ai đọc/ghi | Bảo vệ bằng |
|---|---|---|
| `ClientManager.allClients`, `onlineUsers` | mọi `ClientHandler` | `CopyOnWriteArrayList`, `ConcurrentHashMap` |
| `GameManager.sessions`, `userToGameMap` | mọi `ClientHandler`, `GameSession` | `ConcurrentHashMap` |
| `Player.status` + `pendingTargetId`/`pendingChallengerId`/`pendingChallengeToken` (sảnh) | `ClientHandler` của cả hai người + `Challenge-Timer` | `synchronized (clientManager)` |
| `ClientHandler.user` / `player` | thread của chính client (ghi), thread khác (đọc) | `volatile`; khi `LOGOUT` thì gán `null` trong `synchronized (clientManager)` |
| Trạng thái trận (`state`, bàn cờ, thống kê) | 2 `ClientHandler` + thread đồng hồ | `synchronized (gameLock)` |
| "Trận đã kết thúc chưa" | mọi nguồn kết thúc trận | `AtomicBoolean finished` + `compareAndSet` (chỉ kết thúc 1 lần) |
| Ghi vào một socket | bất kỳ thread nào gửi cho client đó | `synchronized sendMessage()` |

**Thứ tự khóa (tránh deadlock):** `clientManager` → (tạo session), hoặc `gameLock` → `handler.sendMessage`. Không có đường nào giữ khóa của handler rồi mới xin `gameLock` hay `clientManager`, nên không thể có vòng chờ.

**Vì sao `finishGame` cần `AtomicBoolean` khi đã có `gameLock`?** Vì nó được gọi cả từ những chỗ **không** giữ `gameLock`: `handleForfeit` và `cleanup` trong `ClientHandler`. `compareAndSet(false, true)` là một phép "kiểm tra và đặt" nguyên tử, nên chỉ đúng một thread vượt qua được.

## 6.3. Máy trạng thái của người chơi (`Player.status`)

```text
                 LOGIN thành công
                        │
                        ▼
                 ┌──────────────┐  CHALLENGE (mời / bị mời)   ┌──────────────┐
         ┌──────►│     IDLE     │────────────────────────────►│ CHALLENGING  │
         │       └──────────────┘◄────────────────────────────└──────┬───────┘
         │              ▲   REJECT / hết 30s / bên kia rời/LOGOUT   │ ACCEPT
         │              │                                            ▼
         │              │                                    ┌──────────────┐
         │              └──── EXIT / rời mạng ───────────────│   PLAYING    │◄─┐
         │                                                   └──────┬───────┘  │ PLAY_AGAIN
         │                                                          └──────────┘ (cả hai)
         └── (người còn lại khi đối thủ EXIT cũng về IDLE)
```

- `PLAYING` bao gồm cả READY, đang chơi và **phòng kết quả**. Nhờ vậy trong lúc xem kết quả, người khác không mời được mình.
- Danh sách online hiển thị đúng 3 giá trị này.

## 6.4. Máy trạng thái của trận (`GameSession.state`)

```text
 new GameSession ─► READY ──5s──► PLAYING ──(trúng mìn | đủ 121 ô | hết giờ | đầu hàng | rớt mạng)──► FINISHED
                      │                                                                          │
                      └──────────── đầu hàng / rớt mạng trong READY ─────────────────────────────┘
 FINISHED ─► (cả hai PLAY_AGAIN) session mới   |   (EXIT hoặc rớt mạng) removeSession
```

Mọi thao tác chơi chỉ được nhận khi `state == PLAYING` và `!finished`.

## 6.5. Phía client (tóm tắt, chi tiết ở [08](08-tang-mang-client.md))

| Thread | Làm gì |
|---|---|
| `Client-Network-Listener` (daemon) | chặn ở `readLine()`, nhận message rồi gọi các listener |
| Event Dispatch Thread (EDT) của Swing | vẽ giao diện, xử lý click, chạy `javax.swing.Timer` |

**Quy tắc vàng của Swing:** chỉ được đụng vào giao diện trên EDT. Vì vậy mọi listener đều bọc `SwingUtilities.invokeLater(() -> ...)` để chuyển việc cập nhật UI từ thread mạng sang EDT.

## 6.6. Các điểm cần biết (giới hạn hiện tại)

- Các trường bên trong `Player` (như `status`) không dùng `volatile`. Khi `getOnlinePlayers()` đọc `status` mà không khóa, có thể thấy giá trị **cũ trong chốc lát**. Điều này chỉ ảnh hưởng hiển thị; các quyết định quan trọng (mời, chấp nhận, hết hạn) đều đọc trong khóa.
- `GameSession` gửi tin (ghi socket) **trong lúc giữ `gameLock`**. Nếu một client bị treo không đọc dữ liệu, bộ đệm TCP đầy làm `print()` chặn, và trận đó bị khựng theo. Với quy mô bài tập thì chấp nhận được.
