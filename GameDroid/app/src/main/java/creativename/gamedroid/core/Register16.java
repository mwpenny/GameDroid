package creativename.gamedroid.core;

/**
 * Created by alan on 10/2/16.
 */

public class Register16 {
    protected char value;

//    public Register16(char value) {
//        this.value = value;
//    }

    public char read() {
        return value;
    }

    public void write(char value) {
        this.value = value;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }
}
