package creativename.gamedroid.core;

import java.util.HashMap;

/* Sharp LR35902 interpreter */
public class CPU {
    public Register a, b, c, d, e, h, l, af, bc, de, hl, sp, pc;
    public FlagRegister f;
    public MMU mmu;

    /* These are not actual cursors to read or write from, but special singleton values used as
       signals for interpreting instruction operands. */
    public static ConstantCursor8 immediate8 = new ConstantCursor8((char) 0);
    public static ConstantCursor16 immediate16 = new ConstantCursor16((char) 0);
    public static ConstantCursor8 oneByteIndirect8 = new ConstantCursor8((char) 0);
    public static ConstantCursor8 twoByteIndirect8 = new ConstantCursor8((char) 0);
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
        h = new Register8();
        l = new Register8();
        f = new FlagRegister();
        af = new ConcatRegister((Register8)a, f);
        bc = new ConcatRegister((Register8)b, (Register8)c);
        de = new ConcatRegister((Register8)d, (Register8)e);
        hl = new ConcatRegister((Register8)h, (Register8)l);
        sp = new Register16();
        pc = new Register16();
        reset();

        IndirectRegister16Cursor ibc = new IndirectRegister16Cursor(bc);
        IndirectRegister16Cursor ide = new IndirectRegister16Cursor(de);
        IndirectRegister16Cursor ihl = new IndirectRegister16Cursor(hl);
        IndirectRegister8Cursor ic = new IndirectRegister8Cursor(c);

        InstructionRoot inc8 = new INC8();
        InstructionRoot inc16 = new INC16();
        InstructionRoot dec8 = new DEC8();
        InstructionRoot dec16 = new DEC16();
        InstructionRoot and = new AND();
        InstructionRoot xor = new XOR();
        InstructionRoot or = new OR();
        InstructionRoot cp = new CP();
        InstructionRoot ld = new LD();
        InstructionRoot ldi = new LDI();
        InstructionRoot ldd = new LDD();
        InstructionRoot push = new PUSH();
        InstructionRoot pop = new POP();

        /* Build the one-byte instruction lookup table */
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
        oneByteInstructions.put((char) 0x34, new InstructionForm(inc8, new Cursor[] {ihl}));
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
        oneByteInstructions.put((char) 0x35, new InstructionForm(dec8, new Cursor[] {ihl}));
        oneByteInstructions.put((char) 0x3B, new InstructionForm(dec16, new Cursor[] {sp}));
        oneByteInstructions.put((char) 0x3D, new InstructionForm(dec8, new Cursor[] {a}));

        // AND
        oneByteInstructions.put((char) 0xA0, new InstructionForm(and, new Cursor[] {b}));
        oneByteInstructions.put((char) 0xA1, new InstructionForm(and, new Cursor[] {c}));
        oneByteInstructions.put((char) 0xA2, new InstructionForm(and, new Cursor[] {d}));
        oneByteInstructions.put((char) 0xA3, new InstructionForm(and, new Cursor[] {e}));
        oneByteInstructions.put((char) 0xA4, new InstructionForm(and, new Cursor[] {h}));
        oneByteInstructions.put((char) 0xA5, new InstructionForm(and, new Cursor[] {l}));
        oneByteInstructions.put((char) 0xA6, new InstructionForm(and, new Cursor[] {ihl}));
        oneByteInstructions.put((char) 0xA7, new InstructionForm(and, new Cursor[] {a}));
        oneByteInstructions.put((char) 0xE6, new InstructionForm(and, new Cursor[] {immediate8}));

        // XOR
        oneByteInstructions.put((char) 0xA8, new InstructionForm(xor, new Cursor[] {b}));
        oneByteInstructions.put((char) 0xA9, new InstructionForm(xor, new Cursor[] {c}));
        oneByteInstructions.put((char) 0xAA, new InstructionForm(xor, new Cursor[] {d}));
        oneByteInstructions.put((char) 0xAB, new InstructionForm(xor, new Cursor[] {e}));
        oneByteInstructions.put((char) 0xAC, new InstructionForm(xor, new Cursor[] {h}));
        oneByteInstructions.put((char) 0xAD, new InstructionForm(xor, new Cursor[] {l}));
        oneByteInstructions.put((char) 0xAE, new InstructionForm(xor, new Cursor[] {ihl}));
        oneByteInstructions.put((char) 0xAF, new InstructionForm(xor, new Cursor[] {a}));
        oneByteInstructions.put((char) 0xEE, new InstructionForm(xor, new Cursor[] {immediate8}));

