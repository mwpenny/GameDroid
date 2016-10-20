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
|**x0**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x1**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x2**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x3**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x4**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x5**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x6**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x7**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x8**|          |  |      |      |   |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**x9**|        |    |    |        |   |          |          |         |          |         |           |         |          |         |   |      |
|**xA**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xB**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xC**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xD**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xE**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |
|**xF**|          |         |          |         |           |         |          |         |           |         |          |         |          |        |          |       |


* [Original opcode table](http://pastraiser.com/cpu/gameboy/gameboy_opcodes.html) with more info (such as flags modified and cycle count).
* [Opcode summary](http://gameboy.mongenel.com/dmg/opcodes.html)

# Unsupported cartridge types

| Number | Description                 |
|--------|-----------------------------|
| 0x03   | MBC1+RAM+BATT               |
| 0x05   | MBC2                        |
| 0x06   | MBC2+BATT                   |
| 0x0B   | MMM01                       |
| 0x0C   | MMM01+RAM                   |
| 0x0D   | MMM01+RAM+BATT              |
| 0x0F   | MBC3+RTC+BATT               |
| 0x10   | MBC3+RTC+RAM+BATT           |
| 0x11   | MBC3                        |
| 0x12   | MBC3+RAM                    |
| 0x13   | MBC3+RAM+BATT               |
| 0x15   | MBC4                        |
| 0x16   | MBC4+RAM                    |
| 0x17   | MBC4+RAM+BATT               |
| 0x1B   | MBC5+RAM+BATT               |
| 0x1C   | MBC5+RUMBLE                 |
| 0x1D   | MBC5+RUMBLE+RAM             |
| 0x1E   | MBC5+RUMBLE+RAM+BATT        |
| 0x20   | MBC6                        |
| 0x22   | MBC7+SENSOR+RUMBLE+RAM+BATT |
| 0xFC   | POCKET CAMERA               |
| 0xFD   | BANDAI TAMA5                |
| 0xFE   | HuC3                        |
| 0xFF   | HuC1+RAM+BATT               |

Major features:
* Nothing (yet)

Supported MBCs:
* MBC1
* MBC5

Minimum Android version: 4.0.3 (Ice Cream Sandwich)
