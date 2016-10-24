package creativename.gamedroid.core;

/* An instruction whose behavior depends on the state of a CPU flag */
public abstract class ConditionalInstruction implements InstructionRoot {
    CPU.FlagRegister.Flag flagToCheck;
    boolean expectedFlagValue;

    public ConditionalInstruction(CPU.FlagRegister.Flag flagToCheck, boolean expectedFlagValue) {
        this.flagToCheck = flagToCheck;
        this.expectedFlagValue = expectedFlagValue;
    }

    public abstract int execute(CPU cpu, Cursor[] operands);
}