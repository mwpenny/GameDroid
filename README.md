# GameDroid
---

A GameBoy emulator for Android. This is the term project for COMP 3004 at Carleton University.

Repository structure:
* `dev_logs/` contains weekly development logs for each team member
* `docs/` contains documents related to the project. This is mostly documentation of GameBoy hardware to refer to while implementing the emulator. Unless otherwise stated, this material is by other authors and is used purely as reference material
* `GameDroid/` contains the application's source code

# CPU instructions to implement (single byte)

|      | x0       | x1      | x2       | x3      | x4        | x5      | x6       | x7      | x8        | x9      | xA       | xB      | xC       | xD     | xE       | xF    |
|------|----------|---------|----------|---------|-----------|---------|----------|---------|-----------|---------|----------|---------|----------|--------|----------|-------|
|**x0**|          |         |LD (BC),A |         |           |         |          |RLCA     |LD (a16),SP|ADD HL,BC|LD A,(BC) |         |          |        |          |RRCA   |
|**x1**|STOP 0    |         |LD (DE),A |         |           |         |          |RLA      |JR r8      |ADD HL,DE|LD A,(DE) |         |          |        |          |RRA    |
|**x2**|JR NZ,r8  |         |LD (HL+),A|         |           |         |          |DAA      |JR Z,r8    |ADD HL,HL|LD A,(HL+)|         |          |        |          |CPL    |
|**x3**|JR NC,r8  |         |LD (HL-),A|         |INC (HL)   |DEC (HL) |LD (HL),d8|         |JR C,r8    |ADD HL,SP|LD A,(HL-)|         |          |        |          |       |
|**x4**|          |         |          |         |           |         |LD B,(HL) |         |           |         |          |         |          |        |LD C,(HL) |       |
|**x5**|          |         |          |         |           |         |LD D,(HL) |         |           |         |          |         |          |        |LD E,(HL) |       |
|**x6**|          |         |          |         |           |         |LD H,(HL) |         |           |         |          |         |          |        |LD L,(HL) |       |
|**x7**|LD (HL),B |LD (HL),C|LD (HL),D |LD (HL),E|LD (HL),H  |LD (HL),L|HALT      |LD (HL),A|           |         |          |         |          |        |LD A,(HL) |       |
|**x8**|ADD A,B   |ADD A,C  |ADD A,D   |ADD A,E  |ADD A,H    |ADD A,L  |ADD A,(HL)|ADD A,A  |ADC A,B    |ADC A,C  |ADC A,D   |ADC A,E  |ADC A,H   |ADC A,L |ADC A,(HL)|ADC A,A|
|**x9**|SUB B     |SUB C    |SUB D     |SUB E    |SUB H      |SUB L    |SUB (HL)  |SUB A    |SBC A,B    |SBC A,C  |SBC A,D   |SBC A,E  |SBC A,H   |SBC A,L |SBC A,(HL)|SBC A,A|
|**xA**|          |         |          |         |           |         |AND (HL)  |         |           |         |          |         |          |        |XOR (HL)  |       |
|**xB**|          |         |          |         |           |         |OR (HL)   |         |           |         |          |         |          |        |CP (HL)   |       |
|**xC**|RET NZ    |POP BC   |JP NZ,a16 |JP a16   |CALL NZ,a16|PUSH BC  |ADD A,d8  |RST 00H  |RET Z      |RET      |JP Z,a16  |         |CALL Z,a16|CALL a16|ADC A,d8  |RST 08H|
|**xD**|RET NC    |POP DE   |JP NC,a16 |         |CALL NC,a16|PUSH DE  |SUB d8    |RST 10H  |RET C      |RETI     |JP C,a16  |         |CALL C,a16|        |SBC A,d8  |RST 18H|
|**xE**|LDH (a8),A|POP HL   |LD (C),A  |         |           |PUSH HL  |AND d8    |RST 20H  |ADD SP,r8  |JP (HL)  |LD (a16),A|         |          |        |XOR d8    |RST 28H|
|**xF**|LDH A,(a8)|POP AF   |LD A,(C)  |DI       |           |PUSH AF  |OR d8     |RST 30H  |LD HL,SP+r8|LD SP,HL |LD A,(a16)|EI       |          |        |          |RST 38H|

* [Original opcode table](http://pastraiser.com/cpu/gameboy/gameboy_opcodes.html) with more info (such as flags modified and cycle count).
* [Opcode summary](http://gameboy.mongenel.com/dmg/opcodes.html)

Major features:
* Nothing (yet)


Minimum Android version: 4.0.3 (Ice Cream Sandwich)
