package creativename.gamedroid.ui;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

// rewind state tracker
public class RewindManager {
    private boolean rewinding;
    private boolean rewindAborted;
    private Deque<RewindPoint> rewindBuffer;
    private Iterator<RewindPoint> rewindIterator;

    public RewindManager() {
        rewindBuffer = new LinkedList<>();
    }

    public synchronized void startRewinding() {
        if (rewindBuffer.isEmpty()) {
            return;
        }

        rewindIterator = rewindBuffer.descendingIterator();
        rewinding = true;
        rewindAborted = false;
    }

    public synchronized RewindPoint rewindOneStep() {
        if (rewindIterator.hasNext() && rewinding) {
            return rewindIterator.next();
        }
        return null;
    }

    public synchronized void commitRewind() {
        if (rewinding && !rewindAborted) {
            rewinding = false;
            rewindBuffer.clear();
        }
    }

    public synchronized void reset() {
        rewindBuffer.clear();
    }

    public synchronized void abortRewind() {
        rewindAborted = true;
        rewinding = false;
    }

    public synchronized boolean isRewinding() {
        return rewinding;
    }

    public synchronized boolean isRewindAborted() {
        return rewindAborted;
    }

    public synchronized void addRewindPoint(byte[] saveState, Bitmap renderedFrame) {
        if (rewinding) {
            return;
        }
        rewindBuffer.addLast(new RewindPoint(saveState, renderedFrame));
        if (rewindBuffer.size() > 1500) {
            rewindBuffer.removeFirst();
        }
    }
}


class RewindPoint {
    public byte[] saveState;
    public byte[] renderedFrame;
    public RewindPoint(byte[] saveState, Bitmap renderedFrame) {
        this.saveState = saveState;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        renderedFrame.compress(Bitmap.CompressFormat.JPEG, 50, bos);
        this.renderedFrame = bos.toByteArray();
    }
}
