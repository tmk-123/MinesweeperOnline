package minesweeperonline.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {

    private static final int PORT = 2209;

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("================================");
            System.out.println("      MINESWEEPER SERVER");
            System.out.println("================================");
            System.out.println("Server started at port: " + PORT);
            System.out.println("Waiting for clients...");

            while (true) {

                Socket clientSocket = serverSocket.accept();

                System.out.println(
                    "Client connected: "
                    + clientSocket.getInetAddress()
                    + ":"
                    + clientSocket.getPort()
                );

                ClientHandler handler =
                        new ClientHandler(clientSocket);

                Thread thread = new Thread(handler);

                thread.start();
            }

        } catch (IOException e) {

            System.out.println("Cannot start server!");
            e.printStackTrace();
        }
    }
}