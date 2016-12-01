package creativename.gamedroid.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/* Rewind state tracker */
public class RewindManager {
    private AtomicBoolean rewinding;
    private RingBuffer<RewindPoint> rewindBuffer;

    public RewindManager() {
        // 30 seconds x 60fps = 1800 frames
        rewindBuffer = new RingBuffer<>(1800);
        rewinding = new AtomicBoolean(false);
    }

    public RewindPoint rewind(GameboyScreen screen) {
        // Rewinds through the states until aborted
        RewindPoint rewindPoint = null;
        while (rewinding.get()) {
            if (!rewindBuffer.isEmpty()) {
                rewindPoint = rewindBuffer.pop();
                Bitmap savedFrame = BitmapFactory.decodeByteArray(rewindPoint.renderedFrame, 0, rewindPoint.renderedFrame.length);
                screen.renderBitmap(savedFrame);
            }
        }
        return rewindPoint;
    }

    public synchronized void startRewinding() {
        if (!rewindBuffer.isEmpty()) {
            rewinding.set(true);
        }
    }

    public synchronized void commitRewind() {
        if (rewinding.get()) {
            rewinding.set(false);
        }
    }

    public synchronized void reset() {
        rewindBuffer.clear();
    }

    public synchronized void addRewindPoint(byte[] saveState, Bitmap renderedFrame) {
        if (!rewinding.get()) {
            rewindBuffer.push(new RewindPoint(saveState, renderedFrame));
        }
    }
}
