package creativename.gamedroid.core;

/* MBC1:
     * Max 2MB ROM
     * Max 32KB RAM
*/
public class MBC1 extends MBC {
    private boolean settingRamBank;

    public MBC1(byte[] rom, int extRamSize) {
        super(rom, extRamSize);
        settingRamBank = false;
    }

    protected void writeMBC(char address, byte value) {
        if (address < 0x2000) {
            // Value enables/disables RAM
            if (((value & 0x0A) == 0x0A))
                ramEnabled = true;
            else if (value == 0)
                ramEnabled = false;
        } else if (address < 0x4000) {
            // Value sets lower 5 bits of ROM bank number (0 gets written as 1)
            romBankNum = (romBankNum & 0xE0) | ((value == 0) ? 1 : (value & 0x1F));
        } else if (address < 0x6000) {
            /* Depending on mode, value either selects RAM bank or upper 2 bits
               of ROM bank */
            if (settingRamBank)
                ramBankNum = value & 3;
            else
                romBankNum = (romBankNum & 0x9F) | ((value & 3) << 5);

        } else if (address < 0x8000) {
            /* Value selects if writes to $4000-$5FFF select the RAM bank
               number or the upper 2 bits of the ROM bank number */
            if (value == 0) {
                // Mode 0 - enable ROM banking and force RAM bank 0
                settingRamBank = false;
                ramBankNum = 0;
            } else if (value == 1) {
                // Mode 1 - enable RAM banking and restrict ROM bank num to $01-1F
                settingRamBank = true;
                romBankNum &= 0x9F;
            }
        }
    }
}
