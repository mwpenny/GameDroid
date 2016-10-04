package creativename.gamedroid.core;

/**
 * The memory mapping unit.
 */

public class MMU {
    private byte workRam[];

    public MMU() {
        workRam = new byte[8196];
        reset();
    }

    protected char read8(char addr){
        switch (addr & 0xF000) {
            case 0xC000:
            case 0xD000:
            case 0xE000:
                return (char) workRam[addr & 0x1FFF];
        }
        System.err.println("Warning: invalid memory read at " + (int) addr);
        return 0;
    }

    protected void write8(char addr, char value){
        switch (addr & 0xF000) {
            case 0xC000:
            case 0xD000:
            case 0xE000:
                workRam[addr & 0x1FFF] = (byte) value;
        }
        System.err.println("Warning: invalid memory write at " + (int) addr);
    }

    protected void write16(char addr, char value) {
        write8(addr, (char)(value << 8));
        addr++;
        write8(addr, value);
    }

    protected char read16(char address) {
        address++;
        char ret = read8(address);
        address--;
        ret &= read8(address) << 8;
        return ret;
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

    private class MemoryCursor16 implements Cursor {
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

    private class MemoryCursor8 extends MemoryCursor16 implements Cursor {
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

