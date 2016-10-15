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

    public GameBoy() {
        cartridge = null;  // For now
        mmu = new MMU(this);
        cpu = new CPU(this);
        lcd = new LCD(this);
        gamepad = new Controller(this);
    }

    public void run() {
        /* TODO: load cartridge and start emulation loop
           e.g.,

           while (true)
              cpu.execInstruction(); */
    }
}
