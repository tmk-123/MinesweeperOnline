package minesweeperonline.client.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/** Draws a shared minefield scene beneath opaque, readable Swing panels. */
public class MinesweeperBackgroundPanel extends JPanel {

    public enum Scene { LOBBY, DUEL, RESULT }

    private static final Map<Scene, BufferedImage> IMAGE_CACHE = new EnumMap<>(Scene.class);
    private final Scene scene;

    public MinesweeperBackgroundPanel(Scene scene, LayoutManager layout) {
        super(layout);
        this.scene = scene;
        setOpaque(true);
        setBackground(UITheme.BG_MAIN);
    }

    public MinesweeperBackgroundPanel(Scene scene) {
        this(scene, new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        BufferedImage image = getSceneImage(scene);
        if (image == null || getWidth() <= 0 || getHeight() <= 0) return;

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double scale = Math.max(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight());
        int drawWidth = (int) Math.ceil(image.getWidth() * scale);
        int drawHeight = (int) Math.ceil(image.getHeight() * scale);
        int drawX = (getWidth() - drawWidth) / 2;
        int drawY = (getHeight() - drawHeight) / 2;
        g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, this);

        int veilAlpha = scene == Scene.RESULT ? 146 : scene == Scene.DUEL ? 132 : 118;
        g2.setColor(new Color(UITheme.BG_MAIN.getRed(), UITheme.BG_MAIN.getGreen(), UITheme.BG_MAIN.getBlue(), veilAlpha));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setPaint(new GradientPaint(0, 0, new Color(7, 12, 17, 38),
                0, getHeight(), new Color(7, 12, 17, scene == Scene.RESULT ? 88 : 62)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private static synchronized BufferedImage getSceneImage(Scene scene) {
        if (IMAGE_CACHE.containsKey(scene)) return IMAGE_CACHE.get(scene);
        String fileName;
        switch (scene) {
            case DUEL: fileName = "minefield-duel.png"; break;
            case RESULT: fileName = "minefield-result.png"; break;
            default: fileName = "minefield-lobby.png"; break;
        }

        BufferedImage image = null;
        String resourcePath = "/minesweeperonline/client/ui/assets/" + fileName;
        try (InputStream stream = MinesweeperBackgroundPanel.class.getResourceAsStream(resourcePath)) {
            if (stream != null) image = ImageIO.read(stream);
        } catch (IOException exception) {
            System.err.println("Unable to load UI background " + resourcePath + ": " + exception.getMessage());
        }
        IMAGE_CACHE.put(scene, image);
        return image;
    }
}
