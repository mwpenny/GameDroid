package creativename.gamedroid.core;

import java.io.Serializable;

/* A byte that can be mapped to a memory location. Useful for general-purpose registers as well
   as those with special functions (i.e., LCD OAM DMA) */
public class MappableByte implements MemoryMappable, Serializable {
    public byte data;

    @Override
    public byte read(char address) {
        return data;
    }

    @Override
    public void write(char address, byte value) {
        data = value;
    }
}