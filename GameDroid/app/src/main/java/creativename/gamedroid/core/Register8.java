package creativename.gamedroid.core;

public class Register8 extends Register16 {
    @Override
    public char read() {
        return (char)(value & 0xFF);
    }

    @Override
    public void increment() {
        value = (char)((value + 1) & 0xFF);
    }

    @Override
    public void decrement() {
        value = (char)((value - 1) & 0xFF);
    }
}
