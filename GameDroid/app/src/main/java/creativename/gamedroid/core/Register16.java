package creativename.gamedroid.core;

public class Register16 {
    protected char value;  // char used as 16 bit unsigned integer

    public char read() {
        return value;
    }

    public void write(char value) {
        this.value = value;
    }

    public void increment() {
        ++value;
    }

    public void decrement() {
        --value;
    }
}
