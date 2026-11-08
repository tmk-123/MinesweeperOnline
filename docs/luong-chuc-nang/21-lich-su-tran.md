# Lịch sử trận đấu

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Tình huống cụ thể

A bấm Lịch sử đấu. Trận gần nhất a1b2c3d4 kéo dài 52 giây, A thắng B, mở 23 ô, cắm cờ 1 lần, tổng 2 thao tác.

## Hàm nào gọi hàm nào, ở đâu?

| Bước | Vị trí | Hàm / dấu mốc | Dữ liệu và việc thực hiện |
|---|---|---|---|
| 1 | [MatchHistoryDialog.java:179](../../src/minesweeperonline/client/ui/MatchHistoryDialog.java#L179) | `public void refresh()` | Khi mở hoặc bấm Làm mới, gọi createGetMatchHistory(50), không dùng giới hạn mặc định 20. |
| 2 | [ClientHandler.java:372](../../src/minesweeperonline/server/ClientHandler.java#L372) | `private void handleGetMatchHistory(` | Lấy userId=101 từ tài khoản đăng nhập, chặn limit trong 1–100 rồi gọi DAO. |
| 3 | [MatchPlayerDAO.java:171](../../src/minesweeperonline/database/MatchPlayerDAO.java#L171) | `public List<MatchHistoryEntry> getMatchHistory(` | JOIN dữ liệu của A với Match/đối thủ, lấy trận A tham gia và sắp trận mới nhất trước. |
| 4 | [ClientHandler.java:385](../../src/minesweeperonline/server/ClientHandler.java#L385) | `payload.add("history", JsonProtocol.getGson().toJsonTree(list))` | Đưa List<MatchHistoryEntry> vào payload.history rồi gửi MATCH_HISTORY. |
| 5 | [MainMenuFrame.java:416](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L416) | `private void handleMatchHistory(` | Chuyển message tới hộp thoại lịch sử nếu cửa sổ còn mở. |
| 6 | [MatchHistoryDialog.java:191](../../src/minesweeperonline/client/ui/MatchHistoryDialog.java#L191) | `public void updateHistoryFromMessage(` | Gson chuyển từng bản ghi thành MatchHistoryEntry, gọi updateHistory. |
| 7 | [MatchHistoryDialog.java:213](../../src/minesweeperonline/client/ui/MatchHistoryDialog.java#L213) | `public void updateHistory(` | Hiện điểm/thời lượng/kết quả; tiêu đề Ô an toàn /121, dòng chỉ hiện 23. Tổng kết tính trên các dòng vừa tải. |

## Message và kết quả trong tình huống này

A → server:

```json
{
  "type": "GET_MATCH_HISTORY",
  "payload": {
    "limit": 50
  }
}
```

Server → A:

```json
{
  "type": "MATCH_HISTORY",
  "payload": {
    "history": [
      {
        "matchId": "a1b2c3d4",
        "opponentName": "binh_demo",
        "result": "VICTORY",
        "scoreDelta": 5,
        "openedSafeCells": 23,
        "flagsPlaced": 1,
        "totalActions": 2,
        "durationSeconds": 52,
        "playedAt": "08/11/2026 10:00:00",
        "outcomeReason": "CLICKED_MINE"
      }
    ]
  }
}
```

Ghi chú Mở trúng mìn lấy nguyên nhân chung của trận: A thắng thì người mở mìn là B, không phải A. openedSafeCells tính cả ô mở lan, không phải số click. Tổng kết lịch sử tính tối đa 50 dòng hiện tải nên có thể khác số trận/tỷ lệ thắng trên bảng xếp hạng tính toàn bộ trận.

Các JSON minh họa được xuống dòng để dễ đọc; trên socket, mỗi gói là **một dòng UTF-8 kết thúc bằng `\n`**. ID, mã trận `a1b2c3d4`, vị trí mìn và số liệu là ví dụ, không phải dữ liệu thật. Client không tự sinh requestId cho những thao tác này; Gson bỏ các trường đang null.

## Các bước và code

### Bước 1. Tạo yêu cầu với giới hạn số bản ghi.

**Code:** [JsonProtocol.java — dòng 235](../../src/minesweeperonline/common/protocol/JsonProtocol.java#L235). Tìm `public static Message createGetMatchHistory(int limit)`.

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

**Code:** [ClientHandler.java — dòng 372](../../src/minesweeperonline/server/ClientHandler.java#L372). Tìm `private void handleGetMatchHistory(`.

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

**Code:** [MainMenuFrame.java — dòng 416](../../src/minesweeperonline/client/ui/MainMenuFrame.java#L416). Tìm `private void handleMatchHistory(`.

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
