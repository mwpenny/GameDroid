package creativename.gamedroid;

import org.junit.Test;
import static org.junit.Assert.*;

import creativename.gamedroid.core.CPU;
import creativename.gamedroid.core.MMU;


public class CPUTest {
    @Test
    public void indirectReg() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
            0x21, 0x00, 0xC0, // LD HL,$C000
            0x36, 0x01,       // LD (HL),$01
            0x36, 0x02        // LD (HL),$02
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(1, cpu.mmu.read8((char)0xC000));
        cpu.execInstruction();
        assertEquals(2, cpu.mmu.read8((char)0xC000));
    }

    @Test
    public void immediate8bitLoads() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
            0x06, 0x11,
            0x16, 0x40,
            0x26, 0x99,
            0x0E, 0x55,
            0x1E, 0xFF,
            0x2E, 0x01,
            0x3E, 0xCF,
        }));
        cpu.execInstruction();
        assertEquals(0x11, cpu.b.read());
        cpu.execInstruction();
        assertEquals(0x40, cpu.d.read());
        cpu.execInstruction();
        assertEquals(0x99, cpu.h.read());
        cpu.execInstruction();
        assertEquals(0x55, cpu.c.read());
        cpu.execInstruction();
        assertEquals(0xFF, cpu.e.read());
        cpu.execInstruction();
        assertEquals(0x01, cpu.l.read());
        cpu.execInstruction();
        assertEquals(0xCF, cpu.a.read());
    }

    @Test
    public void loadBetweenRegisters() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
            0x26, 0x8F, // H <- 0x8F
            0x54,       // D <- H
            0x42,       // B <- D
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x8F, cpu.b.read());
    }

    @Test
    public void indirectLoads() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x31, 0x04, 0x30, // LD SP,$3004
                0x08, 0x00, 0xC0, // LD ($C000),SP
                0x3E, 0x12,       // LD A,$12
                0xEA, 0x00, 0xC0, // LD ($C000),A
                0xFA, 0x01, 0xC0  // LD A,($C001)
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x3004, cpu.mmu.read16((char)0xC000));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x12, cpu.mmu.read8((char)0xC000));
        cpu.execInstruction();
        assertEquals(0x30, cpu.a.read());
    }

    @Test
    public void ldhl() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x31, 0x0E, 0x00, // LD SP,$000E
                0xF8, 0x01,       // LD HL,SP+$01
                0xF8, 0x02,       // LD HL,SP+$F2
                0x31, 0xFF, 0x00, // LD SP,$00FF
                0xF8, 0x01,       // LD HL,SP+$01
                0xF8, 0xFF        // LD HL,SP+$FF (=SP-1)
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x000F, cpu.hl.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        cpu.execInstruction();
        assertEquals(0x0010, cpu.hl.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x0100, cpu.hl.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        cpu.execInstruction();
        assertEquals(0x00FE, cpu.hl.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }

    @Test
    public void flagOps() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x37,  // SCF
                0x3F   // CCF
        }));
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }

    @Test
    public void compare() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0x16,  // LD A,$16
                0x06, 0x08,  // LD B,$08
                0xBF,        // CP A
                0xB8,        // CP B
                0xFE, 0x26   // CP $26
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
    }

    @Test
    public void inc() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0xFF,  // LD A,$FF
                0x06, 0x07,  // LD B,$07
                0x0E, 0xFF,  // LD C,$FF
                0x3C,        // INC A
                0x04,        // INC B
                0x03         // INC BC
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertEquals(8, cpu.b.read());
        cpu.execInstruction();
        assertEquals(0x900, cpu.bc.read());
    }

    @Test
    public void dec() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0x00,  // LD A,$00
                0x06, 0xFF,  // LD B,$FF
                0x0E, 0x01,  // LD C,$01
                0x3D,        // DEC A
                0x05,        // DEC B
                0x0D,        // DEC C
                0x0B         // DEC BC
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertEquals(0xFE, cpu.b.read());
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        cpu.execInstruction();
        assertEquals(0xFDFF, cpu.bc.read());
    }

    @Test
    public void and() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0x12,  // LD A,$12
                0x06, 0x03,  // LD B,$03
                0xA0,        // AND B
                0xE6, 0x01   // AND $01
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        assertEquals(2, cpu.a.read());
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void or() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0x12,  // LD A,$12
                0x06, 0x03,  // LD B,$03
                0xB0,        // OR B
                0x3E, 0x00,  // LD A,$0
                0xF6, 0x00   // OR $00
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        assertEquals(0x13, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void xor() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0x12,  // LD A,$12
                0x06, 0x03,  // LD B,$03
                0xA8,        // XOR B
                0xEE, 0x11   // XOR $11
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        assertEquals(0x11, cpu.a.read());
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void pushPop() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x01, 0x04, 0x30,  // LD BC,$3004
                0xC5,              // PUSH BC
                0xC5,              // PUSH BC
                0xF1,              // POP AF
                0xF5,              // PUSH AF
                0xD1,              // POP DE
                0xD5,              // PUSH DE
                0xE1,              // POP HL
                0xE5,              // PUSH HL
                0xC1               // POP BC
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x3004, cpu.bc.read());
    }

    @Test
    public void swap() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0xF3,       // LD A,$F3
                0xCB, 0x37,       // SWAP A
                0xEA, 0x00, 0xC0, // LD ($C000),A
                0x21, 0x00, 0xC0, // LD HL,$C000
                0xCB, 0x36        // SWAP (HL)
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x3F, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xF3, cpu.mmu.read8((char)0xC000));
    }

    @Test
    public void bit() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0xF3,       // LD A,$F3
                0xEA, 0x00, 0xC0, // LD ($C000),A
                0xCB, 0x57,       // BIT 2,A
                0xCB, 0x7F,       // BIT 7,A
                0x21, 0x00, 0xC0, // LD HL,$C000
                0xCB, 0x5E,       // BIT 3,(HL)
                0xCB, 0x76        // BIT 6,(HL)
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
    }

    @Test
    public void setRes() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0x00, // LD A,0
                0x4F,       // LD C,A
                0xCB, 0xC1, // SET 0,C
                0xCB, 0xD1, // SET 2,C
                0xCB, 0xE1, // SET 4,C
                0xCB, 0x81, // RES 0,C
                0xCB, 0x91, // RES 2,C
                0xCB, 0xA1  // RES 4,C
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x15, cpu.c.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0, cpu.c.read());
    }

    @Test
    public void relativeJumps() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
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
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
    }

    @Test
    public void absoluteJumps() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
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
                0x30, 0x24, 0x01, // JP NC,$0124
                0x3E, 0x00,       // LD A,0

                // $0124
                0x00              // NOP
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0xFF, cpu.a.read());
    }

    @Test
    public void functions() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
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
        }));
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x00, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x01, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x02, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x03, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x04, cpu.a.read());
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0x05, cpu.a.read());
        cpu.execInstruction();
        assertEquals(0x10, cpu.pc.read());
    }

    @Test
    public void rotateLeft() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
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

                0x3E, 0b10000000,  // LD A, 0b00011010
                0x17,              // RLA
        }));

        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b11010101, cpu.a.read());
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b10110100, cpu.a.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b00110101, cpu.a.read());
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b00110100, cpu.a.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void rotateRight() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
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

                0x3E, 0b00000001,  // LD A, 0b00011010
                0x1F,              // RRA
        }));

        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b11110101, cpu.a.read());
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b00101101, cpu.a.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b11001101, cpu.a.read());
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b00001101, cpu.a.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }

    @Test
    public void logicalShiftLeft() throws Exception {
        CPU cpu = new CPU(new FixtureMMU(new int[]{
                0x3E, 0b11101011,  // LD A, 0b11101011
                0xCB, 0x27,        // SLA A

                0x3E, 0b01011010,  // LD A, 0b01011010
                0xCB, 0x27,        // SLA A

                0x3E, 0,           // LD A, 0
                0xCB, 0x27,        // SLA A

                0x3E, 0b10000000,   // LD A, 0
                0xCB, 0x27,        // SLA A
        }));

        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b11010110, cpu.a.read());
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));

        cpu.execInstruction();
        cpu.execInstruction();
        assertEquals(0b10110100, cpu.a.read());
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.CARRY));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));

        cpu.execInstruction();
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
    }
}

 class FixtureMMU extends MMU {
     int[] fixtureRom;


     public FixtureMMU(int[] machineCode) {
         this.fixtureRom = machineCode;
     }

     @Override
     public char read8(char address) {
         int offset = address - 0x100;
         if (offset <= fixtureRom.length && offset >= 0) {
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
