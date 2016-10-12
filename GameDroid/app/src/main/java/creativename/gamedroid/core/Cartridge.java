package creativename.gamedroid.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/* Loads, stores, and provides access to data from ROM files */
public class Cartridge {
    public enum ColorMode {
        MONOCHROME,
        COLOR_SUPPORTED,
        COLOR_REQUIRED
    }

    public enum GameLocale {
        JAPAN,
        WORLD
    }

    // Nintendo logo bitmap (verified by boot procedure)
    // (and why doesn't Java have unsigned types!?!)
    private static final byte[] LOGO_BITMAP = {
            (byte)0xCE, (byte)0xED, 0x66, 0x66, (byte)0xCC, 0x0D, 0x00,
            0x0B, 0x03, 0x73, 0x00, (byte)0x83, 0x00, 0x0C,
            0x00, 0x0D, 0x00, 0x08, 0x11, 0x1F, (byte)0x88,
            (byte)0x89, 0x00, 0x0E, (byte)0xDC, (byte)0xCC, 0x6E, (byte)0xE6,
            (byte)0xDD, (byte)0xDD, (byte)0xD9, (byte)0x99, (byte)0xBB, (byte)0xBB, 0x67,
            0x63, 0x6E, 0x0E, (byte)0xEC, (byte)0xCC, (byte)0xDD, (byte)0xDC,
            (byte)0x99, (byte)0x9F, (byte)0xBB, (byte)0xB9, 0x33, 0x3E
    };

