package minesweeperonline.client.network;

import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý kết nối TCP Socket từ Client tới Server.
 * Chạy một listener thread nền để đọc các message JSON phân tách bằng ký tự xuống dòng
 * và chuyển chúng tới các listener đã đăng ký.
 *
 * Cả client chỉ có MỘT đối tượng ServerConnection (tạo trong MainClient) và mọi màn hình
 * dùng chung nó: gửi bằng sendMessage(), nhận bằng cách đăng ký MessageListener.
 * Đây là phía client tương ứng với ClientHandler ở server.
 */
public class ServerConnection {

    /**
     * Người nghe message từ server (mẫu Observer). Mỗi màn hình tự đăng ký một listener
     * và tự lọc theo message.getType().
     * Lưu ý: hàm này được gọi trên thread mạng, muốn cập nhật giao diện phải bọc
     * trong SwingUtilities.invokeLater(...).
     */
    public interface MessageListener {
        void onMessageReceived(Message message);
    }

    /**
     * Người nghe sự kiện kết nối / mất kết nối.
     * Hiện chưa màn hình nào đăng ký (client chưa báo cho người chơi khi mất kết nối).
     */
    public interface ConnectionListener {
        void onConnected();
        void onDisconnected(String reason);
    }

    private Socket socket;
    private BufferedReader reader;     // đọc từng dòng JSON từ server
    private PrintWriter writer;        // ghi JSON lên server
    private Thread listenerThread;     // thread nền chạy listenForIncomingMessages()
    // volatile: thread giao diện và thread mạng cùng đọc/ghi biến này
    private volatile boolean connected = false;

    // CopyOnWriteArrayList: cho phép thêm/xóa listener ngay trong lúc đang duyệt danh sách
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

