package creativename.gamedroid.core;

/* MBC5:
     * Max 8MB ROM
     * Max 128KB RAM
*/
public class MBC5 extends MBC {
    public MBC5(byte[] rom, int extRamSize) {
        super(rom, extRamSize);
    }

    protected void writeMBC(char address, byte value) {
        if (address < 0x2000) {
            // Value enables/disables RAM
            if (((value & 0x0A) == 0x0A))
                ramEnabled = true;
            else if (value == 0)
                ramEnabled = false;
        } else if (address < 0x3000) {
            // Value sets lower 8 bits of ROM bank number
            romBankNum = (romBankNum & 0x100) | (value & 0xFF);
        } else if (address < 0x4000) {
            // Value sets 9th bit of ROM bank number
            romBankNum = (romBankNum & 0xFF) | ((value & 1) << 8);
        } else if (address < 0x6000) {
            // Value sets RAM bank number
            ramBankNum = value & 0x0F;
        }
    }
}
