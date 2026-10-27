package minesweeperonline;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.MessageType;
import minesweeperonline.server.MainServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TestQuickMatch {

    private static final int TEST_PORT = 2212;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("          RUNNING QUICK MATCH TEST                ");
        System.out.println("==================================================");

        MainServer server = new MainServer(TEST_PORT);
        Thread serverThread = new Thread(server::start, "TestServer-QuickMatch");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300);

        ServerConnection client1 = new ServerConnection();
        CountDownLatch loginLatch1 = new CountDownLatch(1);
        CountDownLatch matchLatch1 = new CountDownLatch(1);
        AtomicReference<String> opp1 = new AtomicReference<>("");

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                loginLatch1.countDown();
            } else if (msg.getType() == MessageType.MATCH_CREATED) {
                opp1.set(msg.getPayload().get("opponentUsername").getAsString());
                matchLatch1.countDown();
            }
        });

        assert client1.connect("127.0.0.1", TEST_PORT) : "Client 1 failed to connect!";
        client1.sendMessage(JsonProtocol.createLogin("k", "1"));
        assert loginLatch1.await(3, TimeUnit.SECONDS) : "Client 1 login timed out!";
        System.out.println("[PASS] Client 1 (k) logged in.");

        ServerConnection client2 = new ServerConnection();
        CountDownLatch loginLatch2 = new CountDownLatch(1);
        CountDownLatch matchLatch2 = new CountDownLatch(1);
        AtomicReference<String> opp2 = new AtomicReference<>("");

        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                loginLatch2.countDown();
            } else if (msg.getType() == MessageType.MATCH_CREATED) {
                opp2.set(msg.getPayload().get("opponentUsername").getAsString());
                matchLatch2.countDown();
            }
        });

        assert client2.connect("127.0.0.1", TEST_PORT) : "Client 2 failed to connect!";
        client2.sendMessage(JsonProtocol.createLogin("khoi", "123456"));
        assert loginLatch2.await(3, TimeUnit.SECONDS) : "Client 2 login timed out!";
        System.out.println("[PASS] Client 2 (khoi) logged in.");

        // Client 1 bấm tìm trận nhanh
        System.out.println("Client 1 joining quick match queue...");
        client1.sendMessage(JsonProtocol.createQuickMatch());
        Thread.sleep(200);

        // Client 2 bấm tìm trận nhanh -> Server tự động ghép cặp
        System.out.println("Client 2 joining quick match queue...");
        client2.sendMessage(JsonProtocol.createQuickMatch());

        boolean ok1 = matchLatch1.await(3, TimeUnit.SECONDS);
        boolean ok2 = matchLatch2.await(3, TimeUnit.SECONDS);

        assert ok1 : "Client 1 did not receive MATCH_CREATED!";
        assert ok2 : "Client 2 did not receive MATCH_CREATED!";
        assert "khoi".equals(opp1.get()) : "Client 1 opponent is wrong: " + opp1.get();
        assert "k".equals(opp2.get()) : "Client 2 opponent is wrong: " + opp2.get();

        System.out.println("[PASS] Quick match successfully matched [" + opp2.get() + "] with [" + opp1.get() + "]!");

        client1.disconnect("Test finished");
        client2.disconnect("Test finished");
        server.stop();

        System.out.println("==================================================");
        System.out.println("       QUICK MATCH TEST PASSED SUCCESSFULLY!      ");
        System.out.println("==================================================");
    }
}
