package creativename.gamedroid.core;

import java.util.HashMap;

public class CPU {
    public Register8 a, b, c, d, e, f, h, l;
    public Register16 sp, pc;  // char used as 16 bit unsigned integer
    public ConcatRegister af, bc, de, hl;
    public MMU mmu;

    // These are not actual cursors to read or right from, but special singleton values used as
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


        InstructionRoot ld8 = new LD8();
        // build the instruction lookup table
        oneByteInstructions = new HashMap<>();
        oneByteInstructions.put((char) 0x06, new InstructionForm(ld8, new Cursor[] {b, immediate8}));
        oneByteInstructions.put((char) 0x16, new InstructionForm(ld8, new Cursor[] {d, immediate8}));
        oneByteInstructions.put((char) 0x26, new InstructionForm(ld8, new Cursor[] {h, immediate8}));
        oneByteInstructions.put((char) 0x0E, new InstructionForm(ld8, new Cursor[] {c, immediate8}));
        oneByteInstructions.put((char) 0x1E, new InstructionForm(ld8, new Cursor[] {e, immediate8}));
        oneByteInstructions.put((char) 0x2E, new InstructionForm(ld8, new Cursor[] {l, immediate8}));
        oneByteInstructions.put((char) 0x3E, new InstructionForm(ld8, new Cursor[] {a, immediate8}));

        oneByteInstructions.put((char) 0x40, new InstructionForm(ld8, new Cursor[] {b, b}));
        oneByteInstructions.put((char) 0x41, new InstructionForm(ld8, new Cursor[] {b, c}));
        oneByteInstructions.put((char) 0x42, new InstructionForm(ld8, new Cursor[] {b, d}));
        oneByteInstructions.put((char) 0x43, new InstructionForm(ld8, new Cursor[] {b, e}));
        oneByteInstructions.put((char) 0x44, new InstructionForm(ld8, new Cursor[] {b, h}));
        oneByteInstructions.put((char) 0x45, new InstructionForm(ld8, new Cursor[] {b, l}));

        oneByteInstructions.put((char) 0x50, new InstructionForm(ld8, new Cursor[] {d, b}));
        oneByteInstructions.put((char) 0x51, new InstructionForm(ld8, new Cursor[] {d, c}));
        oneByteInstructions.put((char) 0x52, new InstructionForm(ld8, new Cursor[] {d, d}));
        oneByteInstructions.put((char) 0x53, new InstructionForm(ld8, new Cursor[] {d, e}));
        oneByteInstructions.put((char) 0x54, new InstructionForm(ld8, new Cursor[] {d, h}));
        oneByteInstructions.put((char) 0x55, new InstructionForm(ld8, new Cursor[] {d, l}));

        oneByteInstructions.put((char) 0x60, new InstructionForm(ld8, new Cursor[] {h, b}));
        oneByteInstructions.put((char) 0x61, new InstructionForm(ld8, new Cursor[] {h, c}));
        oneByteInstructions.put((char) 0x62, new InstructionForm(ld8, new Cursor[] {h, d}));
        oneByteInstructions.put((char) 0x63, new InstructionForm(ld8, new Cursor[] {h, e}));
        oneByteInstructions.put((char) 0x64, new InstructionForm(ld8, new Cursor[] {h, h}));
        oneByteInstructions.put((char) 0x65, new InstructionForm(ld8, new Cursor[] {h, l}));
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

    public static class LD8 implements InstructionRoot {
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