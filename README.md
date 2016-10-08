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
|**x0**|          |         |          |         |           |         |          |RLCA     |           |ADD HL,BC|          |         |          |        |          |RRCA   |
|**x1**|STOP 0    |         |          |         |           |         |          |RLA      |           |ADD HL,DE|          |         |          |        |          |RRA    |
|**x2**|          |         |          |         |           |         |          |DAA      |           |ADD HL,HL|          |         |          |        |          |CPL    |
|**x3**|          |         |          |         |           |         |          |         |           |ADD HL,SP|          |         |          |        |          |       |
|**x4**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x5**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x6**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x7**|          |         |          |         |           |         |HALT      |         |           |         |          |         |          |        |          |       |
|**x8**|ADD A,B   |ADD A,C  |ADD A,D   |ADD A,E  |ADD A,H    |ADD A,L  |ADD A,(HL)|ADD A,A  |ADC A,B    |ADC A,C  |ADC A,D   |ADC A,E  |ADC A,H   |ADC A,L |ADC A,(HL)|ADC A,A|
|**x9**|SUB B     |SUB C    |SUB D     |SUB E    |SUB H      |SUB L    |SUB (HL)  |SUB A    |SBC A,B    |SBC A,C  |SBC A,D   |SBC A,E  |SBC A,H   |SBC A,L |SBC A,(HL)|SBC A,A|
|**xA**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xB**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xC**|          |         |          |         |           |         |ADD A,d8  |         |           |         |          |         |          |        |ADC A,d8  |       |
|**xD**|          |         |          |         |           |         |SUB d8    |         |           |         |          |         |          |        |SBC A,d8  |       |
|**xE**|          |         |          |         |           |         |          |         |ADD SP,r8  |         |          |         |          |        |          |       |
|**xF**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |


# CPU instructions to implement (prefix CB)

|      | x0       | x1      | x2       | x3      | x4        | x5      | x6       | x7      | x8        | x9      | xA       | xB      | xC       | xD     | xE       | xF    |
|------|----------|---------|----------|---------|-----------|---------|----------|---------|-----------|---------|----------|---------|----------|--------|----------|-------|
|**x0**|RLC B     |RLC C    |RLC D     |RLC E    |RLC H      |RLC L    |RLC (HL)  |RLC A    |RRC B      |RRC C    |RRC D     |RRC E    |RRC H     |RRC L   |RRC (HL)  |RRC A  |
|**x1**|RL B      |RL C     |RL D      |RL E     |RL H       |RL L     |RL (HL)   |RL A     |RR B       |RR C     |RR D      |RR E     |RR H      |RR L    |RR (HL)   |RR A   |
|**x2**|SLA B     |SLA C    |SLA D     |SLA E    |SLA H      |SLA L    |SLA (HL)  |SLA A    |SRA B      |SRA C    |SRA D     |SRA E    |SRA H     |SRA L   |SRA (HL)  |SRA A  |
|**x3**|          |         |          |         |           |         |          |         |SRL B      |SRL C    |SRL D     |SRL E    |SRL H     |SRL L   |SRL (HL)  |SRL A  |
|**x4**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x5**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x6**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x7**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x8**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x9**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xA**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xB**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xC**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xD**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xE**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xF**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |


* [Original opcode table](http://pastraiser.com/cpu/gameboy/gameboy_opcodes.html) with more info (such as flags modified and cycle count).
* [Opcode summary](http://gameboy.mongenel.com/dmg/opcodes.html)

Major features:
* Nothing (yet)


Minimum Android version: 4.0.3 (Ice Cream Sandwich)
