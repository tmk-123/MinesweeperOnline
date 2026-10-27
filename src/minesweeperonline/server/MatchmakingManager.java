package minesweeperonline.server;

import com.google.gson.JsonObject;
import minesweeperonline.common.model.Player;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Quản lý hàng đợi ghép trận tự động (Quick Match / Matchmaking).
 * Đảm bảo đồng bộ đa luồng (thread-safe), tự động ghép 2 người chơi rảnh rỗi vào một GameSession mới.
 */
public class MatchmakingManager {

    private final Queue<ClientHandler> queue = new LinkedList<>();
    private final ClientManager clientManager;
    private final GameManager gameManager;

    public MatchmakingManager(ClientManager clientManager, GameManager gameManager) {
        this.clientManager = clientManager;
        this.gameManager = gameManager;
    }

    /**
     * Người chơi gửi yêu cầu tham gia hàng đợi tìm trận nhanh.
     */
    public synchronized void joinQueue(ClientHandler client) {
        if (client == null || client.getUser() == null || client.getPlayer() == null) {
            return;
        }

        int userId = client.getUser().getId();
        if (gameManager.isUserInGame(userId)) {
            client.sendMessage(JsonProtocol.createError(null, "Bạn đang trong một ván đấu khác!"));
            return;
        }

        // Bỏ khỏi hàng đợi nếu đã có từ trước để tránh trùng lặp
        queue.remove(client);

        // Tìm một đối thủ hợp lệ đang chờ trong hàng đợi
        ClientHandler opponent = null;
        while (!queue.isEmpty()) {
            ClientHandler candidate = queue.poll();
            if (candidate != null && candidate != client
                    && candidate.isRunning()
                    && candidate.getUser() != null
                    && candidate.getPlayer() != null
                    && clientManager.isUserOnline(candidate.getUser().getId())
                    && !gameManager.isUserInGame(candidate.getUser().getId())) {
                opponent = candidate;
                break;
            }
        }

        if (opponent != null) {
            // Đã tìm thấy đối thủ -> Chuyển cả hai sang PLAYING và tạo ván đấu mới
            opponent.getPlayer().setStatus(Player.STATUS_PLAYING);
            client.getPlayer().setStatus(Player.STATUS_PLAYING);

            GameSession session = gameManager.createSession(
                    opponent.getPlayer(), opponent,
                    client.getPlayer(), client
            );

            System.out.println("[Matchmaking] Match found: [" + opponent.getUser().getUsername() +
                    "] vs [" + client.getUser().getUsername() + "] -> GameId: " + session.getGameId());

            // Gửi MATCH_CREATED cho đối thủ (Player 1)
            Message matchCreatedP1 = new Message(MessageType.MATCH_CREATED);
            JsonObject p1Data = new JsonObject();
            p1Data.addProperty("gameId", session.getGameId());
            p1Data.addProperty("opponentId", client.getUser().getId());
            p1Data.addProperty("opponentUsername", client.getUser().getUsername());
            matchCreatedP1.setPayload(p1Data);
            opponent.sendMessage(matchCreatedP1);

            // Gửi MATCH_CREATED cho người vừa tìm (Player 2)
            Message matchCreatedP2 = new Message(MessageType.MATCH_CREATED);
            JsonObject p2Data = new JsonObject();
            p2Data.addProperty("gameId", session.getGameId());
            p2Data.addProperty("opponentId", opponent.getUser().getId());
            p2Data.addProperty("opponentUsername", opponent.getUser().getUsername());
            matchCreatedP2.setPayload(p2Data);
            client.sendMessage(matchCreatedP2);

        } else {
            // Chưa có đối thủ -> Đưa vào hàng đợi và chuyển trạng thái sang QUEUED
            client.getPlayer().setStatus(Player.STATUS_QUEUED);
            queue.add(client);
            System.out.println("[Matchmaking] User queued: " + client.getUser().getUsername() +
                    " | Total in queue: " + queue.size());
        }
    }

    /**
     * Hủy tìm trận hoặc người chơi thoát/ngắt kết nối khi đang ở trong hàng đợi.
     */
    public synchronized void leaveQueue(ClientHandler client) {
        if (client == null) return;
        boolean removed = queue.remove(client);
        if (removed) {
            if (client.getPlayer() != null && Player.STATUS_QUEUED.equals(client.getPlayer().getStatus())) {
                client.getPlayer().setStatus(Player.STATUS_IDLE);
            }
            if (client.getUser() != null) {
                System.out.println("[Matchmaking] User left queue: " + client.getUser().getUsername() +
                        " | Total in queue: " + queue.size());
            }
        }
    }

    public synchronized boolean isInQueue(ClientHandler client) {
        return queue.contains(client);
    }
}
