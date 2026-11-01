package minesweeperonline.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Small vector emblems and character portraits for the Minesweeper UI. */
public final class GameUiIcons {
    private GameUiIcons() { }

    public static Icon rankTierIcon(int score, int size) {
        return new RankTierIcon(score, size);
    }

    public static Icon podiumMedalIcon(int rank, int size) {
        return rank >= 1 && rank <= 3 ? new PodiumMedalIcon(rank, size) : null;
    }

    public static Icon playerAvatarIcon(String username, Color accent, int size) {
        return playerAvatarIcon(username, accent, size, true);
    }

    public static Icon playerAvatarIcon(String username, Color accent, int size, boolean showTeamRing) {
        return new PlayerAvatarIcon(username, accent, size, showTeamRing);
    }

    private static int tierForScore(int score) {
        if (score >= 1000) return 4;
        if (score >= 500) return 3;
        if (score >= 200) return 2;
        if (score >= 50) return 1;
        return 0;
    }

    private static Color tierColor(int tier) {
        switch (tier) {
            case 4: return UITheme.COLOR_PRIMARY;
            case 3: return new Color(164, 132, 223);
            case 2: return UITheme.COLOR_BLUE;
            case 1: return UITheme.COLOR_SILVER;
            default: return UITheme.COLOR_BRONZE;
        }
    }

    private static class RankTierIcon implements Icon {
        private final int tier;
        private final int size;

