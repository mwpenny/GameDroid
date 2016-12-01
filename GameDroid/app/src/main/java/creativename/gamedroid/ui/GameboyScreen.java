package creativename.gamedroid.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import creativename.gamedroid.core.GameBoy;
import creativename.gamedroid.core.RenderTarget;

/* Renders the GameBoy screen from its framebuffer */
public class GameboyScreen extends Activity implements SurfaceHolder.Callback, RenderTarget {
    final static Paint p = new Paint();
    final static Rect screenDimensions = new Rect(0, 0, 160, 144);

    private SurfaceHolder holder;
    private Bitmap frameBuff;
    private GameBoy gb;
    private RewindManager rewindManager;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        holder.setKeepScreenOn(true);
        this.holder = holder;
        if (frameBuff == null) {
            frameBuff = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888);
        }
        Canvas c = holder.lockCanvas();
        c.drawBitmap(frameBuff, screenDimensions, holder.getSurfaceFrame(), p);
        holder.unlockCanvasAndPost(c);
    }

    public void renderBitmap(Bitmap bmp) {
        Canvas c;
        if (holder != null && (c = holder.lockCanvas()) != null) {
            c.drawBitmap(bmp, screenDimensions, holder.getSurfaceFrame(), p);
            holder.unlockCanvasAndPost(c);
        }
    }

    @Override
    public void frameReady(int[] newFrame) {
        frameBuff.setPixels(newFrame, 0, 160, 0, 0, 160, 144);
        renderBitmap(frameBuff);

        // Add rewind point for this frame
        ByteArrayOutputStream saveStateStream = new ByteArrayOutputStream();
        try {
            gb.saveState(saveStateStream);
        } catch (IOException e) {
            // Should never happen since this is an in-memory stream
            return;
        }
        rewindManager.addRewindPoint(saveStateStream.toByteArray(), frameBuff);
    }

    public void setGb(GameBoy gb) {
        this.gb = gb;
    }

    public void setRewindManager(RewindManager rewindManager) {
        this.rewindManager = rewindManager;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        System.out.println("surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        System.out.println("surf destroyed?");
    }
}

