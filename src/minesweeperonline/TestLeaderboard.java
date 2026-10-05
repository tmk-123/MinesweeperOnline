package minesweeperonline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.LeaderboardEntry;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.MessageType;
import minesweeperonline.database.UserDAO;
import minesweeperonline.server.MainServer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verification test for Leaderboard feature:
 * 1. UserDAO getLeaderboard ranking verification.
 * 2. Client-Server TCP query and response verification.
 */
public class TestLeaderboard {

    private static final int TEST_PORT = 2213;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("       RUNNING LEADERBOARD INTEGRATION TEST       ");
        System.out.println("==================================================");

        // 1. Direct DAO Verification
        UserDAO userDAO = new UserDAO();
        List<LeaderboardEntry> list = userDAO.getLeaderboard(10);
        assert list != null && !list.isEmpty() : "Leaderboard list should not be empty!";
        System.out.println("[PASS] UserDAO returned " + list.size() + " leaderboard entries:");
        for (LeaderboardEntry e : list) {
            System.out.println("   " + e);
        }

        // Verify ordering: score of item i >= score of item i+1
        for (int i = 0; i < list.size() - 1; i++) {
            assert list.get(i).getTotalScore() >= list.get(i + 1).getTotalScore() :
                    "Leaderboard sorting violation at index " + i;
            assert list.get(i).getRank() == i + 1 : "Rank numbering should start at 1 and be consecutive!";
        }
        System.out.println("[PASS] Leaderboard score ordering and rank numbering verified.");

        // 2. Client-Server TCP Protocol Verification
        MainServer server = new MainServer(TEST_PORT);
        new Thread(server::start, "TestServer-Leaderboard").start();
        Thread.sleep(500);

        ServerConnection client = new ServerConnection();
        boolean connected = client.connect("127.0.0.1", TEST_PORT);
        assert connected : "Failed to connect to test server!";

        String testUser = "top_player_" + (System.currentTimeMillis() % 10000);
        client.sendMessage(JsonProtocol.createRegister(testUser, "pass123"));
        Thread.sleep(200);

        CountDownLatch loginLatch = new CountDownLatch(1);
        client.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                loginLatch.countDown();
            }
        });
        client.sendMessage(JsonProtocol.createLogin(testUser, "pass123"));
        boolean loggedIn = loginLatch.await(3, TimeUnit.SECONDS);
        assert loggedIn : "Login failed!";

        // Send GET_LEADERBOARD
        CountDownLatch lbLatch = new CountDownLatch(1);
        AtomicReference<JsonArray> lbArrayRef = new AtomicReference<>();

        client.addMessageListener(msg -> {
            if (msg.getType() == MessageType.LEADERBOARD) {
                JsonObject p = msg.getPayload();
                if (p != null && p.has("leaderboard") && p.get("leaderboard").isJsonArray()) {
                    lbArrayRef.set(p.getAsJsonArray("leaderboard"));
                    lbLatch.countDown();
                }
            }
        });

        client.sendMessage(JsonProtocol.createGetLeaderboard());
        boolean lbReceived = lbLatch.await(3, TimeUnit.SECONDS);
        assert lbReceived : "Did not receive LEADERBOARD response from server!";

        JsonArray arr = lbArrayRef.get();
        assert arr.size() > 0 : "LEADERBOARD payload array should not be empty!";
        System.out.println("[PASS] Client received LEADERBOARD message over TCP Socket (" + arr.size() + " entries):");
        for (int i = 0; i < Math.min(arr.size(), 3); i++) {
            JsonObject o = arr.get(i).getAsJsonObject();
            System.out.println("   Rank " + o.get("rank").getAsInt() + ": " +
                    o.get("username").getAsString() + " | Score=" + o.get("totalScore").getAsInt() +
                    " | Wins=" + o.get("wins").getAsInt());
        }

        client.disconnect("Leaderboard test complete");
        server.stop();

        System.out.println("==================================================");
        System.out.println("     ALL LEADERBOARD CHECKS PASSED 100%!          ");
        System.out.println("==================================================");
    }
}
