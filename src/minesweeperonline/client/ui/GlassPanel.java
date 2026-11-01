package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;

/** A tinted panel that lets the illustrated scene remain visible beneath its contents. */
public class GlassPanel extends JPanel {
    private Color tint;

    public GlassPanel(LayoutManager layout, Color baseColor, int alpha) {
        super(layout);
        this.tint = UITheme.withAlpha(baseColor, alpha);
        setOpaque(false);
    }

    public GlassPanel(LayoutManager layout, int alpha) {
        this(layout, UITheme.BG_PANEL, alpha);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setColor(tint);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
