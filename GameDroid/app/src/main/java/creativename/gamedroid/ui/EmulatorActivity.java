package creativename.gamedroid.ui;

import android.app.Activity;

import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import creativename.gamedroid.R;
import creativename.gamedroid.core.Cartridge;
import creativename.gamedroid.core.Controller;
import creativename.gamedroid.core.GameBoy;

public class EmulatorActivity extends Activity implements View.OnTouchListener
{
    private GameBoy gb;
    private SaveStateRunnable saveState;
    private LoadStateRunnable loadState;
    private Toast toast;

    public EmulatorActivity() {
        saveState = new SaveStateRunnable();
        loadState = new LoadStateRunnable();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emulator);
        setResult(RESULT_OK, getIntent());

        // Set up listeners
        findViewById(R.id.controller_dpad).setOnTouchListener(this);

        findViewById(R.id.controller_a).setOnTouchListener(this);
        findViewById(R.id.controller_b).setOnTouchListener(this);
        findViewById(R.id.controller_select).setOnTouchListener(this);
        findViewById(R.id.controller_start).setOnTouchListener(this);
        findViewById(R.id.controller_save).setOnTouchListener(this);
        findViewById(R.id.controller_load).setOnTouchListener(this);

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

    private void showToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast = Toast.makeText(EmulatorActivity.this, s, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private class SaveStateRunnable implements Runnable {
        @Override
        public void run() {
            File f = getStateFile();
            String s = "";
            if (toast != null)
                toast.cancel();
            try {
                gb.saveStateToFile(f);
                s = getString(R.string.state_saved);
            } catch (IOException e) {
                s = String.format(getString(R.string.state_save_error), e.getMessage());
            } finally {
                showToast(s);
            }
        }
    }

    private class LoadStateRunnable implements Runnable {
        @Override
        public void run() {
            File f = getStateFile();
            String s = "";
            if (toast != null)
                toast.cancel();
            if (f.exists()) {
                try {
                    gb.loadStateFromFile(f);
                    s = getString(R.string.state_loaded);
                } catch (Exception e) {
                    s = String.format(getString(R.string.state_load_error), e.getMessage());
                } finally {
                    showToast(s);
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

    @Override
    public boolean onTouch(View btn, MotionEvent e) {
        // Handle gamepad button presses
        int action = e.getAction();
        if (btn.getId() == R.id.controller_dpad) {
            // Determine which d-pad keys are pressed
            boolean up, down, left, right;
            if (action != MotionEvent.ACTION_UP) {
                int x = (int) e.getX();
                int y = (int) e.getY();
                up = y < btn.getHeight() / 3;
                down = y > 2 * btn.getHeight() / 3;
                left = x < btn.getWidth() / 3;
                right = x > 2 * btn.getWidth() / 3;
            } else {
                up = down = left = right = false;
            }

            // Update emulator gamepad
            gb.gamepad.updateButton(Controller.Button.UP, up);
            gb.gamepad.updateButton(Controller.Button.DOWN, down);
            gb.gamepad.updateButton(Controller.Button.LEFT, left);
            gb.gamepad.updateButton(Controller.Button.RIGHT, right);

            // Update UI gamepad
            findViewById(R.id.controller_dpad_up).setPressed(up);
            findViewById(R.id.controller_dpad_down).setPressed(down);
            findViewById(R.id.controller_dpad_left).setPressed(left);
            findViewById(R.id.controller_dpad_right).setPressed(right);


        } else if (btn.getId() == R.id.controller_load || btn.getId() == R.id.controller_save) {
            // Save/load states
            if (action == MotionEvent.ACTION_UP) {
                gb.queueRunnable((btn.getId() == R.id.controller_save) ? saveState : loadState);
            }
        } else {
            // Determine which controller buttons are pressed
            Controller.Button b = null;
            switch (btn.getId()) {
                case R.id.controller_select:
                    b = Controller.Button.SELECT;
                    break;
                case R.id.controller_start:
                    b = Controller.Button.START;
                    break;
                case R.id.controller_a:
                    b = Controller.Button.A;
                    break;
                case R.id.controller_b:
                    b = Controller.Button.B;
                    break;
            }
            if (b != null) {
                if (action == MotionEvent.ACTION_DOWN) {
                    btn.setPressed(true);
                    gb.gamepad.updateButton(b, true);
                }
                else if (action == MotionEvent.ACTION_UP) {
                    btn.setPressed(false);
                    gb.gamepad.updateButton(b, false);
                }
            }
        }
        return (action == MotionEvent.ACTION_DOWN);
    }
}