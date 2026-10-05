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
 * Represents an active match session between two players.
 * Server-authoritative game lifecycle, countdown timers, action validation,
 * and transactional result recording.
 */
public class GameSession {

    private final String gameId;

    private final Player player1;
    private final ClientHandler handler1;
    private final Board board1;

    private final Player player2;
    private final ClientHandler handler2;
    private final Board board2;

    private GameState state;
    private long startedAt;
    private long endedAt;
    private int remainingSeconds;

    private int winnerUserId = -1; // -1 = ongoing, 0 = draw, >0 = winner userId
    private String endReason = "";

    private final Object gameLock = new Object();
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final Set<Integer> playAgainRequests = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> matchTimerTask;

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
     * Starts the 5-second READY countdown before transitioning into PLAYING.
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

                // Broadcast GAME_START to both players
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
     * Ticks every second during PLAYING state. Triggers TIMEOUT when reaching 0.
     */
    private void startPlayingTimer() {
        matchTimerTask = timerScheduler.scheduleAtFixedRate(() -> {
            synchronized (gameLock) {
                if (finished.get()) {
                    stopTimers();
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
            matchTimerTask.cancel(true);
        }
        timerScheduler.shutdownNow();
    }

    /**
     * Processes OPEN_CELL action from a player.
     */
    public void handleOpenCell(int userId, int row, int col) {
        synchronized (gameLock) {
            if (finished.get() || state != GameState.PLAYING) {
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
                // Stepped on a mine -> Immediate Defeat
                finishGame(opponentPlayer.getUserId(), "CLICKED_MINE");

                // Notify loser
                Message loseMsg = new Message(MessageType.GAME_LOSE);
                loseMsg.setGameId(gameId);
                JsonObject pLose = new JsonObject();
                pLose.addProperty("reason", "Ban da mo phai min tai (" + row + ", " + col + ")!");
                pLose.addProperty("scoreDelta", ServerConfig.SCORE_DEFEAT);
                loseMsg.setPayload(pLose);
                actingHandler.sendMessage(loseMsg);

                // Notify winner
                Message winMsg = new Message(MessageType.GAME_WIN);
                winMsg.setGameId(gameId);
                JsonObject pWin = new JsonObject();
                pWin.addProperty("reason", "Doi thu [" + actingPlayer.getUsername() + "] da mo phai min!");
                pWin.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                winMsg.setPayload(pWin);
                opponentHandler.sendMessage(winMsg);

            } else if (result == Board.OpenResult.SAFE) {
                // Check if all 121 safe cells have been cleared
                if (targetBoard.isAllSafeCellsOpened()) {
                    finishGame(actingPlayer.getUserId(), "CLEARED_ALL_SAFE_CELLS");

                    Message winMsg = new Message(MessageType.GAME_WIN);
                    winMsg.setGameId(gameId);
                    JsonObject pWin = new JsonObject();
                    pWin.addProperty("reason", "Ban da xuat sac mo het tat ca 121 o an toan!");
                    pWin.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
                    winMsg.setPayload(pWin);
                    actingHandler.sendMessage(winMsg);

                    Message loseMsg = new Message(MessageType.GAME_LOSE);
                    loseMsg.setGameId(gameId);
                    JsonObject pLose = new JsonObject();
                    pLose.addProperty("reason", "Doi thu da hoan thanh mo toan bo 121 o an toan truoc!");
                    pLose.addProperty("scoreDelta", ServerConfig.SCORE_DEFEAT);
                    loseMsg.setPayload(pLose);
                    opponentHandler.sendMessage(loseMsg);

                } else {
                    // Send opened cells to acting player
                    Message updateSelf = new Message(MessageType.CELL_OPENED);
                    updateSelf.setGameId(gameId);
                    JsonObject pSelf = new JsonObject();
                    pSelf.add("openedCells", serializeOpenedCells(newlyOpened));
                    pSelf.addProperty("openedSafeCells", targetBoard.getOpenedSafeCells());
                    pSelf.addProperty("totalActions", targetBoard.getTotalActions());
                    updateSelf.setPayload(pSelf);
                    actingHandler.sendMessage(updateSelf);

                    // Send progress update to opponent (without secret mine positions)
                    Message updateOpp = new Message(MessageType.GAME_UPDATE);
                    updateOpp.setGameId(gameId);
                    JsonObject pOpp = new JsonObject();
                    pOpp.addProperty("opponentId", actingPlayer.getUserId());
                    pOpp.addProperty("opponentOpenedSafeCells", targetBoard.getOpenedSafeCells());
                    pOpp.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
                    pOpp.addProperty("opponentTotalActions", targetBoard.getTotalActions());

                    // Miniature board sync: send newly opened coordinates (row, col)
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
     * Processes PLACE_FLAG action from a player.
     */
    public void handlePlaceFlag(int userId, int row, int col) {
        synchronized (gameLock) {
            if (finished.get() || state != GameState.PLAYING) return;
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

                // Notify opponent of flag count & mini-board flag placement
                Message oppUpdate = new Message(MessageType.GAME_UPDATE);
                oppUpdate.setGameId(gameId);
                JsonObject op = new JsonObject();
                op.addProperty("opponentId", actingPlayer.getUserId());
                op.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
                op.addProperty("flagRow", row);
                op.addProperty("flagCol", col);
                op.addProperty("flagState", true);
                oppUpdate.setPayload(op);
                opponentHandler.sendMessage(oppUpdate);
            }
        }
    }

    /**
     * Processes REMOVE_FLAG action from a player.
     */
    public void handleRemoveFlag(int userId, int row, int col) {
        synchronized (gameLock) {
            if (finished.get() || state != GameState.PLAYING) return;
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

                // Notify opponent of flag count & mini-board flag removal
                Message oppUpdate = new Message(MessageType.GAME_UPDATE);
                oppUpdate.setGameId(gameId);
                JsonObject op = new JsonObject();
                op.addProperty("opponentId", actingPlayer.getUserId());
                op.addProperty("opponentCurrentFlags", targetBoard.getCurrentFlags());
                op.addProperty("flagRow", row);
                op.addProperty("flagCol", col);
                op.addProperty("flagState", false);
                oppUpdate.setPayload(op);
                opponentHandler.sendMessage(oppUpdate);
            }
        }
    }

    /**
     * Handles TIMEOUT arbitration when the 5-minute game clock reaches 0.
     */
    private void handleTimeout() {
        int winner = GameRules.arbitrateTimeout(player1, player2);
        finishGame(winner, "TIMEOUT");

        if (winner == 0) {
            // DRAW
            Message drawMsg = new Message(MessageType.GAME_DRAW);
            drawMsg.setGameId(gameId);
            JsonObject p = new JsonObject();
            p.addProperty("reason", "Het gio thi dau! Ket qua hoa.");
            p.addProperty("scoreDelta", ServerConfig.SCORE_DRAW);
            drawMsg.setPayload(p);
            handler1.sendMessage(drawMsg);
            handler2.sendMessage(drawMsg);
        } else {
            ClientHandler winHandler = (winner == player1.getUserId()) ? handler1 : handler2;
            ClientHandler loseHandler = (winner == player1.getUserId()) ? handler2 : handler1;

            Message winMsg = new Message(MessageType.GAME_WIN);
            winMsg.setGameId(gameId);
            JsonObject pw = new JsonObject();
            pw.addProperty("reason", "Het gio! Ban thang nho chi so vuot troi.");
            pw.addProperty("scoreDelta", ServerConfig.SCORE_VICTORY);
            winMsg.setPayload(pw);
            winHandler.sendMessage(winMsg);

            Message loseMsg = new Message(MessageType.GAME_LOSE);
            loseMsg.setGameId(gameId);
            JsonObject pl = new JsonObject();
            pl.addProperty("reason", "Het gio! Doi thu thang nho chi so vuot troi.");
            pl.addProperty("scoreDelta", ServerConfig.SCORE_DEFEAT);
            loseMsg.setPayload(pl);
            loseHandler.sendMessage(loseMsg);
        }
    }

    /**
     * Handles PLAY_AGAIN request from a player after a match concludes.
     * When both players vote PLAY_AGAIN, a fresh GameSession is instantiated.
     */
    public void handlePlayAgain(int userId, GameManager gameManager) {
        synchronized (gameLock) {
            if (!finished.get()) return;

            playAgainRequests.add(userId);
            Player acting = getPlayer(userId);
            ClientHandler actingHandler = (userId == player1.getUserId()) ? handler1 : handler2;
            ClientHandler opponentHandler = (userId == player1.getUserId()) ? handler2 : handler1;

            if (playAgainRequests.size() >= 2) {
                // Both players want to play again -> Start rematch!
                gameManager.removeSession(this.gameId);

                GameSession newSession = gameManager.createSession(
                        player1, handler1,
                        player2, handler2
                );

                // Send MATCH_CREATED to both players
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
                // 1 player requested -> Send WAITING to self and REQUESTED to opponent
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
     * Handles EXIT request from a player after a match concludes.
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
     * Transactional database recording for match outcome and scores.
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

        MatchPlayerDAO dao = new MatchPlayerDAO();
        dao.saveFullMatchTransaction(
                gameId, "FINISHED", startedAt, System.currentTimeMillis(), (winnerId > 0 ? winnerId : null), reason,
                player1.getUserId(), player1.getUsername(), res1, board1.getOpenedSafeCells(), board1.getTotalActions(), board1.getFlagsPlaced(), delta1,
                player2.getUserId(), player2.getUsername(), res2, board2.getOpenedSafeCells(), board2.getTotalActions(), board2.getFlagsPlaced(), delta2
        );
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
     * Thread-safe, single-execution finish method that marks session finished,
     * stops all timers, updates statuses, and executes database transaction recording.
     */
    public boolean finishGame(int winnerUserId, String reason) {
        if (finished.compareAndSet(false, true)) {
            stopTimers();
            synchronized (gameLock) {
                this.state = GameState.FINISHED;
                this.endedAt = System.currentTimeMillis();
                this.winnerUserId = winnerUserId;
                this.endReason = reason;

                player1.setStatus(Player.STATUS_IDLE);
                player2.setStatus(Player.STATUS_IDLE);
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
}
