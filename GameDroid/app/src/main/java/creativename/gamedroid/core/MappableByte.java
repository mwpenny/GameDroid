package creativename.gamedroid.core;

/* A byte that can be mapped to a memory location. Useful for general-purpose registers as well
   as those with special functions (i.e., LCD OAM DMA)
 */
public class MappableByte implements MemoryMappable {
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