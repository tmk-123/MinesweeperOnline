package minesweeperonline.client.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Trình quản lý âm thanh thuần Java JDK (javax.sound.sampled.Clip).
 * Hiệu ứng được tổng hợp trong bộ nhớ; nhạc nền WAV được nạp từ tài nguyên ứng dụng.
 * Không cần thư viện bên thứ ba. Lỗi âm thanh không làm gián đoạn game.
 */
public class SoundManager {

    private static final float SAMPLE_RATE = 44100.0f;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final String BACKGROUND_MUSIC_RESOURCE =
            "/minesweeperonline/client/sound/assets/a-tricky-puzzle.wav";

    private static final SoundManager INSTANCE = new SoundManager();

    /** Trả về SoundManager dùng chung để các màn hình đồng bộ cài đặt âm thanh. */
    public static SoundManager getInstance() {
        return INSTANCE;
    }

    private final ExecutorService soundPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Sound-Player");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean muted = false;
    private volatile boolean musicMuted = false;
    // Âm lượng 13% gần tương đương mức nhạc nền trước đây là -18 dB.
    private volatile int musicVolume = 13;
    private final PropertyChangeSupport settingsChanges = new PropertyChangeSupport(this);

    // Cache các mảng byte âm thanh đã tổng hợp
    private byte[] dataClick;
    private byte[] dataFlag;
    private byte[] dataExplosion;
    private byte[] dataTick;
    private byte[] dataVictory;
    private byte[] dataDefeat;

    // Pool Clip cho âm click nhanh (tránh bị ngắt khi click liên tục)
    private final Clip[] clickClips = new Clip[4];
    private int clickClipIndex = 0;

    private Clip clipFlag;
    private Clip clipExplosion;
    private Clip clipTick;
    private Clip clipVictory;
    private Clip clipDefeat;
    // Chỉ truy cập nhạc nền trên soundPool để tránh phát chồng hoặc tranh chấp khi tắt/bật.
    private Clip backgroundMusic;
    private boolean backgroundMusicEnabled;

