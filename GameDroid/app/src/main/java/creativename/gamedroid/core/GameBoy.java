package creativename.gamedroid.core;

/*
     _n_________________
    |_|_______________|_|
    |  ,-------------.  |
    | |  .---------.  | |
    | |  |         |  | |
    | |  | GAME    |  | |
    | |  |   DROID |  | |
    | |  |         |  | |
    | |  `---------'  | |
    | `---------------' |
    |   _               |
    | _| |_         ,-. |
    ||_ O _|   ,-. "._,"|
    |  |_|    "._,"   A |
    |    _  _    B      |
    |   // //           |
    |  // //    \\\\\\  |
    |  `  `      \\\\\\ ,
    |________...______,"

    CreativeName 2016:
      * Matt Penny
      * Alan Wu
      * Hammad Asad
      * Brendan Marko
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/* Entry point to the emulator core */
public class GameBoy {
    public Cartridge cartridge;
    public MMU mmu;
    public CPU cpu;
    public LCD lcd;
    public Controller gamepad;
    public boolean stopped;
    public Timer timer;
    public Divider divider;
    public RenderTarget renderTarget;
    private AtomicBoolean terminated;
    private Runnable runAtLoopEnd;

    public GameBoy() {
        cartridge = null;
        cpu = new CPU(this);
        lcd = new LCD(this);
        timer = new Timer();
        divider = new Divider();
        gamepad = new Controller(this);
        mmu = new MMU(this);
        stopped = false;
        terminated = new AtomicBoolean(false);

        this.renderTarget = new RenderTarget() {
            @Override
            public void frameReady(int[] frameBuffer) {}
        };
    }

    public GameBoy(RenderTarget target) {
        this();
        this.renderTarget = target;
    }

    public void terminate() {
        terminated.set(true);
    }

    public void queueRunnable(Runnable r) {
        runAtLoopEnd = r;
    }

    public void run() {
        while (!terminated.get()) {
            if (!stopped) {
                int cyclesUsed = cpu.execInstruction();
                if (timer.notifyCyclesPassed(cyclesUsed)) {
                    // Timer overflowed: raise interrupt
                    cpu.raiseInterrupt(CPU.Interrupt.TIMER);
                }
                divider.notifyCyclesPassed(cyclesUsed);

                while (cyclesUsed-- > 0)
                    lcd.tick();
            }

            if (runAtLoopEnd != null) {
                runAtLoopEnd.run();
                runAtLoopEnd = null;
            }
        }
    }

    public void saveStateToFile(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        GZIPOutputStream zos = new GZIPOutputStream(fos);
        ObjectOutputStream out = new ObjectOutputStream(zos);

        try {
            out.writeInt(cartridge.mbc.romBankNum);
            out.writeInt(cartridge.mbc.ramBankNum);
            out.writeBoolean(cartridge.mbc.ramEnabled);
            out.writeObject(mmu);
            out.writeObject(cpu);
            out.writeObject(lcd);
            out.writeObject(timer);
            out.writeObject(divider);
            out.writeBoolean(stopped);
            out.close();
            zos.close();
            fos.close();
        } catch (IOException e) {
            out.close();
            zos.close();
            fos.close();
            throw e;
        }
    }

    public void loadStateFromFile(File f) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(f);
        GZIPInputStream zis = new GZIPInputStream(fis);
        ObjectInputStream in = new ObjectInputStream(zis);

        try {
            cartridge.mbc.romBankNum = in.readInt();
            cartridge.mbc.ramBankNum = in.readInt();
            cartridge.mbc.ramEnabled = in.readBoolean();
            mmu = (MMU) in.readObject();
            mmu.gb = this;
            cpu = (CPU) in.readObject();
            cpu.gb = this;
            lcd = (LCD) in.readObject();
            lcd.gb = this;
            timer = (Timer) in.readObject();
            divider = (Divider) in.readObject();
            stopped = in.readBoolean();
            in.close();
            zis.close();
            fis.close();
        } catch (IOException e) {
            in.close();
            zis.close();
            fis.close();
            throw e;
        }
    }
}
