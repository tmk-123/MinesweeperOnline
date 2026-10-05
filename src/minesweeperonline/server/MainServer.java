package minesweeperonline.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main TCP Server for MinesweeperOnline.
 * Listens on port 2209 and spawns a dedicated Thread for each connecting Client.
 */
public class MainServer {

    private final int port;
    private final ClientManager clientManager;
    private final GameManager gameManager;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public MainServer(int port) {
        this.port = port;
        this.clientManager = new ClientManager();
        this.gameManager = new GameManager();
    }

    public void start() {
        System.out.println("==================================================");
        System.out.println("      MINESWEEPER ONLINE - TCP SERVER             ");
        System.out.println("==================================================");

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[MainServer] Server started successfully on port: " + port);
            System.out.println("[MainServer] Waiting for client connections...");

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[MainServer] Accepted new connection from: " +
                            clientSocket.getRemoteSocketAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, clientManager, gameManager);
                    Thread clientThread = new Thread(handler, "Client-" + clientSocket.getPort());
                    clientThread.start();

                } catch (IOException e) {
                    if (!running) {
                        System.out.println("[MainServer] Server socket closed.");
                        break;
                    }
                    System.err.println("[MainServer] Error accepting client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("[MainServer] Could not listen on port " + port + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[MainServer] Error closing server socket: " + e.getMessage());
        }
        System.out.println("[MainServer] Server stopped.");
    }

    public ClientManager getClientManager() {
        return clientManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public static void main(String[] args) {
        int port = ServerConfig.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port parameter, using default: " + port);
            }
        }
        MainServer server = new MainServer(port);
        server.start();
    }
}