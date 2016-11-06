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

import java.util.concurrent.atomic.AtomicBoolean;

/* Entry point to the emulator core */
public class GameBoy {
    public Cartridge cartridge;
    public MMU mmu;
    public final CPU cpu;
    public final LCD lcd;
    public final Controller gamepad;
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
        this.runAtLoopEnd = new Runnable() {
            @Override
            public void run() {}
        };
    }

    public GameBoy(RenderTarget target) {
        this();
        this.renderTarget = target;
    }

    public void terminate() {
        terminated.set(true);
    }

    public void run() {
        while (true) {
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
            if (terminated.get()) return;
        }
    }
}
