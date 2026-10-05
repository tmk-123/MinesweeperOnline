package minesweeperonline.server;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {

            System.out.println(
                "Handling client: "
                + socket.getInetAddress()
                + ":"
                + socket.getPort()
            );

            // Tạm thời giữ kết nối
            // Sau này sẽ đọc/ghi dữ liệu với Client

        } catch (Exception e) {

            System.out.println("Client error: " + e.getMessage());

        } finally {

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Client disconnected.");
        }
    }
}