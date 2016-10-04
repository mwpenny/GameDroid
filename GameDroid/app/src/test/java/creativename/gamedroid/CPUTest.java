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
             address++;
             char ret = (char) fixtureRom[address];
             address--;
             ret &= fixtureRom[address] << 8;
             return ret;
         }
         return super.read16(address);
     }
 }
