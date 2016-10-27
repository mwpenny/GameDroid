package creativename.gamedroid.core;

/* CPU frequency divider */
public class Divider implements MemoryMappable {
    private int cycleReservoir;
    private char counter;

    // Tell the divider that some number of cycles have passed
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
        cycleReservoir = 0;
        counter = 0;
    }
}
