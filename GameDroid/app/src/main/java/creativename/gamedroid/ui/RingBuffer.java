package creativename.gamedroid.ui;

import java.util.Arrays;

/* An array-backed circular buffer. An array is used to ensure data locality */
public class RingBuffer<T> {
    private int pos, count;
    private Object[] buffer;

    public RingBuffer(int size) {
        pos = count = 0;
        buffer = new Object[size];
    }

    public void clear() {
        Arrays.fill(buffer, null);
        count = 0;
    }

    public boolean isEmpty() {
        return (count == 0);
    }

    public void push(T elem) {
        // Insert new element, wrapping around if necessary
        pos = (pos + 1) % buffer.length;
        buffer[pos] = elem;
        count = Math.min(count + 1, buffer.length);
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        /* We know that push() only inserts elements of type T, so
           no type checking is required when popping */
        T elem = (T)buffer[pos];
        if (--pos < 0)
            pos = buffer.length-1;
        count = Math.max(count - 1, 0);
        return elem;
    }
}
