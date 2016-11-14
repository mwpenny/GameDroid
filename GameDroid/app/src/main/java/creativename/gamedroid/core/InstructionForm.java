package creativename.gamedroid.core;

/* CPU instruction wrapper. Performs operand translation */
public class InstructionForm {
    protected InstructionRoot root;
    protected Cursor[] operandTemplate;

    public InstructionForm(InstructionRoot root, Cursor[] operandTemplate) {
        this.root = root;
        this.operandTemplate = operandTemplate;
    }

    final static ConstantCursor8 cache8 = new ConstantCursor8((char) 1);
    final static ConstantCursor16 cache16 = new ConstantCursor16((char) 1);

    /* Translates operands and fetches data as necessary */
    protected Cursor[] readOperands(Cursor[] operandTemplate, CPU cpu, Cursor[] operands) {
        for (int i = 0; i < operandTemplate.length; i++) {
            if (operandTemplate[i] == CPU.immediate8) {
                // Next byte is operand
                char val = cpu.gb.mmu.read8(cpu.pc.read());
                cache8.value = val;
                operands[i] = cache8;
                cpu.pc.increment();
            } else if (operandTemplate[i] == CPU.immediate16) {
                // Next two bytes make operand
                char val = cpu.gb.mmu.read16(cpu.pc.read());
                cache16.value = val;
                operands[i] = cache16;
                cpu.pc.increment();
                cpu.pc.increment();
            } else if (operandTemplate[i] == CPU.oneByteIndirect8) {
                // Next byte + $FF00 makes pointer to 8-bit operand
                char address = (char)(0xFF00 | cpu.gb.mmu.read8(cpu.pc.read()));
                operands[i] = cpu.gb.mmu.getCursor8(address);
                cpu.pc.increment();
            } else if (operandTemplate[i] == CPU.twoByteIndirect8) {
                // Next twoOperandCache bytes make pointer to 8-bit operand
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
                // No translation required
                operands[i] = operandTemplate[i];
            }
        }
        return operands;
    }

    Cursor[] oneOperandCache = new Cursor[1];
    Cursor[] twoOperandCache = new Cursor[2];
    public int execute(CPU cpu) {
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


        Cursor[] operands = oneOperandCache;
        if (operandTemplate.length == 1) {
            operands = readOperands(operandTemplate, cpu, oneOperandCache);
        } else if (operandTemplate.length == 2) {
            operands = readOperands(operandTemplate, cpu, twoOperandCache);
        }
        return root.execute(cpu, operands);
    }
}
