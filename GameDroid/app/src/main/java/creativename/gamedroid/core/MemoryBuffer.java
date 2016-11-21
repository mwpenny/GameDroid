package creativename.gamedroid.core;

import java.io.Serializable;

/* A data buffer that can be mapped to a memory location */
public class MemoryBuffer implements MemoryMappable, Serializable {
    public byte data[];
    boolean enabled;
    int offset, mask;

    public MemoryBuffer(int size, int ofs, int mask) {
        data = new byte[size];
        enabled = true;
        offset = ofs;
        this.mask = mask;
    }

    @Override
    public byte read(char address) {
        if (enabled)
            return data[(address - offset) & mask];
        System.err.format("Warning: read from disabled memory buffer ($%04X)\n", (int) address);
        return (byte)0xFF;
    }

    @Override
    public void write(char address, byte value) {
        if (enabled)
            data[(address - offset) & mask] = value;
        else
            System.err.format("Warning: write to disabled memory buffer ($%04X)\n", (int) address);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
