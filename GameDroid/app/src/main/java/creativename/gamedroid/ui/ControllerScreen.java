package creativename.gamedroid.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;

import creativename.gamedroid.R;

public class ControllerScreen extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_screen);
    }

    /* Button Handlers for the Controller displayed within the
       ControllerScreen class [using controller_layout] */
    public void up_arrow_handler(View currView)
    {

        System.out.println("Inside up_arrow_handler");

    }

    public void down_arrow_handler(View currView)
    {

        System.out.println("Inside down_arrow_handler");

    }

    public void right_arrow_handler(View currView)
    {

        System.out.println("Inside right_arrow_handler");

    }

    public void left_arrow_handler(View currView)
    {

        System.out.println("Inside left_arrow_handler");

    }

    public void select_button_handler(View currView)
    {

        System.out.println("Inside select_button_handler");

    }

    public void start_button_handler(View currView) {

        System.out.println("Inside start_button_handler");

    }

    public void a_button_handler(View currView) {

        System.out.println("Inside a_button_handler");

    }

    public void b_button_handler(View currView)
    {

        System.out.println("Inside b_button_handler");

    }
}