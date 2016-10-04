package creativename.gamedroid.core;

/**
 * Implementor of this interface are cursors into either some general purpose memory location or
 * register.
 */

public interface WriteCursor {
    void write(char value);
}
