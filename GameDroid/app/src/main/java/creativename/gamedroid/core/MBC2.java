package creativename.gamedroid.core;

/* MBC2:
     * Max 256KB ROM
     * 512x4b RAM
*/
public class MBC2 extends MBC {
    public MBC2(byte[] rom, int extRamSize) {
        super(rom, 512);
    }

    @Override
    public void write(char address, byte value) {
        /* MBC2 RAM only consists of 4-bit values (upper 4-bits in the
           bytes do not exist */
        if (address > 0x9FFF && address < 0xC000)
            value &= 0xF;
        super.write(address, value);
    }

    @Override
    protected void writeMBC(char address, byte value) {
        if (address < 0x2000) {
            /* Value enables/disables RAM (least significant bit of
               upper address byte must be 0 to enable/disable RAM) */
            if ((address & 0x100) == 0) {
                if (((value & 0x0A) == 0x0A))
                    ramEnabled = true;
                else if (value == 0)
                    ramEnabled = false;
            }
        } else if (address < 0x4000) {
            /* Value selects ROM bank (least significant bit of
               upper address byte must be 1 to select bank) */
            if ((address & 0x100) != 0)
                romBankNum = (value == 0 ? 1 : value & 0xF);
        }
    }
}
