package creativename.gamedroid.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import creativename.gamedroid.core.GameBoy;
import creativename.gamedroid.core.RenderTarget;

/* OpenGL surface for rendering the GameBoy's screen */
public class GameboyScreen extends GLSurfaceView implements GLSurfaceView.Renderer, RenderTarget {
    private static final float[] vertices = {
        -1f, -1f,  // Bottom left
        -1f, 1f,   // Top left
        1f, 1f,    // Top right
        1f, -1f    // Bottom right
    };
    private static final float[] textureCoords = {
        0f, 1f,  // Top left
        0f, 0f,  // Bottom left
        1f, 0f,  // Bottom right
        1f, 1f   // Top right
    };
    private static final byte[] indices = {
        0, 1, 2,  // First triangle
        0, 2, 3   // Second triangle
    };

    private ByteBuffer indicesBuffer;
    private int[] textures;  // Texture handles
    private Bitmap frame;
    private GameBoy gb;
    private RewindManager rewindManager;
    private long lastRenderTimeMs;
    private long msPerFrame;

    private void initScreen() {
        textures = new int[1];
        frame = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888);

        // Only render when there's a change
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public GameboyScreen(Context context) {
        super(context);
        initScreen();
    }

    public GameboyScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        initScreen();
    }

    public void setGb(GameBoy gb) {
        this.gb = gb;
    }

    public void setFPS(int fps) {
        msPerFrame = 1000 / fps;
    }

    public void setRewindManager(RewindManager rewindManager) {
        this.rewindManager = rewindManager;
    }

    @Override
    public void frameReady(int[] newFrame) {
        frame.setPixels(newFrame, 0, 160, 0, 0, 160, 144);

        // Add rewind point for this frame
        ByteArrayOutputStream saveStateStream = new ByteArrayOutputStream();
        try {
            gb.saveState(saveStateStream);
        } catch (IOException e) {
            // Should never happen since this is an in-memory stream
            e.printStackTrace();
            return;
        }
        rewindManager.addRewindPoint(saveStateStream.toByteArray(), frame);

        if (msPerFrame > 0) {
            // Limit framerate
            long dt = SystemClock.uptimeMillis() - lastRenderTimeMs;
            if (dt < msPerFrame) {
                SystemClock.sleep(msPerFrame - dt);
            }
        }

        requestRender();
        lastRenderTimeMs = SystemClock.uptimeMillis();
    }

    public void renderBitmap(Bitmap bmp) {
        frame = bmp;
        requestRender();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Render full screen quad textured with the image for this frame
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, frame, 0);
        gl.glDrawElements(GLES10.GL_TRIANGLE_STRIP, indicesBuffer.capacity(), GLES10.GL_UNSIGNED_BYTE, indicesBuffer);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Init vertex buffer (cover entire screen)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);  // 4 bytes in float
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuffer;
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);

        // Init texture coordinate buffer (use whole texture)
        byteBuffer = ByteBuffer.allocateDirect(textureCoords.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer textureCoordBuffer;
        textureCoordBuffer = byteBuffer.asFloatBuffer();
        textureCoordBuffer.put(textureCoords);
        textureCoordBuffer.position(0);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureCoordBuffer);

        // Init vertex index buffer
        indicesBuffer = ByteBuffer.allocateDirect(indices.length);
        indicesBuffer.order(ByteOrder.nativeOrder());
        indicesBuffer.put(indices);
        indicesBuffer.position(0);

        // Init texture for GameBoy game frame
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glGenTextures(1, textures, 0);
        gl.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
        gl.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);

        // Set the background frame color
        gl.glClearColor(1f, 1.0f, 1f, 1f);
        gl.glClearDepthf(1.0f);

        getHolder().setKeepScreenOn(true);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // Resize view to fit new size
        gl.glViewport(0, 0, width, height);
    }
}