        // OR
        oneByteInstructions.put((char) 0xB0, new InstructionForm(or, new Cursor[] {b}));
        oneByteInstructions.put((char) 0xB1, new InstructionForm(or, new Cursor[] {c}));
        oneByteInstructions.put((char) 0xB2, new InstructionForm(or, new Cursor[] {d}));
        oneByteInstructions.put((char) 0xB3, new InstructionForm(or, new Cursor[] {e}));
        oneByteInstructions.put((char) 0xB4, new InstructionForm(or, new Cursor[] {h}));
        oneByteInstructions.put((char) 0xB5, new InstructionForm(or, new Cursor[] {l}));
        oneByteInstructions.put((char) 0xB6, new InstructionForm(or, new Cursor[] {ihl}));
        oneByteInstructions.put((char) 0xB7, new InstructionForm(or, new Cursor[] {a}));
        oneByteInstructions.put((char) 0xF6, new InstructionForm(or, new Cursor[] {immediate8}));

        // CP
        oneByteInstructions.put((char) 0xB8, new InstructionForm(cp, new Cursor[] {b}));
        oneByteInstructions.put((char) 0xB9, new InstructionForm(cp, new Cursor[] {c}));
        oneByteInstructions.put((char) 0xBA, new InstructionForm(cp, new Cursor[] {d}));
        oneByteInstructions.put((char) 0xBB, new InstructionForm(cp, new Cursor[] {e}));
        oneByteInstructions.put((char) 0xBC, new InstructionForm(cp, new Cursor[] {h}));
        oneByteInstructions.put((char) 0xBD, new InstructionForm(cp, new Cursor[] {l}));
        oneByteInstructions.put((char) 0xBE, new InstructionForm(cp, new Cursor[] {ihl}));
        oneByteInstructions.put((char) 0xBF, new InstructionForm(cp, new Cursor[] {a}));
        oneByteInstructions.put((char) 0xFE, new InstructionForm(cp, new Cursor[] {immediate8}));

        // LD
        oneByteInstructions.put((char) 0x01, new InstructionForm(ld, new Cursor[] {bc, immediate16}));
        oneByteInstructions.put((char) 0x06, new InstructionForm(ld, new Cursor[] {b, immediate8}));
        oneByteInstructions.put((char) 0x11, new InstructionForm(ld, new Cursor[] {de, immediate16}));
        oneByteInstructions.put((char) 0x16, new InstructionForm(ld, new Cursor[] {d, immediate8}));
        oneByteInstructions.put((char) 0x21, new InstructionForm(ld, new Cursor[] {hl, immediate16}));
        oneByteInstructions.put((char) 0x26, new InstructionForm(ld, new Cursor[] {h, immediate8}));
        oneByteInstructions.put((char) 0x0E, new InstructionForm(ld, new Cursor[] {c, immediate8}));
        oneByteInstructions.put((char) 0x1E, new InstructionForm(ld, new Cursor[] {e, immediate8}));
        oneByteInstructions.put((char) 0x2E, new InstructionForm(ld, new Cursor[] {l, immediate8}));
        oneByteInstructions.put((char) 0x31, new InstructionForm(ld, new Cursor[] {sp, immediate16}));
        oneByteInstructions.put((char) 0x3E, new InstructionForm(ld, new Cursor[] {a, immediate8}));

        oneByteInstructions.put((char) 0x08, new InstructionForm(ld, new Cursor[] {indirect16, sp}));
        oneByteInstructions.put((char) 0x4E, new InstructionForm(ld, new Cursor[] {c, ihl}));
        oneByteInstructions.put((char) 0x5E, new InstructionForm(ld, new Cursor[] {e, ihl}));
        oneByteInstructions.put((char) 0x6E, new InstructionForm(ld, new Cursor[] {l, ihl}));
        oneByteInstructions.put((char) 0x7E, new InstructionForm(ld, new Cursor[] {a, ihl}));
        oneByteInstructions.put((char) 0xE0, new InstructionForm(ld, new Cursor[] {oneByteIndirect8, a}));
        oneByteInstructions.put((char) 0xE2, new InstructionForm(ld, new Cursor[] {ic, a}));
        oneByteInstructions.put((char) 0xEA, new InstructionForm(ld, new Cursor[] {twoByteIndirect8, a}));
        oneByteInstructions.put((char) 0xF0, new InstructionForm(ld, new Cursor[] {a, oneByteIndirect8}));
        oneByteInstructions.put((char) 0xF2, new InstructionForm(ld, new Cursor[] {a, ic}));
        oneByteInstructions.put((char) 0xFA, new InstructionForm(ld, new Cursor[] {a, twoByteIndirect8}));

