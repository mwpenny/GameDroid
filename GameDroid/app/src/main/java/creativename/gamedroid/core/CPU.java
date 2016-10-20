package creativename.gamedroid.core;

import java.util.HashMap;

/* Sharp LR35902 interpreter */
public class CPU {
    public Register a, b, c, d, e, h, l, af, bc, de, hl, sp, pc;
    public FlagRegister f;
    public final GameBoy gb;
    private boolean interruptsEnabled;
    private boolean halted;
    public boolean haltBugTriggered;

    /* These are not actual cursors to read or write from, but special singleton values used as
       signals for interpreting instruction operands. */
    public static ConstantCursor8 immediate8 = new ConstantCursor8((char) 0);
    public static ConstantCursor16 immediate16 = new ConstantCursor16((char) 0);
    public static ConstantCursor8 oneByteIndirect8 = new ConstantCursor8((char) 0);
    public static ConstantCursor8 twoByteIndirect8 = new ConstantCursor8((char) 0);
    public static ConstantCursor16 indirect16 = new ConstantCursor16((char) 0);

    private HashMap<Character, InstructionForm> oneByteInstructions;
    private HashMap<Character, InstructionForm> twoByteInstructions;

    public enum Interrupt {
        VBLANK(0x01),
        LCD(0x02),
        TIMER(0x04),
        SERIAL(0x08),
        JOYPAD(0x10);

        private final byte bitmask;

        Interrupt(int mask) {
            bitmask = (byte) mask;
        }

        public byte getBitmask() {
            return bitmask;
        }
    }

