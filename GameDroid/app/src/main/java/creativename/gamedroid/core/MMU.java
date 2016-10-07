package creativename.gamedroid.core;

/**
 * The memory mapping unit.
 */

public class MMU {
    private byte workRam[];
    private byte stack[];

    // TODO: $D000-$DFFF does not mirror $C000-$CFFF

    public MMU() {
        workRam = new byte[0x2000];
        stack = new byte[0x7F];
        reset();
    }

    public char read8(char addr){
        switch (addr & 0xF000) {
            case 0xC000:
            case 0xD000:
            case 0xE000:
                return (char) workRam[addr & 0x1FFF];
        }
        if (addr >= 0xFF80 && addr <= 0xFFFE)
            return (char) stack[addr - 0xFF80];

        System.err.println(String.format("Warning: invalid memory read at $%04X", (int)addr));
        return 0;
    }

    public void write8(char addr, char value){
        switch (addr & 0xF000) {
            case 0xC000:
            case 0xD000:
            case 0xE000:
                workRam[addr & 0x1FFF] = (byte) value;
                return;
        }
        if (addr >= 0xFF80 && addr <= 0xFFFE) {
            stack[addr - 0xFF80] = (byte) value;
            return;
        }
        System.err.println(String.format("Warning: invalid memory write at $%04X", (int)addr));
    }

    public void write16(char addr, char value) {
        // Write 2 bytes (little-endian)
        write8(addr++, value);
        write8(addr, (char)(value >>> 8));
    }

    public char read16(char address) {
        // Read 2 bytes (little-endian)
        return (char)(read8(address++) | (read8(address) << 8));
    }

    public MemoryCursor8 getCursor8(char address) {
        return new MemoryCursor8(address);
    }

    public MemoryCursor16 getCursor16(char address) {
        return new MemoryCursor16(address);
    }

    // these are effective output of the boot rom
    public void reset() {
        write8((char) 0xFF05, (char) 0x00);
        write8((char) 0xFF06, (char) 0x00);
        write8((char) 0xFF07, (char) 0x00);
        write8((char) 0xFF10, (char) 0x80);
        write8((char) 0xFF11, (char) 0xBF);
        write8((char) 0xFF12, (char) 0xF3);
        write8((char) 0xFF14, (char) 0xBF);
        write8((char) 0xFF16, (char) 0x3F);
        write8((char) 0xFF17, (char) 0x00);
        write8((char) 0xFF19, (char) 0xBF);
        write8((char) 0xFF1A, (char) 0x7F);
        write8((char) 0xFF1B, (char) 0xFF);
        write8((char) 0xFF1C, (char) 0x9F);
        write8((char) 0xFF1E, (char) 0xBF);
        write8((char) 0xFF20, (char) 0xFF);
        write8((char) 0xFF21, (char) 0x00);
        write8((char) 0xFF22, (char) 0x00);
        write8((char) 0xFF23, (char) 0xBF);
        write8((char) 0xFF24, (char) 0x77);
        write8((char) 0xFF25, (char) 0xF3);
        write8((char) 0xFF26, (char) 0xF1);
        write8((char) 0xFF40, (char) 0x91);
        write8((char) 0xFF42, (char) 0x00);
        write8((char) 0xFF43, (char) 0x00);
        write8((char) 0xFF45, (char) 0x00);
        write8((char) 0xFF47, (char) 0xFC);
        write8((char) 0xFF48, (char) 0xFF);
        write8((char) 0xFF49, (char) 0xFF);
        write8((char) 0xFF4A, (char) 0x00);
        write8((char) 0xFF4B, (char) 0x00);
        write8((char) 0xFFFF, (char) 0x00);
    }

    public class MemoryCursor16 implements Cursor {
        protected char address;

        public MemoryCursor16(char address) {
            this.address = address;
        }

        @Override
        public char read() {
            return read16(address);
        }

        @Override
        public void write(char value) {
            write16(address, value);
        }
    }

    public class MemoryCursor8 extends MemoryCursor16 implements Cursor {
        public MemoryCursor8(char address) {
            super(address);
        }

        @Override
        public char read() {
            return read8(address);
        }

        @Override
        public void write(char value) {
            write8(address, value);
        }
    }
}

