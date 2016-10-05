package creativename.gamedroid.core;

import java.util.HashMap;

public class CPU {
    public Register8 a, b, c, d, e, f, h, l;
    public Register16 sp, pc;
    public ConcatRegister af, bc, de, hl;
    public MMU mmu;

    public enum Flag {
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

        InstructionRoot inc8 = new INC8();
        InstructionRoot inc16 = new INC16();
        InstructionRoot dec8 = new DEC8();
        InstructionRoot dec16 = new DEC16();
        InstructionRoot and = new AND();
        InstructionRoot xor = new XOR();
        InstructionRoot or = new OR();;
        InstructionRoot cp = new CP();
        InstructionRoot ld = new LD();

        // Build the instruction lookup table
        oneByteInstructions = new HashMap<>();

        // Misc
        oneByteInstructions.put((char) 0x00, new InstructionForm(new NOP(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x37, new InstructionForm(new SCF(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x3F, new InstructionForm(new CCF(), new Cursor[]{}));

        // INC
        oneByteInstructions.put((char) 0x03, new InstructionForm(inc16, new Cursor[] {bc}));
        oneByteInstructions.put((char) 0x04, new InstructionForm(inc8, new Cursor[] {b}));
        oneByteInstructions.put((char) 0x0C, new InstructionForm(inc8, new Cursor[] {c}));
        oneByteInstructions.put((char) 0x13, new InstructionForm(inc16, new Cursor[] {de}));
        oneByteInstructions.put((char) 0x14, new InstructionForm(inc8, new Cursor[] {d}));
        oneByteInstructions.put((char) 0x1C, new InstructionForm(inc8, new Cursor[] {e}));
        oneByteInstructions.put((char) 0x23, new InstructionForm(inc16, new Cursor[] {hl}));
        oneByteInstructions.put((char) 0x24, new InstructionForm(inc8, new Cursor[] {h}));
        oneByteInstructions.put((char) 0x2C, new InstructionForm(inc8, new Cursor[] {l}));
        oneByteInstructions.put((char) 0x33, new InstructionForm(inc16, new Cursor[] {sp}));
        oneByteInstructions.put((char) 0x3C, new InstructionForm(inc8, new Cursor[] {a}));

        // DEC
        oneByteInstructions.put((char) 0x05, new InstructionForm(dec8, new Cursor[] {b}));
        oneByteInstructions.put((char) 0x0B, new InstructionForm(dec16, new Cursor[] {bc}));
        oneByteInstructions.put((char) 0x0D, new InstructionForm(dec8, new Cursor[] {c}));
        oneByteInstructions.put((char) 0x15, new InstructionForm(dec8, new Cursor[] {d}));
        oneByteInstructions.put((char) 0x1B, new InstructionForm(dec16, new Cursor[] {de}));
        oneByteInstructions.put((char) 0x1D, new InstructionForm(dec8, new Cursor[] {e}));
        oneByteInstructions.put((char) 0x25, new InstructionForm(dec8, new Cursor[] {l}));
        oneByteInstructions.put((char) 0x2B, new InstructionForm(dec16, new Cursor[] {hl}));
        oneByteInstructions.put((char) 0x2D, new InstructionForm(dec8, new Cursor[] {h}));
        oneByteInstructions.put((char) 0x3B, new InstructionForm(dec16, new Cursor[] {sp}));
        oneByteInstructions.put((char) 0x3D, new InstructionForm(dec8, new Cursor[] {a}));

        // AND
        oneByteInstructions.put((char) 0xA0, new InstructionForm(and, new Cursor[] {b}));
        oneByteInstructions.put((char) 0xA1, new InstructionForm(and, new Cursor[] {c}));
        oneByteInstructions.put((char) 0xA2, new InstructionForm(and, new Cursor[] {d}));
        oneByteInstructions.put((char) 0xA3, new InstructionForm(and, new Cursor[] {e}));
        oneByteInstructions.put((char) 0xA4, new InstructionForm(and, new Cursor[] {h}));
        oneByteInstructions.put((char) 0xA5, new InstructionForm(and, new Cursor[] {l}));
        oneByteInstructions.put((char) 0xA7, new InstructionForm(and, new Cursor[] {a}));
        oneByteInstructions.put((char) 0xE6, new InstructionForm(and, new Cursor[] {immediate8}));

        // XOR
        oneByteInstructions.put((char) 0xA8, new InstructionForm(xor, new Cursor[] {b}));
        oneByteInstructions.put((char) 0xA9, new InstructionForm(xor, new Cursor[] {c}));
        oneByteInstructions.put((char) 0xAA, new InstructionForm(xor, new Cursor[] {d}));
        oneByteInstructions.put((char) 0xAB, new InstructionForm(xor, new Cursor[] {e}));
        oneByteInstructions.put((char) 0xAC, new InstructionForm(xor, new Cursor[] {h}));
        oneByteInstructions.put((char) 0xAD, new InstructionForm(xor, new Cursor[] {l}));
        oneByteInstructions.put((char) 0xAF, new InstructionForm(xor, new Cursor[] {a}));
        oneByteInstructions.put((char) 0xEE, new InstructionForm(xor, new Cursor[] {immediate8}));

        // OR
        oneByteInstructions.put((char) 0xB0, new InstructionForm(or, new Cursor[] {b}));
        oneByteInstructions.put((char) 0xB1, new InstructionForm(or, new Cursor[] {c}));
        oneByteInstructions.put((char) 0xB2, new InstructionForm(or, new Cursor[] {d}));
        oneByteInstructions.put((char) 0xB3, new InstructionForm(or, new Cursor[] {e}));
        oneByteInstructions.put((char) 0xB4, new InstructionForm(or, new Cursor[] {h}));
        oneByteInstructions.put((char) 0xB5, new InstructionForm(or, new Cursor[] {l}));
        oneByteInstructions.put((char) 0xB7, new InstructionForm(or, new Cursor[] {a}));
        oneByteInstructions.put((char) 0xF6, new InstructionForm(or, new Cursor[] {immediate8}));

        // CP
        oneByteInstructions.put((char) 0xB8, new InstructionForm(cp, new Cursor[] {b}));
        oneByteInstructions.put((char) 0xB9, new InstructionForm(cp, new Cursor[] {c}));
        oneByteInstructions.put((char) 0xBA, new InstructionForm(cp, new Cursor[] {d}));
        oneByteInstructions.put((char) 0xBB, new InstructionForm(cp, new Cursor[] {e}));
        oneByteInstructions.put((char) 0xBC, new InstructionForm(cp, new Cursor[] {h}));
        oneByteInstructions.put((char) 0xBD, new InstructionForm(cp, new Cursor[] {l}));
        oneByteInstructions.put((char) 0xBF, new InstructionForm(cp, new Cursor[] {a}));
        oneByteInstructions.put((char) 0xFE, new InstructionForm(cp, new Cursor[] {immediate8}));

        // LD
        oneByteInstructions.put((char) 0x01, new InstructionForm(ld, new Cursor[] {bc, immediate16}));
        oneByteInstructions.put((char) 0x06, new InstructionForm(ld, new Cursor[] {b, immediate8}));
        oneByteInstructions.put((char) 0x11, new InstructionForm(ld, new Cursor[] {de, immediate16}));
        oneByteInstructions.put((char) 0x16, new InstructionForm(ld, new Cursor[] {d, immediate8}));
        oneByteInstructions.put((char) 0x11, new InstructionForm(ld, new Cursor[] {hl, immediate16}));
        oneByteInstructions.put((char) 0x26, new InstructionForm(ld, new Cursor[] {h, immediate8}));
        oneByteInstructions.put((char) 0x0E, new InstructionForm(ld, new Cursor[] {c, immediate8}));
        oneByteInstructions.put((char) 0x1E, new InstructionForm(ld, new Cursor[] {e, immediate8}));
        oneByteInstructions.put((char) 0x2E, new InstructionForm(ld, new Cursor[] {l, immediate8}));
        oneByteInstructions.put((char) 0x11, new InstructionForm(ld, new Cursor[] {sp, immediate16}));
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

    public void updateFlag(Flag flag, boolean set) {
        // Sets/unsets a CPU flag
        char val = this.f.read();
        if (set)
            val |= flag.getBitmask();
        else
            val &= ~flag.getBitmask();
        this.f.write(val);
    }

    public boolean isFlagSet(Flag flag) {
        return (f.read() & flag.getBitmask()) != 0;
    }

    /* Instructions */

    // LD - load data
    private static class LD implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[0].write(operands[1].read());
        }
    }

    // NOP - no operation
    private static class NOP implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
        }
    }

