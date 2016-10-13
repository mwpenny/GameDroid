package creativename.gamedroid.core;

/**
 * The memory mapping unit.
 */

public class MMU {
    final private MemoryMappable workRam;
    final private MemoryMappable stack;
    final private MemoryMappable raisedInterrupts;
    final private MemoryMappable enabledInterrupts;
    final static private InvalidRegion invalidMemory = new InvalidRegion();

    // TODO: $D000-$DFFF does not mirror $C000-$CFFF

    public MMU() {
        workRam = new Ram();
        stack = new Stack();
        raisedInterrupts = new ByteRegion();
        enabledInterrupts = new ByteRegion();
        reset();
    }

    private MemoryMappable dispatchAddress(char addr) {
        switch (addr & 0xF000) {
            case 0xC000:
            case 0xD000:
            case 0xE000:
                return workRam;
        }
        if (addr == 0xFF0F)
            return raisedInterrupts;
        else if (addr >= 0xFF80 && addr <= 0xFFFE)
            return stack;
        else if (addr == 0xFFFF)
            return enabledInterrupts;
        return invalidMemory;
    }

    public char read8(char addr) {
        return (char) (dispatchAddress(addr).read(addr) & 0xFF);
    }

    public void write8(char addr, char value) {
        dispatchAddress(addr).write(addr, (byte) value);
    }

    public void write16(char addr, char value) {
        // Write 2 bytes (little-endian)
        write8(addr++, value);
        write8(addr, (char) (value >>> 8));
    }

    public char read16(char address) {
        // Read 2 bytes (little-endian)
        return (char) (read8(address++) | (read8(address) << 8));
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

    private static class Ram implements MemoryMappable {
        byte data[];
        final int MASK = 0x1FFF;

        public Ram() {
            data = new byte[0x2000];
        }

        @Override
        public byte read(char address) {
            return data[address & MASK];
        }

        @Override
        public void write(char address, byte value) {
            data[address & MASK] = value;
        }
    }

    private static class Stack implements MemoryMappable {
        byte data[];
        final int OFFSET = 0xFF80;

        public Stack() {
            data = new byte[0x7F];
        }

        @Override
        public byte read(char address) {
            return data[address - OFFSET];
        }

        @Override
        public void write(char address, byte value) {
            data[address - OFFSET] = value;
        }
    }

    private static class ByteRegion implements MemoryMappable {
        byte data;

        @Override
        public byte read(char address) {
            return data;
        }

        @Override
        public void write(char address, byte value) {
            data = value;
        }
    }

    // null object for MemoryMappable
    private static class InvalidRegion implements MemoryMappable {
        @Override
        public byte read(char address) {
            System.err.format("Warning: invalid memory read at $%04X", (int) address);
            return 0;
        }

        @Override
        public void write(char address, byte value) {
            System.err.format("Warning: invalid memory write at $%04X\n", (int) address);
        }
    }
}

