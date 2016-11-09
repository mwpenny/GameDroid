package creativename.gamedroid.core;

import java.io.Serializable;

/* 16-bit CPU register */
public class Register16 implements Register, Serializable {
    private char value;

    @Override
    public char read() {
        return value;
    }

    @Override
    public void write(char value) {
        this.value = value;
    }

    @Override
    public void increment() {
        ++value;
    }

    @Override
    public void decrement() {
        --value;
    }
}