    /** Khởi tạo bộ âm thanh dùng chung và chuẩn bị các hiệu ứng để phát trong game. */
    private SoundManager() {
        try {
            synthesizeAllSounds();
            initClips();
        } catch (Throwable t) {
            System.err.println("[SoundManager] Warning: Audio system initialization: " + t.getMessage());
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        boolean previous = this.muted;
        this.muted = muted;
        soundPool.submit(() -> {
            updateBackgroundMusic();
            if (this.muted) {
                for (Clip clip : clickClips) stopClip(clip);
                for (Clip clip : new Clip[]{clipFlag, clipExplosion, clipTick, clipVictory, clipDefeat}) {
                    stopClip(clip);
                }
            }
        });
        settingsChanges.firePropertyChange("muted", previous, muted);
    }

    /** Đảo trạng thái tắt toàn bộ âm thanh và trả về trạng thái mới. */
    public boolean toggleMute() {
        setMuted(!muted);
        return this.muted;
    }

    public boolean isMusicMuted() {
        return musicMuted;
    }

    public void setMusicMuted(boolean musicMuted) {
        boolean previous = this.musicMuted;
        this.musicMuted = musicMuted;
        soundPool.submit(this::updateBackgroundMusic);
        settingsChanges.firePropertyChange("musicMuted", previous, musicMuted);
    }

    public int getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(int volume) {
        int previous = musicVolume;
        musicVolume = Math.max(0, Math.min(100, volume));
        soundPool.submit(this::updateBackgroundMusic);
        settingsChanges.firePropertyChange("musicVolume", previous, musicVolume);
    }

    /** Đăng ký nhận thay đổi cài đặt để các bộ điều khiển âm thanh cập nhật đồng bộ. */
    public void addSettingsListener(PropertyChangeListener listener) {
        settingsChanges.addPropertyChangeListener(listener);
    }

    /** Hủy listener cài đặt khi component không còn được sử dụng. */
    public void removeSettingsListener(PropertyChangeListener listener) {
        settingsChanges.removePropertyChangeListener(listener);
    }

    /** Nạp nhạc ngoài EDT, sau đó phát lặp xuyên suốt phiên client. */
    public void startBackgroundMusic() {
        soundPool.submit(() -> {
            backgroundMusicEnabled = true;
            if (backgroundMusic == null) {
                backgroundMusic = loadBackgroundMusic();
            }
            updateBackgroundMusic();
        });
    }

    /** Dừng phát nhạc nền nếu clip đã được nạp. */
    public void stopBackgroundMusic() {
        soundPool.submit(() -> {
            backgroundMusicEnabled = false;
            updateBackgroundMusic();
        });
    }

    /** Đồng bộ việc phát nhạc với trạng thái tắt âm thanh, tắt nhạc riêng và mức âm lượng. */
    private void updateBackgroundMusic() {
        if (backgroundMusic == null) return;
        applyMusicVolume();
        if (backgroundMusicEnabled && !muted && !musicMuted && musicVolume > 0) {
            if (!backgroundMusic.isRunning()) {
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } else {
            // Giữ vị trí hiện tại để bật lại tiếp tục đoạn nhạc đang nghe.
            backgroundMusic.stop();
        }
    }

    /** Đổi mức âm lượng nhạc sang giá trị điều khiển mà clip hỗ trợ. */
    private void applyMusicVolume() {
        if (backgroundMusic == null || musicVolume == 0) return;
        if (backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
            float decibels = (float) (20.0 * Math.log10(musicVolume / 100.0));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
        } else if (backgroundMusic.isControlSupported(FloatControl.Type.VOLUME)) {
            FloatControl volume = (FloatControl) backgroundMusic.getControl(FloatControl.Type.VOLUME);
            volume.setValue(Math.max(volume.getMinimum(), Math.min(volume.getMaximum(), musicVolume / 100.0f)));
        }
    }

    /** Đọc tài nguyên nhạc nền và tạo Clip; xử lý trường hợp thiếu file hoặc thiết bị âm thanh. */
    private Clip loadBackgroundMusic() {
        URL resource = SoundManager.class.getResource(BACKGROUND_MUSIC_RESOURCE);
        if (resource == null) {
            System.err.println("[SoundManager] Background music resource not found: " + BACKGROUND_MUSIC_RESOURCE);
            return null;
        }

        Clip clip = null;
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
            clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException
                | IllegalArgumentException | SecurityException e) {
            if (clip != null) clip.close();
            System.err.println("[SoundManager] Background music could not be opened: " + e.getMessage());
            return null;
        }
    }

    // --- Các phương thức phát âm thanh công khai ---

    public void playClick() {
        if (muted) return;
        soundPool.submit(() -> {
            if (muted) return;
            try {
                synchronized (clickClips) {
                    Clip c = clickClips[clickClipIndex];
                    clickClipIndex = (clickClipIndex + 1) % clickClips.length;
                    if (c != null) {
                        if (c.isRunning()) c.stop();
                        c.setFramePosition(0);
                        c.start();
                    }
                }
            } catch (Throwable ignored) {}
        });
    }

    /** Phát hiệu ứng cắm hoặc gỡ cờ qua bộ phát âm thanh dùng chung. */
    public void playFlag() {
        playClip(clipFlag);
    }

    /** Phát hiệu ứng nổ khi người chơi mở trúng mìn. */
    public void playExplosion() {
        playClip(clipExplosion);
    }

    /** Phát tiếng nhịp dùng cho các bộ đếm ngược. */
    public void playTick() {
        playClip(clipTick);
    }

    /** Phát giai điệu báo chiến thắng. */
    public void playVictory() {
        playClip(clipVictory);
    }

    /** Phát giai điệu báo thất bại. */
    public void playDefeat() {
        playClip(clipDefeat);
    }

    /** Đưa việc phát clip sang hàng đợi âm thanh để tránh chặn luồng giao diện. */
    private void playClip(Clip clip) {
        if (muted || clip == null) return;
        soundPool.submit(() -> {
            if (muted) return;
            try {
                synchronized (clip) {
                    if (clip.isRunning()) clip.stop();
                    clip.setFramePosition(0);
                    clip.start();
                }
            } catch (Throwable ignored) {}
        });
    }

    /** Dừng clip nếu clip đã được khởi tạo. */
    private void stopClip(Clip clip) {
        if (clip != null && clip.isRunning()) clip.stop();
    }

    // --- Khởi tạo và tổng hợp âm thanh bằng thuật toán ---

    private void synthesizeAllSounds() {
        dataClick = synthesizeClick();
        dataFlag = synthesizeFlag();
        dataExplosion = synthesizeExplosion();
        dataTick = synthesizeTick();
        dataVictory = synthesizeVictory();
        dataDefeat = synthesizeDefeat();
    }

    /** Chuyển dữ liệu PCM của các hiệu ứng đã tổng hợp thành Clip có thể phát. */
    private void initClips() {
        for (int i = 0; i < clickClips.length; i++) {
            clickClips[i] = createClipFromBytes(dataClick);
        }
        clipFlag = createClipFromBytes(dataFlag);
        clipExplosion = createClipFromBytes(dataExplosion);
        clipTick = createClipFromBytes(dataTick);
        clipVictory = createClipFromBytes(dataVictory);
        clipDefeat = createClipFromBytes(dataDefeat);
    }

    /** Mở Clip từ mảng byte PCM theo AUDIO_FORMAT; trả về null nếu không tạo được. */
    private Clip createClipFromBytes(byte[] audioData) {
        if (audioData == null || audioData.length == 0) return null;
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AUDIO_FORMAT, audioData, 0, audioData.length);
            return clip;
        } catch (LineUnavailableException | IllegalArgumentException e) {
            System.err.println("[SoundManager] Clip could not be opened: " + e.getMessage());
            return null;
        }
    }

    /**
     * Tiếng click ("tách" nhẹ, ngắn 35ms)
     */
    private byte[] synthesizeClick() {
        double duration = 0.035;
        int numSamples = (int) (SAMPLE_RATE * duration);
        byte[] bytes = new byte[numSamples * 2];
        double phase = 0.0;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = t / duration;
            // Tần số quét nhanh từ 1400Hz xuống 350Hz
            double freq = 1400.0 - 1050.0 * progress;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;

            // Biên độ giảm theo hàm mũ
            double amp = Math.pow(1.0 - progress, 2.5) * 0.75;
            double sample = amp * Math.sin(phase);

            writeSample(bytes, i * 2, sample);
        }
        return bytes;
    }

    /**
     * Tiếng cắm/gỡ cờ (chime nhẹ 2 nốt 660Hz -> 880Hz, 80ms)
     */
    private byte[] synthesizeFlag() {
        double duration = 0.080;
        int numSamples = (int) (SAMPLE_RATE * duration);
        byte[] bytes = new byte[numSamples * 2];
        double phase = 0.0;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = (t < 0.040) ? 660.0 : 880.0;
            double partT = (t < 0.040) ? (t / 0.040) : ((t - 0.040) / 0.040);

            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            double amp = Math.pow(1.0 - partT, 1.8) * 0.6;
            double sample = amp * Math.sin(phase);

            writeSample(bytes, i * 2, sample);
        }
        return bytes;
    }

    /**
     * Tiếng bom: xung nổ nhanh, thân âm trầm hạ tần và đuôi nhiễu tắt dần.
     * Biên độ có headroom và fade hai đầu để tránh rè/click trên loa nhỏ.
     */
    private byte[] synthesizeExplosion() {
        double duration = 1.050;
        int numSamples = (int) (SAMPLE_RATE * duration);
        byte[] bytes = new byte[numSamples * 2];
        Random random = new Random(2209L);
        double phase = 0.0;
        double lowNoise = 0.0;
        double bodyNoise = 0.0;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double noise = random.nextDouble() * 2.0 - 1.0;
            lowNoise += 0.035 * (noise - lowNoise);
            bodyNoise += 0.22 * (noise - bodyNoise);
            double frequency = 42.0 + 105.0 * Math.exp(-t * 18.0);
            phase += 2.0 * Math.PI * frequency / SAMPLE_RATE;
            double crack = (noise - bodyNoise) * Math.exp(-t * 65.0) * 0.52;
            double body = bodyNoise * Math.exp(-t * 8.0) * 1.35;
            double thump = (Math.sin(phase) + 0.25 * Math.sin(phase * 1.7))
                    * Math.exp(-t * 9.0) * 0.62;
            double debris = lowNoise * Math.exp(-t * 4.8) * 1.8;
            double attack = Math.min(1.0, t / 0.0015);
            double release = Math.min(1.0, (duration - t) / 0.08);
            double sample = 0.88 * Math.tanh(crack + body + thump + debris) * attack * release;
            writeSample(bytes, i * 2, sample);
        }
        return bytes;
    }

    /**
     * Tiếng đếm ngược tick (beep sắc nét 1100Hz, 30ms)
     */
    private byte[] synthesizeTick() {
        double duration = 0.030;
        int numSamples = (int) (SAMPLE_RATE * duration);
        byte[] bytes = new byte[numSamples * 2];
        double phase = 0.0;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            phase += 2.0 * Math.PI * 1100.0 / SAMPLE_RATE;
            double amp = Math.pow(1.0 - (t / duration), 2.0) * 0.65;
            double sample = amp * Math.sin(phase);

            writeSample(bytes, i * 2, sample);
        }
        return bytes;
    }

    /**
     * Nhạc hiệu chiến thắng (Victory Fanfare: C5 - E5 - G5 - C6, 700ms)
     */
    private byte[] synthesizeVictory() {
        double duration = 0.700;
        int numSamples = (int) (SAMPLE_RATE * duration);
        byte[] bytes = new byte[numSamples * 2];

        double[] notes = {523.25, 659.25, 783.99, 1046.50}; // C5, E5, G5, C6
        double[] noteStarts = {0.00, 0.12, 0.24, 0.38};
        double[] noteEnds = {0.12, 0.24, 0.38, 0.70};

        double phase = 0.0;
        int curNoteIdx = 0;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;

            while (curNoteIdx < notes.length - 1 && t >= noteStarts[curNoteIdx + 1]) {
                curNoteIdx++;
            }

            double freq = notes[curNoteIdx];
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;

            double noteDur = noteEnds[curNoteIdx] - noteStarts[curNoteIdx];
            double progress = (t - noteStarts[curNoteIdx]) / noteDur;

            double amp;
            if (curNoteIdx == notes.length - 1) {
                amp = Math.pow(1.0 - progress, 1.2) * 0.75; // Nốt cuối ngân vang
            } else {
                amp = Math.pow(1.0 - progress, 1.6) * 0.65;
            }

            // Hòa âm bậc 2 để âm thanh dày và trong trẻo như đàn piano/chuông
            double sample = amp * (0.85 * Math.sin(phase) + 0.15 * Math.sin(phase * 2.0));
            writeSample(bytes, i * 2, sample);
        }
        return bytes;
    }

    /**
     * Nhạc hiệu thất bại (Defeat: G4 - Eb4 - C4 buồn, 750ms)
     */
    private byte[] synthesizeDefeat() {
        double duration = 0.750;
        int numSamples = (int) (SAMPLE_RATE * duration);
        byte[] bytes = new byte[numSamples * 2];

        double[] notes = {392.00, 311.13, 261.63}; // G4, Eb4, C4
        double[] noteStarts = {0.00, 0.20, 0.42};
        double[] noteEnds = {0.20, 0.42, 0.75};

        double phase = 0.0;
        int curNoteIdx = 0;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;

            while (curNoteIdx < notes.length - 1 && t >= noteStarts[curNoteIdx + 1]) {
                curNoteIdx++;
            }

            double freq = notes[curNoteIdx];
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;

            double noteDur = noteEnds[curNoteIdx] - noteStarts[curNoteIdx];
            double progress = (t - noteStarts[curNoteIdx]) / noteDur;
            double amp = Math.pow(1.0 - progress, 1.3) * 0.60;

            double sample = amp * Math.sin(phase);
            writeSample(bytes, i * 2, sample);
        }
        return bytes;
    }

    /** Ghi một mẫu âm thanh vào bộ đệm PCM 16 bit theo định dạng dùng chung. */
    private void writeSample(byte[] buffer, int offset, double sample) {
        if (sample > 1.0) sample = 1.0;
        if (sample < -1.0) sample = -1.0;
        short val = (short) (sample * 32767.0);
        buffer[offset] = (byte) (val & 0xFF);
        buffer[offset + 1] = (byte) ((val >> 8) & 0xFF);
    }
}
