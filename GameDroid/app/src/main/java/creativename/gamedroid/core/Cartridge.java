package creativename.gamedroid.core;

enum ColorMode {
    MONOCHROME,
    COLOR_SUPPORTED,
    COLOR_REQUIRED
}

enum GameLocale {
    JAPANESE,
    NON_JAPANESE
}

/* Stores game data from loaded ROMs */
public class Cartridge {
    private String title;
    private String manufacturer;
    private ColorMode colorMode;
    private String licensee;
    private boolean supportsSGB;    // SGB = Super GameBoy
    private byte cartType;          // i.e., Which memory bank controller is present (if any)
    private byte[] rom;             // Size depends on ROM header
    private int romBank1Start;      // Where in the rom array does $4000 point?
    private byte[] extRam;          // Not always allocated (depends on ROM header)
    private int extRamStart;        // Where in the extRam array (if allocated) does $A000 point?
    private GameLocale locale;
    private byte gameVersion;

    // Getters for public data
    public String getTitle() { return title; }
    public String getManufacturer() { return manufacturer; }
    public ColorMode getColorMode() { return colorMode; }
    public String getLicensee() { return licensee; }
    public boolean supportsSGB() { return supportsSGB; }
    public byte getCartType() { return cartType; }
    public GameLocale getLocale() { return locale; }
    public byte getGameVersion() { return gameVersion; }

    public byte read(int address) {
        // ROM bank 0 is fixed, 1 is switchable
        if (address < 0x4000)
            return rom[address];
        else if (address < 0x8000)
            return rom[romBank1Start + (address - 0x4000)];

        // Cartridge RAM may not be present
        else if (extRam.length > 0 && address > 0x9FFF && address < 0xC000)
            return extRam[extRamStart + (address - 0xA000)];

        else {
            // TODO: exceptions for invalid memory locations?
            return 0;
        }
    }

    public void write(int address, byte value) {
        // TODO: MBC logic (where value "written" actually signifies which bank to switch to)

        // Write to cartridge RAM if present
        // TODO: also write this information to disk if cartridge contains battery (check cartType)
        if (extRam.length > 0 && address > 0x9FFF && address < 0xC000)
            extRam[extRamStart + (address - 0xA000)] = value;
    }

    public Cartridge(String path) {
        // TODO: parse header fields and store in Cartridge instance
        // TODO: maybe use another function for parsing, which returns success/failure
    }
}
