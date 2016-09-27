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
    private boolean supportsSGB;    // SGB = Super GameBoy?
    private byte mbcType;          // Memory bank controller present
    private byte romBankCount;      // Number of 32KB ROM banks present
    private byte extRamSize;        // Number of KB of extra RAM in cartridge
    private GameLocale locale;
    private byte gameVersion;

    // TODO: probably best to store ROM and graphics data here too

    // Getters for public data
    public String getTitle() { return title; }
    public String getManufacturer() { return manufacturer; }
    public ColorMode getColorMode() { return colorMode; }
    public String getLicensee() { return licensee; }
    public boolean isSupportsSGB() { return supportsSGB; }
    public byte getCartType() { return cartType; }
    public byte getRomBankCount() { return romBankCount; }
    public byte getExtRamSize() { return extRamSize; }
    public GameLocale getLocale() { return locale; }
    public byte getGaneVersion() { return gameVersion; }

    public Cartridge(String path) {
        // TODO: parse header fields and store in Cartridge instance
    }
}
