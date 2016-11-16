package creativename.gamedroid.core;

/* Cursor into constant 8-bit value */
public class ConstantCursor8 implements Cursor {
    public char value;

    ConstantCursor8(char value) {
        this.value = (char)(value & 255);
    }

    public char read() {
        return value;
    }

    public void write(char value) {
        System.err.println("Error: write called on a constant value cursor");
    }
}