        private RankTierIcon(int score, int size) {
            this.tier = tierForScore(score);
            this.size = size;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = tierColor(tier);

            Path2D shield = new Path2D.Double();
            shield.moveTo(x + size * 0.5, y + size * 0.04);
            shield.lineTo(x + size * 0.91, y + size * 0.2);
            shield.lineTo(x + size * 0.83, y + size * 0.68);
            shield.lineTo(x + size * 0.5, y + size * 0.96);
            shield.lineTo(x + size * 0.17, y + size * 0.68);
            shield.lineTo(x + size * 0.09, y + size * 0.2);
            shield.closePath();

            g2.setColor(new Color(0, 0, 0, 70));
            g2.translate(0, Math.max(1, size * 0.04));
            g2.fill(shield);
            g2.translate(0, -Math.max(1, size * 0.04));
            g2.setPaint(new GradientPaint(x, y, color.brighter(), x + size, y + size, color.darker()));
            g2.fill(shield);
            g2.setStroke(new BasicStroke(Math.max(1f, size * 0.055f)));
            g2.setColor(new Color(246, 236, 210, 220));
            g2.draw(shield);

            g2.setColor(new Color(17, 27, 35, 235));
            g2.setStroke(new BasicStroke(Math.max(1.4f, size * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            paintTierSymbol(g2, x, y, size, tier);
            g2.dispose();
        }

        private void paintTierSymbol(Graphics2D g2, int x, int y, int size, int tier) {
            float cx = x + size * 0.5f;
            float cy = y + size * 0.48f;
            float unit = size / 32f;
            switch (tier) {
                case 4: {
                    int[] px = {(int)(cx - 10 * unit), (int)(cx - 7 * unit), (int)cx, (int)(cx + 7 * unit), (int)(cx + 10 * unit), (int)(cx + 8 * unit), (int)(cx - 8 * unit)};
                    int[] py = {(int)(cy - 5 * unit), (int)(cy + 1 * unit), (int)(cy - 7 * unit), (int)(cy + 1 * unit), (int)(cy - 5 * unit), (int)(cy + 6 * unit), (int)(cy + 6 * unit)};
                    g2.fillPolygon(px, py, px.length);
                    g2.fillRoundRect((int)(cx - 8 * unit), (int)(cy + 8 * unit), (int)(16 * unit), Math.max(2, (int)(3 * unit)), 2, 2);
                    break;
                }
                case 3: {
                    Path2D star = star(cx, cy, size * 0.28, size * 0.12, 5);
                    g2.fill(star);
                    break;
                }
                case 2:
                    g2.drawLine((int)(cx - 7 * unit), (int)(cy - 8 * unit), (int)(cx + 7 * unit), (int)(cy + 8 * unit));
                    g2.drawLine((int)(cx + 7 * unit), (int)(cy - 8 * unit), (int)(cx - 7 * unit), (int)(cy + 8 * unit));
                    g2.fillOval((int)(cx - 2 * unit), (int)(cy - 2 * unit), Math.max(3, (int)(4 * unit)), Math.max(3, (int)(4 * unit)));
                    break;
                case 1:
                    g2.drawLine((int)(cx - 8 * unit), (int)(cy - 2 * unit), (int)cx, (int)(cy + 5 * unit));
                    g2.drawLine((int)cx, (int)(cy + 5 * unit), (int)(cx + 8 * unit), (int)(cy - 2 * unit));
                    g2.drawLine((int)(cx - 6 * unit), (int)(cy + 6 * unit), (int)(cx + 6 * unit), (int)(cy + 6 * unit));
                    break;
                default:
                    g2.drawLine((int)(cx - 8 * unit), (int)(cy + 5 * unit), (int)(cx + 7 * unit), (int)(cy - 7 * unit));
                    g2.drawLine((int)(cx - 8 * unit), (int)(cy + 5 * unit), (int)(cx + 1 * unit), (int)(cy + 5 * unit));
                    g2.drawLine((int)(cx - 3 * unit), (int)(cy + 1 * unit), (int)(cx + 1 * unit), (int)(cy + 5 * unit));
                    break;
            }
        }

        private Path2D star(double cx, double cy, double outerRadius, double innerRadius, int points) {
            Path2D shape = new Path2D.Double();
            for (int i = 0; i < points * 2; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / points;
                double radius = (i % 2 == 0) ? outerRadius : innerRadius;
                double px = cx + Math.cos(angle) * radius;
                double py = cy + Math.sin(angle) * radius;
                if (i == 0) shape.moveTo(px, py); else shape.lineTo(px, py);
            }
            shape.closePath();
            return shape;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    private static class PodiumMedalIcon implements Icon {
        private final int rank;
        private final int size;
        private PodiumMedalIcon(int rank, int size) { this.rank = rank; this.size = size; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color medal = rank == 1 ? UITheme.COLOR_PRIMARY : rank == 2 ? UITheme.COLOR_SILVER : UITheme.COLOR_BRONZE;
            int ribbonWidth = Math.max(5, size / 4);
            int cx = x + size / 2;
            Polygon leftRibbon = new Polygon(new int[]{cx - ribbonWidth, cx - 1, cx - 2, cx - ribbonWidth},
                    new int[]{y + size / 2, y + size / 2, y + size - 1, y + size - size / 5}, 4);
            Polygon rightRibbon = new Polygon(new int[]{cx + 1, cx + ribbonWidth, cx + ribbonWidth, cx + 2},
                    new int[]{y + size / 2, y + size / 2, y + size - size / 5, y + size - 1}, 4);
            g2.setColor(medal.darker());
            g2.fill(leftRibbon);
            g2.fill(rightRibbon);
            g2.setPaint(new GradientPaint(x, y, medal.brighter(), x + size, y + size, medal.darker()));
            g2.fillOval(x + 1, y + 1, size - 2, size - 2);
            g2.setStroke(new BasicStroke(Math.max(1f, size * 0.06f)));
            g2.setColor(new Color(255, 246, 214, 230));
            g2.drawOval(x + 2, y + 2, size - 4, size - 4);
            g2.setColor(new Color(54, 42, 26, 220));
            Path2D star = new Path2D.Double();
            double centerX = x + size / 2.0;
            double centerY = y + size / 2.0;
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                double radius = (i % 2 == 0) ? size * 0.27 : size * 0.12;
                double px = centerX + Math.cos(angle) * radius;
                double py = centerY + Math.sin(angle) * radius;
                if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
            }
            star.closePath();
            g2.fill(star);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    private static class PlayerAvatarIcon implements Icon {
        private final String username;
        private final Color accent;
        private final int size;
        private final boolean showTeamRing;

        private PlayerAvatarIcon(String username, Color accent, int size, boolean showTeamRing) {
            this.username = username == null ? "" : username;
            this.accent = accent == null ? UITheme.COLOR_BLUE : accent;
            this.size = size;
            this.showTeamRing = showTeamRing;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = size / 64.0;
            g2.translate(x, y);
            g2.scale(scale, scale);
            g2.setClip(new Ellipse2D.Double(2, 2, 60, 60));

            boolean alternateArmor = (username.toLowerCase().hashCode() & 1) != 0;
            Color armor = new Color(18, 28, 36);
            Color armorLight = new Color(43, 57, 66);
            Color visor = alternateArmor ? new Color(255, 114, 78) : new Color(100, 220, 233);

            g2.setPaint(new GradientPaint(4, 4, accent.brighter(), 60, 60, new Color(8, 15, 21)));
            g2.fillOval(2, 2, 60, 60);
            g2.setColor(new Color(255, 255, 255, 25));
            g2.fillOval(9, 7, 42, 18);

            // Armored shoulders and a compact chest plate.
            Path2D torso = new Path2D.Double();
            torso.moveTo(8, 63); torso.lineTo(12, 48); torso.lineTo(22, 43);
            torso.lineTo(42, 43); torso.lineTo(53, 49); torso.lineTo(58, 63); torso.closePath();
            g2.setPaint(new GradientPaint(18, 43, armorLight, 42, 63, armor));
            g2.fill(torso);
            g2.setColor(accent.darker());
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(17, 52, 24, 48);
            g2.drawLine(47, 48, 53, 53);
            g2.setColor(new Color(188, 207, 214, 190));
            g2.fillOval(16, 53, 3, 3);
            g2.fillOval(46, 53, 3, 3);

            // Helmet silhouette, faceted cheek guards, and a glowing tactical visor.
            Path2D helmet = new Path2D.Double();
            helmet.moveTo(11, 31); helmet.lineTo(12, 22); helmet.lineTo(19, 14);
            helmet.lineTo(31, 10); helmet.lineTo(45, 13); helmet.lineTo(53, 21);
            helmet.lineTo(54, 31); helmet.lineTo(48, 39); helmet.lineTo(16, 39); helmet.closePath();
            g2.setPaint(new GradientPaint(17, 13, new Color(72, 88, 96), 46, 40, armor));
            g2.fill(helmet);
            g2.setColor(new Color(5, 12, 17));
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(helmet);

            if (alternateArmor) {
                g2.setColor(accent.brighter());
                Polygon crest = new Polygon(new int[]{25, 32, 39}, new int[]{14, 5, 14}, 3);
                g2.fillPolygon(crest);
                g2.setColor(new Color(5, 12, 17));
                g2.drawLine(32, 8, 32, 14);
            } else {
                g2.setColor(accent.brighter());
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(21, 16, 42, 13);
                g2.drawLine(18, 19, 46, 17);
            }

            Path2D visorPlate = new Path2D.Double();
            visorPlate.moveTo(14, 24); visorPlate.lineTo(50, 24); visorPlate.lineTo(46, 34);
            visorPlate.lineTo(18, 34); visorPlate.closePath();
            g2.setPaint(new GradientPaint(18, 25, visor.brighter(), 46, 34, new Color(14, 29, 37)));
            g2.fill(visorPlate);
            g2.setColor(new Color(210, 247, 255, 220));
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawLine(19, 26, 44, 26);
            g2.setColor(new Color(8, 17, 23));
            g2.fillRoundRect(24, 36, 16, 11, 4, 4);
            g2.setColor(visor);
            g2.fillRoundRect(27, 38, 10, 2, 2, 2);
            g2.setColor(new Color(146, 166, 174));
            g2.fillOval(20, 37, 5, 7);
            g2.fillOval(39, 37, 5, 7);

            // Side radio and a small headlamp make the two username variants distinct.
            g2.setColor(armorLight);
            g2.fillRoundRect(alternateArmor ? 8 : 49, 27, 7, 12, 3, 3);
            g2.setColor(accent.brighter());
            g2.fillOval(alternateArmor ? 10 : 51, 30, 3, 3);
            g2.setColor(new Color(255, 223, 123));
            g2.fillOval(29, 18, 6, 3);

            g2.setClip(null);
            if (showTeamRing) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210));
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawOval(2, 2, 60, 60);
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }
}
