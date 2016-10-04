package creativename.gamedroid.core;

/**
 * Created by alan on 10/2/16.
 */

public class Register8 extends Register16 implements Cursor {
//    public Register8(char value) {
//        super(value);
//    }

    @Override
    public char read() {
        return (char) (value & 255);
    }
}
