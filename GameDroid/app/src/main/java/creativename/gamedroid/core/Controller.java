package creativename.gamedroid.core;

/* Gamepad interface */
public class Controller implements MemoryMappable {
    public enum Button {
        RIGHT(1),
        LEFT(2),
        UP(4),
        DOWN(8),
        A(16),
        B(32),
        SELECT(64),
        START(128);

        private final byte buttonCode;
        Button(int code) {
            buttonCode = (byte)code;
        }

        public byte getButtonCode() {
            return buttonCode;
        }
    }

    /* state: SCBA DULR
              |||| ||||
              |||| |||+----- Right
              |||| ||+------ Left
              |||| |+------- Up
              |||| +-------- Down
              |||+---------- A
              ||+----------- B
              |+------------ Select
              +------------- Start */
    private byte state;
    private boolean readingDpad;

    public Controller() {
        state = (byte)0xFF;  // No buttons pressed
        readingDpad = true;
    }

    public void updateButton(Button button, boolean pressed) {
        // 0 = pressed
        byte code = button.getButtonCode();
        state = pressed ? (byte)(state & ~code) : (byte)(state | code);

        /* TODO: joypad interrupt
            - Only enabled when buttons/dpad are enabled by writing 0 to bits 4/5 of $FF00
            - Fires when button transitions from high -> low (i.e., has become pressed)
            - If 0 is written to both bits 4 and 5 of $FF00, doesn't fire if the newly pressed
              button/direction's corresponding button/direction is already pressed (e.g., right/a)
         */
    }

    public byte read(char address) {
        /* $FF00: xxxxBBBB
                      ||||
                      |||+----- Down/Start
                      ||+------ Up/Select
                      |+------- Left/B
                      +-------- Right/A

            It is unknown whether d-pad or buttons are selected when both
            bits 4 and 5 are unset. We will assume it's the d-pad */
        if (address == 0xFF00)
            return (byte)(readingDpad ? (state & 0xF) : (state >>> 4));
        throw new IllegalArgumentException(String.format("Invalid controller I/O read address ($%04X)", (int)address));
    }

    public void write(char address, byte value) {
        /* $FF00: xxDBxxxx
                    ||
                    |+----- Select d-pad (0 = select)
                    +------ Select buttons (0 = select) */
        if (address == 0xFF00)
            readingDpad = ((value & 0x10) == 0);
        else if (address > 0x7FFF)
            throw new IllegalArgumentException(String.format("Invalid controller I/O write address ($%04X)", (int)address));
    }
}
