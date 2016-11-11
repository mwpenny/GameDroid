package creativename.gamedroid.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/* Memory bank controller base class
   Subclasses should implement cartridge ROM/RAM bank switching in writeMBC() */
public abstract class MBC implements MemoryMappable {
    private byte[] rom;
    public int romBankNum;      // Which ROM bank is mapped to $4000-$7FFF?
    private byte[] extRam;      // Not always allocated (depends on cartridge type)
    public int ramBankNum;      // Which RAM bank is mapped to $A000-$BFFF?
    public boolean ramEnabled;

    protected MBC(byte[] rom, int extRamSize) {
        this.rom = rom;
        this.extRam = new byte[extRamSize];
        romBankNum = 1;
        ramBankNum = 0;
        ramEnabled = true;
    }

    public void saveRamToFile(File f) throws IOException {
        // Save the cartridge RAM (i.e., the game's save file)
        FileOutputStream fos = new FileOutputStream(f);
        try {
            fos.write(extRam);
            fos.close();
        } catch (IOException e) {
            fos.close();
            throw e;
        }
    }

    public void loadRamFromFile(File f) throws IOException {
        // Load the cartridge RAM (i.e., the game's save file)
        FileInputStream fis = new FileInputStream(f);
        try {
            byte[] buf = new byte[extRam.length];
            if (fis.read(buf) == buf.length) {
                extRam = buf;
            } else {
                throw new IOException("Could read all bytes");
            }
            fis.close();
        } catch (IOException e) {
            fis.close();
            throw e;
        }
    }

    // Handles custom MBC logic (i.e., bank switching, enabling/disabling RAM, etc.)
    protected abstract void writeMBC(char address, byte value);

    private int getRamBankIndex(char addr) {
        if (extRam.length == 0)
            return -1;
        return (((addr - 0xA000) + (ramBankNum * 0x2000)) % extRam.length);
    }

    private boolean ramLocationAccessible(int idx) {
        return ramEnabled && extRam.length > 0 && idx < extRam.length;
    }

    // TODO: RTC registers
    @Override
    public final byte read(char address) {
        // ROM bank 0 is fixed, 1 is switchable
        if (address < 0x4000)
            return rom[address];
        else if (address < 0x8000)
            return rom[((address - 0x4000) + (romBankNum * 0x4000)) % rom.length];

        else if (address > 0x9FFF && address < 0xC000) {
            int i = getRamBankIndex(address);
            // $FF is returned if RAM is disabled or not present at the address specified
            if (i > -1 && ramLocationAccessible(i))
                return extRam[i];
            else
                return (byte)0xFF;
        }
        else
            throw new IllegalArgumentException(String.format("Invalid cartridge read address ($%04X)", (int)address));
    }

    @Override
    public final void write(char address, byte value) {
        if (address < 0x8000)
            writeMBC(address, value);
        else if (address > 0x9FFF && address < 0xC000) {
            int i = getRamBankIndex(address);
            if (i > -1 && ramLocationAccessible(i)) {
                // Write to cartridge RAM if present
                extRam[i] = value;
            }
        }
        else if (address > 0x7FFF)
            throw new IllegalArgumentException(String.format("Invalid cartridge write address ($%04X)", (int)address));
    }
}
