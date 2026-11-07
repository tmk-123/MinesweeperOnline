# Lưu kết quả và cập nhật điểm

[Mục lục các luồng](README.md) · [Câu hỏi tổng hợp](../ON_TAP_VAN_DAP.md)

## Sự kiện bắt đầu

Một nhánh kết thúc gọi finishGame.

## Luồng tổng quát

```text
finishGame → saveMatchToDatabase → saveFullMatchTransaction → commit hoặc xử lý lỗi
```

## Các bước và code

### Bước 1. Chốt kết thúc một lần bằng compareAndSet.

**Code:** [GameSession.java — dòng 551](../../src/minesweeperonline/server/GameSession.java#L551). Tìm `public boolean finishGame(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public boolean finishGame(int winnerUserId, String reason) {
    if (finished.compareAndSet(false, true)) {
        stopTimers();
        synchronized (gameLock) {
            this.state = GameState.FINISHED;
            this.endedAt = System.currentTimeMillis();
            this.winnerUserId = winnerUserId;
            this.endReason = reason;
        }
        saveMatchToDatabase(winnerUserId, reason);
        return true;
    }
    return false;
```

### Bước 2. Tính kết quả và điểm mỗi người theo lý do kết thúc.

**Code:** [GameSession.java — dòng 475](../../src/minesweeperonline/server/GameSession.java#L475). Tìm `public void saveMatchToDatabase(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
public void saveMatchToDatabase(int winnerId, String reason) {
    String res1, res2;
    int delta1, delta2;

    if (winnerId == 0) {
        res1 = "DRAW";
        res2 = "DRAW";
        delta1 = ServerConfig.SCORE_DRAW;
        delta2 = ServerConfig.SCORE_DRAW;
    } else if (winnerId == player1.getUserId()) {
        res1 = "VICTORY";
        res2 = (reason != null && reason.contains("FORFEIT")) ? "FORFEIT" : "DEFEAT";
        delta1 = ServerConfig.SCORE_VICTORY;
        delta2 = (res2.equals("FORFEIT")) ? ServerConfig.SCORE_FORFEIT : ServerConfig.SCORE_DEFEAT;
    } else {
        res2 = "VICTORY";
        res1 = (reason != null && reason.contains("FORFEIT")) ? "FORFEIT" : "DEFEAT";
        delta2 = ServerConfig.SCORE_VICTORY;
        delta1 = (res1.equals("FORFEIT")) ? ServerConfig.SCORE_FORFEIT : ServerConfig.SCORE_DEFEAT;
    }

    // Giữ điểm trong bộ nhớ đồng bộ với DB (hiển thị ở danh sách người chơi online)
    player1.setTotalScore(Math.max(0, player1.getTotalScore() + delta1));
    player2.setTotalScore(Math.max(0, player2.getTotalScore() + delta2));

    MatchPlayerDAO dao = new MatchPlayerDAO();
    dao.saveFullMatchTransaction(
            gameId, "FINISHED", startedAt, System.currentTimeMillis(), (winnerId > 0 ? winnerId : null), reason,
            player1.getUserId(), player1.getUsername(), res1, board1.getOpenedSafeCells(), board1.getTotalActions(), board1.getFlagsPlaced(), delta1,
            player2.getUserId(), player2.getUsername(), res2, board2.getOpenedSafeCells(), board2.getTotalActions(), board2.getFlagsPlaced(), delta2
    );
}

```

### Bước 3. Truyền dữ liệu trận và thống kê hai người vào DAO.

**Code:** [GameSession.java — dòng 501](../../src/minesweeperonline/server/GameSession.java#L501). Tìm `dao.saveFullMatchTransaction(`.

Trích đoạn bắt đầu tại vị trí trên:

```java
dao.saveFullMatchTransaction(
        gameId, "FINISHED", startedAt, System.currentTimeMillis(), (winnerId > 0 ? winnerId : null), reason,
        player1.getUserId(), player1.getUsername(), res1, board1.getOpenedSafeCells(), board1.getTotalActions(), board1.getFlagsPlaced(), delta1,
        player2.getUserId(), player2.getUsername(), res2, board2.getOpenedSafeCells(), board2.getTotalActions(), board2.getFlagsPlaced(), delta2
);
```

### Bước 4. Transaction thêm Match, hai MatchPlayer và cập nhật điểm.

**Code:** [MatchPlayerDAO.java — dòng 99](../../src/minesweeperonline/database/MatchPlayerDAO.java#L99). Tìm `conn.setAutoCommit(false)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
conn.setAutoCommit(false); // Bắt đầu transaction

// 1. Thêm bản ghi Match
MatchDAO matchDAO = new MatchDAO();
matchDAO.insertMatch(conn, matchId, matchStatus, startedAt, endedAt, winnerId, outcomeDescription);

// 2. Thêm MatchPlayer của người chơi 1
insertMatchPlayer(conn, matchId, user1Id, res1, opened1, actions1, flags1, scoreDelta1);

// 3. Thêm MatchPlayer của người chơi 2
insertMatchPlayer(conn, matchId, user2Id, res2, opened2, actions2, flags2, scoreDelta2);

// 4. Cập nhật điểm User 1 (điểm không được âm: GREATEST(0, totalScore + delta))
String sqlScore = "UPDATE `User` SET totalScore = GREATEST(0, totalScore + ?) WHERE id = ?";
try (PreparedStatement ps1 = conn.prepareStatement(sqlScore)) {
    ps1.setInt(1, scoreDelta1);
    ps1.setInt(2, user1Id);
    ps1.executeUpdate();
}

// 5. Cập nhật điểm User 2
try (PreparedStatement ps2 = conn.prepareStatement(sqlScore)) {
    ps2.setInt(1, scoreDelta2);
    ps2.setInt(2, user2Id);
    ps2.executeUpdate();
}

conn.commit(); // Commit transaction
System.out.println("[MatchPlayerDAO] Transaction committed successfully for match: " + matchId);
recordFallbackMatchDetails(matchId, outcomeDescription, startedAt, endedAt,
```

### Bước 5. Commit và ghi dữ liệu fallback.

**Code:** [MatchPlayerDAO.java — dòng 126](../../src/minesweeperonline/database/MatchPlayerDAO.java#L126). Tìm `conn.commit()`.

Trích đoạn bắt đầu tại vị trí trên:

```java
conn.commit(); // Commit transaction
System.out.println("[MatchPlayerDAO] Transaction committed successfully for match: " + matchId);
recordFallbackMatchDetails(matchId, outcomeDescription, startedAt, endedAt,
        user1Id, user1Name, res1, opened1, actions1, flags1, scoreDelta1,
        user2Id, user2Name, res2, opened2, actions2, flags2, scoreDelta2);
return true;

```

### Bước 6. Nhánh lỗi có cập nhật fallback và cố rollback.

**Code:** [MatchPlayerDAO.java — dòng 133](../../src/minesweeperonline/database/MatchPlayerDAO.java#L133). Tìm `} catch (SQLException e)`.

Trích đoạn bắt đầu tại vị trí trên:

```java
} catch (SQLException e) {
    System.err.println("[MatchPlayerDAO] Transaction error, rolling back: " + e.getMessage());
    // Fallback: cập nhật điểm và thống kê của user trong bộ nhớ
    UserDAO userDAO = new UserDAO();
    User u1 = userDAO.getUserById(user1Id);
    if (u1 != null) {
        userDAO.updateScore(user1Id, u1.getTotalScore() + scoreDelta1);
    }
    User u2 = userDAO.getUserById(user2Id);
    if (u2 != null) {
        userDAO.updateScore(user2Id, u2.getTotalScore() + scoreDelta2);
    }
    recordFallbackMatchDetails(matchId, outcomeDescription, startedAt, endedAt,
            user1Id, user1Name, res1, opened1, actions1, flags1, scoreDelta1,
            user2Id, user2Name, res2, opened2, actions2, flags2, scoreDelta2);

    if (conn != null) {
        try {
            conn.rollback();
        } catch (SQLException rbEx) {
            System.err.println("[MatchPlayerDAO] Rollback error: " + rbEx.getMessage());
        }
    }
    return false;
} finally {
    if (conn != null) {
        try {
            conn.setAutoCommit(true);
            conn.close();
```

## Điểm cần nắm

Fallback không tương đương lưu bền. compareAndSet không tự thử lại SQL; hàm gọi hiện không kiểm tra boolean DAO trả về.

## Trả lời khi bảo vệ

> Kết quả, thống kê và điểm được lưu bằng transaction; source còn có nhánh fallback khi SQL lỗi.
