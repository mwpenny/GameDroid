package creativename.gamedroid.core;

/* CPU register */
public interface Register extends Cursor {
    char read();
    void write(char value);
    void increment();
    void decrement();
}