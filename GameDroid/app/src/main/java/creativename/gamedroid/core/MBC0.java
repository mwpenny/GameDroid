package creativename.gamedroid.core;

/* MBC0:
     * 32KB ROM only (no bank switching)
     * Max 8KB of RAM
*/
public class MBC0 extends MBC {
    public MBC0(byte[] rom, int extRamSize) {
        super(rom, extRamSize);
    }

    protected void writeMBC(char address, byte value) {
        // MBC0 does not bank switch
    }
}
