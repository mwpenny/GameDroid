package creativename.gamedroid.core;

/* Memory bank controller base class
   Subclasses should implement cartridge ROM/RAM bank switching in writeMBC() */
public abstract class MBC implements MemoryMappable {
    private byte[] rom;
    protected int romBankNum;      // Which ROM bank is mapped to $4000-$7FFF?
    private byte[] extRam;         // Not always allocated (depends on cartridge type)
    protected int ramBankNum;      // Which RAM bank is mapped to $A000-$BFFF?
    protected boolean ramEnabled;
    private boolean hasBattery;

    protected MBC(byte[] rom, int extRamSize, boolean hasBattery) {
        this.rom = rom;
        this.extRam = new byte[extRamSize];
        romBankNum = 1;
        ramBankNum = 0;
        ramEnabled = true;
        this.hasBattery = hasBattery;
    }

    // Handles custom MBC logic (i.e., bank switching, enabling/disabling RAM, etc.)
    protected abstract void writeMBC(char address, byte value);

    // TODO: RTC registers
    @Override
    public byte read(char address) {
        // ROM bank 0 is fixed, 1 is switchable
        if (address < 0x4000)
            return rom[address];
        else if (address < 0x8000)
            return rom[(address - 0x4000) + (romBankNum * 0x4000)];

        else if (address > 0x9FFF && address < 0xC000) {
            // $FF is returned if RAM is disabled or not present at the address specified
            if (ramEnabled && extRam.length > 0 && (address - 0xA000) < extRam.length)
                return extRam[(address - 0xA000) + (ramBankNum * 0x2000)];
            else
                return (byte)0xFF;
        }
        else
            throw new IllegalArgumentException(String.format("Invalid ROM read address ($%04X)", (int)address));
    }

    @Override
    public void write(char address, byte value) {
        if (address < 0x8000)
            writeMBC(address, value);
        else if (ramEnabled && address > 0x9FFF && address < 0xC000 &&
                 extRam.length > 0 && (address - 0xA000) < extRam.length) {
            // Write to cartridge RAM if present
            extRam[(address - 0xA000) + (ramBankNum * 0x2000)] = value;
            if (hasBattery) {
                // TODO: save cartridge RAM to disk (game save file)
            }
        }
        else if (address > 0x7FFF)
            throw new IllegalArgumentException(String.format("Invalid ROM write address ($%04X)", (int)address));
    }
}