        oneByteInstructions.put((char) 0x02, new InstructionForm(ld, new Cursor[] {ibc, a}));
        oneByteInstructions.put((char) 0x0A, new InstructionForm(ld, new Cursor[] {a, ibc}));
        oneByteInstructions.put((char) 0x12, new InstructionForm(ld, new Cursor[] {ide, a}));
        oneByteInstructions.put((char) 0x1A, new InstructionForm(ld, new Cursor[] {a, ide}));
        oneByteInstructions.put((char) 0x46, new InstructionForm(ld, new Cursor[] {b, ihl}));
        oneByteInstructions.put((char) 0x56, new InstructionForm(ld, new Cursor[] {d, ihl}));
        oneByteInstructions.put((char) 0x66, new InstructionForm(ld, new Cursor[] {h, ihl}));

        oneByteInstructions.put((char) 0x36, new InstructionForm(ld, new Cursor[] {ihl, immediate8}));
        oneByteInstructions.put((char) 0x70, new InstructionForm(ld, new Cursor[] {ihl, b}));
        oneByteInstructions.put((char) 0x71, new InstructionForm(ld, new Cursor[] {ihl, c}));
        oneByteInstructions.put((char) 0x72, new InstructionForm(ld, new Cursor[] {ihl, d}));
        oneByteInstructions.put((char) 0x73, new InstructionForm(ld, new Cursor[] {ihl, e}));
        oneByteInstructions.put((char) 0x74, new InstructionForm(ld, new Cursor[] {ihl, h}));
        oneByteInstructions.put((char) 0x75, new InstructionForm(ld, new Cursor[] {ihl, l}));
        oneByteInstructions.put((char) 0x77, new InstructionForm(ld, new Cursor[] {ihl, a}));

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
        oneByteInstructions.put((char) 0xF9, new InstructionForm(ld, new Cursor[] {sp, hl}));

        oneByteInstructions.put((char) 0x22, new InstructionForm(ldi, new Cursor[] {ihl, a}));
        oneByteInstructions.put((char) 0x2A, new InstructionForm(ldi, new Cursor[] {a, ihl}));
        oneByteInstructions.put((char) 0x32, new InstructionForm(ldd, new Cursor[] {ihl, a}));
        oneByteInstructions.put((char) 0x3A, new InstructionForm(ldd, new Cursor[] {a, ihl}));

        // PUSH/POP
        oneByteInstructions.put((char) 0xF1, new InstructionForm(pop, new Cursor[] {af}));
        oneByteInstructions.put((char) 0xF5, new InstructionForm(push, new Cursor[] {af}));
        oneByteInstructions.put((char) 0xC1, new InstructionForm(pop, new Cursor[] {bc}));
        oneByteInstructions.put((char) 0xC5, new InstructionForm(push, new Cursor[] {bc}));
        oneByteInstructions.put((char) 0xD1, new InstructionForm(pop, new Cursor[] {de}));
        oneByteInstructions.put((char) 0xD5, new InstructionForm(push, new Cursor[] {de}));
        oneByteInstructions.put((char) 0xE1, new InstructionForm(pop, new Cursor[] {hl}));
        oneByteInstructions.put((char) 0xE5, new InstructionForm(push, new Cursor[] {hl}));


