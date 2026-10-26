package minesweeperonline.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import minesweeperonline.common.model.GameState;
import minesweeperonline.common.model.Player;
import minesweeperonline.database.MatchPlayerDAO;
import minesweeperonline.game.Board;
import minesweeperonline.game.Cell;
import minesweeperonline.game.GameRules;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Đại diện cho một phiên đấu (session) đang diễn ra giữa hai người chơi.
 * Server nắm quyền quyết định toàn bộ vòng đời trận đấu, các timer đếm ngược,
 * kiểm tra hợp lệ thao tác và ghi kết quả bằng transaction.
 */
public class GameSession {

    private final String gameId;

    private final Player player1;
    private volatile ClientHandler handler1;
    private final Board board1;

    private final Player player2;
    private volatile ClientHandler handler2;
    private final Board board2;

    private GameState state;
    private long startedAt;
    private long endedAt;
    private int remainingSeconds;

    private int winnerUserId = -1; // -1 = đang đấu, 0 = hòa, >0 = userId người thắng
    private String endReason = "";

    private final Object gameLock = new Object();
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final Set<Integer> playAgainRequests = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> matchTimerTask;
    private ScheduledFuture<?> disconnectGraceTask;
    private volatile int disconnectedUserId = -1;

    public GameSession(Player player1, ClientHandler handler1, Player player2, ClientHandler handler2) {
        this.gameId = UUID.randomUUID().toString().substring(0, 8);
        this.player1 = player1;
        this.handler1 = handler1;
        this.board1 = new Board();

        this.player2 = player2;
        this.handler2 = handler2;
        this.board2 = new Board();

        this.player1.setStatus(Player.STATUS_PLAYING);
        this.player2.setStatus(Player.STATUS_PLAYING);
        this.player1.resetMatchStats();
        this.player2.resetMatchStats();

        this.state = GameState.READY;
        this.startedAt = System.currentTimeMillis();
        this.remainingSeconds = ServerConfig.MATCH_DURATION_SECONDS;

        startReadyCountdown();
    }

