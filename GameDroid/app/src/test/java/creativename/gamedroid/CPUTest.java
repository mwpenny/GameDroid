package creativename.gamedroid;

import org.junit.Test;
import static org.junit.Assert.*;

import creativename.gamedroid.core.CPU;
import creativename.gamedroid.core.MMU;


public class CPUTest {
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
        assertEquals(cpu.b.read(), 8);
        cpu.execInstruction();
        assertEquals(cpu.bc.read(), 0x900);
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
        assertEquals(cpu.b.read(), 0xFE);
        cpu.execInstruction();
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.ZERO));
        assertTrue(cpu.f.isFlagSet(CPU.FlagRegister.Flag.SUBTRACTION));
        assertFalse(cpu.f.isFlagSet(CPU.FlagRegister.Flag.HALF_CARRY));
        cpu.execInstruction();
        assertEquals(cpu.bc.read(), 0xFDFF);
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
        assertEquals(cpu.a.read(), 2);
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
        assertEquals(cpu.a.read(), 0x13);
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
        assertEquals(cpu.a.read(), 0x11);
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
        assertEquals(cpu.bc.read(), 0x3004);
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
     protected char read16(char address) {
         int offset = address - 0x100;
         if (offset <= fixtureRom.length && offset >= 0) {
             return (char)(fixtureRom[offset++] | fixtureRom[offset] << 8);
         }
         return super.read16(address);
     }
 }
