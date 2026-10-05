package minesweeperonline;

import minesweeperonline.client.network.ServerConnection;
import minesweeperonline.common.model.User;
import minesweeperonline.common.protocol.JsonProtocol;
import minesweeperonline.common.protocol.MessageType;
import minesweeperonline.database.DatabaseConnection;
import minesweeperonline.database.UserDAO;
import minesweeperonline.server.MainServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verification test for Phase 3:
 * 1. Test DatabaseConnection & UserDAO registration & authentication.
 * 2. Test Client network REGISTER (success & duplicate prevention).
 * 3. Test Client network LOGIN (wrong password & correct password).
 */
public class TestPhase3 {

    private static final int TEST_PORT = 2211;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("       RUNNING PHASE 3 INTEGRATION TEST           ");
        System.out.println("==================================================");

        // 1. Direct DAO verification
        UserDAO userDAO = new UserDAO();
        String testUser = "testuser_" + System.currentTimeMillis() % 10000;
        boolean regOk = userDAO.register(testUser, "secretPass");
        assert regOk : "UserDAO.register failed!";
        System.out.println("[PASS] UserDAO.register created user: " + testUser);

        boolean regDup = userDAO.register(testUser, "secretPass");
        assert !regDup : "UserDAO allowed duplicate username!";
        System.out.println("[PASS] UserDAO duplicate registration rejected as expected.");

        User authFail = userDAO.authenticate(testUser, "wrongPassword");
        assert authFail == null : "UserDAO allowed invalid password!";
        System.out.println("[PASS] UserDAO rejected wrong credentials as expected.");

        User authOk = userDAO.authenticate(testUser, "secretPass");
        assert authOk != null && authOk.getUsername().equals(testUser) : "UserDAO authenticate failed!";
        System.out.println("[PASS] UserDAO authenticated successfully: " + authOk);

        // 2. Start TCP Server on test port
        MainServer server = new MainServer(TEST_PORT);
        Thread serverThread = new Thread(server::start, "Phase3-Server");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300);

        // 3. Client network connection test for REGISTER & LOGIN
        ServerConnection client = new ServerConnection();
        boolean connected = client.connect("127.0.0.1", TEST_PORT);
        assert connected : "Client failed to connect to server!";

        String netUser = "netuser_" + (System.currentTimeMillis() % 10000);
        CountDownLatch regLatch = new CountDownLatch(1);
        CountDownLatch regDupLatch = new CountDownLatch(1);
        CountDownLatch loginFailLatch = new CountDownLatch(1);
        CountDownLatch loginSuccessLatch = new CountDownLatch(1);
        AtomicReference<String> errorReason = new AtomicReference<>("");

        client.addMessageListener(msg -> {
            if (msg.getType() == MessageType.REGISTER_SUCCESS) {
                regLatch.countDown();
            } else if (msg.getType() == MessageType.REGISTER_FAILED) {
                errorReason.set(msg.getPayload().get("reason").getAsString());
                regDupLatch.countDown();
            } else if (msg.getType() == MessageType.LOGIN_FAILED) {
                loginFailLatch.countDown();
            } else if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                loginSuccessLatch.countDown();
            }
        });

        // Test REGISTER via Network
        client.sendMessage(JsonProtocol.createRegister(netUser, "pass123"));
        boolean regNetOk = regLatch.await(3, TimeUnit.SECONDS);
        assert regNetOk : "Client REGISTER request timed out!";
        System.out.println("[PASS] Client received REGISTER_SUCCESS for " + netUser);

        // Test Duplicate REGISTER via Network
        client.sendMessage(JsonProtocol.createRegister(netUser, "pass123"));
        boolean regNetDup = regDupLatch.await(3, TimeUnit.SECONDS);
        assert regNetDup : "Client duplicate REGISTER did not receive REGISTER_FAILED!";
        System.out.println("[PASS] Client received REGISTER_FAILED on duplicate: " + errorReason.get());

        // Test LOGIN with wrong password via Network
        client.sendMessage(JsonProtocol.createLogin(netUser, "wrongPassword"));
        boolean loginFailOk = loginFailLatch.await(3, TimeUnit.SECONDS);
        assert loginFailOk : "Client did not receive LOGIN_FAILED for wrong password!";
        System.out.println("[PASS] Client received LOGIN_FAILED for incorrect credentials.");

        // Test LOGIN with correct password via Network
        client.sendMessage(JsonProtocol.createLogin(netUser, "pass123"));
        boolean loginSuccessOk = loginSuccessLatch.await(3, TimeUnit.SECONDS);
        assert loginSuccessOk : "Client did not receive LOGIN_SUCCESS!";
        System.out.println("[PASS] Client received LOGIN_SUCCESS for valid credentials.");

        client.disconnect("Phase 3 testing complete");
        server.stop();

        System.out.println("==================================================");
        System.out.println("   ALL PHASE 3 TESTS PASSED SUCCESSFULLY!         ");
        System.out.println("==================================================");
    }
}
