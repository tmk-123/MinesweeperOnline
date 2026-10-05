package minesweeperonline.server;

import minesweeperonline.common.model.Player;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active GameSessions on the server.
 */
public class GameManager {

    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> userToGameMap = new ConcurrentHashMap<>();

    /**
     * Creates and registers a new GameSession for two players.
     */
    public GameSession createSession(Player p1, ClientHandler h1, Player p2, ClientHandler h2) {
        GameSession session = new GameSession(p1, h1, p2, h2);
        sessions.put(session.getGameId(), session);
        userToGameMap.put(p1.getUserId(), session.getGameId());
        userToGameMap.put(p2.getUserId(), session.getGameId());
        System.out.println("[GameManager] Created GameSession: " + session.getGameId() +
                " for [" + p1.getUsername() + "] vs [" + p2.getUsername() + "]");
        return session;
    }

    public GameSession getSession(String gameId) {
        if (gameId == null) return null;
        return sessions.get(gameId);
    }

    public GameSession getSessionByUserId(int userId) {
        String gameId = userToGameMap.get(userId);
        if (gameId == null) return null;
        return sessions.get(gameId);
    }

    public boolean isUserInGame(int userId) {
        return userToGameMap.containsKey(userId);
    }

    /**
     * Concludes a game session and marks it finished.
     */
    public boolean endSession(String gameId, int winnerUserId, String reason) {
        GameSession session = sessions.get(gameId);
        if (session != null) {
            boolean ended = session.finishGame(winnerUserId, reason);
            if (ended) {
                userToGameMap.remove(session.getPlayer1().getUserId());
                userToGameMap.remove(session.getPlayer2().getUserId());
                System.out.println("[GameManager] Ended GameSession: " + gameId + " | reason=" + reason);
            }
            return ended;
        }
        return false;
    }

    /**
     * Removes session from memory.
     */
    public void removeSession(String gameId) {
        GameSession session = sessions.remove(gameId);
        if (session != null) {
            userToGameMap.remove(session.getPlayer1().getUserId());
            userToGameMap.remove(session.getPlayer2().getUserId());
            System.out.println("[GameManager] Removed GameSession: " + gameId);
        }
    }

    public Collection<GameSession> getAllSessions() {
        return sessions.values();
    }
}
