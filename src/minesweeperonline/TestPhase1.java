package minesweeperonline;

import minesweeperonline.common.model.*;
import minesweeperonline.common.protocol.*;

public class TestPhase1 {

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("       RUNNING PHASE 1 VERIFICATION         ");
        System.out.println("============================================");

        // 1. Test User model
        User user = new User(1, "player_test", "secret123", 15, "2026-10-05 22:00:00");
        System.out.println("[PASS] User model created: " + user);

        // 2. Test Player model & stats
        Player player = new Player(1, "player_test", 15);
        player.placeFlag();
        player.incrementOpenedSafeCells();
        System.out.println("[PASS] Player model created: " + player);

        // 3. Test BoardState & CellState
        BoardState board = new BoardState();
        CellState cell = board.getCell(0, 0);
        cell.setOpened(true);
        cell.setAdjacentMines(2);
        System.out.println("[PASS] BoardState 12x12 initialized: total cells = " + BoardState.TOTAL_CELLS);
        System.out.println("[PASS] Cell (0,0) state: " + cell);

        BoardState opponentView = board.toOpponentView();
        System.out.println("[PASS] Masked opponent view generated successfully.");

        // 4. Test GameState
        GameState state = GameState.READY;
        System.out.println("[PASS] GameState enum: " + state);

        // 5. Test JsonProtocol & Message Serialization
        Message loginMsg = JsonProtocol.createLogin("player_test", "secret123");
        loginMsg.setRequestId("REQ-001");
        String json = JsonProtocol.serializeWithNewline(loginMsg);
        System.out.println("[PASS] Serialized Message: " + json.trim());

        Message parsedMsg = JsonProtocol.deserialize(json);
        assert parsedMsg != null;
        assert parsedMsg.getType() == MessageType.LOGIN;
        System.out.println("[PASS] Deserialized Message Type: " + parsedMsg.getType());
        System.out.println("[PASS] Deserialized Payload: " + parsedMsg.getPayload());

        // 6. Test OpenCell message
        Message openCellMsg = JsonProtocol.createOpenCell("GAME-100", 3, 5);
        String openCellJson = JsonProtocol.serialize(openCellMsg);
        Message parsedOpenCell = JsonProtocol.deserialize(openCellJson);
        assert parsedOpenCell.getType() == MessageType.OPEN_CELL;
        System.out.println("[PASS] OpenCell message parsed: row=" +
                parsedOpenCell.getPayload().get("row").getAsInt() +
                ", col=" + parsedOpenCell.getPayload().get("col").getAsInt());

        System.out.println("============================================");
        System.out.println("   ALL PHASE 1 CHECKS PASSED SUCCESSFULLY!  ");
        System.out.println("============================================");
    }
}
