package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Random;

/** Lightweight Java2D illustration used on authentication screens. */
final class MinesweeperHeroPanel extends JPanel {
    MinesweeperHeroPanel() {
        setOpaque(true);
        setBackground(UITheme.BG_MAIN);
        setPreferredSize(new Dimension(430, 520));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, new Color(23, 38, 47), 0, h, new Color(9, 15, 19)));
        g2.fillRect(0, 0, w, h);

        // Distant minefield ridges
        drawPolygon(g2, new int[]{0, w / 3, w / 2, w}, new int[]{h * 3 / 5, h / 3, h / 2, h * 2 / 5}, new Color(35, 49, 55));
        drawPolygon(g2, new int[]{0, w / 4, w / 2, w * 3 / 4, w}, new int[]{h * 3 / 4, h / 2, h * 3 / 5, h * 2 / 5, h * 3 / 5}, new Color(27, 40, 45));

        Random random = new Random(19);
        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(Math.max(1, w));
            int y = h / 2 + random.nextInt(Math.max(1, h / 2));
            int r = 8 + random.nextInt(34);
            int[] xs = {x - r, x - r / 2, x + r / 2, x + r, x + r / 3, x - r / 3};
            int[] ys = {y, y - r / 2, y - r / 3, y + r / 3, y + r, y + r / 2};
            Color rock = i % 3 == 0 ? new Color(46, 61, 66) : new Color(31, 43, 48);
            g2.setColor(rock);
            g2.fillPolygon(xs, ys, xs.length);
            g2.setColor(new Color(82, 89, 83, 90));
            g2.drawLine(xs[0], ys[0], xs[1], ys[1]);
        }

        // Glowing flag and base
        int poleX = w * 2 / 3;
        int poleY = h * 2 / 5;
        g2.setColor(new Color(5, 9, 11, 150));
        g2.fillOval(poleX - 57, poleY + 100, 125, 30);
        g2.setColor(new Color(173, 131, 60));
        g2.fillOval(poleX - 39, poleY + 83, 86, 20);
        g2.setColor(new Color(92, 75, 49));
        g2.fillOval(poleX - 31, poleY + 77, 70, 18);
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(189, 158, 101));
        g2.drawLine(poleX, poleY + 83, poleX, poleY - 23);
        Path2D flag = new Path2D.Float();
        flag.moveTo(poleX + 3, poleY - 20);
        flag.lineTo(poleX + 73, poleY + 2);
        flag.lineTo(poleX + 3, poleY + 26);
        flag.closePath();
        g2.setPaint(new GradientPaint(poleX, poleY - 15, new Color(255, 202, 92), poleX + 65, poleY + 19, new Color(218, 92, 40)));
        g2.fill(flag);
        g2.setColor(new Color(255, 222, 142));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(flag);
        g2.setColor(new Color(246, 184, 66, 35));
        g2.fillOval(poleX - 90, poleY - 86, 175, 175);

        // Branding area over a subtle dark fade
        g2.setPaint(new GradientPaint(0, h * 2 / 3, new Color(9, 15, 19, 0), 0, h, new Color(9, 15, 19, 230)));
        g2.fillRect(0, h * 2 / 3, w, h / 3);
        g2.setColor(UITheme.COLOR_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(25, Math.min(38, w / 10))));
        g2.drawString("MINESWEEPER", 30, h - 87);
        g2.setColor(UITheme.TEXT_TITLE);
        g2.drawString("ONLINE", 30, h - 48);
        g2.setFont(UITheme.FONT_REGULAR);
        g2.setColor(UITheme.TEXT_MUTED);
        g2.drawString("Đấu trí dò mìn đối kháng thời gian thực", 32, h - 23);
        g2.dispose();
    }

    private static void drawPolygon(Graphics2D g, int[] xs, int[] ys, Color color) {
        g.setColor(color);
        g.fillPolygon(xs, ys, xs.length);
    }
}
