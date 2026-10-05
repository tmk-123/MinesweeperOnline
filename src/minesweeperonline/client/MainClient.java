package minesweeperonline.client;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.client.ui.LoginFrame;
import minesweeperonline.server.ServerConfig;

import javax.swing.*;

/**
 * Main entry point for the Minesweeper Swing Client application.
 */
public class MainClient {

    private final ServerConnection connection;

    public MainClient() {
        this.connection = new ServerConnection();
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
        boolean connected = connection.connect(ServerConfig.DEFAULT_HOST, ServerConfig.DEFAULT_PORT);
        if (connected) {
            LoginFrame loginFrame = new LoginFrame(connection);
            loginFrame.setVisible(true);
        } else {
            int option = JOptionPane.showConfirmDialog(null,
                    "Không thể kết nối đến Máy chủ (127.0.0.1:" + ServerConfig.DEFAULT_PORT + ")!\n" +
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

    public static void main(String[] args) {
        MainClient app = new MainClient();
        app.launch();
    }
}
