package creativename.gamedroid.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import java.math.BigInteger;

import creativename.gamedroid.core.GameBoy;
import creativename.gamedroid.core.RenderTarget;

public class GameboyScreen extends Activity implements SurfaceHolder.Callback, RenderTarget {
    SurfaceHolder holder;
    Bitmap frameBuff;
    GameBoy gb;

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

    @Override
    public void frameReady(int[] newFrame) {
        if (holder == null) return;
        Canvas c = holder.lockCanvas();
        if (c == null) return;
        frameBuff.setPixels(newFrame, 0, 160, 0, 0, 160, 144);
        c.drawBitmap(frameBuff, new Rect(0, 0, 160, 144), holder.getSurfaceFrame(), p);
        holder.unlockCanvasAndPost(c);
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
