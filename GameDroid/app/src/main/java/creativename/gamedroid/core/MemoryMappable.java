package creativename.gamedroid.core;

public interface MemoryMappable {
    byte read(int address);
    void write(int address, byte value);
}
