package minesweeperonline.client.ui;

import minesweeperonline.client.sound.SoundManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.beans.PropertyChangeListener;

/** Bộ điều khiển âm thanh dùng chung, đồng bộ khi chuyển giữa các màn hình. */
public final class AudioControlsPanel extends JPanel {
    private final SoundManager sound = SoundManager.getInstance();
    private final JButton allSound = new JButton();
    private final JButton music = new JButton();
    private final JSlider volume = new JSlider(0, 100);
    private final JLabel percent = new JLabel("", SwingConstants.RIGHT);
    private boolean refreshing;
    private final PropertyChangeListener settingsListener = event -> {
        if (SwingUtilities.isEventDispatchThread()) refresh();
        else SwingUtilities.invokeLater(this::refresh);
    };

    /** Tạo nút bật/tắt âm thanh và thanh âm lượng, liên kết chúng với SoundManager dùng chung. */
    public AudioControlsPanel() {
        super(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        setOpaque(false);
        UITheme.styleOutlineButton(allSound, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_BODY);
        allSound.setToolTipText("Bật/tắt toàn bộ nhạc nền và hiệu ứng âm thanh");
        allSound.addActionListener(event -> sound.toggleMute());
        UITheme.styleOutlineButton(music, UITheme.BORDER_CARD, UITheme.COLOR_SECONDARY_HOVER, UITheme.TEXT_BODY);
        music.setToolTipText("Bật/tắt riêng nhạc nền; giữ nguyên hiệu ứng trong game");
        music.addActionListener(event -> sound.setMusicMuted(!sound.isMusicMuted()));

        volume.setOpaque(false);
        volume.setPreferredSize(new Dimension(130, 28));
        volume.setToolTipText("Âm lượng nhạc nền (0–100%)");
        volume.getAccessibleContext().setAccessibleName("Âm lượng nhạc nền");
        volume.setUI(new BasicSliderUI(volume) {
            @Override protected Dimension getThumbSize() { return new Dimension(14, 20); }
            @Override public void paintTrack(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int y = trackRect.y + trackRect.height / 2 - 3;
                int end = thumbRect.x + thumbRect.width / 2;
                g.setColor(UITheme.BG_ELEVATED);
                g.fillRoundRect(trackRect.x, y, trackRect.width, 6, 6, 6);
                g.setColor(UITheme.COLOR_PRIMARY);
                g.fillRoundRect(trackRect.x, y, Math.max(0, end - trackRect.x), 6, 6, 6);
                g.dispose();
            }
            @Override public void paintThumb(Graphics graphics) {
                graphics.setColor(UITheme.COLOR_PRIMARY);
                graphics.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, 8, 8);
            }
            @Override public void paintFocus(Graphics graphics) {
                graphics.setColor(UITheme.BORDER_FOCUS);
                graphics.drawRect(0, 0, slider.getWidth() - 1, slider.getHeight() - 1);
            }
        });
        volume.addChangeListener(event -> {
            if (!refreshing) sound.setMusicVolume(volume.getValue());
        });
        percent.setFont(UITheme.FONT_SMALL_BOLD);
        percent.setForeground(UITheme.TEXT_BODY);
        percent.setPreferredSize(new Dimension(42, 28));
        percent.setLabelFor(volume);
        add(allSound);
        add(music);
        add(volume);
        add(percent);
        refresh();
    }

    /** Đọc cài đặt chung để cập nhật nút và slider; cờ refreshing tránh việc cập nhật slider gửi ngược thay đổi. */
    private void refresh() {
        refreshing = true;
        try {
            allSound.setText(sound.isMuted() ? "Âm thanh: Tắt" : "Âm thanh: Bật");
            music.setText(sound.isMusicMuted() ? "Nhạc nền: Tắt" : "Nhạc nền: Bật");
            volume.setValue(sound.getMusicVolume());
            percent.setText(sound.getMusicVolume() + "%");
        } finally {
            refreshing = false;
        }
    }

    /** Đăng ký theo dõi cài đặt âm thanh khi panel được gắn vào cây component. */
    @Override public void addNotify() {
        super.addNotify();
        sound.addSettingsListener(settingsListener);
        refresh();
    }

    /** Hủy theo dõi cài đặt âm thanh khi panel rời cây component để tránh giữ listener thừa. */
    @Override public void removeNotify() {
        sound.removeSettingsListener(settingsListener);
        super.removeNotify();
    }
}
