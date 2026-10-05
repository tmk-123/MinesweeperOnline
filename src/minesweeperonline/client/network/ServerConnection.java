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
 * Manages the TCP Socket connection from Client to Server.
 * Runs a background listener thread to read newline-delimited JSON messages
 * and dispatches them to registered listeners.
 */
public class ServerConnection {

    public interface MessageListener {
        void onMessageReceived(Message message);
    }

    public interface ConnectionListener {
        void onConnected();
        void onDisconnected(String reason);
    }

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread listenerThread;
    private volatile boolean connected = false;

    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

    public synchronized boolean connect(String host, int port) {
        if (connected) {
            return true;
        }

        try {
            socket = new Socket();
            // Connect with 5-second timeout
            socket.connect(new InetSocketAddress(host, port), 5000);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            connected = true;

            // Start background receiver thread
            listenerThread = new Thread(this::listenForIncomingMessages, "Client-Network-Listener");
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
            // Ignored
        }

        notifyDisconnected(reason);
        System.out.println("[ServerConnection] Disconnected: " + reason);
    }

    private void listenForIncomingMessages() {
        try {
            String line;
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

    public void addMessageListener(MessageListener listener) {
        if (listener != null && !messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null && !connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
        }
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    private void notifyConnected() {
        for (ConnectionListener l : connectionListeners) {
            l.onConnected();
        }
    }

    private void notifyDisconnected(String reason) {
        for (ConnectionListener l : connectionListeners) {
            l.onDisconnected(reason);
        }
    }

    private void notifyMessageReceived(Message message) {
        for (MessageListener l : messageListeners) {
            try {
                l.onMessageReceived(message);
            } catch (Exception e) {
                System.err.println("[ServerConnection] Exception in message listener: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
