package minesweeperonline.game;

import minesweeperonline.common.model.BoardState;
import minesweeperonline.common.model.CellState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Bàn cờ Minesweeper do server quản lý (12x12, 23 mìn, 121 ô an toàn).
 * Xử lý các thao tác trong game (mở ô, cắm/gỡ cờ) và mở ô lan rộng bằng BFS flood-fill.
 */
public class Board {

    public static final int ROWS = 12;
    public static final int COLS = 12;
    public static final int TOTAL_CELLS = ROWS * COLS; // 144
    public static final int TOTAL_MINES = 23;
    public static final int TOTAL_SAFE_CELLS = TOTAL_CELLS - TOTAL_MINES; // 121

    public enum OpenResult {
        INVALID,
        SAFE,
        MINE
    }

    private final Cell[][] grid;
    private int openedSafeCells;
    private int currentFlags;
    private int flagsPlaced;
    private int flagsRemoved;
    private int totalActions;
    private boolean minesGenerated;

    public Board() {
        this.grid = new Cell[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                this.grid[r][c] = new Cell(r, c);
            }
        }
        initialize();
    }

    /**
     * Khởi tạo trạng thái bàn cờ nhưng chưa đặt mìn.
     * Mìn chỉ được sinh ra (lazy) khi người chơi mở ô đầu tiên.
     */
    public final void initialize() {
        this.openedSafeCells = 0;
        this.currentFlags = 0;
        this.flagsPlaced = 0;
        this.flagsRemoved = 0;
        this.totalActions = 0;
        this.minesGenerated = false;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                this.grid[r][c].reset();
            }
        }
    }

    public boolean isValidCoordinate(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    public Cell getCell(int r, int c) {
        if (!isValidCoordinate(r, c)) return null;
        return grid[r][c];
    }

    public boolean isMinesGenerated() {
        return minesGenerated;
    }

    /**
     * Trả về tất cả ô có mìn (dùng để hiện hết bom khi trận kết thúc).
     * Danh sách rỗng nếu người chơi chưa click lần nào (mìn chưa được sinh).
     */
    public List<Cell> getMineCells() {
        List<Cell> mines = new ArrayList<>();
        if (!minesGenerated) return mines;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c].isMine()) {
                    mines.add(grid[r][c]);
                }
            }
        }
        return mines;
    }

    /**
     * Thực hiện thao tác OPEN_CELL.
     * Nếu đây là lần mở ô đầu tiên thì sinh 23 mìn, đảm bảo ô đầu tiên
     * có 0 mìn xung quanh (vùng 3x3 an toàn) rồi mở lan bằng BFS.
     *
     * @param r tọa độ hàng (0-11)
     * @param c tọa độ cột (0-11)
     * @param openedList danh sách đầu ra chứa tất cả các ô vừa được mở (để gửi cập nhật qua mạng)
     * @return OpenResult (INVALID, SAFE hoặc MINE)
     */
    public OpenResult openCell(int r, int c, List<Cell> openedList) {
        if (!isValidCoordinate(r, c)) {
            return OpenResult.INVALID;
        }

        Cell target = grid[r][c];
        if (target.isOpened() || target.isFlagged()) {
            return OpenResult.INVALID;
        }

        // Sinh mìn ở lần click đầu tiên: đảm bảo ô được chọn có 0 mìn xung quanh
        if (!this.minesGenerated) {
            MineGenerator.generateMines(grid, ROWS, COLS, TOTAL_MINES, r, c);
            this.minesGenerated = true;
        }

        // Mỗi lần click hợp lệ của người chơi tăng totalActions thêm 1
        this.totalActions++;

        // Trường hợp 1: Mở trúng mìn -> thua ngay lập tức
        if (target.isMine()) {
            target.setExploded(true);
            target.setOpened(true);
            if (openedList != null) openedList.add(target);
            return OpenResult.MINE;
        }

        // Trường hợp 2: Mở ô có số (adjacentMines > 0) -> chỉ mở đúng ô đó
        if (target.getAdjacentMines() > 0) {
            target.setOpened(true);
            this.openedSafeCells++;
            if (openedList != null) openedList.add(target);
            return OpenResult.SAFE;
        }

        // Trường hợp 3: Mở ô trống (adjacentMines == 0) -> mở lan bằng BFS
        Queue<Cell> queue = new ArrayDeque<>();
        target.setOpened(true);
        this.openedSafeCells++;
        if (openedList != null) openedList.add(target);
        queue.add(target);

        while (!queue.isEmpty()) {
            Cell curr = queue.poll();

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = curr.getRow() + dr;
                    int nc = curr.getCol() + dc;

                    if (isValidCoordinate(nr, nc)) {
                        Cell neighbor = grid[nr][nc];
                        if (!neighbor.isOpened() && !neighbor.isFlagged() && !neighbor.isMine()) {
                            neighbor.setOpened(true);
                            this.openedSafeCells++;
                            if (openedList != null) openedList.add(neighbor);

                            // Chỉ đưa vào hàng đợi để lan tiếp với các ô có giá trị 0
                            if (neighbor.getAdjacentMines() == 0) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        return OpenResult.SAFE;
    }

    /**
     * Thực hiện thao tác PLACE_FLAG (cắm cờ).
     */
    public boolean placeFlag(int r, int c) {
        if (!isValidCoordinate(r, c)) return false;
        Cell cell = grid[r][c];
        if (cell.isOpened() || cell.isFlagged() || currentFlags >= TOTAL_MINES) {
            return false;
        }

        cell.setFlagged(true);
        this.currentFlags++;
        this.flagsPlaced++;
        this.totalActions++;
        return true;
    }

    /**
     * Thực hiện thao tác REMOVE_FLAG (gỡ cờ).
     */
    public boolean removeFlag(int r, int c) {
        if (!isValidCoordinate(r, c)) return false;
        Cell cell = grid[r][c];
        if (!cell.isFlagged()) {
            return false;
        }

        cell.setFlagged(false);
        if (this.currentFlags > 0) {
            this.currentFlags--;
        }
        this.flagsRemoved++;
        this.totalActions++;
        return true;
    }

    public boolean isAllSafeCellsOpened() {
        return openedSafeCells >= TOTAL_SAFE_CELLS;
    }

    /**
     * Chuyển bàn cờ này sang model BoardState.
     *
     * @param maskMines nếu true thì ẩn vị trí mìn của các ô chưa mở (góc nhìn của đối thủ)
     */
    public BoardState toBoardState(boolean maskMines) {
        BoardState state = new BoardState();
        state.setOpenedSafeCells(this.openedSafeCells);
        state.setCurrentFlags(this.currentFlags);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Cell src = grid[r][c];
                CellState dst = new CellState(r, c);
                dst.setOpened(src.isOpened());
                dst.setFlagged(src.isFlagged());
                dst.setExploded(src.isExploded());

                if (src.isOpened() || !maskMines) {
                    dst.setMine(src.isMine());
                    dst.setAdjacentMines(src.getAdjacentMines());
                } else {
                    dst.setMine(false);
                    dst.setAdjacentMines(-1);
                }
                state.setCell(r, c, dst);
            }
        }
        return state;
    }

    public int getOpenedSafeCells() {
        return openedSafeCells;
    }

    public int getCurrentFlags() {
        return currentFlags;
    }

    public int getFlagsPlaced() {
        return flagsPlaced;
    }

    public int getFlagsRemoved() {
        return flagsRemoved;
    }

    public int getTotalActions() {
        return totalActions;
    }
}
