package minesweeperonline;

import com.google.gson.JsonObject;
import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;
import minesweeperonline.server.MainServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verification test for Phase 8 (Disconnect + Result + Score) & Phase 9 (Play Again):
 * 1. Match creation & real-time move synchronization.
 * 2. Mid-match Forfeit -> Verify WIN/LOSE score deltas (+5 / -5).
 * 3. Mutual Play Again -> Verify new match session creation (rematch).
 * 4. Mid-match Disconnection -> Verify opponent receives OPPONENT_DISCONNECTED victory.
 */
public class TestPhase8_9 {

    private static final int TEST_PORT = 2212;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("       RUNNING PHASE 8 & 9 INTEGRATION TEST       ");
        System.out.println("==================================================");

        // 1. Start Server
        MainServer server = new MainServer(TEST_PORT);
        new Thread(server::start, "TestServer-Phase89").start();
        Thread.sleep(500);

        ServerConnection client1 = new ServerConnection();
        ServerConnection client2 = new ServerConnection();

        boolean c1Connected = client1.connect("127.0.0.1", TEST_PORT);
        boolean c2Connected = client2.connect("127.0.0.1", TEST_PORT);
        assert c1Connected && c2Connected : "Failed to connect test clients!";

        String user1 = "alpha_" + System.currentTimeMillis() % 10000;
        String user2 = "beta_" + System.currentTimeMillis() % 10000;

        // Register & Login both
        client1.sendMessage(JsonProtocol.createRegister(user1, "p123"));
        client2.sendMessage(JsonProtocol.createRegister(user2, "p123"));
        Thread.sleep(300);

