# GameDroid
---

A GameBoy emulator for Android. This is the term project for COMP 3004 at Carleton University.

Repository structure:
* `dev_logs/` contains weekly development logs for each team member
* `docs/` contains documents related to the project. This is mostly documentation of GameBoy hardware to refer to while implementing the emulator. Unless otherwise stated, this material is by other authors and is used purely as reference material
* `GameDroid/` contains the application's source code

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
| 0x13   | MBC3+RAM+BATT               |
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
* GameBoy emulation
* Easy to use UI for organizing and searching game library

Planned features:
* SRAM saves
* Save states
* Rewind
* Customizable screen resolution
* Landscape layout

Supported MBCs:
* MBC1
* MBC3
* MBC5

Minimum Android version: 4.0.3 (Ice Cream Sandwich)
