package creativename.gamedroid.core;

public class Divider implements MemoryMappable {
    private int cycleReservoir;
    private char counter;

    public void notifyCyclesPassed(int cycles) {
        cycleReservoir += cycles;
        counter += cycleReservoir / 256;
        cycleReservoir %= 256;
        counter %= 0x100;
    }

    @Override
    public byte read(char address) {
        return (byte) counter;
    }

    @Override
    public void write(char address, byte value) {
        counter = 0;
    }
}
