package creativename.gamedroid.core;

public class ConstantCycleInstruction extends InstructionForm {
    private int cycles;
    public ConstantCycleInstruction(InstructionRoot root, Cursor[] operandTemplate, int cycles) {
        super(root, operandTemplate);
        this.cycles = cycles;
    }

    @Override
    public int execute(CPU cpu) {
        super.execute(cpu);
        return cycles;
    }
}
