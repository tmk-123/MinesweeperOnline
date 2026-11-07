# Bảng xếp hạng

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Người dùng yêu cầu bảng xếp hạng ở sảnh.

## Luồng tổng quát

```text
GET_LEADERBOARD → UserDAO.getLeaderboard → LEADERBOARD → MainMenuFrame
```

## Các bước và code

### Bước 1. Tạo yêu cầu.

**Code:** [JsonProtocol.java — dòng 210](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L210). Tìm `public static Message createGetLeaderboard()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static Message createGetLeaderboard() {
    return new Message(MessageType.GET_LEADERBOARD);
}
```

### Bước 2. Lấy top 10 và đóng gói dữ liệu thống kê.

**Code:** [ClientHandler.java — dòng 337](../../src/minesweeperonline/server/ClientHandler.java#L337). Tìm `private void handleGetLeaderboard(`.

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

**Code:** [MainMenuFrame.java — dòng 385](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L385). Tìm `private void handleLeaderboard(`.

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
    if (leaderboardDialog != null && leaderboardDialog.isDisplayable()) {
        leaderboardDialog.updateLeaderboard(list);
```

## Điểm cần nắm

DAO có nhánh fallback phía sau; xem toàn hàm nếu cần giải thích trường hợp SQL lỗi.

## Trả lời khi bảo vệ

> Server lấy top người chơi và gửi dữ liệu cho client hiển thị.
