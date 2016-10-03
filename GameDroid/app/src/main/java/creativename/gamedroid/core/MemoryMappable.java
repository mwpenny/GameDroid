package creativename.gamedroid.core;

public interface MemoryMappable {
    byte read(char address);
    void write(char address, byte value);
}
