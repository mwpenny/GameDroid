package creativename.gamedroid.ui;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;

import java.io.IOException;

import creativename.gamedroid.R;
import creativename.gamedroid.core.Cartridge;
import creativename.gamedroid.core.GameBoy;

public class ControllerScreen extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_screen);

        String romPath = getIntent().getExtras().getString("rom");
        SurfaceView screen = (SurfaceView) findViewById(R.id.gbscreen);
        final GameboyScreen cb = new GameboyScreen();
        final GameBoy gb = new GameBoy(cb);
        try {
            gb.cartridge = new Cartridge(romPath, Cartridge.LoadMode.LOAD_ROM);
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        screen.getHolder().addCallback(cb);
        // Start simulating once surface is created
        screen.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        gb.run();
                    }
                }).start();
                holder.removeCallback(this);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
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