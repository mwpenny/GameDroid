package creativename.gamedroid.ui;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.Intent;
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
import java.util.Date;

import creativename.gamedroid.R;
import creativename.gamedroid.core.Cartridge;
import creativename.gamedroid.core.Controller;
import creativename.gamedroid.core.GameBoy;

public class ControllerScreen extends Activity
{
    GameBoy gb;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_screen);
        setResult(RESULT_OK, getIntent());

        String romPath = getIntent().getStringExtra("rom_path");
        SurfaceView screen = (SurfaceView) findViewById(R.id.gbscreen);
        final GameboyScreen cb = new GameboyScreen();
        final GameBoy gb = new GameBoy(cb);
        this.gb = gb;
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
                Thread emulator = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        gb.run();
                    }
                }, "Emulation: " + gb.cartridge.getTitle());
                emulator.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.gb.terminate();
    }

    private Controller.Button getButtonCode(View btn) {
        switch (btn.getId()) {
            case R.id.controller_up_arrow:
                return Controller.Button.UP;
            case R.id.controller_down_arrow:
                return Controller.Button.DOWN;
            case R.id.controller_left_arrow:
                return Controller.Button.LEFT;
            case R.id.controller_right_arrow:
                return Controller.Button.RIGHT;
            case R.id.controller_select:
                return Controller.Button.SELECT;
            case R.id.controller_start:
                return Controller.Button.START;
            case R.id.controller_a_button:
                return Controller.Button.A;
            case R.id.controller_b_button:
                return Controller.Button.B;
            default:
                return null;
        }
    }

    public void buttonPress(View btn)
    {
        // TODO: use onTouch events so we can detect pressing/releasing
        Controller.Button b = getButtonCode(btn);
        if (b != null)
            gb.gamepad.updateButton(b, true);
    }
}