    /**
     * Bắt đầu đếm ngược READY 5 giây trước khi chuyển sang PLAYING.
     */
    private void startReadyCountdown() {
        System.out.println("[GameSession] Match " + gameId + " created. Starting " +
                ServerConfig.READY_COUNTDOWN_SECONDS + "s READY countdown...");
        timerScheduler.schedule(() -> {
            try {
                synchronized (gameLock) {
                    if (finished.get()) return;
                    this.state = GameState.PLAYING;
                    this.startedAt = System.currentTimeMillis();
                }

                // Gửi GAME_START cho cả hai người chơi
                Message startMsg = new Message(MessageType.GAME_START);
                startMsg.setGameId(gameId);
                JsonObject p = new JsonObject();
                p.addProperty("durationSeconds", ServerConfig.MATCH_DURATION_SECONDS);
                startMsg.setPayload(p);

                System.out.println("[GameSession] Broadcasted GAME_START for match: " + gameId);
                handler1.sendMessage(startMsg);
                handler2.sendMessage(startMsg);

                startPlayingTimer();
            } catch (Exception e) {
                System.err.println("[GameSession] Error starting game: " + e.getMessage());
            }
        }, ServerConfig.READY_COUNTDOWN_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Đếm mỗi giây trong trạng thái PLAYING. Kích hoạt TIMEOUT khi về 0.
     */
    private void startPlayingTimer() {
        matchTimerTask = timerScheduler.scheduleAtFixedRate(() -> {
            synchronized (gameLock) {
                if (finished.get()) {
                    stopTimers();
                    return;
                }

                // Nếu đang tạm dừng chờ đối thủ kết nối lại thì không trừ thời gian ván đấu
                if (disconnectedUserId >= 0) {
                    return;
                }

                remainingSeconds--;
                if (remainingSeconds <= 0) {
                    handleTimeout();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopTimers() {
        if (matchTimerTask != null) {
            matchTimerTask.cancel(false);
        }
        if (disconnectGraceTask != null) {
            disconnectGraceTask.cancel(false);
        }
        timerScheduler.shutdown();
    }

    /**
     * Xử lý thao tác OPEN_CELL của một người chơi.
     */
    public void handleOpenCell(int userId, int row, int col) {
        synchronized (gameLock) {
            if (finished.get() || state != GameState.PLAYING || disconnectedUserId >= 0) {
                return;
            }

            Board targetBoard = getBoardForPlayer(userId);
            Player actingPlayer = getPlayer(userId);
            Player opponentPlayer = getOpponent(userId);
            ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
            ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

            if (targetBoard == null || actingPlayer == null) return;

            List<Cell> newlyOpened = new ArrayList<>();
            Board.OpenResult result = targetBoard.openCell(row, col, newlyOpened);

            if (result == Board.OpenResult.INVALID) {
                return;
            }

            actingPlayer.setOpenedSafeCells(targetBoard.getOpenedSafeCells());
            actingPlayer.setTotalActions(targetBoard.getTotalActions());

            if (result == Board.OpenResult.MINE) {
                // Mở trúng mìn -> thua ngay lập tức
                finishGame(opponentPlayer.getUserId(), "CLICKED_MINE");

                // Báo cho người thua
                Message loseMsg = new Message(MessageType.GAME_LOSE);
                loseMsg.setGameId(gameId);
                JsonObject pLose = new JsonObject();
                pLose.addProperty("reason", "Ban da mo phai min tai (" + row + ", " + col + ")!");
                pLose.addProperty("scoreDelta", ServerConfig.SCORE_DEFEAT);
                attachMines(pLose, actingPlayer.getUserId());
                loseMsg.setPayload(pLose);
                actingHandler.sendMessage(loseMsg);

                // Báo cho người thắng
                Message winMsg = new Message(MessageType.GAME_WIN);
                winMsg.setGameId(gameId);
                JsonObject pWin = new JsonObject();
                pWin.addProperty("reason", "Doi thu [" + actingPlayer.getUsername() + "] da mo phai min!");
                pWin.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                attachMines(pWin, opponentPlayer.getUserId());
                winMsg.setPayload(pWin);
                opponentHandler.sendMessage(winMsg);

            } else if (result == Board.OpenResult.SAFE) {
                // Kiểm tra đã mở hết 121 ô an toàn chưa
                if (targetBoard.isAllSafeCellsOpened()) {
                    finishGame(actingPlayer.getUserId(), "CLEARED_ALL_SAFE_CELLS");

                    Message winMsg = new Message(MessageType.GAME_WIN);
                    winMsg.setGameId(gameId);
                    JsonObject pWin = new JsonObject();
                    pWin.addProperty("reason", "Ban da xuat sac mo het tat ca 121 o an toan!");
                    pWin.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                    attachMines(pWin, actingPlayer.getUserId());
                    winMsg.setPayload(pWin);
                    actingHandler.sendMessage(winMsg);

                    Message loseMsg = new Message(MessageType.GAME_LOSE);
                    loseMsg.setGameId(gameId);
                    JsonObject pLose = new JsonObject();
                    pLose.addProperty("reason", "Doi thu da hoan thanh mo toan bo 121 o an toan truoc!");
                    pLose.addProperty("scoreDelta", ServerConfig.SCORE_DEFEAT);
                    attachMines(pLose, opponentPlayer.getUserId());
                    loseMsg.setPayload(pLose);
                    opponentHandler.sendMessage(loseMsg);

                } else {
                    // Gửi các ô vừa mở cho người chơi thực hiện thao tác
                    Message updateSelf = new Message(MessageType.CELL_OPENED);
                    updateSelf.setGameId(gameId);
                    JsonObject pSelf = new JsonObject();
                    pSelf.add("openedCells", serializeOpenedCells(newlyOpened));
                    pSelf.addProperty("openedSafeCells", targetBoard.getOpenedSafeCells());
                    pSelf.addProperty("totalActions", targetBoard.getTotalActions());
                    updateSelf.setPayload(pSelf);
                    actingHandler.sendMessage(updateSelf);

                    // Gửi cập nhật tiến độ cho đối thủ (không lộ vị trí mìn)
                    Message updateOpp = new Message(MessageType.GAME_UPDATE);
                    updateOpp.setGameId(gameId);
                    JsonObject pOpp = new JsonObject();
                    pOpp.addProperty("opponentId", actingPlayer.getUserId());
                    pOpp.addProperty("opponentOpenedSafeCells", targetBoard.getOpenedSafeCells());
                    pOpp.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
                    pOpp.addProperty("opponentTotalActions", targetBoard.getTotalActions());

                    // Đồng bộ bàn cờ thu nhỏ: gửi tọa độ (row, col) các ô vừa mở
                    JsonArray openedCoords = new JsonArray();
                    for (Cell c : newlyOpened) {
                        JsonObject coord = new JsonObject();
                        coord.addProperty("r", c.getRow());
                        coord.addProperty("c", c.getCol());
                        openedCoords.add(coord);
                    }
                    pOpp.add("openedCoords", openedCoords);

                    updateOpp.setPayload(pOpp);
                    opponentHandler.sendMessage(updateOpp);
                }
            }
        }
    }

    /**
     * Xử lý thao tác PLACE_FLAG của một người chơi.
     */
    public void handlePlaceFlag(int userId, int row, int col) {
        synchronized (gameLock) {
            if (finished.get() || state != GameState.PLAYING || disconnectedUserId >= 0) return;
            Board targetBoard = getBoardForPlayer(userId);
            Player actingPlayer = getPlayer(userId);
            ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
            ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

            if (targetBoard != null && targetBoard.placeFlag(row, col)) {
                actingPlayer.setCurrentFlags(targetBoard.getCurrentFlags());
                actingPlayer.setFlagsPlaced(targetBoard.getFlagsPlaced());
                actingPlayer.setTotalActions(targetBoard.getTotalActions());

                Message res = new Message(MessageType.FLAG_PLACED);
                res.setGameId(gameId);
                JsonObject p = new JsonObject();
                p.addProperty("row", row);
                p.addProperty("col", col);
                p.addProperty("currentFlags", targetBoard.getCurrentFlags());
                p.addProperty("totalActions", targetBoard.getTotalActions());
                res.setPayload(p);
                actingHandler.sendMessage(res);

                // Báo đối thủ số cờ hiện tại & vị trí cắm cờ trên bàn cờ thu nhỏ
                Message oppUpdate = new Message(MessageType.GAME_UPDATE);
                oppUpdate.setGameId(gameId);
                JsonObject op = new JsonObject();
                op.addProperty("opponentId", actingPlayer.getUserId());
                op.addProperty("opponentOpenedSafeCells", targetBoard.getOpenedSafeCells());
                op.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
                op.addProperty("opponentTotalActions", targetBoard.getTotalActions());
                op.addProperty("flagRow", row);
                op.addProperty("flagCol", col);
                op.addProperty("flagState", true);
                oppUpdate.setPayload(op);
                opponentHandler.sendMessage(oppUpdate);
            }
        }
    }

    /**
     * Xử lý thao tác REMOVE_FLAG của một người chơi.
     */
    public void handleRemoveFlag(int userId, int row, int col) {
        synchronized (gameLock) {
            if (finished.get() || state != GameState.PLAYING || disconnectedUserId >= 0) return;
            Board targetBoard = getBoardForPlayer(userId);
            Player actingPlayer = getPlayer(userId);
            ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
            ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

            if (targetBoard != null && targetBoard.removeFlag(row, col)) {
                actingPlayer.setCurrentFlags(targetBoard.getCurrentFlags());
                actingPlayer.setFlagsRemoved(targetBoard.getFlagsRemoved());
                actingPlayer.setTotalActions(targetBoard.getTotalActions());

                Message res = new Message(MessageType.FLAG_REMOVED);
                res.setGameId(gameId);
                JsonObject p = new JsonObject();
                p.addProperty("row", row);
                p.addProperty("col", col);
                p.addProperty("currentFlags", targetBoard.getCurrentFlags());
                p.addProperty("totalActions", targetBoard.getTotalActions());
                res.setPayload(p);
                actingHandler.sendMessage(res);

                // Báo đối thủ số cờ hiện tại & vị trí gỡ cờ trên bàn cờ thu nhỏ
                Message oppUpdate = new Message(MessageType.GAME_UPDATE);
                oppUpdate.setGameId(gameId);
                JsonObject op = new JsonObject();
                op.addProperty("opponentId", actingPlayer.getUserId());
                op.addProperty("opponentOpenedSafeCells", targetBoard.getOpenedSafeCells());
                op.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
                op.addProperty("opponentTotalActions", targetBoard.getTotalActions());
                op.addProperty("flagRow", row);
                op.addProperty("flagCol", col);
                op.addProperty("flagState", false);
                oppUpdate.setPayload(op);
                opponentHandler.sendMessage(oppUpdate);
            }
        }
    }

    /**
     * Phân xử TIMEOUT khi đồng hồ trận đấu 5 phút về 0.
     */
    private void handleTimeout() {
        int winner = GameRules.arbitrateTimeout(player1, player2);
        finishGame(winner, "TIMEOUT");

        if (winner == 0) {
            // Hòa (DRAW): mỗi người một message riêng vì danh sách mìn của hai bàn khác nhau
            for (Player p : new Player[]{player1, player2}) {
                Message drawMsg = new Message(MessageType.GAME_DRAW);
                drawMsg.setGameId(gameId);
                JsonObject pd = new JsonObject();
                pd.addProperty("reason", "Het gio thi dau! Ket qua hoa.");
                pd.addProperty("scoreDelta", ServerConfig.SCORE_DRAW);
                attachMines(pd, p.getUserId());
                drawMsg.setPayload(pd);
                (p == player1 ? handler1 : handler2).sendMessage(drawMsg);
            }
        } else {
            ClientHandler winHandler = (winner == player1.getUserId()) ? handler1 : handler2;
            ClientHandler loseHandler = (winner == player1.getUserId()) ? handler2 : handler1;
            int loser = (winner == player1.getUserId()) ? player2.getUserId() : player1.getUserId();

            Message winMsg = new Message(MessageType.GAME_WIN);
            winMsg.setGameId(gameId);
            JsonObject pw = new JsonObject();
            pw.addProperty("reason", "Het gio! Ban thang nho chi so vuot troi.");
            pw.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
            attachMines(pw, winner);
            winMsg.setPayload(pw);
            winHandler.sendMessage(winMsg);

            Message loseMsg = new Message(MessageType.GAME_LOSE);
            loseMsg.setGameId(gameId);
            JsonObject pl = new JsonObject();
            pl.addProperty("reason", "Het gio! Doi thu thang nho chi so vuot troi.");
            pl.addProperty("scoreDelta", ServerConfig.SCORE_DEFEAT);
            attachMines(pl, loser);
            loseMsg.setPayload(pl);
            loseHandler.sendMessage(loseMsg);
        }
    }

    /**
     * Xử lý yêu cầu PLAY_AGAIN của người chơi sau khi trận đấu kết thúc.
     * Khi cả hai người chơi cùng chọn PLAY_AGAIN thì tạo một GameSession mới.
     */
    public void handlePlayAgain(int userId, GameManager gameManager) {
        synchronized (gameLock) {
            if (!finished.get()) return;

            playAgainRequests.add(userId);
            Player acting = getPlayer(userId);
            ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
            ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

            if (playAgainRequests.size() >= 2) {
                // Cả hai đều muốn chơi lại -> bắt đầu tái đấu!
                gameManager.removeSession(this.gameId);

                GameSession newSession = gameManager.createSession(
                        player1, handler1,
                        player2, handler2
                );

                // Gửi MATCH_CREATED cho cả hai người chơi
                Message p1Msg = new Message(MessageType.MATCH_CREATED);
                JsonObject p1 = new JsonObject();
                p1.addProperty("gameId", newSession.getGameId());
                p1.addProperty("opponentId", player2.getUserId());
                p1.addProperty("opponentUsername", player2.getUsername());
                p1Msg.setPayload(p1);
                handler1.sendMessage(p1Msg);

                Message p2Msg = new Message(MessageType.MATCH_CREATED);
                JsonObject p2 = new JsonObject();
                p2.addProperty("gameId", newSession.getGameId());
                p2.addProperty("opponentId", player1.getUserId());
                p2.addProperty("opponentUsername", player1.getUsername());
                p2Msg.setPayload(p2);
                handler2.sendMessage(p2Msg);

                System.out.println("[GameSession] Rematch created: " + newSession.getGameId() +
                        " between [" + player1.getUsername() + "] and [" + player2.getUsername() + "]");
            } else {
                // Mới có 1 người yêu cầu -> gửi WAITING cho người đó và REQUESTED cho đối thủ
                Message reqMsg = new Message(MessageType.PLAY_AGAIN);
                JsonObject pReq = new JsonObject();
                pReq.addProperty("status", "WAITING");
                pReq.addProperty("message", "Đang chờ đối thủ đồng ý chơi lại...");
                reqMsg.setPayload(pReq);
                actingHandler.sendMessage(reqMsg);

                Message oppMsg = new Message(MessageType.PLAY_AGAIN);
                JsonObject pOpp = new JsonObject();
                pOpp.addProperty("status", "REQUESTED");
                pOpp.addProperty("requestedByUserId", userId);
                pOpp.addProperty("requestedByUsername", acting.getUsername());
                pOpp.addProperty("message", "Đối thủ [" + acting.getUsername() + "] muốn tái đấu ván mới!");
                oppMsg.setPayload(pOpp);
                opponentHandler.sendMessage(oppMsg);
            }
        }
    }

    /**
     * Xử lý yêu cầu EXIT của người chơi sau khi trận đấu kết thúc.
     */
    public void handleExit(int userId, GameManager gameManager) {
        synchronized (gameLock) {
            playAgainRequests.remove(userId);
            ClientHandler opponentHandler = getOpponentHandler(userId);

            if (opponentHandler != null) {
                Message exitMsg = new Message(MessageType.EXIT);
                JsonObject p = new JsonObject();
                p.addProperty("message", "Đối thủ đã rời phòng ra sảnh.");
                exitMsg.setPayload(p);
                opponentHandler.sendMessage(exitMsg);
            }

            player1.setStatus(Player.STATUS_IDLE);
            player2.setStatus(Player.STATUS_IDLE);
            gameManager.removeSession(this.gameId);
        }
    }

    /**
     * Ghi kết quả trận đấu và điểm số vào database bằng transaction.
     */
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

    /**
     * Gắn danh sách mìn trên bàn cờ của userId vào payload của message kết thúc trận
     * (GAME_WIN / GAME_LOSE / GAME_DRAW / OPPONENT_DISCONNECTED), dạng "mines": [{"r":..,"c":..}].
     * Client dùng nó để hiện hết bom và đánh dấu X lên cờ cắm sai.
     *
     * CHỈ gọi khi trận đã kết thúc: trong lúc đang chơi, vị trí mìn không bao giờ được gửi
     * cho client (nguyên tắc server-authoritative, chống gian lận).
     * Nếu người chơi chưa click lần nào thì mìn chưa được sinh, không gắn gì.
     */
    public void attachMines(JsonObject payload, int userId) {
        synchronized (gameLock) {
            Board board = getBoardForPlayer(userId);
            if (board == null || !board.isMinesGenerated()) return;
            JsonArray mines = new JsonArray();
            for (Cell c : board.getMineCells()) {
                JsonObject coord = new JsonObject();
                coord.addProperty("r", c.getRow());
                coord.addProperty("c", c.getCol());
                mines.add(coord);
            }
            payload.add("mines", mines);
        }
    }

    private JsonArray serializeOpenedCells(List<Cell> cells) {
        JsonArray array = new JsonArray();
        for (Cell c : cells) {
            JsonObject o = new JsonObject();
            o.addProperty("row", c.getRow());
            o.addProperty("col", c.getCol());
            o.addProperty("adjacentMines", c.getAdjacentMines());
            o.addProperty("isMine", c.isMine());
            o.addProperty("isExploded", c.isExploded());
            array.add(o);
        }
        return array;
    }

    /**
     * Hàm kết thúc trận thread-safe, chỉ thực thi một lần: đánh dấu session đã kết thúc,
     * dừng mọi timer và ghi kết quả vào database bằng transaction.
     * Người chơi vẫn ở trạng thái PLAYING (trong phòng kết quả) cho đến khi EXIT / mất kết nối.
     */
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
    }

    public String getGameId() { return gameId; }
    public Player getPlayer1() { return player1; }
    public ClientHandler getHandler1() { return handler1; }
    public Board getBoard1() { return board1; }
    public Player getPlayer2() { return player2; }
    public ClientHandler getHandler2() { return handler2; }
    public Board getBoard2() { return board2; }
    public GameState getState() { synchronized (gameLock) { return state; } }
    public boolean isFinished() { return finished.get(); }
    public int getRemainingSeconds() { return remainingSeconds; }

    public Player getOpponent(int userId) {
        if (player1.getUserId() == userId) return player2;
        if (player2.getUserId() == userId) return player1;
        return null;
    }

    public ClientHandler getOpponentHandler(int userId) {
        if (player1.getUserId() == userId) return handler2;
        if (player2.getUserId() == userId) return handler1;
        return null;
    }

    public Board getBoardForPlayer(int userId) {
        if (player1.getUserId() == userId) return board1;
        if (player2.getUserId() == userId) return board2;
        return null;
    }

    public Player getPlayer(int userId) {
        if (player1.getUserId() == userId) return player1;
        if (player2.getUserId() == userId) return player2;
        return null;
    }

    public boolean isPlayerDisconnected(int userId) {
        synchronized (gameLock) {
            return disconnectedUserId == userId;
        }
    }

    /**
     * Xử lý khi một người chơi bị ngắt kết nối giữa trận:
     * Tạm dừng ván đấu, thông báo cho đối thủ và cho phép kết nối lại trong vòng 15 giây.
     */
    public void handlePlayerDisconnect(int userId, GameManager gameManager) {
        synchronized (gameLock) {
            if (finished.get() || (state != GameState.READY && state != GameState.PLAYING)) return;
            this.disconnectedUserId = userId;

            System.out.println("[GameSession] Player " + userId + " disconnected. Pausing match " + gameId +
                    " for 15s reconnection window...");

            ClientHandler oppHandler = getOpponentHandler(userId);
            if (oppHandler != null) {
                Message msg = new Message(MessageType.OPPONENT_DISCONNECTED);
                msg.setGameId(gameId);
                JsonObject p = new JsonObject();
                p.addProperty("status", "WAITING_RECONNECT");
                p.addProperty("countdownSeconds", 15);
                p.addProperty("disconnectedUsername", getPlayer(userId) != null ? getPlayer(userId).getUsername() : "Doi thu");
                p.addProperty("message", "Đối thủ bị mất kết nối! Đang chờ kết nối lại (15s)...");
                msg.setPayload(p);
                oppHandler.sendMessage(msg);
            }

            if (disconnectGraceTask != null) {
                disconnectGraceTask.cancel(false);
            }

            disconnectGraceTask = timerScheduler.schedule(() -> {
                synchronized (gameLock) {
                    if (finished.get() || disconnectedUserId != userId) return;

                    System.out.println("[GameSession] 15s reconnection window expired for player " + userId + " in match " + gameId);
                    Player opponent = getOpponent(userId);
                    int winnerId = (opponent != null) ? opponent.getUserId() : 0;
                    if (finishGame(winnerId, "FORFEIT_DISCONNECT_TIMEOUT")) {
                        ClientHandler handler = getOpponentHandler(userId);
                        if (handler != null) {
                            Message forfeitMsg = new Message(MessageType.OPPONENT_DISCONNECTED);
                            forfeitMsg.setGameId(gameId);
                            JsonObject p = new JsonObject();
                            p.addProperty("status", "FORFEIT");
                            p.addProperty("reason", "Đối thủ không kết nối lại sau 15 giây!");
                            p.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                            attachMines(p, winnerId);
                            forfeitMsg.setPayload(p);
                            handler.sendMessage(forfeitMsg);
                        }
                    }
                    gameManager.removeSession(gameId);
                }
            }, 15, TimeUnit.SECONDS);
        }
    }

    /**
     * Khôi phục kết nối cho người chơi quay lại trận đấu trong thời gian cho phép.
     */
    public boolean handlePlayerReconnect(int userId, ClientHandler newHandler, Player newPlayer) {
        synchronized (gameLock) {
            if (finished.get() || disconnectedUserId != userId) return false;

            System.out.println("[GameSession] Player " + userId + " successfully reconnected to match " + gameId + "! Resuming match.");

            if (disconnectGraceTask != null) {
                disconnectGraceTask.cancel(false);
                disconnectGraceTask = null;
            }
            this.disconnectedUserId = -1;

            if (userId == player1.getUserId()) {
                this.handler1 = newHandler;
            } else {
                this.handler2 = newHandler;
            }

            // Báo đối thủ là người chơi đã kết nối lại
            ClientHandler oppHandler = getOpponentHandler(userId);
            if (oppHandler != null) {
                Message oppMsg = new Message(MessageType.OPPONENT_RECONNECTED);
                oppMsg.setGameId(gameId);
                JsonObject p = new JsonObject();
                p.addProperty("reconnectedUsername", newPlayer.getUsername());
                p.addProperty("message", "Đối thủ đã kết nối lại! Ván đấu tiếp tục.");
                oppMsg.setPayload(p);
                oppHandler.sendMessage(oppMsg);
            }

            // Gửi toàn bộ dữ liệu bàn cờ khôi phục cho người chơi vừa vào lại
            Message recMsg = new Message(MessageType.GAME_RECONNECTED);
            recMsg.setGameId(gameId);
            JsonObject p = new JsonObject();
            p.addProperty("gameId", gameId);

            Player opp = getOpponent(userId);
            p.addProperty("opponentId", opp != null ? opp.getUserId() : 0);
            p.addProperty("opponentUsername", opp != null ? opp.getUsername() : "Doi thu");
            p.addProperty("remainingSeconds", remainingSeconds);

            Board myBoard = getBoardForPlayer(userId);
            Board oppBoard = (myBoard == board1) ? board2 : board1;

            p.addProperty("mySafeCells", myBoard.getOpenedSafeCells());
            p.addProperty("myFlags", myBoard.getCurrentFlags());
            p.addProperty("myActions", myBoard.getTotalActions());

            p.addProperty("oppSafeCells", oppBoard.getOpenedSafeCells());
            p.addProperty("oppFlags", oppBoard.getCurrentFlags());
            p.addProperty("oppActions", oppBoard.getTotalActions());

            JsonArray myOpenedArr = new JsonArray();
            for (int r = 0; r < 12; r++) {
                for (int c = 0; c < 12; c++) {
                    Cell cell = myBoard.getCell(r, c);
                    if (cell.isOpened()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("row", r);
                        o.addProperty("col", c);
                        o.addProperty("adjacentMines", cell.getAdjacentMines());
                        o.addProperty("isMine", cell.isMine());
                        o.addProperty("isExploded", cell.isExploded());
                        myOpenedArr.add(o);
                    }
                }
            }
            p.add("myOpenedCells", myOpenedArr);

            JsonArray myFlaggedArr = new JsonArray();
            for (int r = 0; r < 12; r++) {
                for (int c = 0; c < 12; c++) {
                    if (myBoard.getCell(r, c).isFlagged()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("row", r);
                        o.addProperty("col", c);
                        myFlaggedArr.add(o);
                    }
                }
            }
            p.add("myFlaggedCells", myFlaggedArr);

            JsonArray oppOpenedArr = new JsonArray();
            JsonArray oppFlaggedArr = new JsonArray();
            for (int r = 0; r < 12; r++) {
                for (int c = 0; c < 12; c++) {
                    Cell cell = oppBoard.getCell(r, c);
                    if (cell.isOpened()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("r", r);
                        o.addProperty("c", c);
                        oppOpenedArr.add(o);
                    } else if (cell.isFlagged()) {
                        JsonObject o = new JsonObject();
                        o.addProperty("r", r);
                        o.addProperty("c", c);
                        oppFlaggedArr.add(o);
                    }
                }
            }
            p.add("oppOpenedCoords", oppOpenedArr);
            p.add("oppFlaggedCoords", oppFlaggedArr);

            recMsg.setPayload(p);
            newHandler.sendMessage(recMsg);
            return true;
        }
    }
}
