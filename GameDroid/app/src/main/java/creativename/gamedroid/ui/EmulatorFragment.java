package creativename.gamedroid.ui;

import android.app.Fragment;
import android.os.Bundle;

import java.io.ByteArrayInputStream;

import creativename.gamedroid.core.GameBoy;

/* Fragment for retaining an instance of the emulator core */
public class EmulatorFragment extends Fragment {
    public GameBoy gb;
    public GameboyScreen screen;
    public RewindManager rewindManager;
    private Thread emulationThread, rewindThread;
    private boolean rewindAfterEmulation, emulateAfterRewind;

    public synchronized boolean isEmulationPaused() {
        return (emulationThread != null && !emulationThread.isAlive());
    }

    public synchronized void stopEmulation(boolean abort) {
        if (gb != null) {
            rewindAfterEmulation = !abort;
            gb.terminate();
        }
    }

    public synchronized void stopRewind(boolean abort) {
        if (rewindManager != null) {
            emulateAfterRewind = !abort;
            rewindManager.commitRewind();
        }
    }

    public synchronized void startEmulation() {
        if (emulationThread == null || !emulationThread.isAlive()) {
            emulationThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    gb.run();
                    if (rewindAfterEmulation)
                        startRewind();
                }
            }, "Emulation: " + gb.cartridge.getTitle());
            emulationThread.start();
        }
    }

    public synchronized void startRewind() {
        // Setup rewind thread to run after emulation is terminated
        if (rewindThread == null || !rewindThread.isAlive()) {
            rewindManager.startRewinding();
            rewindThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    RewindPoint rewindPoint = rewindManager.rewind(screen);

                    if (rewindPoint != null) {
                        ByteArrayInputStream state = new ByteArrayInputStream(rewindPoint.saveState);
                        try {
                            gb.loadState(state);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (emulateAfterRewind)
                        startEmulation();
                }
            }, "Rewind");
            rewindThread.start();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across activity restarts
        setRetainInstance(true);
    }
}