    // SCF - set carry flag
    private static class SCF implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.updateFlag(Flag.CARRY, true);
        }
    }

    // CCF - clear carry flag
    private static class CCF implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.updateFlag(Flag.CARRY, false);
        }
    }

    // CP - compare A with n
    private static class CP implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = cpu.a.read(), y = operands[0].read();
            cpu.updateFlag(Flag.ZERO, (x - y) == 0);
            cpu.updateFlag(Flag.SUBTRACTION, true);
            cpu.updateFlag(Flag.HALF_CARRY, (y & 0xF) > (x & 0xF));
            cpu.updateFlag(Flag.CARRY, y > x);
        }
    }

    // INC - increment
    private static class INC8 implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = operands[0].read();
            operands[0].write((char)(x + 1));
            cpu.updateFlag(Flag.ZERO, x == 0xFF);
            cpu.updateFlag(Flag.SUBTRACTION, false);
            cpu.updateFlag(Flag.HALF_CARRY, (x & 0xF) + 1 > 0xF);
        }
    }
    private static class INC16 implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[0].write((char)(operands[0].read() + 1));
        }
    }

    // DEC - decrement
    private static class DEC8 implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = operands[0].read();
            operands[0].write((char)(x - 1));
            cpu.updateFlag(Flag.ZERO, x == 1);
            cpu.updateFlag(Flag.SUBTRACTION, true);
            cpu.updateFlag(Flag.HALF_CARRY, (x & 0xF) - 1 < 0);
        }
    }
    private static class DEC16 implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = operands[0].read();
            operands[0].write((char)(x - 1));
        }
    }

    // AND - AND n with A
    private static class AND implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char result = (char)(cpu.a.read() & operands[0].read());
            cpu.a.write(result);
            cpu.updateFlag(Flag.ZERO, result == 0);
            cpu.updateFlag(Flag.SUBTRACTION, false);
            cpu.updateFlag(Flag.HALF_CARRY, true);
            cpu.updateFlag(Flag.CARRY, false);
        }
    }

    // XOR - XOR n with A
    private static class XOR implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char result = (char)(cpu.a.read() ^ operands[0].read());
            cpu.a.write(result);
            cpu.updateFlag(Flag.ZERO, result == 0);
            cpu.updateFlag(Flag.SUBTRACTION, false);
            cpu.updateFlag(Flag.HALF_CARRY, false);
            cpu.updateFlag(Flag.CARRY, false);
        }
    }

    // OR - OR n with A
    private static class OR implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char result = (char)(cpu.a.read() | operands[0].read());
            cpu.a.write(result);
            cpu.updateFlag(Flag.ZERO, result == 0);
            cpu.updateFlag(Flag.SUBTRACTION, false);
            cpu.updateFlag(Flag.HALF_CARRY, false);
            cpu.updateFlag(Flag.CARRY, false);
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