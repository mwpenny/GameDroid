package creativename.gamedroid.core;

/* GameBoy CPU timer */
public class Timer implements MemoryMappable {
    private char tima; // Timer counter
    private char tma;  // Timer Modulo
    private char tac;  // Timer control
    private int cycleReservoir;

    private int currentReservoirCeiling() {
        int ceiling = 0;
        switch (tac & 0b11) {  // different timer frequencies
            case 0b00:
                ceiling = 1024;
                break;
            case 0b01:
                ceiling = 16;
                break;
            case 0b10:
                ceiling = 64;
                break;
            case 0b11:
                ceiling = 256;
                break;
        }
        return ceiling;
    }

    /* Tell the timer that some number of cycles have passed.
       returns whether an interrupt has happened */
    public boolean notifyCyclesPassed(int cycles) {
        if ((tac & 0b100) == 0) return false;  // timer stopped
        cycleReservoir += cycles;

        int reservoirCeiling = currentReservoirCeiling();
        tima += cycleReservoir / reservoirCeiling;
        cycleReservoir %= reservoirCeiling;
        final boolean overflowed = tima > 0xFF;
        tima %= 0x100;
        return overflowed;
    }

    // Returns the number of cycles until next interrupt, then resets the counting
    public int advanceUntilInterrupt() {
        int countUntilOverflow = 0x100 - tima;
        int reservoirCeiling = currentReservoirCeiling();
        int cycleNeeded = reservoirCeiling - cycleReservoir;  // complete the current increment in progress
        countUntilOverflow--;
        cycleNeeded += countUntilOverflow * reservoirCeiling;

        cycleReservoir = 0;
        tima = 0;
        return cycleNeeded;
    }

    @Override
    public byte read(char address) {
        if (address == 0xFF05) {
            return (byte) tima;
        } else if (address == 0xFF06) {
            return (byte) tma;
        } else if (address == 0xFF07) {
            return (byte) tac;
        } else {
            System.err.format("Warning: read dispatched to timer with invalid address $%04X\n", (int) address);
            return (byte)0xFF;
        }
    }

    @Override
    public void write(char address, byte value) {
        char val = (char) value;
        if (address == 0xFF05) {
            tima = val;
        } else if (address == 0xFF06) {
            tma = val;
        } else if (address == 0xFF07) {
            // TODO: is this how the actual hardware behave?
            if ((tac & 0b11) != (val & 0b11)) {
                cycleReservoir = 0;
            }
            tac = val;
        }
    }
}
