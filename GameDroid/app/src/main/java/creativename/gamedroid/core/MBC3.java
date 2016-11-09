package creativename.gamedroid.core;

/* MBC3:
     * Max 2MB ROM
     * Max 32KB RAM
*/
public class MBC3 extends MBC {
    public MBC3(byte[] rom, int extRamSize) {
        super(rom, extRamSize);
    }

    protected void writeMBC(char address, byte value) {
        if (address < 0x2000) {
            // Value enables/disables RAM and RTC registers
            // TODO: also enable/disable RTC
            if (((value & 0x0A) == 0x0A))
                ramEnabled = true;
            else if (value == 0)
                ramEnabled = false;
        } else if (address < 0x4000) {
            // 7-bit value sets ROM bank number (0 gets written as 1)
            romBankNum &= 0x80;
            romBankNum = (value == 0) ? 1 : (value & 0x7F);
        } else if (address < 0x6000) {
            /* Depending on mode, value either selects RAM bank or maps RTC
               registers into RAM */
            if (value < 4)
                ramBankNum = value & 3;
            else if (value > 7 && value < 0x0D) {
                // TODO: Map corresponding RTC register into memory
            }
        } else if (address < 0x8000) {
            /* TODO: When writing 00h, and then 01h to this register, the current
               time becomes latched into the RTC registers

               08h  RTC S   Seconds   0-59 (0-3Bh)
               09h  RTC M   Minutes   0-59 (0-3Bh)
               0Ah  RTC H   Hours     0-23 (0-17h)
               0Bh  RTC DL  Lower 8 bits of Day Counter (0-FFh)
               0Ch  RTC DH  Upper 1 bit of Day Counter, Carry Bit, Halt Flag
                     Bit 0  Most significant bit of Day Counter (Bit 8)
                     Bit 6  Halt (0=Active, 1=Stop Timer)
                     Bit 7  Day Counter Carry Bit (1=Counter Overflow) */
        }
    }
}