        /* Build the two-byte instruction lookup table */
        twoByteInstructions = new HashMap<>();
    }

    private void pushStack(char value) {
        // SP points to the address where the next byte will be pushed
        sp.decrement();
        mmu.write16(sp.read(), value);
        sp.decrement();
    }

    private char popStack() {
        // SP points to the address where the next byte will be pushed
        sp.increment();
        char val = mmu.read16(sp.read());
        sp.increment();
        return val;
    }

    public void reset() {
        // These are the effective output of the boot rom (an internal rom inside every GameBoy)
        af.write((char) 0x0EB0);
        bc.write((char) 0x0013);
        de.write((char) 0x00D8);
        hl.write((char) 0x014D);
        pc.write((char) 0x0100);
        sp.write((char) 0xFFFE);
    }

    public void execInstruction() {
        char optByte = mmu.read8(pc.read());

        // $CB prefix -> instruction is two bytes
        if (optByte == 0xCB) {
            pc.increment();
            twoByteInstructions.get(mmu.read8(pc.read())).execute(this);
        } else {
            oneByteInstructions.get(optByte).execute(this);
        }
    }

    /* 8-bit CPU register */
    private static class Register8 implements Register {
        protected char value;

        public char read() {
            return value;
        }

        public void write(char value) {
            this.value = (char) (value & 255);
        }

        public void increment() {
            value = (char)((value + 1) & 0xFF);
        }

        public void decrement() {
            value = (char)((value - 1) & 0xFF);
        }
    }

    /* Register for CPU flags */
    public static class FlagRegister extends Register8 {
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

        public void updateFlag(Flag flag, boolean set) {
            if (set)
                value |= flag.getBitmask();
            else
                value &= ~flag.getBitmask();
        }

        public boolean isFlagSet(Flag flag) {
            return (value & flag.getBitmask()) != 0;
        }
    }

    /* 16-bit CPU register */
    private static class Register16 implements Register {
        private char value;

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

    /* Used to represent the concatenation of two 8 bit registers.
       No additional data is stored in the class */
    private static class ConcatRegister implements Register {
        Register8 high;
        Register8 low;

        public ConcatRegister(Register8 high, Register8 low) {
            this.high = high;
            this.low = low;
        }

        public void write(char value) {
            high.write((char) (value >>> 8));
            low.write(value);
        }

        public char read() {
            return (char)((high.read() << 8) | low.read());
        }

        public void increment() {
            write((char)(read()+1));
        }

        public void decrement() {
            write((char)(read()-1));
        }
    }

    private class IndirectRegister8Cursor implements Cursor {
        private Register reg;

        public IndirectRegister8Cursor(Register r) {
            reg = r;
        }

        public char read() {
            // Get value pointed to by address in register + $FF00
            return mmu.read8((char)(0xFF00 | reg.read()));
        }

        public void write(char value) {
            // Write to address contained in register
            mmu.write8((char)(0xFF00 | reg.read()), value);
        }
    }

    private class IndirectRegister16Cursor implements Cursor {
        private Register reg;

        public IndirectRegister16Cursor(Register r) {
            reg = r;
        }

        public char read() {
            // Get value pointed to by address in register
            return mmu.read8(reg.read());
        }

        public void write(char value) {
            // Write to address contained in register
            mmu.write8(reg.read(), value);
        }
    }

    /* Instructions */

    // LD - load data
    private static class LD implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[0].write(operands[1].read());
        }
    }
    private static class LDI implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[0].write(operands[1].read());
            cpu.hl.increment();
        }
    }
    private static class LDD implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[0].write(operands[1].read());
            cpu.hl.decrement();
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
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, true);
        }
    }

    // CCF - clear carry flag
    private static class CCF implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, false);
        }
    }

    // CP - compare A with n
    private static class CP implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = cpu.a.read(), y = operands[0].read();
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, (x - y) == 0);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, true);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, (y & 0xF) > (x & 0xF));
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, y > x);
        }
    }

    // INC - increment
    private static class INC8 implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = operands[0].read();
            operands[0].write((char)(x + 1));
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, x == 0xFF);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, (x & 0xF) + 1 > 0xF);
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
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, x == 1);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, true);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, (x & 0xF) - 1 < 0);
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
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, result == 0);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, true);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, false);
        }
    }

    // XOR - XOR n with A
    private static class XOR implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char result = (char)(cpu.a.read() ^ operands[0].read());
            cpu.a.write(result);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, result == 0);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, false);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, false);
        }
    }

    // OR - OR n with A
    private static class OR implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char result = (char)(cpu.a.read() | operands[0].read());
            cpu.a.write(result);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, result == 0);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, false);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, false);
        }
    }

    // PUSH - push register pair onto stack
    private static class PUSH implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.pushStack(operands[0].read());
        }
    }

    // POP - pop 2 bytes off stack into register pair
    private static class POP implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[0].write(cpu.popStack());
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