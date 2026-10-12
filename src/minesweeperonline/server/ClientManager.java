package minesweeperonline.server;

import minesweeperonline.common.model.Player;
import minesweeperonline.common.protocol.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý tất cả kết nối client đang hoạt động và các người chơi đã đăng nhập (online).
 */
public class ClientManager {

    // Tất cả handler đang kết nối
    private final List<ClientHandler> allClients = new CopyOnWriteArrayList<>();

    // Map userId -> ClientHandler đã đăng nhập
    private final ConcurrentHashMap<Integer, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public void addClient(ClientHandler client) {
        if (client != null && !allClients.contains(client)) {
            allClients.add(client);
            System.out.println("[ClientManager] Client connected. Total active connections: " + allClients.size());
        }
    }

    public void removeClient(ClientHandler client) {
        if (client != null) {
            allClients.remove(client);
            if (client.getUser() != null) {
                onlineUsers.remove(client.getUser().getId());
                System.out.println("[ClientManager] User logged out: " + client.getUser().getUsername());
            }
            System.out.println("[ClientManager] Client removed. Total active connections: " + allClients.size());
        }
    }

    public void registerUser(int userId, ClientHandler client) {
        onlineUsers.put(userId, client);
        System.out.println("[ClientManager] User registered online: userId=" + userId +
                " (" + (client.getUser() != null ? client.getUser().getUsername() : "unknown") + ")");
    }

    public void unregisterUser(int userId) {
        onlineUsers.remove(userId);
    }

    public ClientHandler getClientByUserId(int userId) {
        return onlineUsers.get(userId);
    }

    public boolean isUserOnline(int userId) {
        return onlineUsers.containsKey(userId);
    }

    public void sendMessageToUser(int userId, Message message) {
        ClientHandler handler = onlineUsers.get(userId);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    public void broadcast(Message message) {
        for (ClientHandler client : allClients) {
            client.sendMessage(message);
        }
    }

    public void broadcastExcept(Message message, ClientHandler except) {
        for (ClientHandler client : allClients) {
            if (client != except) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Trả về danh sách người chơi đang online, trừ chính user đang gửi yêu cầu.
     */
    public List<Player> getOnlinePlayers(int excludeUserId) {
        List<Player> list = new ArrayList<>();
        for (ClientHandler handler : onlineUsers.values()) {
            Player p = handler.getPlayer(); // chỉ đọc một lần: có thể thành null khi LOGOUT
            if (p != null && p.getUserId() != excludeUserId) {
                list.add(p);
            }
        }
        return list;
    }

    public int getActiveConnectionCount() {
        return allClients.size();
    }

    public int getOnlineUserCount() {
        return onlineUsers.size();
    }
}
