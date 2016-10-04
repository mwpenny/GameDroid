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
            if (operandTemplate[i] == cpu.immediate8) {
                char val = cpu.mmu.read8(cpu.pc.read());
                operands[i] = new ConstantCursor8(val);
                cpu.pc.increment();
            } else if (operandTemplate[i] == cpu.immediate16) {
                char val = cpu.mmu.read16(cpu.pc.read());
                operands[i] = new ConstantCursor16(val);
                cpu.pc.increment();
                cpu.pc.increment();
            } else if (operandTemplate[i] == cpu.indirect8) {
                char address = cpu.mmu.read16(cpu.pc.read());
                operands[i] = cpu.mmu.getCursor8(address);
                cpu.pc.increment();
                cpu.pc.increment();
            } else if (operandTemplate[i] == cpu.indirect16) {
                char address = cpu.mmu.read16(cpu.pc.read());
                operands[i] = cpu.mmu.getCursor16(address);
                cpu.pc.increment();
                cpu.pc.increment();
            } else {
                operands[i] = operandTemplate[i];
            }
        }
        return operands;
    }

    public void execute(CPU cpu) {
        cpu.pc.increment();
        Cursor[] operands = readOperands(operandTemplate, cpu);
        root.execute(cpu, operands);
    }
}
