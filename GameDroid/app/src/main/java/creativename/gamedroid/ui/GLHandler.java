package creativename.gamedroid.ui;

/* *************************************************************************************************

[ GLHandler ]
This class handles the implementation of GLSurfaceView.Renderer for the ControllerScreen instance
of GLSurfaceView curr_gl_view

************************************************************************************************* */

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLHandler extends SurfaceView implements GLSurfaceView.Renderer
{

    // GLHandler(1-arg)
    public GLHandler(Context curr_context)
    {
        super(curr_context);
        System.out.println("Creating new GLHandler instance [1-arg]");
    }

    // GLHandler(2-arg)
    public GLHandler(Context curr_context, AttributeSet curr_attr)
    {
        super(curr_context, curr_attr);
        System.out.println("Creating new GLHandler instance [2-arg]");
    }

    // onDrawFrame()
    // the System calls this on each redraw of the OpenGL surface, handles gfx drawing
    public void onDrawFrame(GL10 curr_gl)
    {
        System.out.println("Inside onDrawFrame()");
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    }

    // onSurfaceCreated()
    // the System calls this once when creating the GLSurfaceView
    public void onSurfaceCreated(GL10 curr_gl, EGLConfig curr_config)
    {

        System.out.println("Inside onSurfaceCreated()");
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        curr_gl.glClearDepthf(1.0f);

    }

    // onSurfaceChanged()
    // Triggered when geometry changes happen to the surface such as resizing/orientation changes
    public void onSurfaceChanged(GL10 curr_gl, int width, int height)
    {

        System.out.println("Inside onSurfaceChanged()");

        if (height == 0)
        {
            height = 1;
        }

        GLES20.glViewport(0, 0, width, height);

    }

} // end : GLHandler Class
