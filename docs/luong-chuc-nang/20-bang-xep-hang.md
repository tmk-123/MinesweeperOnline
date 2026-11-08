# Bảng xếp hạng

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A mở sảnh hoặc bấm Làm mới. Giả sử chỉ có A:60 điểm,3 thắng/5 trận và B:40 điểm,1 thắng/2 trận trong dữ liệu ví dụ.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [MainMenuFrame.java:48](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L48) | `autoRefreshTimer = new Timer(` | Khi sảnh hiện, mỗi 4 giây gọi refreshLeaderboard; nút Làm mới cũng gọi. |
| 2 | [LeaderboardPanel.java:172](../../src/minesweeperonline/client/ui/LeaderboardPanel.java#L172) | `public void refreshLeaderboard()` | Gửi GET_LEADERBOARD qua kết nối dùng chung. |
| 3 | [ClientHandler.java:347](../../src/minesweeperonline/server/ClientHandler.java#L347) | `private void handleGetLeaderboard(` | Gọi userDAO.getLeaderboard(10), giới hạn Top 10 do server quyết định. |
| 4 | [UserDAO.java:158](../../src/minesweeperonline/database/UserDAO.java#L158) | `public List<LeaderboardEntry> getLeaderboard(` | LEFT JOIN User/MatchPlayer, GROUP BY tài khoản, ORDER BY điểm/tỷ lệ thắng/số thắng/số trận/tên. |
| 5 | [ClientHandler.java:366](../../src/minesweeperonline/server/ClientHandler.java#L366) | `payload.add("leaderboard", array)` | Mỗi dòng có rank,userId,username,totalScore,wins,totalMatches,winRate; gửi LEADERBOARD. |
| 6 | [MainMenuFrame.java:380](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L380) | `private void handleLeaderboard(` | Giải mã thành LeaderboardEntry, gọi leaderboardPanel.updateLeaderboard(list). |
| 7 | [LeaderboardPanel.java:180](../../src/minesweeperonline/client/ui/LeaderboardPanel.java#L180) | `public void updateLeaderboard(` | Đưa lên EDT, thay dòng bảng và tính cấp bậc theo điểm. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "GET_LEADERBOARD",
  "payload": {}
}
```

Server → A:

```json
{
  "type": "LEADERBOARD",
  "payload": {
    "leaderboard": [
      {
        "rank": 1,
        "userId": 101,
        "username": "an_demo",
        "totalScore": 60,
        "wins": 3,
        "totalMatches": 5,
        "winRate": 60.0
      },
      {
        "rank": 2,
        "userId": 102,
        "username": "binh_demo",
        "totalScore": 40,
        "wins": 1,
        "totalMatches": 2,
        "winRate": 50.0
      }
    ]
  }
}
```

winRate trong message là **phần trăm**: 3/5 thành 60.0. Bảng bao gồm cả tài khoản offline. Hiện chỉ giữ bảng trong sảnh; nút mở LeaderboardDialog đã bỏ. Hòa tăng 1 điểm, tăng số trận nhưng không tăng số thắng. Nếu có hơn 10 người, hạng 11 trở xuống không được gửi ở yêu cầu này.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Tạo yêu cầu.

**Code:** [JsonProtocol.java — dòng 230](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L230). Tìm `public static Message createGetLeaderboard()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static Message createGetLeaderboard() {
    return new Message(MessageType.GET_LEADERBOARD);
}
```

### Bước 2. Lấy top 10 và đóng gói dữ liệu thống kê.

**Code:** [ClientHandler.java — dòng 347](../../src/minesweeperonline/server/ClientHandler.java#L347). Tìm `private void handleGetLeaderboard(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleGetLeaderboard(Message message) {
    List<LeaderboardEntry> list = userDAO.getLeaderboard(10);
    Message res = new Message(MessageType.LEADERBOARD);
    res.setRequestId(message.getRequestId());
    JsonObject payload = new JsonObject();
    JsonArray array = new JsonArray();

    for (LeaderboardEntry e : list) {
        JsonObject item = new JsonObject();
        item.addProperty("rank", e.getRank());
        item.addProperty("userId", e.getUserId());
        item.addProperty("username", e.getUsername());
        item.addProperty("totalScore", e.getTotalScore());
        item.addProperty("wins", e.getWins());
        item.addProperty("totalMatches", e.getTotalMatches());
        item.addProperty("winRate", e.getWinRate());
        array.add(item);
    }

    payload.add("leaderboard", array);
    res.setPayload(payload);
    sendMessage(res);
}
```

### Bước 3. DAO truy vấn bảng xếp hạng; xem ORDER BY để biết thứ tự chính xác.

**Code:** [UserDAO.java — dòng 158](../../src/minesweeperonline/database/UserDAO.java#L158). Tìm `public List<LeaderboardEntry> getLeaderboard(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public List<LeaderboardEntry> getLeaderboard(int limit) {
    List<LeaderboardEntry> list = new ArrayList<>();
    String sql = "SELECT u.id, u.username, u.totalScore, " +
            "COUNT(CASE WHEN mp.result = 'VICTORY' THEN 1 END) AS wins, " +
            "COUNT(mp.id) AS totalMatches, " +
            "(CASE WHEN COUNT(mp.id) > 0 THEN COUNT(CASE WHEN mp.result = 'VICTORY' THEN 1 END) * 1.0 / COUNT(mp.id) ELSE 0 END) AS winRate " +
            "FROM `User` u " +
            "LEFT JOIN `MatchPlayer` mp ON u.id = mp.userId " +
            "GROUP BY u.id, u.username, u.totalScore " +
            "ORDER BY u.totalScore DESC, winRate DESC, wins DESC, totalMatches DESC, u.username ASC LIMIT ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, limit);
        try (ResultSet rs = ps.executeQuery()) {
            int rank = 1;
            while (rs.next()) {
                int id = rs.getInt("id");
                String uname = rs.getString("username");
                int score = rs.getInt("totalScore");
                int wins = rs.getInt("wins");
                int matches = rs.getInt("totalMatches");
                list.add(new LeaderboardEntry(rank++, id, uname, score, wins, matches));
            }
            return list;
        }
    } catch (SQLException e) {
        // Bảng xếp hạng fallback lấy từ bộ nhớ trong
        List<User> allUsers = new ArrayList<>(fallbackUsers.values());
```

### Bước 4. Client hiển thị kết quả.

**Code:** [MainMenuFrame.java — dòng 380](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L380). Tìm `private void handleLeaderboard(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleLeaderboard(Message message) {
    JsonObject payload = message.getPayload();
    List<LeaderboardEntry> list = new ArrayList<>();
    if (payload != null && payload.has("leaderboard") && payload.get("leaderboard").isJsonArray()) {
        JsonArray arr = payload.getAsJsonArray("leaderboard");
        for (JsonElement el : arr) {
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                int rank = o.get("rank").getAsInt();
                int userId = o.get("userId").getAsInt();
                String uname = o.get("username").getAsString();
                int score = o.get("totalScore").getAsInt();
                int wins = o.get("wins").getAsInt();
                int matches = o.get("totalMatches").getAsInt();
                list.add(new LeaderboardEntry(rank, userId, uname, score, wins, matches));
            }
        }
    }

    if (leaderboardPanel != null) {
        leaderboardPanel.updateLeaderboard(list);
    }
}
```

## Điểm cần nắm

DAO có nhánh fallback phía sau; xem toàn hàm nếu cần giải thích trường hợp SQL lỗi.

## Trả lời khi bảo vệ

> Server lấy top người chơi và gửi dữ liệu cho client hiển thị.