    public CPU(GameBoy gb) {
        this.gb = gb;

        a = new Register8();
        b = new Register8();
        c = new Register8();
        d = new Register8();
        e = new Register8();
        h = new Register8();
        l = new Register8();
        f = new FlagRegister();
        af = new ConcatRegister((Register8) a, f);
        bc = new ConcatRegister((Register8) b, (Register8) c);
        de = new ConcatRegister((Register8) d, (Register8) e);
        hl = new ConcatRegister((Register8) h, (Register8) l);
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
        InstructionRoot add = new ADD();
        InstructionRoot adc = new ADC();
        //InstructionRoot sub = new SUB();
        //InstructionRoot sbc = new SBC();
        InstructionRoot and = new AND();
        InstructionRoot xor = new XOR();
        InstructionRoot or = new OR();
        InstructionRoot cp = new CP();
        InstructionRoot ld = new LD();
        InstructionRoot ldi = new LDI();
        InstructionRoot ldd = new LDD();
        InstructionRoot push = new PUSH();
        InstructionRoot pop = new POP();
        InstructionRoot swap = new SWAP();
        InstructionRoot bit = new BIT();
        InstructionRoot res = new RES();
        InstructionRoot set = new SET();
        InstructionRoot jp = new JP();
        InstructionRoot jr = new JR();
        InstructionRoot rst = new RST();
        InstructionRoot rl = new RL();
        InstructionRoot rlc = new RLC();
        InstructionRoot rr = new RR();
        InstructionRoot rrc = new RRC();

        /* Build the one-byte instruction lookup table */
        oneByteInstructions = new HashMap<>();

        // Misc
        oneByteInstructions.put((char) 0x00, new InstructionForm(new NOP(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x37, new InstructionForm(new SCF(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x3F, new InstructionForm(new CCF(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x2F, new InstructionForm(new CPL(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x76, new InstructionForm(new HALT(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x10, new InstructionForm(new STOP(), new Cursor[]{immediate8}));
        oneByteInstructions.put((char) 0xF3, new InstructionForm(new DI(), new Cursor[]{}));
        oneByteInstructions.put((char) 0xFB, new InstructionForm(new EI(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x27, new InstructionForm(new DAA(), new Cursor[]{}));
        oneByteInstructions.put((char) 0x07, new InstructionForm(rlc, new Cursor[]{a}));
        oneByteInstructions.put((char) 0x17, new InstructionForm(rl, new Cursor[]{a}));
        oneByteInstructions.put((char) 0x0F, new InstructionForm(rrc, new Cursor[]{a}));
        oneByteInstructions.put((char) 0x1F, new InstructionForm(rr, new Cursor[]{a}));

        // INC
        oneByteInstructions.put((char) 0x03, new InstructionForm(inc16, new Cursor[]{bc}));
        oneByteInstructions.put((char) 0x04, new InstructionForm(inc8, new Cursor[]{b}));
        oneByteInstructions.put((char) 0x0C, new InstructionForm(inc8, new Cursor[]{c}));
        oneByteInstructions.put((char) 0x13, new InstructionForm(inc16, new Cursor[]{de}));
        oneByteInstructions.put((char) 0x14, new InstructionForm(inc8, new Cursor[]{d}));
        oneByteInstructions.put((char) 0x1C, new InstructionForm(inc8, new Cursor[]{e}));
        oneByteInstructions.put((char) 0x23, new InstructionForm(inc16, new Cursor[]{hl}));
        oneByteInstructions.put((char) 0x24, new InstructionForm(inc8, new Cursor[]{h}));
        oneByteInstructions.put((char) 0x2C, new InstructionForm(inc8, new Cursor[]{l}));
        oneByteInstructions.put((char) 0x33, new InstructionForm(inc16, new Cursor[]{sp}));
        oneByteInstructions.put((char) 0x34, new InstructionForm(inc8, new Cursor[]{ihl}));
        oneByteInstructions.put((char) 0x3C, new InstructionForm(inc8, new Cursor[]{a}));

        // DEC
        oneByteInstructions.put((char) 0x05, new InstructionForm(dec8, new Cursor[]{b}));
        oneByteInstructions.put((char) 0x0B, new InstructionForm(dec16, new Cursor[]{bc}));
        oneByteInstructions.put((char) 0x0D, new InstructionForm(dec8, new Cursor[]{c}));
        oneByteInstructions.put((char) 0x15, new InstructionForm(dec8, new Cursor[]{d}));
        oneByteInstructions.put((char) 0x1B, new InstructionForm(dec16, new Cursor[]{de}));
        oneByteInstructions.put((char) 0x1D, new InstructionForm(dec8, new Cursor[]{e}));
        oneByteInstructions.put((char) 0x25, new InstructionForm(dec8, new Cursor[]{l}));
        oneByteInstructions.put((char) 0x2B, new InstructionForm(dec16, new Cursor[]{hl}));
        oneByteInstructions.put((char) 0x2D, new InstructionForm(dec8, new Cursor[]{h}));
        oneByteInstructions.put((char) 0x35, new InstructionForm(dec8, new Cursor[]{ihl}));
        oneByteInstructions.put((char) 0x3B, new InstructionForm(dec16, new Cursor[]{sp}));
        oneByteInstructions.put((char) 0x3D, new InstructionForm(dec8, new Cursor[]{a}));

        // AND
        oneByteInstructions.put((char) 0xA0, new InstructionForm(and, new Cursor[]{b}));
        oneByteInstructions.put((char) 0xA1, new InstructionForm(and, new Cursor[]{c}));
        oneByteInstructions.put((char) 0xA2, new InstructionForm(and, new Cursor[]{d}));
        oneByteInstructions.put((char) 0xA3, new InstructionForm(and, new Cursor[]{e}));
        oneByteInstructions.put((char) 0xA4, new InstructionForm(and, new Cursor[]{h}));
        oneByteInstructions.put((char) 0xA5, new InstructionForm(and, new Cursor[]{l}));
        oneByteInstructions.put((char) 0xA6, new InstructionForm(and, new Cursor[]{ihl}));
        oneByteInstructions.put((char) 0xA7, new InstructionForm(and, new Cursor[]{a}));
        oneByteInstructions.put((char) 0xE6, new InstructionForm(and, new Cursor[]{immediate8}));

        // XOR
        oneByteInstructions.put((char) 0xA8, new InstructionForm(xor, new Cursor[]{b}));
        oneByteInstructions.put((char) 0xA9, new InstructionForm(xor, new Cursor[]{c}));
        oneByteInstructions.put((char) 0xAA, new InstructionForm(xor, new Cursor[]{d}));
        oneByteInstructions.put((char) 0xAB, new InstructionForm(xor, new Cursor[]{e}));
        oneByteInstructions.put((char) 0xAC, new InstructionForm(xor, new Cursor[]{h}));
        oneByteInstructions.put((char) 0xAD, new InstructionForm(xor, new Cursor[]{l}));
        oneByteInstructions.put((char) 0xAE, new InstructionForm(xor, new Cursor[]{ihl}));
        oneByteInstructions.put((char) 0xAF, new InstructionForm(xor, new Cursor[]{a}));
        oneByteInstructions.put((char) 0xEE, new InstructionForm(xor, new Cursor[]{immediate8}));

        // OR
        oneByteInstructions.put((char) 0xB0, new InstructionForm(or, new Cursor[]{b}));
        oneByteInstructions.put((char) 0xB1, new InstructionForm(or, new Cursor[]{c}));
        oneByteInstructions.put((char) 0xB2, new InstructionForm(or, new Cursor[]{d}));
        oneByteInstructions.put((char) 0xB3, new InstructionForm(or, new Cursor[]{e}));
        oneByteInstructions.put((char) 0xB4, new InstructionForm(or, new Cursor[]{h}));
        oneByteInstructions.put((char) 0xB5, new InstructionForm(or, new Cursor[]{l}));
        oneByteInstructions.put((char) 0xB6, new InstructionForm(or, new Cursor[]{ihl}));
        oneByteInstructions.put((char) 0xB7, new InstructionForm(or, new Cursor[]{a}));
        oneByteInstructions.put((char) 0xF6, new InstructionForm(or, new Cursor[]{immediate8}));

        //ADD
        oneByteInstructions.put((char) 0x87, new InstructionForm(add, new Cursor[]{a, a}));
        oneByteInstructions.put((char) 0x80, new InstructionForm(add, new Cursor[]{a, b}));
        oneByteInstructions.put((char) 0x81, new InstructionForm(add, new Cursor[]{a, c}));
        oneByteInstructions.put((char) 0x82, new InstructionForm(add, new Cursor[]{a, d}));
        oneByteInstructions.put((char) 0x83, new InstructionForm(add, new Cursor[]{a, e}));
        oneByteInstructions.put((char) 0x84, new InstructionForm(add, new Cursor[]{a, h}));
        oneByteInstructions.put((char) 0x85, new InstructionForm(add, new Cursor[]{a, l}));
        oneByteInstructions.put((char) 0x86, new InstructionForm(add, new Cursor[]{a, hl}));
        oneByteInstructions.put((char) 0xC6, new InstructionForm(add, new Cursor[]{a, immediate8}));

        //ADC
        oneByteInstructions.put((char) 0x8F, new InstructionForm(adc, new Cursor[]{a, a}));
        oneByteInstructions.put((char) 0x88, new InstructionForm(adc, new Cursor[]{a, b}));
        oneByteInstructions.put((char) 0x89, new InstructionForm(adc, new Cursor[]{a, c}));
        oneByteInstructions.put((char) 0x8A, new InstructionForm(adc, new Cursor[]{a, d}));
        oneByteInstructions.put((char) 0x8B, new InstructionForm(adc, new Cursor[]{a, e}));
        oneByteInstructions.put((char) 0x8C, new InstructionForm(adc, new Cursor[]{a, h}));
        oneByteInstructions.put((char) 0x8D, new InstructionForm(adc, new Cursor[]{a, l}));
        oneByteInstructions.put((char) 0x8E, new InstructionForm(adc, new Cursor[]{a, hl}));
        oneByteInstructions.put((char) 0xCE, new InstructionForm(adc, new Cursor[]{a, immediate8}));

        //SUB
/*        oneByteInstructions.put((char) 0x97, new InstructionForm(sub, new Cursor[]{a}));
        oneByteInstructions.put((char) 0x90, new InstructionForm(sub, new Cursor[]{b}));
        oneByteInstructions.put((char) 0x91, new InstructionForm(sub, new Cursor[]{c}));
        oneByteInstructions.put((char) 0x92, new InstructionForm(sub, new Cursor[]{d}));
        oneByteInstructions.put((char) 0x93, new InstructionForm(sub, new Cursor[]{e}));
        oneByteInstructions.put((char) 0x94, new InstructionForm(sub, new Cursor[]{h}));
        oneByteInstructions.put((char) 0x95, new InstructionForm(sub, new Cursor[]{l}));
        oneByteInstructions.put((char) 0x96, new InstructionForm(sub, new Cursor[]{hl}));
        oneByteInstructions.put((char) 0xD6, new InstructionForm(sub, new Cursor[]{immediate8}));

        //SBC
        oneByteInstructions.put((char) 0x9F, new InstructionForm(sbc, new Cursor[]{a, a}));
        oneByteInstructions.put((char) 0x98, new InstructionForm(sbc, new Cursor[]{a, b}));
        oneByteInstructions.put((char) 0x99, new InstructionForm(sbc, new Cursor[]{a, c}));
        oneByteInstructions.put((char) 0x9A, new InstructionForm(sbc, new Cursor[]{a, d}));
        oneByteInstructions.put((char) 0x9B, new InstructionForm(sbc, new Cursor[]{a, e}));
        oneByteInstructions.put((char) 0x9C, new InstructionForm(sbc, new Cursor[]{a, h}));
        oneByteInstructions.put((char) 0x9D, new InstructionForm(sbc, new Cursor[]{a, l}));
        oneByteInstructions.put((char) 0x9E, new InstructionForm(sbc, new Cursor[]{a, hl}));*/
        //Says ??
        //oneByteInstructions.put((char) 0x9F, new InstructionForm(sbc, new Cursor[]{a, a}));

        // CP
        oneByteInstructions.put((char) 0xB8, new InstructionForm(cp, new Cursor[]{b}));
        oneByteInstructions.put((char) 0xB9, new InstructionForm(cp, new Cursor[]{c}));
        oneByteInstructions.put((char) 0xBA, new InstructionForm(cp, new Cursor[]{d}));
        oneByteInstructions.put((char) 0xBB, new InstructionForm(cp, new Cursor[]{e}));
        oneByteInstructions.put((char) 0xBC, new InstructionForm(cp, new Cursor[]{h}));
        oneByteInstructions.put((char) 0xBD, new InstructionForm(cp, new Cursor[]{l}));
        oneByteInstructions.put((char) 0xBE, new InstructionForm(cp, new Cursor[]{ihl}));
        oneByteInstructions.put((char) 0xBF, new InstructionForm(cp, new Cursor[]{a}));
        oneByteInstructions.put((char) 0xFE, new InstructionForm(cp, new Cursor[]{immediate8}));

        // LD
        oneByteInstructions.put((char) 0x01, new InstructionForm(ld, new Cursor[]{bc, immediate16}));
        oneByteInstructions.put((char) 0x06, new InstructionForm(ld, new Cursor[]{b, immediate8}));
        oneByteInstructions.put((char) 0x11, new InstructionForm(ld, new Cursor[]{de, immediate16}));
        oneByteInstructions.put((char) 0x16, new InstructionForm(ld, new Cursor[]{d, immediate8}));
        oneByteInstructions.put((char) 0x21, new InstructionForm(ld, new Cursor[]{hl, immediate16}));
        oneByteInstructions.put((char) 0x26, new InstructionForm(ld, new Cursor[]{h, immediate8}));
        oneByteInstructions.put((char) 0x0E, new InstructionForm(ld, new Cursor[]{c, immediate8}));
        oneByteInstructions.put((char) 0x1E, new InstructionForm(ld, new Cursor[]{e, immediate8}));
        oneByteInstructions.put((char) 0x2E, new InstructionForm(ld, new Cursor[]{l, immediate8}));
        oneByteInstructions.put((char) 0x31, new InstructionForm(ld, new Cursor[]{sp, immediate16}));
        oneByteInstructions.put((char) 0x3E, new InstructionForm(ld, new Cursor[]{a, immediate8}));

        oneByteInstructions.put((char) 0x08, new InstructionForm(ld, new Cursor[]{indirect16, sp}));
        oneByteInstructions.put((char) 0x4E, new InstructionForm(ld, new Cursor[]{c, ihl}));
        oneByteInstructions.put((char) 0x5E, new InstructionForm(ld, new Cursor[]{e, ihl}));
        oneByteInstructions.put((char) 0x6E, new InstructionForm(ld, new Cursor[]{l, ihl}));
        oneByteInstructions.put((char) 0x7E, new InstructionForm(ld, new Cursor[]{a, ihl}));
        oneByteInstructions.put((char) 0xE0, new InstructionForm(ld, new Cursor[]{oneByteIndirect8, a}));
        oneByteInstructions.put((char) 0xE2, new InstructionForm(ld, new Cursor[]{ic, a}));
        oneByteInstructions.put((char) 0xEA, new InstructionForm(ld, new Cursor[]{twoByteIndirect8, a}));
        oneByteInstructions.put((char) 0xF0, new InstructionForm(ld, new Cursor[]{a, oneByteIndirect8}));
        oneByteInstructions.put((char) 0xF2, new InstructionForm(ld, new Cursor[]{a, ic}));
        oneByteInstructions.put((char) 0xF8, new InstructionForm(new LDHL(), new Cursor[]{immediate8}));
        oneByteInstructions.put((char) 0xFA, new InstructionForm(ld, new Cursor[]{a, twoByteIndirect8}));

        oneByteInstructions.put((char) 0x02, new InstructionForm(ld, new Cursor[]{ibc, a}));
        oneByteInstructions.put((char) 0x0A, new InstructionForm(ld, new Cursor[]{a, ibc}));
        oneByteInstructions.put((char) 0x12, new InstructionForm(ld, new Cursor[]{ide, a}));
        oneByteInstructions.put((char) 0x1A, new InstructionForm(ld, new Cursor[]{a, ide}));
        oneByteInstructions.put((char) 0x46, new InstructionForm(ld, new Cursor[]{b, ihl}));
        oneByteInstructions.put((char) 0x56, new InstructionForm(ld, new Cursor[]{d, ihl}));
        oneByteInstructions.put((char) 0x66, new InstructionForm(ld, new Cursor[]{h, ihl}));

        oneByteInstructions.put((char) 0x36, new InstructionForm(ld, new Cursor[]{ihl, immediate8}));
        oneByteInstructions.put((char) 0x70, new InstructionForm(ld, new Cursor[]{ihl, b}));
        oneByteInstructions.put((char) 0x71, new InstructionForm(ld, new Cursor[]{ihl, c}));
        oneByteInstructions.put((char) 0x72, new InstructionForm(ld, new Cursor[]{ihl, d}));
        oneByteInstructions.put((char) 0x73, new InstructionForm(ld, new Cursor[]{ihl, e}));
        oneByteInstructions.put((char) 0x74, new InstructionForm(ld, new Cursor[]{ihl, h}));
        oneByteInstructions.put((char) 0x75, new InstructionForm(ld, new Cursor[]{ihl, l}));
        oneByteInstructions.put((char) 0x77, new InstructionForm(ld, new Cursor[]{ihl, a}));

        oneByteInstructions.put((char) 0x40, new InstructionForm(ld, new Cursor[]{b, b}));
        oneByteInstructions.put((char) 0x41, new InstructionForm(ld, new Cursor[]{b, c}));
        oneByteInstructions.put((char) 0x42, new InstructionForm(ld, new Cursor[]{b, d}));
        oneByteInstructions.put((char) 0x43, new InstructionForm(ld, new Cursor[]{b, e}));
        oneByteInstructions.put((char) 0x44, new InstructionForm(ld, new Cursor[]{b, h}));
        oneByteInstructions.put((char) 0x45, new InstructionForm(ld, new Cursor[]{b, l}));
        oneByteInstructions.put((char) 0x47, new InstructionForm(ld, new Cursor[]{b, a}));

        oneByteInstructions.put((char) 0x48, new InstructionForm(ld, new Cursor[]{c, b}));
        oneByteInstructions.put((char) 0x49, new InstructionForm(ld, new Cursor[]{c, c}));
        oneByteInstructions.put((char) 0x4A, new InstructionForm(ld, new Cursor[]{c, d}));
        oneByteInstructions.put((char) 0x4B, new InstructionForm(ld, new Cursor[]{c, e}));
        oneByteInstructions.put((char) 0x4C, new InstructionForm(ld, new Cursor[]{c, h}));
        oneByteInstructions.put((char) 0x4D, new InstructionForm(ld, new Cursor[]{c, l}));
        oneByteInstructions.put((char) 0x4F, new InstructionForm(ld, new Cursor[]{c, a}));

        oneByteInstructions.put((char) 0x50, new InstructionForm(ld, new Cursor[]{d, b}));
        oneByteInstructions.put((char) 0x51, new InstructionForm(ld, new Cursor[]{d, c}));
        oneByteInstructions.put((char) 0x52, new InstructionForm(ld, new Cursor[]{d, d}));
        oneByteInstructions.put((char) 0x53, new InstructionForm(ld, new Cursor[]{d, e}));
        oneByteInstructions.put((char) 0x54, new InstructionForm(ld, new Cursor[]{d, h}));
        oneByteInstructions.put((char) 0x55, new InstructionForm(ld, new Cursor[]{d, l}));
        oneByteInstructions.put((char) 0x57, new InstructionForm(ld, new Cursor[]{d, a}));

        oneByteInstructions.put((char) 0x58, new InstructionForm(ld, new Cursor[]{e, b}));
        oneByteInstructions.put((char) 0x59, new InstructionForm(ld, new Cursor[]{e, c}));
        oneByteInstructions.put((char) 0x5A, new InstructionForm(ld, new Cursor[]{e, d}));
        oneByteInstructions.put((char) 0x5B, new InstructionForm(ld, new Cursor[]{e, e}));
        oneByteInstructions.put((char) 0x5C, new InstructionForm(ld, new Cursor[]{e, h}));
        oneByteInstructions.put((char) 0x5D, new InstructionForm(ld, new Cursor[]{e, l}));
        oneByteInstructions.put((char) 0x5F, new InstructionForm(ld, new Cursor[]{e, a}));

        oneByteInstructions.put((char) 0x60, new InstructionForm(ld, new Cursor[]{h, b}));
        oneByteInstructions.put((char) 0x61, new InstructionForm(ld, new Cursor[]{h, c}));
        oneByteInstructions.put((char) 0x62, new InstructionForm(ld, new Cursor[]{h, d}));
        oneByteInstructions.put((char) 0x63, new InstructionForm(ld, new Cursor[]{h, e}));
        oneByteInstructions.put((char) 0x64, new InstructionForm(ld, new Cursor[]{h, h}));
        oneByteInstructions.put((char) 0x65, new InstructionForm(ld, new Cursor[]{h, l}));
        oneByteInstructions.put((char) 0x67, new InstructionForm(ld, new Cursor[]{h, a}));

        oneByteInstructions.put((char) 0x68, new InstructionForm(ld, new Cursor[]{l, b}));
        oneByteInstructions.put((char) 0x69, new InstructionForm(ld, new Cursor[]{l, c}));
        oneByteInstructions.put((char) 0x6A, new InstructionForm(ld, new Cursor[]{l, d}));
        oneByteInstructions.put((char) 0x6B, new InstructionForm(ld, new Cursor[]{l, e}));
        oneByteInstructions.put((char) 0x6C, new InstructionForm(ld, new Cursor[]{l, h}));
        oneByteInstructions.put((char) 0x6D, new InstructionForm(ld, new Cursor[]{l, l}));
        oneByteInstructions.put((char) 0x6F, new InstructionForm(ld, new Cursor[]{l, a}));

        oneByteInstructions.put((char) 0x78, new InstructionForm(ld, new Cursor[]{a, b}));
        oneByteInstructions.put((char) 0x79, new InstructionForm(ld, new Cursor[]{a, c}));
        oneByteInstructions.put((char) 0x7A, new InstructionForm(ld, new Cursor[]{a, d}));
        oneByteInstructions.put((char) 0x7B, new InstructionForm(ld, new Cursor[]{a, e}));
        oneByteInstructions.put((char) 0x7C, new InstructionForm(ld, new Cursor[]{a, h}));
        oneByteInstructions.put((char) 0x7D, new InstructionForm(ld, new Cursor[]{a, l}));
        oneByteInstructions.put((char) 0x7F, new InstructionForm(ld, new Cursor[]{a, a}));
        oneByteInstructions.put((char) 0xF9, new InstructionForm(ld, new Cursor[]{sp, hl}));

        oneByteInstructions.put((char) 0x22, new InstructionForm(ldi, new Cursor[]{ihl, a}));
        oneByteInstructions.put((char) 0x2A, new InstructionForm(ldi, new Cursor[]{a, ihl}));
        oneByteInstructions.put((char) 0x32, new InstructionForm(ldd, new Cursor[]{ihl, a}));
        oneByteInstructions.put((char) 0x3A, new InstructionForm(ldd, new Cursor[]{a, ihl}));

        // Jumps
        oneByteInstructions.put((char) 0xC3, new InstructionForm(jp, new Cursor[]{immediate16}));
        oneByteInstructions.put((char) 0xE9, new InstructionForm(jp, new Cursor[]{hl}));
        oneByteInstructions.put((char) 0xC2, new InstructionForm(new JPNZ(), new Cursor[]{immediate16}));
        oneByteInstructions.put((char) 0xCA, new InstructionForm(new JPZ(), new Cursor[]{immediate16}));
        oneByteInstructions.put((char) 0xD2, new InstructionForm(new JPNC(), new Cursor[]{immediate16}));
        oneByteInstructions.put((char) 0xDA, new InstructionForm(new JPC(), new Cursor[]{immediate16}));

        oneByteInstructions.put((char) 0x18, new InstructionForm(jr, new Cursor[]{immediate8}));
        oneByteInstructions.put((char) 0x20, new InstructionForm(new JRNZ(), new Cursor[]{immediate8}));
        oneByteInstructions.put((char) 0x28, new InstructionForm(new JRZ(), new Cursor[]{immediate8}));
        oneByteInstructions.put((char) 0x30, new InstructionForm(new JRNC(), new Cursor[]{immediate8}));
        oneByteInstructions.put((char) 0x38, new InstructionForm(new JRC(), new Cursor[]{immediate8}));

        // Calls
        oneByteInstructions.put((char) 0xCD, new InstructionForm(new CALL(), new Cursor[]{immediate16}));
        oneByteInstructions.put((char) 0xC4, new InstructionForm(new CALLNZ(), new Cursor[]{immediate16}));
        oneByteInstructions.put((char) 0xCC, new InstructionForm(new CALLZ(), new Cursor[]{immediate16}));
        oneByteInstructions.put((char) 0xD4, new InstructionForm(new CALLNC(), new Cursor[]{immediate16}));
        oneByteInstructions.put((char) 0xDC, new InstructionForm(new CALLC(), new Cursor[]{immediate16}));

        // RST
        oneByteInstructions.put((char) 0xC7, new InstructionForm(rst, new Cursor[]{new ConstantCursor8((char) 0x00)}));
        oneByteInstructions.put((char) 0xCF, new InstructionForm(rst, new Cursor[]{new ConstantCursor8((char) 0x08)}));
        oneByteInstructions.put((char) 0xD7, new InstructionForm(rst, new Cursor[]{new ConstantCursor8((char) 0x10)}));
        oneByteInstructions.put((char) 0xDF, new InstructionForm(rst, new Cursor[]{new ConstantCursor8((char) 0x18)}));
        oneByteInstructions.put((char) 0xE7, new InstructionForm(rst, new Cursor[]{new ConstantCursor8((char) 0x20)}));
        oneByteInstructions.put((char) 0xEF, new InstructionForm(rst, new Cursor[]{new ConstantCursor8((char) 0x28)}));
        oneByteInstructions.put((char) 0xF7, new InstructionForm(rst, new Cursor[]{new ConstantCursor8((char) 0x30)}));
        oneByteInstructions.put((char) 0xFF, new InstructionForm(rst, new Cursor[]{new ConstantCursor8((char) 0x38)}));

        // Returns
        oneByteInstructions.put((char) 0xC9, new InstructionForm(new RET(), new Cursor[]{}));
        oneByteInstructions.put((char) 0xC0, new InstructionForm(new RETNZ(), new Cursor[]{}));
        oneByteInstructions.put((char) 0xC8, new InstructionForm(new RETZ(), new Cursor[]{}));
        oneByteInstructions.put((char) 0xD0, new InstructionForm(new RETNC(), new Cursor[]{}));
        oneByteInstructions.put((char) 0xD8, new InstructionForm(new RETC(), new Cursor[]{}));
        oneByteInstructions.put((char) 0xD9, new InstructionForm(new RETI(), new Cursor[]{}));

        // PUSH/POP
        oneByteInstructions.put((char) 0xF1, new InstructionForm(pop, new Cursor[]{af}));
        oneByteInstructions.put((char) 0xF5, new InstructionForm(push, new Cursor[]{af}));
        oneByteInstructions.put((char) 0xC1, new InstructionForm(pop, new Cursor[]{bc}));
        oneByteInstructions.put((char) 0xC5, new InstructionForm(push, new Cursor[]{bc}));
        oneByteInstructions.put((char) 0xD1, new InstructionForm(pop, new Cursor[]{de}));
        oneByteInstructions.put((char) 0xD5, new InstructionForm(push, new Cursor[]{de}));
        oneByteInstructions.put((char) 0xE1, new InstructionForm(pop, new Cursor[]{hl}));
        oneByteInstructions.put((char) 0xE5, new InstructionForm(push, new Cursor[]{hl}));

        /* Build the two-byte instruction lookup table */
        twoByteInstructions = new HashMap<>();

        // RLC
        twoByteInstructions.put((char) 0x00, new InstructionForm(rlc, new Cursor[]{b}));
        twoByteInstructions.put((char) 0x01, new InstructionForm(rlc, new Cursor[]{c}));
        twoByteInstructions.put((char) 0x02, new InstructionForm(rlc, new Cursor[]{d}));
        twoByteInstructions.put((char) 0x03, new InstructionForm(rlc, new Cursor[]{e}));
        twoByteInstructions.put((char) 0x04, new InstructionForm(rlc, new Cursor[]{h}));
        twoByteInstructions.put((char) 0x05, new InstructionForm(rlc, new Cursor[]{l}));
        twoByteInstructions.put((char) 0x06, new InstructionForm(rlc, new Cursor[]{ihl}));
        twoByteInstructions.put((char) 0x07, new InstructionForm(rlc, new Cursor[]{a}));
        // RL
        twoByteInstructions.put((char) 0x10, new InstructionForm(rl, new Cursor[]{b}));
        twoByteInstructions.put((char) 0x11, new InstructionForm(rl, new Cursor[]{c}));
        twoByteInstructions.put((char) 0x12, new InstructionForm(rl, new Cursor[]{d}));
        twoByteInstructions.put((char) 0x13, new InstructionForm(rl, new Cursor[]{e}));
        twoByteInstructions.put((char) 0x14, new InstructionForm(rl, new Cursor[]{h}));
        twoByteInstructions.put((char) 0x15, new InstructionForm(rl, new Cursor[]{l}));
        twoByteInstructions.put((char) 0x16, new InstructionForm(rl, new Cursor[]{ihl}));
        twoByteInstructions.put((char) 0x17, new InstructionForm(rl, new Cursor[]{a}));
        // RRC
        twoByteInstructions.put((char) 0x08, new InstructionForm(rrc, new Cursor[]{b}));
        twoByteInstructions.put((char) 0x09, new InstructionForm(rrc, new Cursor[]{c}));
        twoByteInstructions.put((char) 0x0A, new InstructionForm(rrc, new Cursor[]{d}));
        twoByteInstructions.put((char) 0x0B, new InstructionForm(rrc, new Cursor[]{e}));
        twoByteInstructions.put((char) 0x0C, new InstructionForm(rrc, new Cursor[]{h}));
        twoByteInstructions.put((char) 0x0D, new InstructionForm(rrc, new Cursor[]{l}));
        twoByteInstructions.put((char) 0x0E, new InstructionForm(rrc, new Cursor[]{ihl}));
        twoByteInstructions.put((char) 0x0F, new InstructionForm(rrc, new Cursor[]{a}));
        // RR
        twoByteInstructions.put((char) 0x18, new InstructionForm(rr, new Cursor[]{b}));
        twoByteInstructions.put((char) 0x19, new InstructionForm(rr, new Cursor[]{c}));
        twoByteInstructions.put((char) 0x1A, new InstructionForm(rr, new Cursor[]{d}));
        twoByteInstructions.put((char) 0x1B, new InstructionForm(rr, new Cursor[]{e}));
        twoByteInstructions.put((char) 0x1C, new InstructionForm(rr, new Cursor[]{h}));
        twoByteInstructions.put((char) 0x1D, new InstructionForm(rr, new Cursor[]{l}));
        twoByteInstructions.put((char) 0x1E, new InstructionForm(rr, new Cursor[]{ihl}));
        twoByteInstructions.put((char) 0x1F, new InstructionForm(rr, new Cursor[]{a}));
        // SLA
        InstructionRoot sla = new SLA();
        twoByteInstructions.put((char) 0x20, new InstructionForm(sla, new Cursor[]{b}));
        twoByteInstructions.put((char) 0x21, new InstructionForm(sla, new Cursor[]{c}));
        twoByteInstructions.put((char) 0x22, new InstructionForm(sla, new Cursor[]{d}));
        twoByteInstructions.put((char) 0x23, new InstructionForm(sla, new Cursor[]{e}));
        twoByteInstructions.put((char) 0x24, new InstructionForm(sla, new Cursor[]{h}));
        twoByteInstructions.put((char) 0x25, new InstructionForm(sla, new Cursor[]{l}));
        twoByteInstructions.put((char) 0x26, new InstructionForm(sla, new Cursor[]{ihl}));
        twoByteInstructions.put((char) 0x27, new InstructionForm(sla, new Cursor[]{a}));
        // SRA
        InstructionRoot sra = new SRA();
        twoByteInstructions.put((char) 0x28, new InstructionForm(sra, new Cursor[]{b}));
        twoByteInstructions.put((char) 0x29, new InstructionForm(sra, new Cursor[]{c}));
        twoByteInstructions.put((char) 0x2A, new InstructionForm(sra, new Cursor[]{d}));
        twoByteInstructions.put((char) 0x2B, new InstructionForm(sra, new Cursor[]{e}));
        twoByteInstructions.put((char) 0x2C, new InstructionForm(sra, new Cursor[]{h}));
        twoByteInstructions.put((char) 0x2D, new InstructionForm(sra, new Cursor[]{l}));
        twoByteInstructions.put((char) 0x2E, new InstructionForm(sra, new Cursor[]{ihl}));
        twoByteInstructions.put((char) 0x2F, new InstructionForm(sra, new Cursor[]{a}));
        // SLA
        InstructionRoot srl = new SRL();
        twoByteInstructions.put((char) 0x38, new InstructionForm(srl, new Cursor[]{b}));
        twoByteInstructions.put((char) 0x39, new InstructionForm(srl, new Cursor[]{c}));
        twoByteInstructions.put((char) 0x3A, new InstructionForm(srl, new Cursor[]{d}));
        twoByteInstructions.put((char) 0x3B, new InstructionForm(srl, new Cursor[]{e}));
        twoByteInstructions.put((char) 0x3C, new InstructionForm(srl, new Cursor[]{h}));
        twoByteInstructions.put((char) 0x3D, new InstructionForm(srl, new Cursor[]{l}));
        twoByteInstructions.put((char) 0x3E, new InstructionForm(srl, new Cursor[]{ihl}));
        twoByteInstructions.put((char) 0x3F, new InstructionForm(srl, new Cursor[]{a}));
        // SWAP
        twoByteInstructions.put((char) 0x30, new InstructionForm(swap, new Cursor[]{b}));
        twoByteInstructions.put((char) 0x31, new InstructionForm(swap, new Cursor[]{c}));
        twoByteInstructions.put((char) 0x32, new InstructionForm(swap, new Cursor[]{d}));
        twoByteInstructions.put((char) 0x33, new InstructionForm(swap, new Cursor[]{e}));
        twoByteInstructions.put((char) 0x34, new InstructionForm(swap, new Cursor[]{h}));
        twoByteInstructions.put((char) 0x35, new InstructionForm(swap, new Cursor[]{l}));
        twoByteInstructions.put((char) 0x36, new InstructionForm(swap, new Cursor[]{ihl}));
        twoByteInstructions.put((char) 0x37, new InstructionForm(swap, new Cursor[]{a}));

        // BIT
        Cursor[] bitCursors = new Cursor[8];

        for (int i = 0; i < 8; ++i) {
            char opcode = (char) (0x40 + 8 * i);
            bitCursors[i] = new ConstantCursor8((char) i);
            twoByteInstructions.put(opcode, new InstructionForm(bit, new Cursor[]{bitCursors[i], b}));
            twoByteInstructions.put((char) (opcode + 1), new InstructionForm(bit, new Cursor[]{bitCursors[i], c}));
            twoByteInstructions.put((char) (opcode + 2), new InstructionForm(bit, new Cursor[]{bitCursors[i], d}));
            twoByteInstructions.put((char) (opcode + 3), new InstructionForm(bit, new Cursor[]{bitCursors[i], e}));
            twoByteInstructions.put((char) (opcode + 4), new InstructionForm(bit, new Cursor[]{bitCursors[i], h}));
            twoByteInstructions.put((char) (opcode + 5), new InstructionForm(bit, new Cursor[]{bitCursors[i], l}));
            twoByteInstructions.put((char) (opcode + 6), new InstructionForm(bit, new Cursor[]{bitCursors[i], ihl}));
            twoByteInstructions.put((char) (opcode + 7), new InstructionForm(bit, new Cursor[]{bitCursors[i], a}));
        }

        // RES
        for (char i = 0; i < 8; ++i) {
            char opcode = (char) (0x80 + 8 * i);
            twoByteInstructions.put(opcode, new InstructionForm(res, new Cursor[]{bitCursors[i], b}));
            twoByteInstructions.put((char) (opcode + 1), new InstructionForm(res, new Cursor[]{bitCursors[i], c}));
            twoByteInstructions.put((char) (opcode + 2), new InstructionForm(res, new Cursor[]{bitCursors[i], d}));
            twoByteInstructions.put((char) (opcode + 3), new InstructionForm(res, new Cursor[]{bitCursors[i], e}));
            twoByteInstructions.put((char) (opcode + 4), new InstructionForm(res, new Cursor[]{bitCursors[i], h}));
            twoByteInstructions.put((char) (opcode + 5), new InstructionForm(res, new Cursor[]{bitCursors[i], l}));
            twoByteInstructions.put((char) (opcode + 6), new InstructionForm(res, new Cursor[]{bitCursors[i], ihl}));
            twoByteInstructions.put((char) (opcode + 7), new InstructionForm(res, new Cursor[]{bitCursors[i], a}));
        }

        // SET
        for (char i = 0; i < 8; ++i) {
            char opcode = (char) (0xC0 + 8 * i);
            twoByteInstructions.put(opcode, new InstructionForm(set, new Cursor[]{bitCursors[i], b}));
            twoByteInstructions.put((char) (opcode + 1), new InstructionForm(set, new Cursor[]{bitCursors[i], c}));
            twoByteInstructions.put((char) (opcode + 2), new InstructionForm(set, new Cursor[]{bitCursors[i], d}));
            twoByteInstructions.put((char) (opcode + 3), new InstructionForm(set, new Cursor[]{bitCursors[i], e}));
            twoByteInstructions.put((char) (opcode + 4), new InstructionForm(set, new Cursor[]{bitCursors[i], h}));
            twoByteInstructions.put((char) (opcode + 5), new InstructionForm(set, new Cursor[]{bitCursors[i], l}));
            twoByteInstructions.put((char) (opcode + 6), new InstructionForm(set, new Cursor[]{bitCursors[i], ihl}));
            twoByteInstructions.put((char) (opcode + 7), new InstructionForm(set, new Cursor[]{bitCursors[i], a}));
        }
    }

    private void pushStack(char value) {
        // SP points to the address where the next byte will be pushed
        sp.decrement();
        gb.mmu.write16(sp.read(), value);
        sp.decrement();
    }

    private char popStack() {
        // SP points to the address where the next byte will be pushed
        sp.increment();
        char val = gb.mmu.read16(sp.read());
        sp.increment();
        return val;
    }

    public void raiseInterrupt(Interrupt interrupt) {
        // Request an interrupt (to be serviced before executing the next instruction)
        char raisedInterrupts = (char) (gb.mmu.read8((char) 0xFF0F) | interrupt.getBitmask());
        gb.mmu.write8((char) 0xFF0F, raisedInterrupts);
        halted = false;
        gb.stopped = false;
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
        if (halted) return;

        char raisedInterrupts = gb.mmu.read8((char) 0xFF0F);
        char enabledInterrupts = gb.mmu.read8((char) 0xFFFF);
        byte raisedEnabledInterrupts = (byte) (enabledInterrupts & raisedInterrupts);

        // Service interrupts
        if (interruptsEnabled && (raisedEnabledInterrupts & 0x1F) != 0) {
            interruptsEnabled = false;
            pushStack(pc.read());

            // Jump to interrupt handler according to priority
            if ((raisedEnabledInterrupts & Interrupt.VBLANK.getBitmask()) != 0) {
                raisedInterrupts &= ~Interrupt.VBLANK.getBitmask();
                pc.write((char) 0x40);
            } else if ((raisedEnabledInterrupts & Interrupt.LCD.getBitmask()) != 0) {
                raisedInterrupts &= ~Interrupt.LCD.getBitmask();
                pc.write((char) 0x48);
            } else if ((raisedEnabledInterrupts & Interrupt.TIMER.getBitmask()) != 0) {
                raisedInterrupts &= ~Interrupt.TIMER.getBitmask();
                pc.write((char) 0x50);
            } else if ((raisedEnabledInterrupts & Interrupt.SERIAL.getBitmask()) != 0) {
                raisedInterrupts &= ~Interrupt.SERIAL.getBitmask();
                pc.write((char) 0x58);
            } else {
                raisedInterrupts &= ~Interrupt.JOYPAD.getBitmask();
                pc.write((char) 0x60);
            }

            // Write new flags (with bit for serviced interrupt unset)
            gb.mmu.write8((char) 0xFF0F, raisedInterrupts);
        }

        char optByte = gb.mmu.read8(pc.read());

        // $CB prefix -> instruction is two bytes
        if (optByte == 0xCB) {
            pc.increment();
            twoByteInstructions.get(gb.mmu.read8(pc.read())).execute(this);
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
            value = (char) ((value + 1) & 0xFF);
        }

        public void decrement() {
            value = (char) ((value - 1) & 0xFF);
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
                bitmask = (byte) mask;
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
            return (char) ((high.read() << 8) | low.read());
        }

        public void increment() {
            write((char) (read() + 1));
        }

        public void decrement() {
            write((char) (read() - 1));
        }
    }

    private class IndirectRegister8Cursor implements Cursor {
        private Register reg;

        public IndirectRegister8Cursor(Register r) {
            reg = r;
        }

        public char read() {
            // Get value pointed to by address in register + $FF00
            return gb.mmu.read8((char) (0xFF00 | reg.read()));
        }

        public void write(char value) {
            // Write to address contained in register
            gb.mmu.write8((char) (0xFF00 | reg.read()), value);
        }
    }

    private class IndirectRegister16Cursor implements Cursor {
        private Register reg;

        public IndirectRegister16Cursor(Register r) {
            reg = r;
        }

        public char read() {
            // Get value pointed to by address in register
            return gb.mmu.read8(reg.read());
        }

        public void write(char value) {
            // Write to address contained in register
            gb.mmu.write8(reg.read(), value);
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

    private static class LDHL implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = cpu.sp.read();
            byte y = (byte) operands[0].read();  // Treat as signed
            int val = x + y;
            cpu.hl.write((char) val);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, false);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, (x & 0xF) + (y & 0xF) > 0xF);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, (x & 0xFF) + y > 0xFF);
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
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, false);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, true);
        }
    }

    // CCF - complement carry flag
    private static class CCF implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, false);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY,
                    !cpu.f.isFlagSet(FlagRegister.Flag.CARRY));
        }
    }

    // CPL - complement A register
    private static class CPL implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, true);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, true);
            cpu.a.write((char) (~cpu.a.read() & 0xFF));
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
            operands[0].write((char) (x + 1));
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, x == 0xFF);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, (x & 0xF) + 1 > 0xF);
        }
    }

    private static class INC16 implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[0].write((char) (operands[0].read() + 1));
        }
    }

    // DEC - decrement
    private static class DEC8 implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = operands[0].read();
            operands[0].write((char) (x - 1));
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, x == 1);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, true);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, (x & 0xF) - 1 < 0);
        }
    }

    private static class DEC16 implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char x = operands[0].read();
            operands[0].write((char) (x - 1));
        }
    }

    private static class ADD implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char result = (char) (cpu.a.read() + operands[1].read());
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, result == 0);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            boolean halfCarryValue = (((cpu.a.read()&0xf) + (operands[1].read()&0xf))&0x10) == 0x10;
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, halfCarryValue);
            boolean carryValue = (((int)result) > 255);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, carryValue);
            cpu.a.write(result);
        }
    }

    private static class ADC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char result = (char) (cpu.a.read() + operands[1].read());
            if(cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                result += 0x01;
            }
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, result == 0);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            boolean halfCarryValue = (((cpu.a.read()&0xF) + (operands[1].read()&0xF))&0x10) == 0x10;
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, halfCarryValue);
            boolean carryValue = (((int)result) > 255);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, carryValue);
            cpu.a.write(result);
        }
    }

    // AND - AND n with A
    private static class AND implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char result = (char) (cpu.a.read() & operands[0].read());
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
            char result = (char) (cpu.a.read() ^ operands[0].read());
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
            char result = (char) (cpu.a.read() | operands[0].read());
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

    // SWAP - swap upper and lower nibbles
    private static class SWAP implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char val = operands[0].read();
            val = (char) (((val << 4) | (val >>> 4)) & 0xFF);
            operands[0].write(val);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, val == 0);
        }
    }

    // BIT - test bit in register
    private static class BIT implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            boolean bitZero = (operands[1].read() & (1 << operands[0].read())) == 0;
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, bitZero);
            cpu.f.updateFlag(FlagRegister.Flag.SUBTRACTION, false);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, true);
        }
    }

    // RES - reset bit in register
    private static class RES implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[1].write((char) (operands[1].read() & ~(1 << operands[0].read())));
        }
    }

    // SET - set bit in register
    private static class SET implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            operands[1].write((char) (operands[1].read() | (1 << operands[0].read())));
        }
    }

    // JP - jump to address
    private static class JP implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.pc.write(operands[0].read());
        }
    }

    // JPNZ - jump to address if zero flag clear
    private static class JPNZ implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (!cpu.f.isFlagSet(FlagRegister.Flag.ZERO))
                cpu.pc.write(operands[0].read());
        }
    }

    // JPZ - jump to address if zero flag set
    private static class JPZ implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (cpu.f.isFlagSet(FlagRegister.Flag.ZERO))
                cpu.pc.write(operands[0].read());
        }
    }

    // JPNC - jump to address if carry flag clear
    private static class JPNC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (!cpu.f.isFlagSet(FlagRegister.Flag.CARRY))
                cpu.pc.write(operands[0].read());
        }
    }

    // JPC - jump to address if carry flag set
    private static class JPC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (cpu.f.isFlagSet(FlagRegister.Flag.CARRY))
                cpu.pc.write(operands[0].read());
        }
    }

    // JR - jump relative to current address in PC
    private static class JR implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            // Operand is signed
            char addr = (char) (cpu.pc.read() + (byte) operands[0].read());
            cpu.pc.write(addr);
        }
    }

    // JRNZ - jump relative to current address in PC if zero flag is clear
    private static class JRNZ implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (!cpu.f.isFlagSet(FlagRegister.Flag.ZERO)) {
                // Operand is signed
                char addr = (char) (cpu.pc.read() + (byte) operands[0].read());
                cpu.pc.write(addr);
            }
        }
    }

    // JRZ - jump relative to current address in PC if zero flag is set
    private static class JRZ implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (cpu.f.isFlagSet(FlagRegister.Flag.ZERO)) {
                // Operand is signed
                char addr = (char) (cpu.pc.read() + (byte) operands[0].read());
                cpu.pc.write(addr);
            }
        }
    }

    // JRNC - jump relative to current address in PC if carry flag is clear
    private static class JRNC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (!cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                // Operand is signed
                char addr = (char) (cpu.pc.read() + (byte) operands[0].read());
                cpu.pc.write(addr);
            }
        }
    }

    // JRC - jump relative to current address in PC if zero flag is clear
    private static class JRC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                // Operand is signed
                char addr = (char) (cpu.pc.read() + (byte) operands[0].read());
                cpu.pc.write(addr);
            }
        }
    }

    // CALL - push address of next op onto stack and jump
    private static class CALL implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.pushStack(cpu.pc.read());
            cpu.pc.write(operands[0].read());
        }
    }

    // CALLNZ - if zero flag is clear, push address of next op onto stack and jump
    private static class CALLNZ implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (!cpu.f.isFlagSet(FlagRegister.Flag.ZERO)) {
                cpu.pushStack(cpu.pc.read());
                cpu.pc.write(operands[0].read());
            }
        }
    }

    // CALLZ - if zero flag is set, push address of next op onto stack and jump
    private static class CALLZ implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (cpu.f.isFlagSet(FlagRegister.Flag.ZERO)) {
                cpu.pushStack(cpu.pc.read());
                cpu.pc.write(operands[0].read());
            }
        }
    }

    // CALLNC - if carry flag is clear, push address of next op onto stack and jump
    private static class CALLNC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (!cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                cpu.pushStack(cpu.pc.read());
                cpu.pc.write(operands[0].read());
            }
        }
    }

    // CALLC - if carry flag is set, push address of next op onto stack and jump
    private static class CALLC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                cpu.pushStack(cpu.pc.read());
                cpu.pc.write(operands[0].read());
            }
        }
    }

    // RST - restart execution
    private static class RST implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.pushStack(cpu.pc.read());
            cpu.pc.write(operands[0].read());
        }
    }

    // RET - return from function
    private static class RET implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.pc.write(cpu.popStack());
        }
    }

    // RETNZ - return from function if zero flag is clear
    private static class RETNZ implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (!cpu.f.isFlagSet(FlagRegister.Flag.ZERO)) {
                cpu.pc.write(cpu.popStack());
            }
        }
    }

    // RETZ - return from function if zero flag is set
    private static class RETZ implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (cpu.f.isFlagSet(FlagRegister.Flag.ZERO)) {
                cpu.pc.write(cpu.popStack());
            }
        }
    }

    // RETNC - return from function if carry flag is clear
    private static class RETNC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (!cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                cpu.pc.write(cpu.popStack());
            }
        }
    }

    // RETC - return from function if carry flag is set
    private static class RETC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            if (cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                cpu.pc.write(cpu.popStack());
            }
        }
    }

    // RETI - return from function and enable interrupts
    private static class RETI implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.pc.write(cpu.popStack());
            cpu.interruptsEnabled = true;
        }
    }

    // HALT - stop execution until an interrupt is raised
    private static class HALT implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char raisedInterrupts = cpu.gb.mmu.read8((char) 0xFF0F);
            char enabledInterrupts = cpu.gb.mmu.read8((char) 0xFFFF);

            /* BUG: If interrupt master enable is unset but some interrupts are enabled and raised,
               halt mode is not entered and PC will not be incremented after fetching the next
               opcode. E.g.,

               $3E $14  (LD A,$14) will be executed as:

               $3E $3E  (LD A,$3E)
               $14      (INC D) */
            cpu.halted = (cpu.interruptsEnabled || (enabledInterrupts & raisedInterrupts & 0x1F) == 0);
            cpu.haltBugTriggered = !cpu.halted;
        }
    }

    // STOP - halt GameBoy hardware
    private static class STOP implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.gb.stopped = true;
        }
    }

    // EI - enable interrupts
    private static class EI implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.interruptsEnabled = true;
        }
    }

    // DI - disable interrupts
    private static class DI implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.interruptsEnabled = false;
        }
    }

    // RL - rotate left through carry flag
    private static class RL implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            int shifted = operands[0].read() << 1;
            if (cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                shifted ^= 1;
            }
            cpu.f.write((char) 0);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, (shifted & 0x100) > 0);
            operands[0].write((char) shifted);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, operands[0].read() == 0);
        }
    }

    // RLC - rotate left
    private static class RLC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            cpu.f.write((char) 0);
            int shifted = operands[0].read() << 1;
            if ((shifted & 0x100) > 0) {
                shifted ^= 1;
                cpu.f.updateFlag(FlagRegister.Flag.CARRY, true);
            }
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, shifted == 0);
            operands[0].write((char) shifted);
        }
    }

    // RR - rotate right through carry flag
    private static class RR implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            int val = operands[0].read();
            final boolean zerothBitWasSet = (val & 1) > 0;
            int shifted = val >>> 1;
            if (cpu.f.isFlagSet(FlagRegister.Flag.CARRY)) {
                shifted ^= 0x80;
            }
            cpu.f.write((char) 0);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, zerothBitWasSet);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, shifted == 0);
            operands[0].write((char) shifted);
        }
    }

    // RRC - rotate right
    private static class RRC implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            int val = operands[0].read();
            final boolean zerothBitWasSet = (val & 1) > 0;
            int shifted = val >>> 1;
            if (zerothBitWasSet) {
                shifted ^= 0x80;
            }
            cpu.f.write((char) 0);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, zerothBitWasSet);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, shifted == 0);
            operands[0].write((char) shifted);
        }
    }

    // SLA - arithmetic shift left
    private static class SLA implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            int shifted = operands[0].read() << 1;
            cpu.f.write((char) 0);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, (shifted & 0x100) != 0);
            operands[0].write((char) shifted);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, operands[0].read() == 0);
        }
    }

    // SRA - arithmetic right shift
    private static class SRA implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char original = operands[0].read();
            cpu.f.write((char) 0);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, (original & 1) > 0);
            int shifted = ((byte) original) >> 1;
            operands[0].write((char) shifted);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, operands[0].read() == 0);
        }
    }

    // SRL - logical right shift
    private static class SRL implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char original = operands[0].read();
            cpu.f.write((char) 0);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, (original & 1) > 0);
            // upper 8 bits always zero. SRL operates on 8-bit values
            int shifted = original >> 1;
            operands[0].write((char) shifted);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, operands[0].read() == 0);
        }
    }

    // DAA - decimal adjust A register
    private static class DAA implements InstructionRoot {
        @Override
        public void execute(CPU cpu, Cursor[] operands) {
            char val = cpu.a.read();

            // Apply BCD correction depending on the last operation performed
            if (cpu.f.isFlagSet(FlagRegister.Flag.SUBTRACTION)) {
                if (cpu.f.isFlagSet(FlagRegister.Flag.HALF_CARRY))
                    val -= 6;
                if (cpu.f.isFlagSet(FlagRegister.Flag.CARRY))
                    val -= 0x60;
            } else {
                if ((val & 0xF) > 9 || cpu.f.isFlagSet(FlagRegister.Flag.HALF_CARRY))
                    val += 6;
                if (val > 0x9F || cpu.f.isFlagSet(FlagRegister.Flag.CARRY))
                    val += 0x60;
            }
            cpu.a.write(val);
            cpu.f.updateFlag(FlagRegister.Flag.ZERO, (val & 0xFF) == 0);
            cpu.f.updateFlag(FlagRegister.Flag.HALF_CARRY, false);
            cpu.f.updateFlag(FlagRegister.Flag.CARRY, (val & 0x100) != 0);
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