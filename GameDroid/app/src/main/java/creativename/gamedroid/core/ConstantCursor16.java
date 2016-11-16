package creativename.gamedroid.core;

/* Cursor into constant 16-bit value */
public class ConstantCursor16 implements Cursor {
    public char value;

    ConstantCursor16(char value) {
        this.value = value;
    }

    @Override
    public char read() {
        return value;
    }

    @Override
    public void write(char value) {
        System.err.println("Error: write called on a constant value cursor");
    }
}

