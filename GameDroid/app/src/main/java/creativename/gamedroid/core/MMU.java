package creativename.gamedroid.core;

import java.io.Serializable;

/* The memory mapping unit */
public class MMU implements Serializable {
    public transient GameBoy gb;
    final private MemoryBuffer workRam;
    final private MemoryBuffer stack;
    final private MappableByte raisedInterrupts;
    final private MappableByte enabledInterrupts;
    final static private InvalidRegion invalidMemory = new InvalidRegion();

    public MMU(GameBoy gb) {
        this.gb = gb;
        workRam = new MemoryBuffer(0x2000, 0, 0x1FFF);
        stack = new MemoryBuffer(0x7F, 0xFF80, ~0);
        raisedInterrupts = new MappableByte();
        enabledInterrupts = new MappableByte();
        reset();
    }

    private MemoryMappable dispatchAddress(char addr) {
        switch (addr & 0xF000) {
            case 0xC000:
            case 0xD000:
            case 0xE000:
                return workRam;
        }
        if (addr >= 0xFF80 && addr <= 0xFFFE)
            return stack;
        else if (addr == 0xFF0F)
            return raisedInterrupts;
        else if (addr == 0xFF04)
            return gb.divider;
        else if (addr >= 0xFF05 && addr <= 0xFF07)
            return gb.timer;
        else if ((addr >= 0x8000 && addr <= 0x9FFF) ||
                 (addr >= 0xFE00 & addr <= 0xFE9F) ||
                 (addr >= 0xFF40 && addr <= 0xFF4B))
            return gb.lcd;
        else if (addr == 0xFF00)
            return gb.gamepad;
        else if (addr == 0xFFFF)
            return enabledInterrupts;
        else if (addr < 0x8000 || (addr >= 0xA000 && addr <= 0xBFFF))
            return gb.cartridge.mbc;
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

    MemoryCursor8 hue = new MemoryCursor8('1');
    MemoryCursor16 hua = new MemoryCursor16('1');
    public MemoryCursor8 getCursor8(char address) {
        hue.address = address;
        return hue;
    }

    public MemoryCursor16 getCursor16(char address) {
        hua.address = address;
        return hua;
    }

    // these are effective output of the boot rom
    public void reset() {
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
        write8((char) 0xFFFF, (char) 0x00);
    }

    private class MemoryCursor16 implements Cursor {
        public char address;

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

    // null object for MemoryMappable
    private static class InvalidRegion implements MemoryMappable {
        byte lastTransferByte;

        @Override
        public byte read(char address) {
            //System.err.format("Warning: invalid memory read at $%04X\n", (int) address);
            return (byte) 0xFF;  // mimic actual hardware
        }

        @Override
        public void write(char address, byte value) {
            if (address == 0xFF01) {
                lastTransferByte = value;
                return;
            }
            if (address == 0xFF02 && value == -127) {
                System.out.print((char) (lastTransferByte % 255));
                return;
            }
            //System.err.format("Warning: invalid memory write at $%04X\n", (int) address);
        }
    }
}

