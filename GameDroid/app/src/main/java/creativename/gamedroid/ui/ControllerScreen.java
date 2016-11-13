package creativename.gamedroid.ui;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import creativename.gamedroid.R;
import creativename.gamedroid.core.Cartridge;
import creativename.gamedroid.core.Controller;
import creativename.gamedroid.core.GameBoy;

public class ControllerScreen extends Activity
{
    private GameBoy gb;
    private SaveStateRunnable saveState;
    private LoadStateRunnable loadState;

    public ControllerScreen() {
        saveState = new SaveStateRunnable();
        loadState = new LoadStateRunnable();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_screen);
        setResult(RESULT_OK, getIntent());

        String romPath = getIntent().getStringExtra("rom_path");
        SurfaceView screen = (SurfaceView) findViewById(R.id.gbscreen);
        final GameboyScreen cb = new GameboyScreen();
        gb = new GameBoy(cb);
        try {
            gb.cartridge = new Cartridge(romPath, Cartridge.LoadMode.LOAD_ROM);
            if (gb.cartridge.hasBattery())
                loadGame();
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

    private File getSaveFile() {
        File path = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_saves));
        return new File(path, getIntent().getStringExtra("rom_title") + ".sav");
    }

    private File getStateFile() {
        File path = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_states));
        return new File(path, getIntent().getStringExtra("rom_title") + ".st");
    }

    private void saveGame() {
        // Save game
        File f = getSaveFile();
        try {
            gb.cartridge.mbc.saveRamToFile(f);
        } catch (IOException e) {
            System.err.format("Could not save game: %s\n", e.getMessage());
        }
    }

    private void loadGame() {
        // Load game from disk
        File f = getSaveFile();
        if (f.exists()) {
            try {
                gb.cartridge.mbc.loadRamFromFile(f);
            } catch (IOException e) {
                System.err.format("Could not load game: %s\n", e.getMessage());
            }
        }
    }

    private class SaveStateRunnable implements Runnable {
        @Override
        public void run() {
            File f = getStateFile();
            try {
                gb.saveStateToFile(f);
            } catch (IOException e) {
                System.err.format("Could not save state: %s\n", e.getMessage());
            }
        }
    }

    private class LoadStateRunnable implements Runnable {
        @Override
        public void run() {
            File f = getStateFile();
            if (f.exists()) {
                try {
                    gb.loadStateFromFile(f);
                } catch (Exception e) {
                    System.err.format("Could not load state: %s\n", e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (gb != null && gb.cartridge != null && gb.cartridge.hasBattery()) {
            saveGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (gb != null) {
            gb.terminate();
        }
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
            /*
            case R.id.save_button:
                return Controller.Button.SAVE;
            case R.id.load_button:
                return Controller.Button.LOAD;
            */
            default:
                return null;
        }
    }

    public void buttonPress(View btn)
    {
        // TODO: use onTouch events so we can detect pressing/releasing
        Controller.Button b = getButtonCode(btn);
        if (b != null) {
            if (b == Controller.Button.DOWN)
                gb.queueRunnable(saveState);
            else if (b == Controller.Button.UP)
                gb.queueRunnable(loadState);

            gb.gamepad.updateButton(b, true);
            //gb.gamepad.updateButton(b, false);
        }
    }
}