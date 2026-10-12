package minesweeperonline.client;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.client.ui.LoginFrame;
import minesweeperonline.server.ServerConfig;

import javax.swing.*;

/**
 * Điểm khởi chạy chính của ứng dụng Client Minesweeper (Swing).
 */
public class MainClient {

    private final ServerConnection connection;
    private final String host;
    private final int port;

    public MainClient(String host, int port) {
        this.connection = new ServerConnection();
        this.host = host;
        this.port = port;
    }

    public void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            connectAndOpenLogin();
        });
    }

    private void connectAndOpenLogin() {
        boolean connected = connection.connect(host, port);
        if (connected) {
            LoginFrame loginFrame = new LoginFrame(connection);
            loginFrame.setVisible(true);
        } else {
            int option = JOptionPane.showConfirmDialog(null,
                    "Không thể kết nối đến Máy chủ (" + host + ":" + port + ")!\n" +
                    "Vui lòng đảm bảo MainServer đã được khởi động.\n\nBạn có muốn thử kết nối lại không?",
                    "Lỗi kết nối Server",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                connectAndOpenLogin();
            } else {
                System.exit(0);
            }
        }
    }

    /**
     * Cách dùng: MainClient [host] [port]  (mặc định 127.0.0.1 2209).
     * Truyền IP LAN của máy chạy server để chơi từ máy tính khác.
     */
    public static void main(String[] args) {
        String host = ServerConfig.DEFAULT_HOST;
        int port = ServerConfig.DEFAULT_PORT;
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port parameter, using default: " + port);
            }
        }
        MainClient app = new MainClient(host, port);
        app.launch();
    }
}
