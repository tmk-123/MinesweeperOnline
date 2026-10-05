# Chương 05: Giao Thức Mạng & Cấu Trúc Thông Điệp (Protocol & Message)

Chương này giải thích ngôn ngữ chung mà Client và Server sử dụng để nói chuyện với nhau: **Chuẩn gói tin JSON ngắt dòng bằng `\n` (Newline-delimited JSON)**.

---

## 1. Vấn Đề "Dính Gói" Trong TCP & Giải Pháp Của Dự Án

### 1.1. Bản chất TCP là dòng chảy (Stream)
Trong mạng TCP, dữ liệu không được gửi thành từng lá thư rời rạc mà là một **dòng chảy liên tục các byte**.
* **Hiện tượng Dính gói (Packet Sticking)**: Nếu Client gửi 2 gói tin liên tiếp:
  Gói 1: `{"type":"LOGIN"}`
  Gói 2: `{"type":"GET_ONLINE"}`
  Ở phía Server, do bộ đệm mạng gom lại, Server có thể đọc được nguyên một chuỗi dính liền:
  `{"type":"LOGIN"}{"type":"GET_ONLINE"}` -> **Lỗi giải mã JSON ngay lập tức!**

### 1.2. Giải pháp: Ký tự phân cách `\n` (Newline-delimited Framing)
Dự án giải quyết triệt để vấn đề này trong file [JsonProtocol.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/JsonProtocol.java):
* **Phía gửi**: Luôn ép ký tự xuống dòng `\n` vào đuôi mỗi gói tin:
  ```java
  public static String serializeWithNewline(Message message) {
      return serialize(message) + "\n";
  }
  ```
* **Phía nhận**: Dùng `reader.readLine()` để chỉ đọc đúng một dòng cho đến khi gặp `\n`:
  Mỗi lần `readLine()` trả về chính xác 100% một gói tin JSON hoàn chỉnh, không bao giờ bị dính gói!

---

## 2. Cấu Trúc Khung Phong Bì Gói Tin ([Message.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/Message.java))

Mọi gói tin truyền qua mạng đều tuân theo cấu trúc 4 trường cố định:

```json
{
  "type": "TÊN_LOẠI_THÔNG_ĐIỆP",
  "requestId": "MÃ_YÊU_CẦU_NẾU_CÓ",
  "gameId": "MÃ_BÀN_ĐẤU_NẾU_CÓ",
  "payload": {
    "DỮ LIỆU CHI TIẾT DƯỚI DẠNG ĐỐI TƯỢNG JSON"
  }
}
```

### Ý nghĩa từng trường:
1. **`type` (MessageType - Bắt buộc)**: Loại hành động (ví dụ: `LOGIN`, `OPEN_CELL`, `GAME_WIN`). Phía nhận sẽ nhìn vào trường này để điều phối đến hàm xử lý thích hợp.
2. **`requestId` (String - Tùy chọn)**: Mã định danh yêu cầu, dùng khi muốn ghép cặp chính xác Request nào sinh ra Response nào.
3. **`gameId` (String - Tùy chọn)**: Mã ván đấu (ví dụ: `4d0b4a22`). Dùng khi trận đấu đang diễn ra để Server biết hành động này thuộc bàn chơi nào.
4. **`payload` (JsonObject - Tùy biến linh hoạt)**: Thân gói tin chứa dữ liệu thực tế (tên đăng nhập, mật khẩu, tọa độ hàng/cột, danh sách đối thủ...).

---

## 3. Danh Mục Các Gói Tin Tiêu Biểu Trong Dự Án

### 3.1. Đăng Nhập (Login)
* **Client gửi Request**:
  ```json
  {
    "type": "LOGIN",
    "requestId": "REQ-101",
    "payload": {
      "username": "alice",
      "password": "123"
    }
  }
  ```
* **Server trả Response thành công**:
  ```json
  {
    "type": "LOGIN_SUCCESS",
    "requestId": "REQ-101",
    "payload": {
      "userId": 1,
      "username": "alice",
      "totalScore": 25
    }
  }
  ```

---

### 3.2. Thách Đấu (Challenge)
* **Client A gửi lời mời thách đấu Client B (userId=2)**:
  ```json
  {
    "type": "CHALLENGE",
    "payload": {
      "targetUserId": 2,
      "targetUsername": "bob"
    }
  }
  ```