    /* Lookup maps mapping licensee codes to company names */
    private static final Map<String, String> LICENSEES_NEW;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("00", "None"); m.put("01", "Nintendo"); m.put("08", "Capcom");
        m.put("13", "Electronic Arts"); m.put("18", "HudsonSoft"); m.put("19", "B-Ai");
        m.put("20", "KSS"); m.put("22", "Pow"); m.put("24", "PCM Complete");
        m.put("25", "San-X"); m.put("28", "Kemco Kapan"); m.put("29", "SETA");
        m.put("30", "Viacom"); m.put("31", "Nintendo"); m.put("32", "Bandai");
        m.put("33", "Ocean"); m.put("34", "Konami"); m.put("35", "Hector");
        m.put("37", "Taito"); m.put("38", "Hudson"); m.put("39", "Banpresto");
        m.put("41", "Ubisoft"); m.put("42", "Atlus"); m.put("44", "Malibu");
        m.put("46", "Angel"); m.put("47", "Bullet-Proof Software"); m.put("49", "IREM");
        m.put("50", "Absolute"); m.put("51", "Acclaim"); m.put("52", "Activision");
        m.put("53", "American Sammy"); m.put("54", "Konami"); m.put("55", "Hi Tech Entertainment");
        m.put("56", "LJN"); m.put("57", "Matchbox"); m.put("58", "Mattel");
        m.put("59", "Milton Bradley"); m.put("60", "Titus"); m.put("61", "Virgin");
        m.put("64", "LucasArts"); m.put("67", "Ocean"); m.put("69", "Electronic Arts");
        m.put("70", "Infogrames"); m.put("71", "Interplay"); m.put("72", "Broderbund");
        m.put("73", "Sculptured"); m.put("75", "SCI"); m.put("78", "THQ");
        m.put("79", "Accolade"); m.put("80", "Misawa"); m.put("83", "LOZC");
        m.put("86", "Tokuma Shoten Intermedia"); m.put("87", "Tsukuda Original"); m.put("91", "Chun Soft");
        m.put("92", "Video System"); m.put("93", "Ocean"); m.put("95", "Varie");
        m.put("96", "Yonezawa"); m.put("97", "Kaneko"); m.put("99", "Pack-In-Soft");
        LICENSEES_NEW = Collections.unmodifiableMap(m);
    }
    private static final Map<Byte, String> LICENSEES_OLD;
    static
    {
        Map<Byte, String> m = new HashMap<>();
        m.put((byte)0x00, "None"); m.put((byte)0x01, "Nintendo"); m.put((byte)0x08, "Capcom");
        m.put((byte)0x09, "Hot-B"); m.put((byte)0x0A, "Jaleco"); m.put((byte)0x0B, "Coconuts");
        m.put((byte)0x0C, "Elite Systems"); m.put((byte)0x13, "Electronic Arts"); m.put((byte)0x18, "Hudson Soft");
        m.put((byte)0x19, "ITC Entertainment"); m.put((byte)0x1A, "Yanoman"); m.put((byte)0x1D, "Clary");
        m.put((byte)0x1F, "Virgin"); m.put((byte)0x24, "PCM Complete"); m.put((byte)0x25, "San-X");
        m.put((byte)0x28, "Kotobuki Systems"); m.put((byte)0x29, "SETA"); m.put((byte)0x30, "Infogrames");
        m.put((byte)0x31, "Nintendo"); m.put((byte)0x32, "Bandai"); m.put((byte)0x34, "Konami");
        m.put((byte)0x35, "Hector"); m.put((byte)0x38, "Capcom"); m.put((byte)0x39, "Banpresto");
        m.put((byte)0x3C, "Entertainment International"); m.put((byte)0x3E, "Gremlin"); m.put((byte)0x41, "Ubisoft");
        m.put((byte)0x42, "Atlus"); m.put((byte)0x44, "Malibu"); m.put((byte)0x46, "Angel");
        m.put((byte)0x47, "Spectrum Holobyte"); m.put((byte)0x49, "IREM"); m.put((byte)0x4A, "Virgin");
        m.put((byte)0x4D, "Malibu"); m.put((byte)0x4F, "U.S. Gold"); m.put((byte)0x50, "Absolute");
        m.put((byte)0x51, "Acclaim"); m.put((byte)0x52, "Activision"); m.put((byte)0x53, "American Sammy");
        m.put((byte)0x54, "GameTek"); m.put((byte)0x55, "Park Place"); m.put((byte)0x56, "LJN");
        m.put((byte)0x57, "Matchbox"); m.put((byte)0x59, "Milton Bradley"); m.put((byte)0x5A, "Mindscape");
        m.put((byte)0x5B, "Romstar"); m.put((byte)0x5C, "Naxat Soft"); m.put((byte)0x5D, "Tradewest");
        m.put((byte)0x60, "Titus"); m.put((byte)0x61, "Virgin"); m.put((byte)0x67, "Ocean");
        m.put((byte)0x69, "Electronic Arts"); m.put((byte)0x6E, "Elite Systems"); m.put((byte)0x6F, "Electro Brain");
        m.put((byte)0x70, "Infogrames"); m.put((byte)0x71, "Interplay"); m.put((byte)0x72, "Broderbund");
        m.put((byte)0x73, "Sculptered Soft"); m.put((byte)0x75, "The Sales Curve"); m.put((byte)0x78, "THQ");
        m.put((byte)0x79, "Accolade"); m.put((byte)0x7A, "Triffix Entertainment"); m.put((byte)0x7C, "Microprose");
        m.put((byte)0x7F, "Kemco"); m.put((byte)0x80, "Misawa Entertainment"); m.put((byte)0x83, "LOZC");
        m.put((byte)0x86, "Tokuma Shoten Intermedia"); m.put((byte)0x8B, "Bullet-Proof Software"); m.put((byte)0x8C, "Vic Tokai");
        m.put((byte)0x8E, "Ape"); m.put((byte)0x8F, "I'Max"); m.put((byte)0x91, "Chun Soft");
        m.put((byte)0x92, "Video System"); m.put((byte)0x93, "Tsuburava"); m.put((byte)0x95, "Varie");
        m.put((byte)0x96, "Yonezawa"); m.put((byte)0x97, "Kaneko"); m.put((byte)0x99, "ARC");
        m.put((byte)0x9A, "Nihon Bussan"); m.put((byte)0x9B, "Tecmo"); m.put((byte)0x9C, "Imagineer");
        m.put((byte)0x9D, "Banpresto"); m.put((byte)0x9F, "Nova"); m.put((byte)0xA1, "Hori Electric");
        m.put((byte)0xA2, "Bandai"); m.put((byte)0xA4, "Konami"); m.put((byte)0xA6, "Kawada");
        m.put((byte)0xA7, "Takara"); m.put((byte)0xA9, "Technos Japan"); m.put((byte)0xAA, "Broderbund");
        m.put((byte)0xAC, "Toei Animation"); m.put((byte)0xAD, "Toho"); m.put((byte)0xAF, "Namco");
        m.put((byte)0xB0, "Acclaim"); m.put((byte)0xB1, "Nexoft"); m.put((byte)0xB2, "Bandai");
        m.put((byte)0xB4, "Enix"); m.put((byte)0xB6, "HAL"); m.put((byte)0xB7, "SNK");
        m.put((byte)0xB9, "Pony Canyon"); m.put((byte)0xBA, "Culture Brain"); m.put((byte)0xBB, "Sunsoft");
        m.put((byte)0xBD, "Sony Imagesoft"); m.put((byte)0xBF, "Sammy"); m.put((byte)0xC0, "Taito");
        m.put((byte)0xC2, "Kemco"); m.put((byte)0xC3, "SquareSoft"); m.put((byte)0xC4, "Tokuma Shoten");
        m.put((byte)0xC5, "Data East"); m.put((byte)0xC6, "Tonkin House"); m.put((byte)0xC8, "Koei");
        m.put((byte)0xC9, "UFL"); m.put((byte)0xCA, "Ultra"); m.put((byte)0xCB, "VAP");
        m.put((byte)0xCC, "Use"); m.put((byte)0xCD, "Meldac"); m.put((byte)0xCE, "Pony Canyon");
        m.put((byte)0xCF, "Angel"); m.put((byte)0xD0, "Taito"); m.put((byte)0xD1, "Sofel");
        m.put((byte)0xD2, "Quest"); m.put((byte)0xD3, "Sigma Enterprises"); m.put((byte)0xD4, "Ask Kodansha");
        m.put((byte)0xD6, "Naxat Soft"); m.put((byte)0xD7, "Copya Systems"); m.put((byte)0xD9, "Banpresto");
        m.put((byte)0xDA, "TOMY"); m.put((byte)0xDB, "LJN"); m.put((byte)0xDD, "NCS");
        m.put((byte)0xDE, "Human"); m.put((byte)0xDF, "Altron"); m.put((byte)0xE0, "Jaleco");
        m.put((byte)0xE1, "Towachiki"); m.put((byte)0xE2, "Uutaka"); m.put((byte)0xE3, "Varie");
        m.put((byte)0xE5, "Epoch"); m.put((byte)0xE7, "Athena"); m.put((byte)0xE8, "Asmik");
        m.put((byte)0xE9, "Natsume"); m.put((byte)0xEA, "King Records"); m.put((byte)0xEB, "Atlus");
        m.put((byte)0xEC, "Epic"); m.put((byte)0xEE, "IGS"); m.put((byte)0xF0, "A Wave");
        m.put((byte)0xF3, "Extreme Entertainment"); m.put((byte)0xFF, "LNJ");
        LICENSEES_OLD = Collections.unmodifiableMap(m);
    }

    private String title;
    private String manufacturer;
    private ColorMode colorMode;
    private String licensee;
    private boolean supportsSGB;    // SGB = Super GameBoy
    private byte cartType;          // i.e., Which MBC, clock, battery, etc. are present
    private int romSize;
    private int ramSize;
    private GameLocale locale;
    private byte gameVersion;
    public MBC mbc;

    // Getters for public data
    public String getTitle() { return title; }
    public String getManufacturer() { return manufacturer; }
    public ColorMode getColorMode() { return colorMode; }
    public String getLicensee() { return licensee; }
    public boolean supportsSGB() { return supportsSGB; }
    public byte getCartType() { return cartType; }
    public int getRomSize() { return romSize; }
    public int getRamSize() { return ramSize; }
    public GameLocale getLocale() { return locale; }
    public byte getGameVersion() { return gameVersion; }

    private boolean isRomValid(byte[] bank0) {
        byte checksum = 0;
        for (int i = 0x134; i < 0x14D; ++i)
            checksum -= bank0[i] + 1;

        // Verify Nintendo logo is present and cartridge checksum is correct
        return (checksum == bank0[0x14D]) &&
               Arrays.equals(LOGO_BITMAP, Arrays.copyOfRange(bank0, 0x104, 0x134));
    }

    private void parseHeader(byte[] bank0) throws IOException {
        // $0134-$143 - Title
        title = new String(Arrays.copyOfRange(bank0, 0x134, 0x144), "ASCII");
        manufacturer = "";
        colorMode = ColorMode.MONOCHROME;

        // CGB flag field used -> newer cartridge header
        if (bank0[0x143] == (byte)0xC0 || bank0[0x143] == (byte)0x80) {
            title = title.substring(0, 15);  // Last byte used for CGB flag

            // Manufacturer field (likely) used
            if (bank0[0x13F] != 0 && bank0[0x140] != 0 && bank0[0x141] != 0 && bank0[0x142] != 0) {
                title = title.substring(0, 11);  // Last 4 bytes used for manufacturer

                // $013F-$0142 - Manufacturer Code
                manufacturer = new String(Arrays.copyOfRange(bank0, 0x13F, 0x143), "ASCII");
            }

            // $0143 - CGB Flag
            switch (bank0[0x143]) {
                case (byte)0x80:
                    colorMode = ColorMode.COLOR_SUPPORTED;
                    break;
                case (byte)0xC0:
                    colorMode = ColorMode.COLOR_REQUIRED;
                    break;
            }
        }

        // $014B - Old Licensee Code (Value of "33" -> use new licensee code at $0144-$0145)
        if (bank0[0x14B] == 0x33)
            licensee = LICENSEES_NEW.get(new String(Arrays.copyOfRange(bank0, 0x144, 0x146), "ASCII"));
        else
            licensee = LICENSEES_OLD.get(bank0[0x14B]);

        supportsSGB = (bank0[0x146] == 3);
        cartType = bank0[0x147];  // TODO: make this field more granular (i.e., hasBattery, hasTimer, MBCType)

        // $0149 - ROM size
        romSize = (32 << bank0[0x148]) * 1024;

        // $0149 - RAM Size (0x00 -> 0KB,  0x01 -> 2KB/16Kb, ...)
        ramSize = ((int)Math.pow(4, bank0[0x149]+1)/8) * 1024;

        // $14A - Game region
        locale = (bank0[0x14A] == 1) ? GameLocale.WORLD : GameLocale.JAPAN;

        // $14C - Game software revision
        gameVersion = bank0[0x14C];
    }

    public Cartridge(String path) throws IOException {
        byte[] buf = new byte[0x4000];
        File f = new File(path);
        FileInputStream in = new FileInputStream(f);

        try {
            // Some preliminary sanity checks
            if (f.length() > Integer.MAX_VALUE)
                throw new IOException("Input file too large");
            if (f.length() < 0x8000)
                throw new IOException("Input file too small");

            // Read first ROM bank and parse header
            if (in.read(buf, 0, 0x4000) != 0x4000)
                throw new IOException("Error reading ROM bank 0");
            if (!isRomValid(buf))
                throw new IOException("Invalid ROM image");

            parseHeader(buf);
            if (f.length() != romSize)
                throw new IOException("ROM size mismatch (expected: " + romSize +
                                      ", actual: " + f.length() + ")");
            else {
                // Good to go! Load ROM
                byte rom[] = new byte[romSize];
                boolean hasBattery = (cartType == 0x03 || cartType == 0x06 || cartType == 0x09 ||
                                      cartType == 0x0D || cartType == 0x0F || cartType == 0x10 ||
                                      cartType == 0x13 || cartType == 0x17 || cartType == 0x1B ||
                                      cartType == 0x1E || cartType == 0x22 || cartType == (byte)0xFF);
                System.arraycopy(buf, 0, rom, 0, buf.length);
                if (in.read(rom, 0x4000, rom.length - 0x4000) != rom.length - 0x4000)
                    throw new IOException("Error reading ROM banks");

                // TODO: detect RTC and rumble
                switch (cartType) {
                    case 0x00:  // ROM ONLY
                    case 0x08:  // ROM+RAM
                    case 0x09:  // ROM+RAM+BATTERY
                        mbc = new MBC0(rom, ramSize, hasBattery);
                        break;
                    case 0x01:  // MBC1
                    case 0x02:  // MBC1+RAM
                    case 0x03:  // MBC1+RAM+BATTERY
                        mbc = new MBC1(rom, ramSize, hasBattery);
                        break;

                    // TODO: other MBCs...

                    case 0x19:  // MBC5
                    case 0x1A:  // MBC5+RAM
                    case 0x1B:  // MBC5+RAM+BATTERY
                    case 0x1C:  // MBC5+RUMBLE
                    case 0x1D:  // MBC5+RUMBLE+RAM
                    case 0x1E:  // MBC5+RUMBLE+RAM+BATTERY
                        mbc = new MBC5(rom, ramSize, hasBattery);
                        break;

                    default:
                        throw new IllegalArgumentException(String.format("Unsupported cartridge type ($%02X)", cartType));
                }
            }
        } catch (IOException ex) {
            in.close();
            throw ex;
        }
    }
}
