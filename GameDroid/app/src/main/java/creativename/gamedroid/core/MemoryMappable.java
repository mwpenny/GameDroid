package creativename.gamedroid.core;

/* Implementors of this interface can be interfaced with via memory reads/writes */
public interface MemoryMappable {
    byte read(char address);
    void write(char address, byte value);
}