* **Server chuyển tiếp lời mời đến màn hình của Bob (`CHALLENGE_REQUEST`)**:
  ```json
  {
    "type": "CHALLENGE_REQUEST",
    "payload": {
      "fromUserId": 1,
      "fromUsername": "alice",
      "timeoutSeconds": 30
    }
  }
  ```
* **Bob đồng ý (`ACCEPT`)**:
  ```json
  {
    "type": "ACCEPT",
    "payload": {
      "challengerId": 1
    }
  }
  ```
* **Server tạo trận và phát cho cả 2 bên (`MATCH_CREATED`)**:
  ```json
  {
    "type": "MATCH_CREATED",
    "gameId": "4d0b4a22",
    "payload": {
      "gameId": "4d0b4a22",
      "opponentId": 2,
      "opponentName": "bob",
      "opponentScore": 15,
      "readyCountdownSeconds": 5
    }
  }
  ```

---

### 3.3. Trong Ván Đấu: Mở Ô & Cắm Cờ
* **Client gửi lệnh mở ô (hàng 3, cột 5)**:
  ```json
  {
    "type": "OPEN_CELL",
    "gameId": "4d0b4a22",
    "payload": {
      "row": 3,
      "col": 5
    }
  }
  ```
* **Server trả kết quả mở ô về cho chính người chơi (`CELL_OPENED`)**:
  * Nếu ô an toàn và xung quanh có ô 0, thuật toán BFS mở rộng nhiều ô cùng lúc:
  ```json
  {
    "type": "CELL_OPENED",
    "gameId": "4d0b4a22",
    "payload": {
      "openedCells": [
        {"row": 3, "col": 5, "adjacentMines": 0, "isMine": false},
        {"row": 3, "col": 4, "adjacentMines": 1, "isMine": false}
      ],
      "openedSafeCells": 2,
      "totalActions": 1
    }
  }
  ```
* **Server đồng bộ tiến độ sang bàn cờ nhỏ của đối thủ (`GAME_UPDATE`)**:
  * Lưu ý: **Server tuyệt đối không gửi vị trí mìn**, chỉ gửi số lượng ô an toàn đã mở để chống gian lận!
  ```json
  {
    "type": "GAME_UPDATE",
    "gameId": "4d0b4a22",
    "payload": {
      "opponentOpenedCells": [
        {"row": 3, "col": 5},
        {"row": 3, "col": 4}
      ],
      "opponentTotalSafeOpened": 2,
      "opponentFlags": 0
    }
  }
  ```

---

### 3.4. Bảng Xếp Hạng & Lịch Sử Đấu
* **Client xin Bảng xếp hạng**: `{"type": "GET_LEADERBOARD"}`
* **Server trả về (`LEADERBOARD`)**:
  ```json
  {
    "type": "LEADERBOARD",
    "payload": {
      "leaderboard": [
        {"rank": 1, "userId": 1, "username": "alice", "totalScore": 30, "wins": 3, "totalMatches": 3, "winRate": 100.0},
        {"rank": 2, "userId": 2, "username": "bob", "totalScore": 12, "wins": 1, "totalMatches": 3, "winRate": 33.3}
      ]
    }
  }
  ```

---

## 4. Xử Lý Khi Nhận Gói Tin Lỗi (Malformed JSON)

Trong [JsonProtocol.java](file:///d:/Ki1_4/LTM/MinesweeperOnline/src/minesweeperonline/common/protocol/JsonProtocol.java#L37):
```java
public static Message deserialize(String json) {
    if (json == null || json.trim().isEmpty()) {
        return null;
    }
    try {
        return gson.fromJson(json, Message.class);
    } catch (JsonSyntaxException e) {
        System.err.println("[JsonProtocol] Failed to parse JSON: " + json + " (" + e.getMessage() + ")");
        return null;
    }
}
```
Nếu có kẻ xấu cố tình chèn dữ liệu rác hoặc đường truyền mạng làm rách chuỗi JSON, `gson.fromJson()` sẽ ném ra ngoại lệ `JsonSyntaxException`. Khối `catch` sẽ bắt lấy lỗi, ghi log cảnh báo và trả về `null`.
Nhờ vậy, cả Server và Client **không bao giờ bị crash** khi gặp gói tin dị tật!
