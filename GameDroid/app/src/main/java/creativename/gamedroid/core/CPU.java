package creativename.gamedroid.core;

import java.util.HashMap;

public class CPU {
    public Register8 a, b, c, d, e, f, h, l;
    public Register16 sp, pc;
    public ConcatRegister af, bc, de, hl;
    public MMU mmu;

    private enum Flag {
        CARRY(0x10),
        HALF_CARRY(0x20),
        SUBTRACTION(0x40),
        ZERO(0x80);

        private final byte bitmask;
        Flag(int mask) {
            bitmask = (byte)mask;
        }

        public byte getBitmask() {
            return bitmask;
        }
    }

    // These are not actual cursors to read or write from, but special singleton values used as
    // signals for interpreting instruction operands.
    public static ConstantCursor8 immediate8 = new ConstantCursor8((char) 0);
    public static ConstantCursor16 immediate16 = new ConstantCursor16((char) 0);
    public static ConstantCursor8 indirect8 = new ConstantCursor8((char) 0);
    public static ConstantCursor16 indirect16 = new ConstantCursor16((char) 0);

    private HashMap<Character, InstructionForm> oneByteInstructions;
    private HashMap<Character, InstructionForm> twoByteInstructions;

    public CPU(MMU mmu) {
        this.mmu = mmu;

        a = new Register8();
        b = new Register8();
        c = new Register8();
        d = new Register8();
        e = new Register8();
        f = new Register8();
        h = new Register8();
        l = new Register8();
        af = new ConcatRegister(a, f);
        bc = new ConcatRegister(b, c);
        de = new ConcatRegister(d, e);
        hl = new ConcatRegister(h, l);
        sp = new Register16();
        pc = new Register16();
        reset();


        InstructionRoot ld = new LD();

        // build the instruction lookup table
        oneByteInstructions = new HashMap<>();
        oneByteInstructions.put((char) 0x06, new InstructionForm(ld, new Cursor[] {b, immediate8}));
        oneByteInstructions.put((char) 0x16, new InstructionForm(ld, new Cursor[] {d, immediate8}));
        oneByteInstructions.put((char) 0x26, new InstructionForm(ld, new Cursor[] {h, immediate8}));
        oneByteInstructions.put((char) 0x0E, new InstructionForm(ld, new Cursor[] {c, immediate8}));
        oneByteInstructions.put((char) 0x1E, new InstructionForm(ld, new Cursor[] {e, immediate8}));
        oneByteInstructions.put((char) 0x2E, new InstructionForm(ld, new Cursor[] {l, immediate8}));
        oneByteInstructions.put((char) 0x3E, new InstructionForm(ld, new Cursor[] {a, immediate8}));

        oneByteInstructions.put((char) 0x40, new InstructionForm(ld, new Cursor[] {b, b}));
        oneByteInstructions.put((char) 0x41, new InstructionForm(ld, new Cursor[] {b, c}));
        oneByteInstructions.put((char) 0x42, new InstructionForm(ld, new Cursor[] {b, d}));
        oneByteInstructions.put((char) 0x43, new InstructionForm(ld, new Cursor[] {b, e}));
        oneByteInstructions.put((char) 0x44, new InstructionForm(ld, new Cursor[] {b, h}));
        oneByteInstructions.put((char) 0x45, new InstructionForm(ld, new Cursor[] {b, l}));
        oneByteInstructions.put((char) 0x47, new InstructionForm(ld, new Cursor[] {b, a}));

        oneByteInstructions.put((char) 0x48, new InstructionForm(ld, new Cursor[] {c, b}));
        oneByteInstructions.put((char) 0x49, new InstructionForm(ld, new Cursor[] {c, c}));
        oneByteInstructions.put((char) 0x4A, new InstructionForm(ld, new Cursor[] {c, d}));
        oneByteInstructions.put((char) 0x4B, new InstructionForm(ld, new Cursor[] {c, e}));
        oneByteInstructions.put((char) 0x4C, new InstructionForm(ld, new Cursor[] {c, h}));
        oneByteInstructions.put((char) 0x4D, new InstructionForm(ld, new Cursor[] {c, l}));
        oneByteInstructions.put((char) 0x4F, new InstructionForm(ld, new Cursor[] {c, a}));

        oneByteInstructions.put((char) 0x50, new InstructionForm(ld, new Cursor[] {d, b}));
        oneByteInstructions.put((char) 0x51, new InstructionForm(ld, new Cursor[] {d, c}));
        oneByteInstructions.put((char) 0x52, new InstructionForm(ld, new Cursor[] {d, d}));
        oneByteInstructions.put((char) 0x53, new InstructionForm(ld, new Cursor[] {d, e}));
        oneByteInstructions.put((char) 0x54, new InstructionForm(ld, new Cursor[] {d, h}));
        oneByteInstructions.put((char) 0x55, new InstructionForm(ld, new Cursor[] {d, l}));
        oneByteInstructions.put((char) 0x57, new InstructionForm(ld, new Cursor[] {d, a}));

        oneByteInstructions.put((char) 0x58, new InstructionForm(ld, new Cursor[] {e, b}));
        oneByteInstructions.put((char) 0x59, new InstructionForm(ld, new Cursor[] {e, c}));
        oneByteInstructions.put((char) 0x5A, new InstructionForm(ld, new Cursor[] {e, d}));
        oneByteInstructions.put((char) 0x5B, new InstructionForm(ld, new Cursor[] {e, e}));
        oneByteInstructions.put((char) 0x5C, new InstructionForm(ld, new Cursor[] {e, h}));
        oneByteInstructions.put((char) 0x5D, new InstructionForm(ld, new Cursor[] {e, l}));
        oneByteInstructions.put((char) 0x5F, new InstructionForm(ld, new Cursor[] {e, a}));

        oneByteInstructions.put((char) 0x60, new InstructionForm(ld, new Cursor[] {h, b}));
        oneByteInstructions.put((char) 0x61, new InstructionForm(ld, new Cursor[] {h, c}));
        oneByteInstructions.put((char) 0x62, new InstructionForm(ld, new Cursor[] {h, d}));
        oneByteInstructions.put((char) 0x63, new InstructionForm(ld, new Cursor[] {h, e}));
        oneByteInstructions.put((char) 0x64, new InstructionForm(ld, new Cursor[] {h, h}));
        oneByteInstructions.put((char) 0x65, new InstructionForm(ld, new Cursor[] {h, l}));
        oneByteInstructions.put((char) 0x67, new InstructionForm(ld, new Cursor[] {h, a}));

        oneByteInstructions.put((char) 0x68, new InstructionForm(ld, new Cursor[] {l, b}));
        oneByteInstructions.put((char) 0x69, new InstructionForm(ld, new Cursor[] {l, c}));
        oneByteInstructions.put((char) 0x6A, new InstructionForm(ld, new Cursor[] {l, d}));
        oneByteInstructions.put((char) 0x6B, new InstructionForm(ld, new Cursor[] {l, e}));
        oneByteInstructions.put((char) 0x6C, new InstructionForm(ld, new Cursor[] {l, h}));
        oneByteInstructions.put((char) 0x6D, new InstructionForm(ld, new Cursor[] {l, l}));
        oneByteInstructions.put((char) 0x6F, new InstructionForm(ld, new Cursor[] {l, a}));

        oneByteInstructions.put((char) 0x78, new InstructionForm(ld, new Cursor[] {a, b}));
        oneByteInstructions.put((char) 0x79, new InstructionForm(ld, new Cursor[] {a, c}));
        oneByteInstructions.put((char) 0x7A, new InstructionForm(ld, new Cursor[] {a, d}));
        oneByteInstructions.put((char) 0x7B, new InstructionForm(ld, new Cursor[] {a, e}));
        oneByteInstructions.put((char) 0x7C, new InstructionForm(ld, new Cursor[] {a, h}));
        oneByteInstructions.put((char) 0x7D, new InstructionForm(ld, new Cursor[] {a, l}));
        oneByteInstructions.put((char) 0x7F, new InstructionForm(ld, new Cursor[] {a, a}));
    }

    // these are the effective output of the boot rom (an internal rom inside every Gameboy).
    public void reset() {
        af.write((char) 0x0EB0);
        bc.write((char) 0x0013);
        de.write((char) 0x00D8);
        hl.write((char) 0x014D);
        pc.write((char) 0x0100);
        sp.write((char) 0xFFFE);
    }

    public void execInstruction() {
        char optByte = mmu.read8(pc.read());
        InstructionForm ins = oneByteInstructions.get(optByte);
        ins.execute(this);
    }

    private void updateFlag(Flag flag, boolean set) {
        // Sets/unsets a CPU flag
        char val = this.f.read();
        if (set)
            val |= flag.getBitmask();
        else
            val &= ~flag.getBitmask();
        this.f.write(val);
    }

    /* Instructions */
    private static class LD implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[0].write(operands[1].read());
        }
    }
}

class ConstantCursor8 extends ConstantCursor16 {
    ConstantCursor8(char value) {
        super(value);
    }

    @Override
    public char read() {
        return (char) (super.read() & 255);
    }
}

class ConstantCursor16 implements Cursor {
    char value;

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