package creativename.gamedroid.core;

/* CPU frequency divider */
public class Divider implements MemoryMappable {
    private char counter;

    // Tell the divider that some number of cycles have passed
    public void notifyCyclesPassed(int cycles) {
        counter += cycles;
    }

    @Override
    public byte read(char address) {
        return (byte) ((counter >> 8) & 0xFF);
    }

    @Override
    public void write(char address, byte value) {
        counter = 0;
    }
}
