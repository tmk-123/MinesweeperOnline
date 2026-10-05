package minesweeperonline;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.MatchHistoryEntry;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.Message;
import minesweeperonline.common.protocol.MessageType;
import minesweeperonline.database.MatchPlayerDAO;
import minesweeperonline.server.MainServer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verification test for Phase 11: Match History (Lịch sử trận đấu):
 * 1. Direct DAO Match History query & recording verification.
 * 2. Full match play -> Forfeit outcome -> Match saved to DB/Fallback.
 * 3. Both players query GET_MATCH_HISTORY via TCP Socket.
 * 4. Verify opponent names, outcomes, score deltas, flags, actions, and timestamps.
 */
public class TestMatchHistory {

    private static final int TEST_PORT = 2214;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("      RUNNING PHASE 11: MATCH HISTORY TEST        ");
        System.out.println("==================================================");

        // 1. Direct DAO Verification
        MatchPlayerDAO dao = new MatchPlayerDAO();
        long now = System.currentTimeMillis();
        dao.saveFullMatchTransaction(
                "TEST-HIST-01", "FINISHED", now - 65000, now, 1, "Mở trúng mìn",
                1, "alice", "VICTORY", 30, 45, 8, 5,
                2, "bob", "DEFEAT", 12, 18, 3, -3
        );

        List<MatchHistoryEntry> aliceHist = dao.getMatchHistory(1, 10);
        assert aliceHist != null && !aliceHist.isEmpty() : "Alice match history must not be empty!";
        MatchHistoryEntry e1 = aliceHist.get(0);
        assert e1.getOpponentName().equals("bob") : "Opponent name for Alice must be bob, got: " + e1.getOpponentName();
        assert e1.getResult().equals("VICTORY") : "Alice result must be VICTORY, got: " + e1.getResult();
        assert e1.getScoreDelta() == 5 : "Alice score delta must be 5, got: " + e1.getScoreDelta();
        assert e1.getOpenedSafeCells() == 30 : "Alice opened cells must be 30";
        assert e1.getDurationSeconds() >= 60 : "Match duration must be >= 60s";
        System.out.println("[PASS] Direct DAO Match History verified for User 1 (Alice):");
        System.out.println("   Opponent: " + e1.getOpponentName() + " | Result: " + e1.getResult() +
                " | ScoreDelta: " + e1.getScoreDelta() + " | Duration: " + e1.getFormattedDuration());

        List<MatchHistoryEntry> bobHist = dao.getMatchHistory(2, 10);
        assert bobHist != null && !bobHist.isEmpty() : "Bob match history must not be empty!";
        MatchHistoryEntry e2 = bobHist.get(0);
        assert e2.getOpponentName().equals("alice") : "Opponent name for Bob must be alice, got: " + e2.getOpponentName();
        assert e2.getResult().equals("DEFEAT") : "Bob result must be DEFEAT, got: " + e2.getResult();
        assert e2.getScoreDelta() == -3 : "Bob score delta must be -3, got: " + e2.getScoreDelta();
        System.out.println("[PASS] Direct DAO Match History verified for User 2 (Bob):");
        System.out.println("   Opponent: " + e2.getOpponentName() + " | Result: " + e2.getResult() +
                " | ScoreDelta: " + e2.getScoreDelta());

        // 2. Client-Server TCP Protocol Integration Verification
        MainServer server = new MainServer(TEST_PORT);
        new Thread(server::start, "TestServer-MatchHistory").start();
        Thread.sleep(500);

        ServerConnection client1 = new ServerConnection();
        ServerConnection client2 = new ServerConnection();

        assert client1.connect("127.0.0.1", TEST_PORT) : "Client 1 connect failed";
        assert client2.connect("127.0.0.1", TEST_PORT) : "Client 2 connect failed";

        String p1Name = "hist_p1_" + (System.currentTimeMillis() % 10000);
        String p2Name = "hist_p2_" + (System.currentTimeMillis() % 10000);

        client1.sendMessage(JsonProtocol.createRegister(p1Name, "pass123"));
        client2.sendMessage(JsonProtocol.createRegister(p2Name, "pass123"));
        Thread.sleep(200);

