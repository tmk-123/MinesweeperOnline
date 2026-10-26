package minesweeperonline.client.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Trình quản lý âm thanh thuần Java JDK (javax.sound.sampled.Clip).
 * Tự động tổng hợp sóng âm (PCM in-memory synthesis) - không phụ thuộc file .wav ngoài,
 * không cần thư viện bên thứ ba, đảm bảo 100% tương thích và không bị lỗi thiếu file.
 */
public class SoundManager {

    private static final float SAMPLE_RATE = 44100.0f;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private static final SoundManager INSTANCE = new SoundManager();

    public static SoundManager getInstance() {
        return INSTANCE;
    }

    private final ExecutorService soundPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Sound-Player");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean muted = false;

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
        this.muted = muted;
    }

    public boolean toggleMute() {
        this.muted = !this.muted;
        return this.muted;
    }

    // --- Các phương thức phát âm thanh công khai ---

    public void playClick() {
        if (muted) return;
        soundPool.submit(() -> {
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

    public void playFlag() {
        playClip(clipFlag);
    }

    public void playExplosion() {
        playClip(clipExplosion);
    }

    public void playTick() {
        playClip(clipTick);
    }

    public void playVictory() {
        playClip(clipVictory);
    }

    public void playDefeat() {
        playClip(clipDefeat);
    }

    private void playClip(Clip clip) {
        if (muted || clip == null) return;
        soundPool.submit(() -> {
            try {
                synchronized (clip) {
                    if (clip.isRunning()) clip.stop();
                    clip.setFramePosition(0);
                    clip.start();
                }
            } catch (Throwable ignored) {}
        });
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
     * Tiếng bom nổ (Boom trầm 550ms: White noise + sóng rumble tần số thấp 75Hz)
     */
    private byte[] synthesizeExplosion() {
        double duration = 0.550;
        int numSamples = (int) (SAMPLE_RATE * duration);
        byte[] bytes = new byte[numSamples * 2];
        double rumblePhase = 0.0;
        double smoothNoise = 0.0;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = t / duration;

            // Nhiễu trắng được làm mượt (low-pass filter)
            double rawNoise = Math.random() * 2.0 - 1.0;
            smoothNoise = 0.82 * smoothNoise + 0.18 * rawNoise;

            // Sóng rumble âm trầm 75Hz quét xuống 45Hz
            double rumbleFreq = 75.0 - 30.0 * progress;
            rumblePhase += 2.0 * Math.PI * rumbleFreq / SAMPLE_RATE;
            double rumble = Math.sin(rumblePhase);

            double amp = Math.exp(-4.8 * progress);
            double sample = (smoothNoise * 0.70 + rumble * 0.45) * amp;

            // Bão hòa mềm (soft-saturation) tạo cảm giác đập mạnh
            sample = Math.tanh(sample * 1.8);

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

    private void writeSample(byte[] buffer, int offset, double sample) {
        if (sample > 1.0) sample = 1.0;
        if (sample < -1.0) sample = -1.0;
        short val = (short) (sample * 32767.0);
        buffer[offset] = (byte) (val & 0xFF);
        buffer[offset + 1] = (byte) ((val >> 8) & 0xFF);
    }
}
