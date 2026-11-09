
package minesweeperonline;

/** Điểm chạy các chương trình kiểm tra từ project NetBeans. */
public class MinesweeperOnline {

    /** Chạy tuần tự các chương trình kiểm tra của project NetBeans; chạy MainServer/MainClient riêng để chơi. */
    public static void main(String[] args) throws Exception {
        System.out.println(">>> Chay MinesweeperOnline (Main Class cua Project NetBeans) <<<");
        TestPhase1.main(args);
        System.out.println();
        TestPhase2.main(args);
        System.out.println();
        TestPhase3.main(args);
        System.out.println();
        TestPhase6.main(args);
        System.out.println();
        TestPhase8_9.main(args);
        System.out.println();
        TestLeaderboard.main(args);
        System.out.println();
        TestMatchHistory.main(args);
    }
    
}
