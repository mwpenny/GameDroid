package creativename.gamedroid.ui;

import android.app.Activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import creativename.gamedroid.R;
import creativename.gamedroid.core.Cartridge;
import creativename.gamedroid.core.Controller;
import creativename.gamedroid.core.GameBoy;

import static creativename.gamedroid.ui.GameboyScreen.p;
import static creativename.gamedroid.ui.GameboyScreen.screenDimensions;

/* View for emulator rendering + gamepad UI */
public class EmulatorActivity extends Activity implements View.OnTouchListener, View.OnClickListener {
    private AlertDialog loadError, yesNoPrompt, resumePlay;
    private EmulatorFragment emulator;
    private SaveStateRunnable saveState;
    private LoadStateRunnable loadState;
    private Toast toast;
    private Thread emulatorThread;

    public EmulatorActivity() {
        saveState = new SaveStateRunnable();
        loadState = new LoadStateRunnable();
    }

    private void initEmulator() {
        String romPath = getIntent().getStringExtra("rom_path");
        FragmentManager fm = getFragmentManager();
        SurfaceView screen = (SurfaceView) findViewById(R.id.gbscreen);
        final GameboyScreen cb = new GameboyScreen();
        final boolean firstRun;

        emulator = (EmulatorFragment) fm.findFragmentByTag("emulator");

        // Create the fragment and data the first time
        if (emulator == null) {
            firstRun = true;

            // Add the fragment
            emulator = new EmulatorFragment();
            fm.beginTransaction().add(emulator, "emulator").commit();

            emulator.gb = new GameBoy(cb);
            emulator.rewindManager = new RewindManager();
            // Parse and load the ROM
            try {
                emulator.gb.cartridge = new Cartridge(romPath, Cartridge.LoadMode.LOAD_ROM);
                if (emulator.gb.cartridge.hasBattery())
                    loadGame();
            } catch (Exception e) {
                // ROM could not be loaded
                exitWithError(getString(R.string.dialog_load_error_title), e.getMessage());
            }
        } else {
            firstRun = false;
            emulator.gb.renderTarget = cb;
        }

        cb.setGb(emulator.gb);
        cb.setRewindManager(emulator.rewindManager);

        screen.getHolder().addCallback(cb);

        // Start simulating once surface is created
        screen.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                launchEmulationThread();

                // Prompt to resume game
                boolean stateExists = getStateFile().exists();
                if (stateExists && firstRun) {
                    promptYesNo(getString(R.string.dialog_load_state_title),
                            getString(R.string.dialog_resume_state_message),
                            loadState, false);
                }
                findViewById(R.id.controller_load).setEnabled(stateExists);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                holder.removeCallback(this);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emulator);
        setResult(RESULT_OK, getIntent());

        initEmulator();

