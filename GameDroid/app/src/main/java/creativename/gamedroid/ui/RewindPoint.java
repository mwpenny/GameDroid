package creativename.gamedroid.ui;


import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

/* An in-memory save state with a frame image */
public class RewindPoint {
    public byte[] saveState;
    public byte[] renderedFrame;
    RewindPoint(byte[] saveState, Bitmap renderedFrame) {
        this.saveState = saveState;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        renderedFrame.compress(Bitmap.CompressFormat.JPEG, 50, bos);
        this.renderedFrame = bos.toByteArray();
    }
}