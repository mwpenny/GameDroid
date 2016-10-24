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

    public GameBoy() {
        cartridge = null;  // For now
        cpu = new CPU(this);
        lcd = new LCD(this);
        timer = new Timer();
        divider = new Divider();
        gamepad = new Controller(this);
        mmu = new MMU(this);
        stopped = false;
    }

    public void run() {
        /* TODO: load cartridge */
        while (true) {
            if (!stopped) {
                int cyclesUsed = cpu.execInstruction();
                if (cyclesUsed == 0) {  // cpu in halt mode
                    // TODO: other interrupts may occur before timer (e.g., LCD, joypad)
                    cyclesUsed += this.timer.advanceUntilInterrupt();
                    cpu.raiseInterrupt(CPU.Interrupt.TIMER);
                } else {
                    boolean raiseInterrupt = this.timer.notifyCyclesPassed(cyclesUsed);
                    if (raiseInterrupt)
                        cpu.raiseInterrupt(CPU.Interrupt.TIMER);
                }
                this.divider.notifyCyclesPassed(cyclesUsed);
                for (int i = 0; i < cyclesUsed; ++i)
                    lcd.tick();
            }
        }
    }
}
