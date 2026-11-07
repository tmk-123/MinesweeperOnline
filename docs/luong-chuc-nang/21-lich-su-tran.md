# Lịch sử trận đấu

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Người dùng yêu cầu lịch sử của mình.

## Luồng tổng quát

```text
GET_MATCH_HISTORY → handler → DAO → MATCH_HISTORY → sảnh
```

## Các bước và code

### Bước 1. Tạo yêu cầu với giới hạn số bản ghi.

**Code:** [JsonProtocol.java — dòng 214](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L214). Tìm `public static Message createGetMatchHistory(int limit)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public static Message createGetMatchHistory(int limit) {
    Message msg = new Message(MessageType.GET_MATCH_HISTORY);
    JsonObject p = new JsonObject();
    p.addProperty("limit", limit);
    msg.setPayload(p);
    return msg;
}
```

### Bước 2. Dùng userId đã đăng nhập, giới hạn trong 1–100, mặc định 20.

**Code:** [ClientHandler.java — dòng 361](../../src/minesweeperonline/server/ClientHandler.java#L361). Tìm `private void handleGetMatchHistory(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
private void handleGetMatchHistory(Message message) {
    if (user == null) return;
    int limit = 20;
    if (message.getPayload() != null && message.getPayload().has("limit")) {
        limit = message.getPayload().get("limit").getAsInt();
    }
    limit = Math.max(1, Math.min(limit, 100));
    MatchPlayerDAO matchPlayerDAO = new MatchPlayerDAO();
    List<MatchHistoryEntry> list = matchPlayerDAO.getMatchHistory(user.getId(), limit);

    Message res = new Message(MessageType.MATCH_HISTORY);
    res.setRequestId(message.getRequestId());
    JsonObject payload = new JsonObject();
    payload.add("history", JsonProtocol.getGson().toJsonTree(list));
    res.setPayload(payload);
    sendMessage(res);
}
```

### Bước 3. DAO lấy lịch sử; xem toàn hàm cho nhánh fallback.

**Code:** [MatchPlayerDAO.java — dòng 171](../../src/minesweeperonline/database/MatchPlayerDAO.java#L171). Tìm `public List<MatchHistoryEntry> getMatchHistory(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public List<MatchHistoryEntry> getMatchHistory(int userId, int limit) {
    List<MatchHistoryEntry> list = new ArrayList<>();
    String sql = "SELECT " +
            "m.id AS matchId, " +
            "COALESCE(opp_u.username, 'Đối thủ') AS opponentName, " +
            "mp.result AS result, " +
            "mp.score AS scoreDelta, " +
            "mp.openedSafeCells AS openedSafeCells, " +
            "mp.flagsPlaced AS flagsPlaced, " +
            "mp.totalActions AS totalActions, " +
            "m.startedAt AS startedAt, " +
            "m.endedAt AS endedAt, " +
            "m.result AS outcomeReason " +
            "FROM `Match` m " +
            "JOIN `MatchPlayer` mp ON m.id = mp.matchId " +
            "LEFT JOIN `MatchPlayer` opp_mp ON m.id = opp_mp.matchId AND opp_mp.userId <> mp.userId " +
            "LEFT JOIN `User` opp_u ON opp_mp.userId = opp_u.id " +
            "WHERE mp.userId = ? " +
            "ORDER BY m.startedAt DESC, m.createdAt DESC " +
            "LIMIT ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, userId);
        ps.setInt(2, limit);
        try (ResultSet rs = ps.executeQuery()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            while (rs.next()) {
                String matchId = rs.getString("matchId");
                String oppName = rs.getString("opponentName");
```

### Bước 4. Client hiển thị lịch sử nhận được.

**Code:** [MainMenuFrame.java — dòng 422](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L422). Tìm `private void handleMatchHistory(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
    private void handleMatchHistory(Message message) {
        if (matchHistoryDialog != null && matchHistoryDialog.isDisplayable()) {
            matchHistoryDialog.updateHistoryFromMessage(message);
        }
    }
}
```

## Điểm cần nắm

Handler lấy userId từ kết nối, không lấy một userId tùy chọn do client gửi.

## Trả lời khi bảo vệ

> Server lấy lịch sử tài khoản đã đăng nhập và trả số bản ghi trong giới hạn cho phép.