        CountDownLatch loginLatch = new CountDownLatch(2);
        AtomicReference<Integer> user1Id = new AtomicReference<>();
        AtomicReference<Integer> user2Id = new AtomicReference<>();

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                user1Id.set(msg.getPayload().get("userId").getAsInt());
                loginLatch.countDown();
            }
        });
        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                user2Id.set(msg.getPayload().get("userId").getAsInt());
                loginLatch.countDown();
            }
        });

        client1.sendMessage(JsonProtocol.createLogin(user1, "p123"));
        client2.sendMessage(JsonProtocol.createLogin(user2, "p123"));

        boolean loggedIn = loginLatch.await(3, TimeUnit.SECONDS);
        assert loggedIn : "Login timed out!";
        System.out.println("[PASS] Both test players authenticated: " + user1 + "(id=" + user1Id.get() + "), " + user2 + "(id=" + user2Id.get() + ")");

        // 2. Challenge & Match Creation
        CountDownLatch matchCreatedLatch = new CountDownLatch(2);
        AtomicReference<String> gameIdRef = new AtomicReference<>();

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.MATCH_CREATED) {
                gameIdRef.set(msg.getPayload().get("gameId").getAsString());
                matchCreatedLatch.countDown();
            }
        });
        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.CHALLENGE_REQUEST) {
                // Accept challenge
                int challengerId = msg.getPayload().get("fromUserId").getAsInt();
                client2.sendMessage(JsonProtocol.createAccept(challengerId));
            } else if (msg.getType() == MessageType.MATCH_CREATED) {
                matchCreatedLatch.countDown();
            }
        });

        client1.sendMessage(JsonProtocol.createChallenge(user2Id.get(), user2));
        boolean matchCreated = matchCreatedLatch.await(3, TimeUnit.SECONDS);
        assert matchCreated : "Match creation failed!";
        String gameId = gameIdRef.get();
        System.out.println("[PASS] Match created with ID: " + gameId);

        // Wait for READY 5s countdown -> GAME_START
        CountDownLatch startLatch = new CountDownLatch(2);
        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.GAME_START) startLatch.countDown();
        });
        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.GAME_START) startLatch.countDown();
        });
        System.out.println("[INFO] Waiting for 5s READY countdown before GAME_START...");
        boolean started = startLatch.await(7, TimeUnit.SECONDS);
        assert started : "Game start countdown timed out!";
        System.out.println("[PASS] GAME_START received by both players, entering PLAYING state.");

        // 3. Test In-Game Move & Opponent Sync
        CountDownLatch updateLatch = new CountDownLatch(1);
        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.GAME_UPDATE) {
                JsonObject p = msg.getPayload();
                if (p.has("flagRow") && p.get("flagRow").getAsInt() == 2 && p.get("flagCol").getAsInt() == 3) {
                    updateLatch.countDown();
                }
            }
        });

        // Player 1 places flag
        client1.sendMessage(JsonProtocol.createPlaceFlag(gameId, 2, 3));
        boolean updateReceived = updateLatch.await(2, TimeUnit.SECONDS);
        assert updateReceived : "Opponent did not receive real-time flag update!";
        System.out.println("[PASS] Real-time move sync verified: Client 2 received flag update from Client 1.");

        // 4. Test Phase 8: Forfeit outcome & score arbitration
        CountDownLatch forfeitLatch = new CountDownLatch(2);
        AtomicReference<Integer> p1Delta = new AtomicReference<>();
        AtomicReference<Integer> p2Delta = new AtomicReference<>();

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.GAME_LOSE) {
                p1Delta.set(msg.getPayload().get("scoreDelta").getAsInt());
                forfeitLatch.countDown();
            }
        });
        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.GAME_WIN) {
                p2Delta.set(msg.getPayload().get("scoreDelta").getAsInt());
                forfeitLatch.countDown();
            }
        });

        // Player 1 forfeits
        client1.sendMessage(JsonProtocol.createForfeit(gameId));
        boolean forfeitHandled = forfeitLatch.await(3, TimeUnit.SECONDS);
        assert forfeitHandled : "Forfeit handling timed out!";
        assert p1Delta.get() == -5 : "Forfeiting player scoreDelta must be -5!";
        assert p2Delta.get() == 5 : "Winner scoreDelta must be +5!";
        System.out.println("[PASS] Phase 8 Forfeit verified: Loser delta=" + p1Delta.get() + ", Winner delta=" + p2Delta.get());

        // 5. Test Phase 9: Play Again Rematch flow
        CountDownLatch rematchLatch = new CountDownLatch(2);
        AtomicReference<String> newGameIdRef = new AtomicReference<>();

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.MATCH_CREATED) {
                newGameIdRef.set(msg.getPayload().get("gameId").getAsString());
                rematchLatch.countDown();
            }
        });
        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.MATCH_CREATED) {
                rematchLatch.countDown();
            }
        });

        // Both players request Play Again
        client1.sendMessage(JsonProtocol.createPlayAgain(gameId));
        Thread.sleep(100);
        client2.sendMessage(JsonProtocol.createPlayAgain(gameId));

        boolean rematchStarted = rematchLatch.await(3, TimeUnit.SECONDS);
        assert rematchStarted : "Rematch creation failed!";
        String newGameId = newGameIdRef.get();
        assert !newGameId.equals(gameId) : "Rematch should generate a brand new game ID!";
        System.out.println("[PASS] Phase 9 Rematch verified: Brand new game session created with ID: " + newGameId);

        // Wait for rematch GAME_START
        CountDownLatch rematchStartLatch = new CountDownLatch(1);
        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.GAME_START) {
                rematchStartLatch.countDown();
            }
        });
        boolean rematchPlaying = rematchStartLatch.await(7, TimeUnit.SECONDS);
        assert rematchPlaying : "Rematch GAME_START timed out!";
        System.out.println("[PASS] Rematch entered PLAYING state.");

        // 6. Test Disconnect Mid-Match (Phase 8)
        CountDownLatch dcLatch = new CountDownLatch(1);
        AtomicReference<Integer> dcDelta = new AtomicReference<>();

        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.OPPONENT_DISCONNECTED) {
                dcDelta.set(msg.getPayload().get("scoreDelta").getAsInt());
                dcLatch.countDown();
            }
        });

        // Client 1 disconnects mid-game
        client1.disconnect("Simulated disconnect test");
        boolean dcHandled = dcLatch.await(3, TimeUnit.SECONDS);
        assert dcHandled : "Opponent disconnect notification timed out!";
        assert dcDelta.get() == 5 : "Remaining player should be awarded victory (+5 points)!";
        System.out.println("[PASS] Phase 8 Disconnect verified: Remaining player awarded victory (+5 points).");

        client2.disconnect("Test completed");
        server.stop();
        System.out.println("==================================================");
        System.out.println("   ALL PHASE 8 & 9 CHECKS PASSED SUCCESSFULLY!    ");
        System.out.println("==================================================");
    }
}
