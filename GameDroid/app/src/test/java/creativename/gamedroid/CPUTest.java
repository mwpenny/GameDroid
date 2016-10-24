package creativename.gamedroid;

import org.junit.Test;
import static org.junit.Assert.*;

import creativename.gamedroid.core.Cartridge;
import creativename.gamedroid.core.GameBoy;
import creativename.gamedroid.core.CPU;
import creativename.gamedroid.core.MMU;


public class CPUTest {
    @Test
    public void indirectReg() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x21, 0x00, 0xC0, // LD HL,$C000
            0x36, 0x01,       // LD (HL),$01
            0x36, 0x02        // LD (HL),$02
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(1, gb.mmu.read8((char)0xC000));
        gb.cpu.execInstruction();
        assertEquals(2, gb.mmu.read8((char)0xC000));
    }

    @Test
    public void immediate8bitLoads() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x06, 0x11,
            0x16, 0x40,
            0x26, 0x99,
            0x0E, 0x55,
            0x1E, 0xFF,
            0x2E, 0x01,
            0x3E, 0xCF,
        });
        gb.cpu.execInstruction();
        assertEquals(0x11, gb.cpu.b.read());
        gb.cpu.execInstruction();
        assertEquals(0x40, gb.cpu.d.read());
        gb.cpu.execInstruction();
        assertEquals(0x99, gb.cpu.h.read());
        gb.cpu.execInstruction();
        assertEquals(0x55, gb.cpu.c.read());
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.e.read());
        gb.cpu.execInstruction();
        assertEquals(0x01, gb.cpu.l.read());
        gb.cpu.execInstruction();
        assertEquals(0xCF, gb.cpu.a.read());
    }

    @Test
    public void loadBetweenRegisters() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x26, 0x8F, // H <- 0x8F
            0x54,       // D <- H
            0x42,       // B <- D
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x8F, gb.cpu.b.read());
    }

    @Test
    public void indirectLoads() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x31, 0x04, 0x30, // LD SP,$3004
            0x08, 0x00, 0xC0, // LD ($C000),SP
            0x3E, 0x12,       // LD A,$12
            0xEA, 0x00, 0xC0, // LD ($C000),A
            0xFA, 0x01, 0xC0  // LD A,($C001)
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x3004, gb.mmu.read16((char)0xC000));
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x12, gb.mmu.read8((char)0xC000));
        gb.cpu.execInstruction();
        assertEquals(0x30, gb.cpu.a.read());
    }

    @Test
    public void ldhl() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x31, 0x0E, 0x00, // LD SP,$000E
            0xF8, 0x01,       // LD HL,SP+$01
            0xF8, 0x02,       // LD HL,SP+$F2
            0x31, 0xFF, 0x00, // LD SP,$00FF
            0xF8, 0x01,       // LD HL,SP+$01
            0xF8, 0xFF        // LD HL,SP+$FF (=SP-1)
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x000F, gb.cpu.hl.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        assertEquals(0x0010, gb.cpu.hl.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x0100, gb.cpu.hl.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        assertEquals(0x00FE, gb.cpu.hl.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }

    @Test
    public void flagOps() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x37,  // SCF
            0x3F   // CCF
        });
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }

    @Test
    public void compare() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0x16,  // LD A,$16
            0x06, 0x08,  // LD B,$08
            0xBF,        // CP A
            0xB8,        // CP B
            0xFE, 0x26   // CP $26
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }

    @Test
    public void inc() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0xFF,  // LD A,$FF
            0x06, 0x07,  // LD B,$07
            0x0E, 0xFF,  // LD C,$FF
            0x3C,        // INC A
            0x04,        // INC B
            0x03         // INC BC
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertEquals(8, gb.cpu.b.read());
        gb.cpu.execInstruction();
        assertEquals(0x900, gb.cpu.bc.read());
    }

    @Test
    public void dec() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0x00,  // LD A,$00
            0x06, 0xFF,  // LD B,$FF
            0x0E, 0x01,  // LD C,$01
            0x3D,        // DEC A
            0x05,        // DEC B
            0x0D,        // DEC C
            0x0B         // DEC BC
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertEquals(0xFE, gb.cpu.b.read());
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        gb.cpu.execInstruction();
        assertEquals(0xFDFF, gb.cpu.bc.read());
    }

    @Test
    public void add() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
                0x3E, 0b11101011,  // LD A, 0b11101011
                0x06, 0b00000001,  // LD B, 0b00000001
                0x80,              // ADD A,B
                0x0E, 0b00001111,  // LD C, 0b00001111
                0x81,              // ADD A,C

                0x3E, 0b11111111,  // LD A, 0b11111111
                0xC6, 0b00000001,  // ADD A, 0b00000001

                0x3E, 0b00000001,  // LD A, 0x01
                0xC6, 0b00000001   // ADD A, 0x01
        });

        //Test 1
        gb.cpu.execInstruction(); //LD A ~
        assertEquals(0b11101011, gb.cpu.a.read());
        gb.cpu.execInstruction(); //LD B ~
        assertEquals(0b00000001, gb.cpu.b.read());
        gb.cpu.execInstruction(); // ADD A,B
        assertEquals(0b11101100, gb.cpu.a.read());

        //Test 2
        gb.cpu.execInstruction(); //LD C, ~
        gb.cpu.execInstruction(); //ADD A,C
        assertEquals(0b11111011, gb.cpu.a.read());

        //Edge case with immediate8 & Flag setting
        gb.cpu.execInstruction(); // LD A ~
        assertEquals(0b11111111, gb.cpu.a.read());
        gb.cpu.execInstruction(); //ADD A, immediate8
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY)); //Half Carry Set?
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY)); //Carry Set?

        //Testing Flags to be False
        gb.cpu.execInstruction(); // LD A, 0x01
        gb.cpu.execInstruction(); // ADD A, 0x01
        assertEquals(0b00000010, gb.cpu.a.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY)); //Half Carry Set?
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY)); //Carry Set?
    }

    @Test
    public void add16() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
                0x21, 0x12, 0xA7,  // LD HL,$A712
                0x01, 0x34, 0x12,  // LD BC,$1234
                0x09,              // ADD HL,BC

                0x21, 0xFF, 0xFF,  // LD HL,$FFFF
                0x01, 0x01, 0x00,  // LD BC,$0001
                0x09,              // ADD HL,BC

                0x21, 0x12, 0x00,  // LD HL,$0012
                0x01, 0xAF, 0x00,  // LD BC,$00AF
                0x09,              // ADD HL,BC

                0x21, 0x00, 0x00,  // LD HL,0000
                0x01, 0x00, 0x00,  // LD BC,$0000
                0x09,              // ADD HL,BC

                0x31, 0x04, 0x30,  // LD SP,$3004
                0xE8, 0xAF         // ADD SP,$AF
        });

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xB946, gb.cpu.hl.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x0000, gb.cpu.hl.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x00C1, gb.cpu.hl.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        // 16-bit ADD shouldn't set the zero flag
        gb.cpu.f.updateFlag(CPU.FlagRegister.Flag.ZERO, false);
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        // ADD SP,d8 should clear the zero flag
        gb.cpu.f.updateFlag(CPU.FlagRegister.Flag.ZERO, true);
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x2FB3, gb.cpu.sp.read());  // Should be signed
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }


    @Test
    public void adc() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
                0x3E, 0b11101011,  // LD A, 0b11101011
                0x06, 0b00000001,  // LD B, 0b00000001
                0x88,              // ADC A,B //No flag should be added

                0x0E, 0b00001111,  // LD C, 0b00001111
                0x89,              // ADC A,C

                0x3E, 0b11111111,  // LD A, 0b11111111
                0xCE, 0b00000001,  // ADC A, 0b00000001

                0x3E, 0b00000001,  // LD A, 0x01
                0xCE, 0b00000001,  // ADC A, 0x01


                0x37,              // SCF
                0x3E, 0b00001111,  // LD A, 0b00001111
                0xCE, 0b00000000   // ADC A, 0b00000000
        });

        //Test 1
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        gb.cpu.execInstruction(); //LD A ~
        assertEquals(0b11101011, gb.cpu.a.read());
        gb.cpu.execInstruction(); //LD B ~
        assertEquals(0b00000001, gb.cpu.b.read());
        gb.cpu.execInstruction(); // ADC A,B
        assertEquals(0b11101101, gb.cpu.a.read()); //Does not remain 11101100

        //Test 2
        gb.cpu.execInstruction(); //LD C, ~
        gb.cpu.execInstruction(); //ADC A,C //Half Carry gets set
        assertEquals(0b11111100, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));

        //immediate8 add Test with Flag
        gb.cpu.execInstruction(); // LD A ~
        assertEquals(0b11111111, gb.cpu.a.read());
        gb.cpu.execInstruction(); //ADC A, immediate8
        assertEquals(0b00000000, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY)); //Half Carry Set?
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY)); //Carry Set?

        //Testing Flags to be False
        gb.cpu.execInstruction(); // LD A, 0x01
        gb.cpu.execInstruction(); // ADC A, 0x01
        assertEquals(0b00000011, gb.cpu.a.read()); //Add extra 1 because of carry flag above
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY)); //Half Carry Set?
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY)); //Carry Set?

        // Carry flag should cause half carry overflow
        gb.cpu.f.updateFlag(CPU.FlagRegister.Flag.HALF_CARRY, false);
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b00010000, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
    }

    @Test
    public void subtract() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
                0x3E, 0b11101011,  // LD A, 0b11101011
                0xD6, 0b00000001,  // SUB 1 => A-1

                0x3E, 0b11110000,  //LD A, 0b11110000
                0xD6, 0b00000001,  //SUB 1 => A-1

                0x3E, 0b00000000,  //LD A, 0b00000000
                0xD6, 0b00000001,  //SUB 1 => A-1
        });

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11101010, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11101111, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11111111, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }

    @Test
    public void sbc() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
                0x3E, 0b11101011,  // LD A, 0b11101011
                0x06, 0b00000001,  // LD B, 0b00000001
                0x98,              // SBC 1 => A-B

                0x3E, 0b00000000,  //LD A, 0b00000000
                0xDE, 0b00000001,  //SUB 1 => A-1

                0x3E, 0b11110000,  //LD A, 0b11110000
                0xD6, 0b00000001,  //SUB 1 => A-1

                0x3E, 0b00000000,  //LD A, 0b00000000
                0x9F, 0b11110000   // A - A (carry bit should cause overflow)
        });

        gb.cpu.f.updateFlag(CPU.FlagRegister.Flag.CARRY, true);
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11101001, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11111111, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11101111, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.f.updateFlag(CPU.FlagRegister.Flag.CARRY, true);
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11111111, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }

    @Test
    public void and() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0x12,  // LD A,$12
            0x06, 0x03,  // LD B,$03
            0xA0,        // AND B
            0xE6, 0x01   // AND $01
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        assertEquals(2, gb.cpu.a.read());
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void or() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0x12,  // LD A,$12
            0x06, 0x03,  // LD B,$03
            0xB0,        // OR B
            0x3E, 0x00,  // LD A,$0
            0xF6, 0x00   // OR $00
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        assertEquals(0x13, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void xor() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0x12,  // LD A,$12
            0x06, 0x03,  // LD B,$03
            0xA8,        // XOR B
            0xEE, 0x11   // XOR $11
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        assertEquals(0x11, gb.cpu.a.read());
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void pushPop() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x01, 0x04, 0x30,  // LD BC,$3004
            0xC5,              // PUSH BC
            0xC5,              // PUSH BC
            0x01, 0x00, 0x00,  // LD BC,$0000
            0xD1,              // POP DE
            0xD5,              // PUSH DE
            0xE1,              // POP HL
            0xE5,              // PUSH HL
            0xC1               // POP BC
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x3004, gb.cpu.bc.read());
    }

    @Test
    public void swap() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0xF3,       // LD A,$F3
            0xCB, 0x37,       // SWAP A
            0xEA, 0x00, 0xC0, // LD ($C000),A
            0x21, 0x00, 0xC0, // LD HL,$C000
            0xCB, 0x36        // SWAP (HL)
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x3F, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xF3, gb.mmu.read8((char)0xC000));
        assertEquals(0, gb.cpu.f.read());
    }

    @Test
    public void bit() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0xF3,       // LD A,$F3
            0xEA, 0x00, 0xC0, // LD ($C000),A
            0xCB, 0x57,       // BIT 2,A
            0xCB, 0x7F,       // BIT 7,A
            0x21, 0x00, 0xC0, // LD HL,$C000
            0xCB, 0x5E,       // BIT 3,(HL)
            0xCB, 0x76,       // BIT 6,(HL)
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
    }

    @Test
    public void setRes() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0x00, // LD A,0
            0x4F,       // LD C,A
            0xCB, 0xC1, // SET 0,C
            0xCB, 0xD1, // SET 2,C
            0xCB, 0xE1, // SET 4,C
            0xCB, 0x81, // RES 0,C
            0xCB, 0x91, // RES 2,C
            0xCB, 0xA1, // RES 4,C
            0xAF,       // XOR A
            0xCB, 0xFF, // SET 7, A
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x15, gb.cpu.c.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0, gb.cpu.c.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b1000_0000, gb.cpu.a.read());
    }

    @Test
    public void relativeJumps() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x18, 0x02, // JR 2
            0x3E, 0x00, // LD A,0
            0x3E, 0xFF, // LD A,$FF

            0xFE, 0x00, // CP A,0
            0x20, 0x02, // JR NZ,2
            0x3E, 0x00, // LD A,0
            0x00,       // NOP

            0xFE, 0xFF, // CP A,$FF
            0x28, 0x02, // JR Z,2
            0x3E, 0x00, // LD A,0
            0x00,       // NOP

            0x37,       // SCF
            0x38, 0x02, // JR C,2
            0x3E, 0,    // LD A,0
            0x00,       // NOP

            0x3F,       // CCF
            0x30, 0x02, // JR NC,2
            0x3E, 0x00, // LD A,0
            0x00        // NOP
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
    }

    @Test
    public void absoluteJumps() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0xC3, 0x05, 0x01, // JP $0105
            0x3E, 0x00,       // LD A,0

            // $0105
            0x3E, 0xFF,       // LD A,$FF

            0xFE, 0x00,       // CP A,0
            0xC2, 0x0E, 0x01, // JP NZ,$010E
            0x3E, 0x00,       // LD A,0

            // $010E
            0x00,             // NOP

            0xFE, 0xFF,       // CP A,$FF
            0xCA, 0x16, 0x01, // JP Z,$0116
            0x3E, 0x00,       // LD A,0

            // $0116
            0x00,             // NOP

            0x37,             // SCF
            0xDA, 0x1D, 0x01, // JP C,$011D
            0x3E, 0,          // LD A,0

            // $011D
            0x00,             // NOP

            0x3F,             // CCF
            0xD2, 0x24, 0x01, // JP NC,$0124
            0x3E, 0x00,       // LD A,0

            // $0124
            0x00              // NOP
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0xFF, gb.cpu.a.read());
    }

    @Test
    public void functions() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0xC3, 0x1B, 0x01, // JP $011B

            // $103
            0x3E, 0x00,  // LD A,$00
            0xC9,        // RET

            // $106
            0x3E, 0x01,  // LD A,$01
            0xFE, 0x01,  // CP A,$01
            0xC8,        // RET Z

            // $10B
            0x3E, 0x02,  // LD A,$02
            0xFE, 0x01,  // CP A,$01
            0xC0,        // RET NZ

            // $110
            0x3E, 0x03,  // LD A,$03
            0x37,        // SCF
            0xD8,        // RET C

            // $114
            0x3E, 0x04,  // LD A,$04
            0x3F,        // CCF
            0xD0,        // RET NC

            // $118
            0x3E, 0x05,  // LD A,$05
            0xD7,        // RST $10

            // $11B
            0xCD, 0x03, 0x01, // CALL $103
            0xCD, 0x06, 0x01, // CALL $106
            0xCC, 0x0B, 0x01, // CALL Z,$10B
            0xC4, 0x10, 0x01, // CALL NZ,$110
            0xDC, 0x14, 0x01, // CALL C,$114
            0xD4, 0x18, 0x01, // CALL NC,$118
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x00, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x01, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x02, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x03, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x04, gb.cpu.a.read());
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x05, gb.cpu.a.read());
        gb.cpu.execInstruction();
        assertEquals(0x10, gb.cpu.pc.read());
    }

    @Test
    public void rotateLeft() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0b11101010,  // LD A, 0b11101010
            0x07,              // RLCA

            0x3E, 0b01011010,  // LD A, 0b01011010
            0x07,              // RLCA

            0x37,              // set carry flag
            0x3E, 0b10011010,  // LD A, 0b10011010
            0x17,              // RLA

            0xAF,              // XOR A, A clear carry
            0x3E, 0b00011010,  // LD A, 0b00011010
            0x17,              // RLA

            0x3E, 0b10000000,  // LD A, 0b10000000
            0x17,              // RLA
        });

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11010101, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b10110100, gb.cpu.a.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b00110101, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b00110100, gb.cpu.a.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void rotateRight() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0b11101011,  // LD A, 0b11101011
            0x0F,              // RRCA

            0x3E, 0b01011010,  // LD A, 0b01011010
            0x0F,              // RRCA

            0x37,              // set carry flag
            0x3E, 0b10011011,  // LD A, 0b10011011
            0x1F,              // RRA

            0xAF,              // XOR A, A clear carry
            0x3E, 0b00011010,  // LD A, 0b00011010
            0x1F,              // RRA

            0x3E, 0b00000001,  // LD A, 0b00000001
            0x1F,              // RRA
        });

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11110101, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b00101101, gb.cpu.a.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11001101, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b00001101, gb.cpu.a.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void logicalLeftShift() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0b11101011,  // LD A, 0b11101011
            0xCB, 0x27,        // SLA A

            0x3E, 0b01011010,  // LD A, 0b01011010
            0xCB, 0x27,        // SLA A

            0x3E, 0,           // LD A, 0
            0xCB, 0x27,        // SLA A

            0x3E, 0b10000000,   // LD A, 0b10000000
            0xCB, 0x27,         // SLA A
        });

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11010110, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b10110100, gb.cpu.a.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void logicalRightShift() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0b11101011,  // LD A, 0b11101011
            0xCB, 0x3F,        // SRL A

            0x3E, 0b01011010,  // LD A, 0b01011010
            0xCB, 0x3F,        // SRL A

            0x3E, 0,           // LD A, 0
            0xCB, 0x3F,        // SRL A

            0x3E, 0b10,        // LD A, 0b10
            0xCB, 0x3F,        // SRL A

            0x3E, 1,           // LD A, 1
            0xCB, 0x3F,        // SRL A
        });

        gb.cpu.f.write((char) 0);
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b01110101, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b00101101, gb.cpu.a.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void arithmeticRightShift() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0b11101011,  // LD A, 0b11101011
            0xCB, 0x2F,        // SRL A

            0x3E, 0b01011010,  // LD A, 0b01011010
            0xCB, 0x2F,        // SRL A

            0x3E, 0,           // LD A, 0
            0xCB, 0x2F,        // SRL A

            0x3E, 0b10,        // LD A, 0b10
            0xCB, 0x2F,        // SRL A

            0x3E, 1,           // LD A, 1
            0xCB, 0x2F,        // SRL A
        });

        gb.cpu.f.write((char) 0);
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b11110101, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b00101101, gb.cpu.a.read());
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }


    @Test
    public void cpl() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x3E, 0b11101011,  // LD A, 0b11101011
            0x2F               // CPL
        });

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0b00010100, gb.cpu.a.read());
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(gb.cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
    }

    @Test
    public void daa() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x37,       // SCF
            0x3F,       // CCF

            0x3E, 0x0F, // LD A,$0F
            0x3C,       // INC A
            0x27,       // DAA

            0x3E, 0x09, // LD A,$09
            0x3C,       // INC A
            0x27,       // DAA

            0x3E, 0xA4, // LD A,$A4
            0x3C,       // INC A
            0x27,       // DAA

            0x3E, 0xA6, // LD A,$A6
            0x3D,       // DEC A
            0x27,       // DAA

            0x3E, 0x10, // LD A,$10
            0x3D,       // DEC A
            0x27        // DAA
        });

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        // A == $10
        gb.cpu.execInstruction();
        assertEquals(0x16, gb.cpu.a.read());

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        // A == $0A
        gb.cpu.execInstruction();
        assertEquals(0x10, gb.cpu.a.read());

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        // A == $A5
        gb.cpu.execInstruction();
        assertEquals(0x05, gb.cpu.a.read());

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        // A == $05
        gb.cpu.execInstruction();
        assertEquals(0x45, gb.cpu.a.read());

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        // A == $0F
        gb.cpu.execInstruction();
        assertEquals(0xA9, gb.cpu.a.read());
    }

    @Test
    public void halting() throws Exception {
        GameBoy gb = new GameBoy();
        gb.mmu = new FixtureMMU(new int[]{
            0x16, 0x00,       // LD D,$00
            0x3E, 0x00,       // LD A,$00
            0xF3,             // DI
            0xEA, 0xFF, 0xFF, // LD ($FFFF),A
            0xEA, 0x0F, 0xFF, // LD ($FF0F),A
            0x76,             // HALT
            0x3E, 0x12,       // LD A,$12

            0x3E, 0x01,       // LD A,$01
            0xEA, 0xFF, 0xFF, // LD ($FFFF),A
            0xEA, 0x0F, 0xFF, // LD ($FF0F),A
            0x76,             // HALT
            0x3E, 0x14        // Due to the halt bug this will become LD A,$3E + INC D
        });
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertFalse(gb.cpu.haltBugTriggered);
        // CPU should be halted
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x00, gb.cpu.a.read());
        // CPU should resume execution, but not jump to joypad interrupt vector
        gb.cpu.raiseInterrupt(CPU.Interrupt.JOYPAD);
        gb.cpu.execInstruction();
        assertEquals(0x12, gb.cpu.a.read());

        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertTrue(gb.cpu.haltBugTriggered);
        gb.cpu.execInstruction();
        gb.cpu.execInstruction();
        assertEquals(0x3E, gb.cpu.a.read());
        assertEquals(0x01, gb.cpu.d.read());
    }
}

 class FixtureMMU extends MMU {
     int[] fixtureRom;

     public FixtureMMU(int[] machineCode) {
         super(null);
         this.fixtureRom = machineCode;
     }

     @Override
     public char read8(char address) {
         int offset = address - 0x100;
         if (offset < fixtureRom.length && offset >= 0) {
             return (char) fixtureRom[offset];
         }
         return super.read8(address);
     }

     @Override
     public char read16(char address) {
         int offset = address - 0x100;
         if (offset <= fixtureRom.length && offset >= 0) {
             return (char)(fixtureRom[offset++] | fixtureRom[offset] << 8);
         }
         return super.read16(address);
     }
 }
