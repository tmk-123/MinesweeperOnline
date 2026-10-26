package minesweeperonline;

import com.google.gson.JsonObject;
import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;
import minesweeperonline.server.MainServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TestReconnection {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM TRA RECONNECTION / GRACEFUL DISCONNECT ===");

        // 1. Khoi dong Server tren port test
        int testPort = 2210;
        MainServer server = new MainServer(testPort);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                System.out.println("[Server] Server stopped or error: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(1000);

            // 2. Client 1 (khoi) va Client 2 (k) ket noi
            ServerConnection c1 = new ServerConnection();
            ServerConnection c2 = new ServerConnection();
            c1.connect("localhost", testPort);
            c2.connect("localhost", testPort);

            CountDownLatch loginLatch = new CountDownLatch(2);
            c1.addMessageListener(m -> {
                if (m.getType() == MessageType.LOGIN_SUCCESS) loginLatch.countDown();
            });
            c2.addMessageListener(m -> {
                if (m.getType() == MessageType.LOGIN_SUCCESS) loginLatch.countDown();
            });

            c1.sendMessage(JsonProtocol.createLogin("khoi", "123456"));
            c2.sendMessage(JsonProtocol.createLogin("k", "1"));

            if (!loginLatch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Dang nhap that bai hoac timeout!");
            }
            System.out.println("-> Ca 2 client da dang nhap thanh cong.");

            // 3. Tim tran nhanh de ghep doi
            CountDownLatch matchLatch = new CountDownLatch(2);
            CountDownLatch startLatch = new CountDownLatch(2);
            AtomicReference<String> gameIdRef = new AtomicReference<>();
            c1.addMessageListener(m -> {
                if (m.getType() == MessageType.MATCH_CREATED) {
                    gameIdRef.set(m.getPayload().get("gameId").getAsString());
                    matchLatch.countDown();
                } else if (m.getType() == MessageType.GAME_START) {
                    startLatch.countDown();
                }
            });
            c2.addMessageListener(m -> {
                if (m.getType() == MessageType.MATCH_CREATED) {
                    matchLatch.countDown();
                } else if (m.getType() == MessageType.GAME_START) {
                    startLatch.countDown();
                }
            });

            c1.sendMessage(JsonProtocol.createQuickMatch());
            c2.sendMessage(JsonProtocol.createQuickMatch());

            if (!matchLatch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Ghep tran timeout!");
            }
            System.out.println("-> Match created thanh cong voi gameId: " + gameIdRef.get());

            System.out.println("-> Cho dem nguoc READY ket thuc va GAME_START...");
            if (!startLatch.await(7, TimeUnit.SECONDS)) {
                throw new AssertionError("Khong nhan duoc GAME_START!");
            }
            System.out.println("-> GAME_START bat dau thi dau!");

            // 4. Client 1 thao tac mo o (0, 0)
            c1.sendMessage(JsonProtocol.createOpenCell(gameIdRef.get(), 0, 0));
            Thread.sleep(600);

            // 5. Client 2 dot ngot rot mang (disconnect)
            CountDownLatch dcWaitingLatch = new CountDownLatch(1);
            c1.addMessageListener(m -> {
                if (m.getType() == MessageType.OPPONENT_DISCONNECTED) {
                    JsonObject p = m.getPayload();
                    if (p != null && "WAITING_RECONNECT".equals(p.has("status") ? p.get("status").getAsString() : "")) {
                        System.out.println("-> Client 1 nhan thong bao DOI THU MAT KET NOI (WAITING_RECONNECT 15s)");
                        dcWaitingLatch.countDown();
                    }
                }
            });

            System.out.println("-> Client 2 (k) dot ngot ngat ket noi TCP...");
            c2.disconnect("Dot ngot mat ket noi");

            if (!dcWaitingLatch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Client 1 khong nhan duoc thong bao WAITING_RECONNECT!");
            }

            // 6. Client 2 ket noi lai bang socket moi va dang nhap lai
            System.out.println("-> Client 2 (k) mo ket noi TCP moi va dang nhap lai...");
            ServerConnection c2New = new ServerConnection();
            c2New.connect("localhost", testPort);

            CountDownLatch reconnectedLatch = new CountDownLatch(2);
            AtomicBoolean stateRestored = new AtomicBoolean(false);

            c2New.addMessageListener(m -> {
                if (m.getType() == MessageType.GAME_RECONNECTED) {
                    JsonObject p = m.getPayload();
                    System.out.println("-> Client 2 nhan duoc GAME_RECONNECTED: gameId=" +
                            p.get("gameId").getAsString() + ", remainingSeconds=" + p.get("remainingSeconds").getAsInt());
                    if (p.has("myOpenedCells") && p.has("oppOpenedCoords")) {
                        stateRestored.set(true);
                    }
                    reconnectedLatch.countDown();
                }
            });

            c1.addMessageListener(m -> {
                if (m.getType() == MessageType.OPPONENT_RECONNECTED) {
                    System.out.println("-> Client 1 nhan duoc OPPONENT_RECONNECTED: " + m.getPayload().get("message").getAsString());
                    reconnectedLatch.countDown();
                }
            });

            c2New.sendMessage(JsonProtocol.createLogin("k", "1"));

            if (!reconnectedLatch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Khong nhan duoc GAME_RECONNECTED hoac OPPONENT_RECONNECTED!");
            }

            if (!stateRestored.get()) {
                throw new AssertionError("Payload GAME_RECONNECTED khong chua day du trang thai ban co!");
            }

            System.out.println("-> TEST RECONNECTION / GRACEFUL DISCONNECT THANH CONG 100%!");

            c1.disconnect("Ket thuc test");
            c2New.disconnect("Ket thuc test");

        } catch (Exception e) {
            System.err.println("TEST THAT BAI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            server.stop();
            System.exit(0);
        }
    }
}
