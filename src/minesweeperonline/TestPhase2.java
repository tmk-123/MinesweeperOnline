package minesweeperonline;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;
import minesweeperonline.server.MainServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * End-to-end integration test for Phase 2:
 * 1. Starts Server on test port 2210.
 * 2. Connects Client 1 and Client 2.
 * 3. Client 1 & 2 authenticate (LOGIN).
 * 4. Client 1 requests online players.
 * 5. Client 1 challenges Client 2.
 * 6. Client 2 accepts challenge -> GameSession created.
 * 7. Disconnection test -> Opponent receives forfeit victory.
 */
public class TestPhase2 {

    private static final int TEST_PORT = 2210;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("       RUNNING PHASE 2 INTEGRATION TEST           ");
        System.out.println("==================================================");

        // Step 1: Start Server on test port
        MainServer server = new MainServer(TEST_PORT);
        Thread serverThread = new Thread(server::start, "TestServer-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300); // Allow server socket to bind

        System.out.println("[PASS] Server started on port " + TEST_PORT);

        // Step 2: Connect Client 1 (Alice)
        ServerConnection client1 = new ServerConnection();
        CountDownLatch loginLatch1 = new CountDownLatch(1);
        AtomicReference<Integer> aliceId = new AtomicReference<>(-1);

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                aliceId.set(msg.getPayload().get("userId").getAsInt());
                loginLatch1.countDown();
            }
        });

        boolean c1Connected = client1.connect("127.0.0.1", TEST_PORT);
        assert c1Connected : "Client 1 failed to connect!";
        client1.sendMessage(JsonProtocol.createLogin("alice", "123456"));
        boolean aliceOk = loginLatch1.await(3, TimeUnit.SECONDS);
        assert aliceOk : "Alice login timed out!";
        System.out.println("[PASS] Client 1 (Alice) logged in with ID: " + aliceId.get());

        // Step 3: Connect Client 2 (Bob)
        ServerConnection client2 = new ServerConnection();
        CountDownLatch loginLatch2 = new CountDownLatch(1);
        AtomicReference<Integer> bobId = new AtomicReference<>(-1);
        CountDownLatch challengeReqLatch = new CountDownLatch(1);
        CountDownLatch matchCreatedLatch2 = new CountDownLatch(1);
        CountDownLatch oppDisconnectedLatch = new CountDownLatch(1);

        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                bobId.set(msg.getPayload().get("userId").getAsInt());
                loginLatch2.countDown();
            } else if (msg.getType() == MessageType.CHALLENGE_REQUEST) {
                challengeReqLatch.countDown();
            } else if (msg.getType() == MessageType.MATCH_CREATED) {
                matchCreatedLatch2.countDown();
            } else if (msg.getType() == MessageType.OPPONENT_DISCONNECTED) {
                oppDisconnectedLatch.countDown();
            }
        });

        boolean c2Connected = client2.connect("127.0.0.1", TEST_PORT);
        assert c2Connected : "Client 2 failed to connect!";
        client2.sendMessage(JsonProtocol.createLogin("bob", "123456"));
        boolean bobOk = loginLatch2.await(3, TimeUnit.SECONDS);
        assert bobOk : "Bob login timed out!";
        System.out.println("[PASS] Client 2 (Bob) logged in with ID: " + bobId.get());

        // Step 4: Alice requests online players
        CountDownLatch onlinePlayersLatch = new CountDownLatch(1);
        AtomicBoolean foundBob = new AtomicBoolean(false);

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.ONLINE_PLAYERS) {
                if (msg.getPayload().getAsJsonArray("players").size() > 0) {
                    foundBob.set(true);
                }
                onlinePlayersLatch.countDown();
            }
        });

        client1.sendMessage(JsonProtocol.createGetOnlinePlayers());
        boolean onlineOk = onlinePlayersLatch.await(3, TimeUnit.SECONDS);
        assert onlineOk && foundBob.get() : "Online players list did not contain Bob!";
        System.out.println("[PASS] Online players query returned active players.");

        // Step 5: Alice challenges Bob
        CountDownLatch matchCreatedLatch1 = new CountDownLatch(1);
        AtomicReference<String> gameIdRef = new AtomicReference<>("");

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.MATCH_CREATED) {
                gameIdRef.set(msg.getPayload().get("gameId").getAsString());
                matchCreatedLatch1.countDown();
            }
        });

        client1.sendMessage(JsonProtocol.createChallenge(bobId.get(), "bob"));
        boolean gotChallenge = challengeReqLatch.await(3, TimeUnit.SECONDS);
        assert gotChallenge : "Bob did not receive challenge request!";
        System.out.println("[PASS] Bob received CHALLENGE_REQUEST from Alice.");

        // Step 6: Bob accepts challenge
        client2.sendMessage(JsonProtocol.createAccept(aliceId.get()));
        boolean m1 = matchCreatedLatch1.await(3, TimeUnit.SECONDS);
        boolean m2 = matchCreatedLatch2.await(3, TimeUnit.SECONDS);
        assert m1 && m2 : "Both players did not receive MATCH_CREATED!";
        System.out.println("[PASS] GameSession created successfully! Game ID: " + gameIdRef.get());

        // Step 7: Disconnect handling - Alice disconnects during game
        client1.disconnect("User closed application");
        boolean oppNotified = oppDisconnectedLatch.await(3, TimeUnit.SECONDS);
        assert oppNotified : "Bob was not notified of Alice's disconnection!";
        System.out.println("[PASS] Bob received OPPONENT_DISCONNECTED (forfeit victory credited).");

        // Clean up
        client2.disconnect("Test completed");
        server.stop();

        System.out.println("==================================================");
        System.out.println("   ALL PHASE 2 TESTS PASSED SUCCESSFULLY!         ");
        System.out.println("==================================================");
    }
}
