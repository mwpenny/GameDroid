package creativename.gamedroid.core;

public interface InstructionRoot {
    /* Execute the instruction and return the number of clock cycles used.

       Note that the clock cycle used by an instruction might vary due to the form of the
       instruction. The cycles returned by this method only correspond to the cycles used by one
       form of the instruction. ConstantCycleInstruction is used to override the cycle value
       returned by this method. */
    int execute(CPU cpu, Cursor[] operands);
}
