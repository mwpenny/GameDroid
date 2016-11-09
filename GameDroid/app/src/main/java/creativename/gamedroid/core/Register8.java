package creativename.gamedroid.core;

import java.io.Serializable;

/* 8-bit CPU register */
public class Register8 implements Register, Serializable {
    protected char value;

    @Override
    public char read() {
        return value;
    }

    @Override
    public void write(char value) {
        this.value = (char) (value & 255);
    }

    @Override
    public void increment() {
        value = (char) ((value + 1) & 0xFF);
    }

    @Override
    public void decrement() {
        value = (char) ((value - 1) & 0xFF);
    }
}
