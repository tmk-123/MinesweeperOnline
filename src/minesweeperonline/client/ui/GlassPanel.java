package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;

/** A tinted panel that lets the illustrated scene remain visible beneath its contents. */
public class GlassPanel extends JPanel {
    private Color tint;

    /** Tạo panel nền bán trong suốt với bố cục, màu và độ trong suốt được truyền vào. */
    public GlassPanel(LayoutManager layout, Color baseColor, int alpha) {
        super(layout);
        this.tint = UITheme.withAlpha(baseColor, alpha);
        setOpaque(false);
    }

    /** Tạo panel nền bán trong suốt với bố cục, màu và độ trong suốt được truyền vào. */
    public GlassPanel(LayoutManager layout, int alpha) {
        this(layout, UITheme.BG_PANEL, alpha);
    }

    /** Vẽ phần nền hoặc trạng thái của panel bằng Graphics; dữ liệu vẽ lấy từ trạng thái hiện tại của component. */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setColor(tint);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