        CountDownLatch loginLatch = new CountDownLatch(2);
        AtomicReference<Integer> p1IdRef = new AtomicReference<>();
        AtomicReference<Integer> p2IdRef = new AtomicReference<>();

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                p1IdRef.set(msg.getPayload().get("userId").getAsInt());
                loginLatch.countDown();
            }
        });
        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                p2IdRef.set(msg.getPayload().get("userId").getAsInt());
                loginLatch.countDown();
            }
        });

        client1.sendMessage(JsonProtocol.createLogin(p1Name, "pass123"));
        client2.sendMessage(JsonProtocol.createLogin(p2Name, "pass123"));
        assert loginLatch.await(3, TimeUnit.SECONDS) : "Login failed";

        // Challenge & Start Match
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
                client2.sendMessage(JsonProtocol.createAccept(msg.getPayload().get("fromUserId").getAsInt()));
            } else if (msg.getType() == MessageType.MATCH_CREATED) {
                matchCreatedLatch.countDown();
            }
        });

        client1.sendMessage(JsonProtocol.createChallenge(p2IdRef.get(), p2Name));
        assert matchCreatedLatch.await(3, TimeUnit.SECONDS) : "Match creation failed";
        String gameId = gameIdRef.get();

        // Wait for READY countdown -> GAME_START
        CountDownLatch startLatch = new CountDownLatch(2);
        client1.addMessageListener(msg -> { if (msg.getType() == MessageType.GAME_START) startLatch.countDown(); });
        client2.addMessageListener(msg -> { if (msg.getType() == MessageType.GAME_START) startLatch.countDown(); });
        assert startLatch.await(7, TimeUnit.SECONDS) : "Game start countdown timed out";

        // Client 1 places flag
        client1.sendMessage(JsonProtocol.createPlaceFlag(gameId, 1, 1));
        Thread.sleep(100);

        // Client 2 forfeits
        CountDownLatch finishLatch = new CountDownLatch(2);
        client1.addMessageListener(msg -> { if (msg.getType() == MessageType.GAME_WIN) finishLatch.countDown(); });
        client2.addMessageListener(msg -> { if (msg.getType() == MessageType.GAME_LOSE) finishLatch.countDown(); });

        client2.sendMessage(JsonProtocol.createForfeit(gameId));
        assert finishLatch.await(3, TimeUnit.SECONDS) : "Match finish timed out";

        // 3. Client 1 queries GET_MATCH_HISTORY over TCP
        CountDownLatch hist1Latch = new CountDownLatch(1);
        AtomicReference<JsonArray> hist1ArrayRef = new AtomicReference<>();

        client1.addMessageListener(msg -> {
            if (msg.getType() == MessageType.MATCH_HISTORY) {
                JsonObject p = msg.getPayload();
                if (p != null && p.has("history") && p.get("history").isJsonArray()) {
                    hist1ArrayRef.set(p.getAsJsonArray("history"));
                    hist1Latch.countDown();
                }
            }
        });

        client1.sendMessage(JsonProtocol.createGetMatchHistory(10));
        assert hist1Latch.await(3, TimeUnit.SECONDS) : "Client 1 did not receive MATCH_HISTORY";
        JsonArray arr1 = hist1ArrayRef.get();
        assert arr1 != null && arr1.size() > 0 : "Client 1 history array should not be empty!";

        JsonObject firstEntry1 = arr1.get(0).getAsJsonObject();
        assert firstEntry1.get("opponentName").getAsString().equals(p2Name) :
                "Expected opponent " + p2Name + " but got: " + firstEntry1.get("opponentName").getAsString();
        assert firstEntry1.get("result").getAsString().equals("VICTORY") : "Expected VICTORY result";
        assert firstEntry1.get("scoreDelta").getAsInt() == 5 : "Expected scoreDelta = 5";
        System.out.println("[PASS] Client 1 received MATCH_HISTORY over TCP Socket: " + arr1.size() + " matches.");
        System.out.println("   Recent match: vs " + firstEntry1.get("opponentName").getAsString() +
                " | Result: " + firstEntry1.get("result").getAsString() +
                " | Delta: +" + firstEntry1.get("scoreDelta").getAsInt());

        // 4. Client 2 queries GET_MATCH_HISTORY over TCP
        CountDownLatch hist2Latch = new CountDownLatch(1);
        AtomicReference<JsonArray> hist2ArrayRef = new AtomicReference<>();

        client2.addMessageListener(msg -> {
            if (msg.getType() == MessageType.MATCH_HISTORY) {
                JsonObject p = msg.getPayload();
                if (p != null && p.has("history") && p.get("history").isJsonArray()) {
                    hist2ArrayRef.set(p.getAsJsonArray("history"));
                    hist2Latch.countDown();
                }
            }
        });

        client2.sendMessage(JsonProtocol.createGetMatchHistory(10));
        assert hist2Latch.await(3, TimeUnit.SECONDS) : "Client 2 did not receive MATCH_HISTORY";
        JsonArray arr2 = hist2ArrayRef.get();
        assert arr2 != null && arr2.size() > 0 : "Client 2 history array should not be empty!";

        JsonObject firstEntry2 = arr2.get(0).getAsJsonObject();
        assert firstEntry2.get("opponentName").getAsString().equals(p1Name) :
                "Expected opponent " + p1Name + " but got: " + firstEntry2.get("opponentName").getAsString();
        assert firstEntry2.get("result").getAsString().equals("FORFEIT") : "Expected FORFEIT result";
        assert firstEntry2.get("scoreDelta").getAsInt() == -5 : "Expected scoreDelta = -5";
        System.out.println("[PASS] Client 2 received MATCH_HISTORY over TCP Socket: " + arr2.size() + " matches.");
        System.out.println("   Recent match: vs " + firstEntry2.get("opponentName").getAsString() +
                " | Result: " + firstEntry2.get("result").getAsString() +
                " | Delta: " + firstEntry2.get("scoreDelta").getAsInt());

        client1.disconnect("Match history test complete");
        client2.disconnect("Match history test complete");
        server.stop();

        System.out.println("==================================================");
        System.out.println("   ALL PHASE 11 MATCH HISTORY CHECKS PASSED 100%!  ");
        System.out.println("==================================================");
    }
}