        // Set up listeners
        findViewById(R.id.controller_dpad).setOnTouchListener(this);
        findViewById(R.id.controller_a).setOnTouchListener(this);
        findViewById(R.id.controller_b).setOnTouchListener(this);
        findViewById(R.id.controller_select).setOnTouchListener(this);
        findViewById(R.id.controller_start).setOnTouchListener(this);
        findViewById(R.id.controller_save).setOnClickListener(this);
        findViewById(R.id.controller_load).setOnClickListener(this);
        findViewById(R.id.braid).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    emulator.rewindManager.startRewinding();
                    terminateEmulationThenLaunchRewind();
                } else if (action == MotionEvent.ACTION_UP) {
                    emulator.rewindManager.commitRewind();
                }
                return action == MotionEvent.ACTION_DOWN;
            }
        });
    }

    boolean rewindThreadRunning = false;
    public synchronized void launchRewindThread() {
        if (rewindThreadRunning) {
            return;
        }
        System.out.println("launching rewind");
        final EmulatorActivity parent = this;
        rewindThreadRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                SurfaceView screen = (SurfaceView) findViewById(R.id.gbscreen);
                byte[] resumeTo = null;
                while (true) {
                    synchronized (emulator.rewindManager) {
                        if (!emulator.rewindManager.isRewinding()) break;

                        SurfaceHolder holder = screen.getHolder();

                        RewindPoint rewindPoint = emulator.rewindManager.rewindOneStep();
                        if (rewindPoint != null) {
                            Canvas c = screen.getHolder().lockCanvas();
                            if (c == null) {
                                emulator.rewindManager.abortRewind();
                                return;
                            }
                            c.drawBitmap(rewindPoint.renderedFrame, screenDimensions, holder.getSurfaceFrame(), p);
                            holder.unlockCanvasAndPost(c);
                            resumeTo = rewindPoint.saveState;
                        }
                    }
                }

                if (resumeTo != null && !emulator.rewindManager.isRewindAborted()) {
                    ByteArrayInputStream state = new ByteArrayInputStream(resumeTo);
                    try {
                        emulator.gb.loadState(state);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                launchEmulationThread();
                synchronized (parent) {
                    rewindThreadRunning = false;
                }
            }
        }, "Rewind").start();
    }

    /* Fragment for retaining an instance of the emulator core */
    public static class EmulatorFragment extends Fragment {
        public GameBoy gb;
        public RewindManager rewindManager;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Retain this fragment across activity restarts
            setRetainInstance(true);
        }
    }

    private File getSaveFile() {
        File path = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_saves));
        return new File(path, getIntent().getStringExtra("rom_title") + ".sav");
    }

    private File getStateFile() {
        File path = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_states));
        return new File(path, getIntent().getStringExtra("rom_title") + ".st");
    }

    private void showToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast != null)
                    toast.cancel();
                toast = Toast.makeText(EmulatorActivity.this, s, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private void promptYesNo(String title, String message, final Runnable action, final boolean finish) {
        // Prompt the user to perform some action (i.e., saving/loading states)
        closeYesNoPrompt();
        yesNoPrompt = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme))
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (action != null)
                            emulator.gb.queueRunnable(action);
                        if (finish)
                            finish();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private boolean launchRewindAfterEmulation = false;
    private synchronized void terminateEmulationThenLaunchRewind() {
        launchRewindAfterEmulation = true;
        emulator.gb.terminate();
    }

    private boolean emulationRunning = false;
    private synchronized void launchEmulationThread() {
        if (emulationRunning) {
            return;
        }
        System.out.println("launching emulation");
        emulationRunning = true;
        final EmulatorActivity parent = this;
        emulatorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                emulator.gb.run();
                synchronized (parent) {
                    emulationRunning = false;
                }
                synchronized (parent) {
                    if (launchRewindAfterEmulation) {
                        launchRewindThread();
                        launchRewindAfterEmulation = false;
                    }
                }
            }
        }, "Emulation: " + emulator.gb.cartridge.getTitle());
        emulatorThread.start();
    }

    private void closeYesNoPrompt() {
        if (yesNoPrompt != null && yesNoPrompt.isShowing())
            yesNoPrompt.dismiss();
    }

    private void exitWithError(String title, String msg) {
        // Display error and leave activity when dismissed
        loadError = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme))
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();
    }

    private void loadGame() {
        // Load game save file from disk
        File f = getSaveFile();
        if (f.exists()) {
            try {
                emulator.gb.cartridge.mbc.loadRamFromFile(f);
            } catch (IOException e) {
                showToast(String.format(getString(R.string.game_load_error), e.getMessage()));
            }
        }
    }

    private void saveGame() {
        // Save game
        File f = getSaveFile();
        try {
            emulator.gb.cartridge.mbc.saveRamToFile(f);
        } catch (IOException e) {
            showToast(String.format(getString(R.string.game_save_error), e.getMessage()));
        }
    }

    private class LoadStateRunnable implements Runnable {
        @Override
        public void run() {
            // Load a previously-saved snapshot of the emulator's state
            File f = getStateFile();
            String s = "";
            if (f.exists()) {
                try {
                    emulator.gb.loadStateFromFile(f);
                    s = getString(R.string.state_loaded);
                    emulator.rewindManager.reset();
                } catch (Exception e) {
                    s = String.format(getString(R.string.state_load_error), e.getMessage());
                } finally {
                    showToast(s);
                }
            }
        }
    }

    private class SaveStateRunnable implements Runnable {
        @Override
        public void run() {
            // Save a snapshot of the emulator's state
            File f = getStateFile();
            String s = "";
            try {
                emulator.gb.saveStateToFile(f);
                s = getString(R.string.state_saved);

                // Enable load state button now that a save state exists
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.controller_load).setEnabled(true);
                    }
                });
            } catch (IOException e) {
                s = String.format(getString(R.string.state_save_error), e.getMessage());
            } finally {
                showToast(s);
            }
        }
    }

    @Override
    public void onBackPressed() {
        promptYesNo(getString(R.string.dialog_exit_title),
                getString(R.string.dialog_exit_message),
                null, true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Make sure the user's game is saved
        if (emulator != null &&
                emulator.gb != null &&
                emulator.gb.cartridge != null &&
                emulator.gb.cartridge.hasBattery()) {
            saveGame();
        }

        if (emulator != null && emulator.gb != null) {
            emulator.gb.terminate();
            emulator.rewindManager.abortRewind();
        }
    }

    private boolean emulationDied() {
        return emulatorThread != null && !emulatorThread.isAlive();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* Emulation thread has died after initialization (due to onPause)
           and the user has not been prompted to restart it */
        if (emulatorThread != null && !emulatorThread.isAlive() &&
                (resumePlay == null || !resumePlay.isShowing())) {
            // Prompt user to resume game (so they have time to react)
            resumePlay = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme))
                    .setMessage(getString(R.string.dialog_paused_title))
                    .setPositiveButton(getString(R.string.dialog_resume_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            launchEmulationThread();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            launchEmulationThread();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (emulator != null && emulator.gb != null) {
            emulator.gb.terminate();
        }

        // Dismiss dialogs
        if (loadError != null && loadError.isShowing())
            loadError.dismiss();
        if (resumePlay != null && resumePlay.isShowing())
            resumePlay.dismiss();
        closeYesNoPrompt();
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
            emulator.gb.gamepad.updateButton(Controller.Button.UP, up);
            emulator.gb.gamepad.updateButton(Controller.Button.DOWN, down);
            emulator.gb.gamepad.updateButton(Controller.Button.LEFT, left);
            emulator.gb.gamepad.updateButton(Controller.Button.RIGHT, right);

            // Update UI gamepad
            findViewById(R.id.controller_dpad_up).setPressed(up);
            findViewById(R.id.controller_dpad_down).setPressed(down);
            findViewById(R.id.controller_dpad_left).setPressed(left);
            findViewById(R.id.controller_dpad_right).setPressed(right);

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
                    emulator.gb.gamepad.updateButton(b, true);
                } else if (action == MotionEvent.ACTION_UP) {
                    btn.setPressed(false);
                    emulator.gb.gamepad.updateButton(b, false);
                }
            }
        }
        return (action == MotionEvent.ACTION_DOWN);
    }

    @Override
    public void onClick(View v) {
        // Handle button presses for miscellaneous (i.e., save/load state)
        if (v.getId() == R.id.controller_save) {
            // Prompt to save state if one exists (don't want to accidentally overwrite)
            if (getStateFile().exists()) {
                promptYesNo(getString(R.string.dialog_save_state_title),
                        getString(R.string.dialog_save_state_overwrite_message),
                        saveState, false);
            } else {
                emulator.gb.queueRunnable(saveState);
            }
        } else {
            // Prompt to load state (don't want to accidentally load and lose progress)
            promptYesNo(getString(R.string.dialog_load_state_title),
                    getString(R.string.dialog_load_state_message),
                    loadState, false);
        }
    }
}