    /**
     * Mở kết nối TCP tới server rồi khởi động thread nền để nhận dữ liệu.
     * Gọi một lần từ MainClient lúc khởi động; nếu đã kết nối thì không mở thêm socket.
     *
     * @param host IP hoặc tên máy chạy server (mặc định 127.0.0.1)
     * @param port cổng server (mặc định 2209)
     * @return true nếu kết nối thành công; false nếu server không chạy, sai IP
     *         hoặc quá 5 giây không bắt tay được
     */
    public synchronized boolean connect(String host, int port) {
        if (connected) {
            return true;
        }

        try {
            socket = new Socket();
            // Kết nối với timeout 5 giây
            socket.connect(new InetSocketAddress(host, port), 5000);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            connected = true;

            // Khởi động thread nền nhận dữ liệu (readLine() chặn nên không chạy trên thread giao diện)
            listenerThread = new Thread(this::listenForIncomingMessages, "Client-Network-Listener");
            // Daemon: đóng cửa sổ cuối cùng là chương trình thoát, không phải chờ thread này
            listenerThread.setDaemon(true);
            listenerThread.start();

            notifyConnected();
            System.out.println("[ServerConnection] Connected to server: " + host + ":" + port);
            return true;

        } catch (IOException e) {
            System.err.println("[ServerConnection] Connection failed to " + host + ":" + port + " - " + e.getMessage());
            disconnect("Connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi một message lên server: chuyển thành một dòng JSON có '\n' ở cuối, ghi vào socket
     * rồi flush để dữ liệu đi ngay. Chưa kết nối thì chỉ in lỗi và bỏ qua.
     *
     * synchronized: nhiều nơi có thể gửi cùng lúc (người chơi click, timer làm mới danh sách...),
     * khóa đảm bảo hai chuỗi JSON không bị ghi xen vào nhau trên socket.
     *
     * Cách dùng: connection.sendMessage(JsonProtocol.createOpenCell(gameId, row, col));
     */
    public synchronized void sendMessage(Message message) {
        if (!connected || writer == null) {
            System.err.println("[ServerConnection] Cannot send message: Not connected to server!");
            return;
        }

        try {
            String json = JsonProtocol.serializeWithNewline(message);
            writer.print(json);
            writer.flush();
        } catch (Exception e) {
            System.err.println("[ServerConnection] Error sending message: " + e.getMessage());
            disconnect("Error sending data");
        }
    }

    /**
     * Ngắt kết nối: đánh dấu connected = false, đóng socket và báo cho các ConnectionListener.
     * Có thể bị gọi từ nhiều nơi (gửi lỗi, thread nhận kết thúc...) nên kiểm tra ở đầu hàm
     * để chỉ ngắt đúng một lần. Đóng socket cũng làm readLine() ở thread nền thoát ra.
     *
     * @param reason lý do ngắt, dùng để ghi log và báo cho listener
     */
    public synchronized void disconnect(String reason) {
        if (!connected && (socket == null || socket.isClosed())) {
            return;
        }

        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Bỏ qua
        }

        notifyDisconnected(reason);
        System.out.println("[ServerConnection] Disconnected: " + reason);
    }

    /**
     * Vòng lặp nhận dữ liệu, chạy trên thread "Client-Network-Listener" suốt thời gian kết nối.
     * Mỗi dòng đọc được là một message JSON: deserialize rồi phát cho mọi MessageListener.
     * JSON hỏng thì bỏ qua dòng đó, không làm dừng vòng lặp.
     *
     * Vòng lặp kết thúc khi server đóng kết nối (readLine() trả về null) hoặc mạng lỗi
     * (IOException); khi đó finally gọi disconnect().
     */
    private void listenForIncomingMessages() {
        try {
            String line;
            // readLine() chặn tới khi server gửi đủ một dòng (kết thúc bằng '\n')
            while (connected && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                Message message = JsonProtocol.deserialize(line);
                if (message != null) {
                    notifyMessageReceived(message);
                } else {
                    System.err.println("[ServerConnection] Ignored unparseable message: " + line);
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("[ServerConnection] Network read error: " + e.getMessage());
            }
        } finally {
            disconnect("Connection closed by server or network loss");
        }
    }

    /**
     * Đăng ký nhận message từ server. Gọi khi mở một màn hình (ví dụ trong constructor).
     * Không thêm trùng nếu listener đã có trong danh sách.
     */
    public void addMessageListener(MessageListener listener) {
        if (listener != null && !messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    /**
     * Hủy đăng ký. BẮT BUỘC gọi khi đóng màn hình (trong dispose()), nếu không cửa sổ
     * đã đóng vẫn tiếp tục phản ứng với message (ví dụ mở hai sảnh sau khi đăng nhập lại).
     */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    /** Đăng ký nghe sự kiện kết nối / mất kết nối. */
    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null && !connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
        }
    }

    /** Hủy đăng ký nghe sự kiện kết nối. */
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    /** Báo cho mọi ConnectionListener rằng vừa kết nối thành công. */
    private void notifyConnected() {
        for (ConnectionListener l : connectionListeners) {
            l.onConnected();
        }
    }

    /** Báo cho mọi ConnectionListener rằng kết nối đã mất, kèm lý do. */
    private void notifyDisconnected(String reason) {
        for (ConnectionListener l : connectionListeners) {
            l.onDisconnected(reason);
        }
    }

    /**
     * Phát một message cho TẤT CẢ MessageListener; mỗi màn hình tự quyết định có xử lý hay không.
     * Mỗi listener được bọc try/catch riêng: một màn hình xử lý lỗi không làm các màn hình khác
     * mất tin, và không làm chết thread nhận dữ liệu.
     */
    private void notifyMessageReceived(Message message) {
        for (MessageListener l : messageListeners) {
            try {
                l.onMessageReceived(message);
            } catch (Exception e) {
                System.err.println("[ServerConnection] Exception in message listener: " + e.getMessage());
            }
        }
    }

    /** @return true nếu đang kết nối và socket chưa bị đóng */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
