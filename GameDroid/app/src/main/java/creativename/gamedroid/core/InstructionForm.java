package creativename.gamedroid.core;

public class InstructionForm {
    protected InstructionRoot root;
    protected Cursor[] operandTemplate;

    public InstructionForm(InstructionRoot root, Cursor[] operandTemplate) {
        this.root = root;
        this.operandTemplate = operandTemplate;
    }

    protected Cursor[] readOperands(Cursor[] operandTemplate, CPU cpu) {
        Cursor[] operands = new Cursor[operandTemplate.length];
        for (int i = 0; i < operandTemplate.length; i++) {
            if (operandTemplate[i] == CPU.immediate8) {
                // Next byte is operand
                char val = cpu.gb.mmu.read8(cpu.pc.read());
                operands[i] = new ConstantCursor8(val);
                cpu.pc.increment();
            } else if (operandTemplate[i] == CPU.immediate16) {
                // Next two bytes make operand
                char val = cpu.gb.mmu.read16(cpu.pc.read());
                operands[i] = new ConstantCursor16(val);
                cpu.pc.increment();
                cpu.pc.increment();
            } else if (operandTemplate[i] == CPU.oneByteIndirect8) {
                // Next byte + $FF00 makes pointer to 8-bit operand
                char address = (char)(0xFF00 | cpu.gb.mmu.read8(cpu.pc.read()));
                operands[i] = cpu.gb.mmu.getCursor8(address);
                cpu.pc.increment();
            } else if (operandTemplate[i] == CPU.twoByteIndirect8) {
                // Next two bytes make pointer to 8-bit operand
                char address = cpu.gb.mmu.read16(cpu.pc.read());
                operands[i] = cpu.gb.mmu.getCursor8(address);
                cpu.pc.increment();
                cpu.pc.increment();
            } else if (operandTemplate[i] == CPU.indirect16) {
                // Next two bytes make pointer to 16-bit operand
                char address = cpu.gb.mmu.read16(cpu.pc.read());
                operands[i] = cpu.gb.mmu.getCursor16(address);
                cpu.pc.increment();
                cpu.pc.increment();
            } else {
                operands[i] = operandTemplate[i];
            }
        }
        return operands;
    }

    public void execute(CPU cpu) {
        /* BUG: If interrupt master enable is unset but some interrupts are enabled and raised,
           halt mode is not entered and PC will not be incremented after fetching the next
           opcode. E.g.,

           $3E $14  (LD A,$14) will be executed as:

           $3E $3E  (LD A,$3E)
           $14      (INC D) */
        if (!cpu.haltBugTriggered)
            cpu.pc.increment();
        else
            cpu.haltBugTriggered = false;
        Cursor[] operands = readOperands(operandTemplate, cpu);
        root.execute(cpu, operands);
    }
}
