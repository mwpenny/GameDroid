package creativename.gamedroid.core;

import java.io.Serializable;

/* GameBoy CPU timer */
public class Timer implements MemoryMappable, Serializable {
    private char tima; // Timer counter
    private char tma;  // Timer Modulo
    private char tac;  // Timer control
    private int cycleReservoir;

    // Different timer frequencies
    private static final int[] reservoirCeilings = {1024, 16, 64, 256};

    /* Tell the timer that some number of cycles have passed. Returns whether the
       timer has overflowed and an interrupt should be raised */
    public boolean notifyCyclesPassed(int cycles) {
        if ((tac & 0b100) == 0) return false;  // timer stopped

        int reservoirCeiling = reservoirCeilings[tac & 3];
        cycleReservoir += cycles;
        tima += cycleReservoir / reservoirCeiling;
        cycleReservoir %= reservoirCeiling;

        if (tima > 0xFF) {
            tima = tma;
            return true;
        }
        return false;
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
        char val = (char) (value & 0xFF);
        if (address == 0xFF05) {
            tima = val;
        } else if (address == 0xFF06) {
            tma = val;
        } else if (address == 0xFF07) {
            // TODO: is this how the actual hardware behave?
            if ((tac & 0b11) != (val & 0b11))
                cycleReservoir = 0;

            tac = val;
        }
    }
}
