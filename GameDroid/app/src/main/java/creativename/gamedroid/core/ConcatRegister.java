package creativename.gamedroid.core;

/**
 * Used to represent the concatenation of two 8 bit registers. No additional data is stored in the
 * class.
 */

public class ConcatRegister {
    Register8 high;
    Register8 low;

    public ConcatRegister(Register8 high, Register8 low) {
        this.high = high;
        this.low = low;
    }

    public void write(char value) {
        high.write((char) (value >> 8));
        low.write(value);
    }

    public char read() {
        return (char)((high.read() << 8) | low.read());
    }
}

