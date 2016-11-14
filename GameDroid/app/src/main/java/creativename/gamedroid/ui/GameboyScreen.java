package creativename.gamedroid.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import creativename.gamedroid.core.RenderTarget;

/* Renders the GameBoy screen from its framebuffer */
public class GameboyScreen extends Activity implements SurfaceHolder.Callback, RenderTarget {
    SurfaceHolder holder;
    Bitmap frameBuff;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        holder.setKeepScreenOn(true);
        this.holder = holder;
        frameBuff = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888);
        Canvas c = holder.lockCanvas();
        c.drawColor(Color.BLUE);
        holder.unlockCanvasAndPost(c);
    }

    static Paint p = new Paint();
    final static Rect screenDimensions = new Rect(0, 0, 160, 144);
    @Override
    public void frameReady(int[] newFrame) {
        Canvas c;
        if (holder != null && (c = holder.lockCanvas()) != null) {
            frameBuff.setPixels(newFrame, 0, 160, 0, 0, 160, 144);
            c.drawBitmap(frameBuff, screenDimensions, holder.getSurfaceFrame(), p);
            holder.unlockCanvasAndPost(c);
        }
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
