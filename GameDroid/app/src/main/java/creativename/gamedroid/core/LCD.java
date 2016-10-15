package creativename.gamedroid.core;

/* Provides GameBoy graphics data manipulation, processing, and output */
public class LCD implements MemoryMappable {
    private enum ScreenState {
        HBLANK(0),
        VBLANK(1),
        OAM_SEARCH(2),
        DATA_TRANSFER(3);

        private final byte code;

        ScreenState(int mask) {
            code = (byte) mask;
        }

        public byte getStateCode() {
            return code;
        }
    }

    // GameBoy monochrome palette
    private static final int[] palette = {
            0xFFFFFF,  // 0% black
            0xC0C0C0,  // 33% black
            0x606060,  // 66% black
            0x000000   // 100% black
    };

    private GameBoy gb;
    private byte[][] framebuffer;
    private ScreenState screenState;
    private MappableByte scanline, cmpScanline;
    private char cycle;
    private MappableByte scrollX, scrollY;
    private MappableByte windowX, windowY;  // Window = BG layer that can overlay normal BG

    // TODO: don't store actual addresses? Look more into how memory accesses are made
    private LCDControlRegister lcdControl;
    private boolean lcdEnabled;
    private char windowTileMapStart;     // Tile map 0 = $9800, tile map 1 = $9C00
    private boolean windowEnabled;
    private char bgTilesetStart;         // Tileset 0 = $8800, tileset 1 = $8000
    private char bgTileMapStart;         // Tile map 0 = $9800, tile map 1 = $9C00
    private boolean tallSpritesEnabled;  // Whether or not to use 8x16 sprites
    private boolean spritesEnabled;
    private boolean bgEnabled;

    private LCDStatusRegister lcdStatus;
    private boolean scanlineCheckEnabled;  // Raise LCD interrupt if scanline=cmpScanline?
    private boolean oamCheckEnabled;       // Raise LCD interrupt if PPU is searching OAM?
    private boolean vblankCheckEnabled;    // Raise LCD interrupt if PPU is in VBlank?
    private boolean hblankCheckEnabled;    // Raise LCD interrupt if PPU is in HBlank?

    /* Each tile in the bitmap buffer is made up of 16
       bytes (2 per 8px row, each one plane of a bitplane).

       The two planes combine to form a 2-bit index into the
       background palette (first byte = low bits, second
       byte = high bits). e.g.:

       .33333..                .33333.. -> 01111100 -> $7C
       22...22.         |\                 01111100 -> $7C
       11...11.   ------| \    22...22. -> 00000000 -> $00
       2222222.   ------| /                11000110 -> $C6
       33...33.         |/     11...11. -> 11000110 -> $C6
       22...22.                            00000000 -> $00
       11...11.                2222222. -> 00000000 -> $00
       ........                            11111110 -> $FE
                               33...33. -> 11000110 -> $C6
                                           11000110 -> $C6
                               22...22. -> 00000000 -> $00
                                           11000110 -> $C6
                               11...11. -> 11000110 -> $C6
                                           00000000 -> $00
                               ........ -> 00000000 -> $00
                                           00000000 -> $00 */
    private MemoryBuffer tileBitmaps;

    // Buffer indices correspond to on-screen location. Values are tile numbers
    private MemoryBuffer bgTileMap1, bgTileMap2;

    /* Palettes: PP PP PP PP
                 || || || ||
                 || || || ++----- Palette for color 0
                 || || ++-------- Palette for color 1
                 || ++----------- Palette for color 2
                 ++-------------- Palette for color 3 */
    private WriteOnlyRegister bgPalette, sprPalette1, sprPalette2;

    /* Object attribute memory (sprites; 4 bytes/sprite)
           Byte 0: Sprite Y position
           Byte 1: Sprite X position
           Byte 2: Tile number (in bitmap buffer)
           Byte 3: flags: PPPPxxxx
                          ||||
                          |||+------ Palette number (1: use sprPalette1, 0: use sprPalette2)
                          ||+------- X flip enable
                          |+-------- Y flip enable
                          +--------- Sprite priority (1: in front of window, 0: between bg and window) */
    private MemoryBuffer oam;
    private OAMDMARegister oamdma;

    public LCD(GameBoy gb) {
        this.gb = gb;

        tileBitmaps = new MemoryBuffer(0x1800, 0x8000, ~0);
        bgTileMap1 = new MemoryBuffer(0x400, 0x9800, ~0);
        bgTileMap2 = new MemoryBuffer(0x400, 0x9C00, ~0);
        oamdma = new OAMDMARegister();
        oam = new MemoryBuffer(0xA0, 0xFE00, ~0);
        framebuffer = new byte[160][144];
        lcdControl = new LCDControlRegister();
        lcdStatus = new LCDStatusRegister();
        scanline = new MappableByte();
        cmpScanline = new MappableByte();
        scrollX = new MappableByte();
        scrollY = new MappableByte();
        windowX = new MappableByte();
        windowY = new MappableByte();
        bgPalette = new WriteOnlyRegister();
        sprPalette1 = new WriteOnlyRegister();
        sprPalette2 = new WriteOnlyRegister();
        reset();
    }

    public void reset() {
        // Effective bootrom output
        lcdEnabled = true;
        windowTileMapStart = 0x9800;
        windowEnabled = false;
        bgTilesetStart = 0x8000;
        bgTileMapStart = 0x9800;
        tallSpritesEnabled = false;
        spritesEnabled = false;
        bgEnabled = true;

        scanlineCheckEnabled = false;
        scanline.data = 0;
        cmpScanline.data = 0;
        oamCheckEnabled = false;
        vblankCheckEnabled = false;
        hblankCheckEnabled = false;

        scrollX.data = 0;
        scrollY.data = 0;
        windowX.data = 0;
        windowY.data = 0;

        bgPalette.data = (byte)0xFC;
        sprPalette1.data = (byte)0xFF;
        sprPalette2.data = (byte)0xFF;

        // Start rendering from the top left of the frame
        cycle = 0;
        screenState = ScreenState.OAM_SEARCH;
    }

    private MemoryMappable dispatchAddress(char address) {
        if (address >= 0x8000 && address <= 0x97FF)
            return tileBitmaps;
        else if (address >= 0x9800 && address <= 0x9BFF)
            return bgTileMap1;
        else if (address >= 0x9C00 && address <= 0x9FFF)
            return bgTileMap2;
        else if (address >= 0xFE00 && address <= 0xFE9F)
            return oam;
        else switch (address) {
            case 0xFF40:
                return lcdControl;
            case 0xFF41:
                return lcdStatus;
            case 0xFF42:
                return scrollY;
            case 0xFF43:
                return scrollX;
            case 0xFF44:
                return scanline;
            case 0xFF45:
                return cmpScanline;
            case 0xFF46:
                return oam;
            case 0xFF47:
                return bgPalette;
            case 0xFF48:
                return sprPalette1;
            case 0xFF49:
                return sprPalette2;
            case 0xFF4A:
                return windowY;
            case 0xFF4B:
                return windowX;
        }
        return null;
    }

    public byte read(char address) {
        MemoryMappable dest = dispatchAddress(address);
        // DMA and palette registers are write-only
        if (dest == null)
            throw new IllegalArgumentException(String.format("Invalid LCD I/O read address ($%04X)", (int)address));
        return dispatchAddress(address).read(address);
    }

    public void write(char address, byte value) {
        MemoryMappable dest = dispatchAddress(address);
        if (dest == null)
            throw new IllegalArgumentException(String.format("Invalid LCD I/O write address ($%04X)", (int)address));
        else
            dispatchAddress(address).write(address, value);
    }

    private void setScreenState(ScreenState state) {
        // VRAM disabled while being accessed by the video controller
        boolean vramEnabled = (state != ScreenState.DATA_TRANSFER);
        oam.setEnabled(state != ScreenState.OAM_SEARCH);
        tileBitmaps.setEnabled(vramEnabled);
        bgTileMap1.setEnabled(vramEnabled);
        bgTileMap2.setEnabled(vramEnabled);
        screenState = state;

        if (state == ScreenState.VBLANK) {
            gb.cpu.raiseInterrupt(CPU.Interrupt.VBLANK);
            if (vblankCheckEnabled) {
                gb.cpu.raiseInterrupt(CPU.Interrupt.LCD);
                return;
            }
        }

        // Raise LCD status interrupt if appropriate
        if ((hblankCheckEnabled && state == ScreenState.HBLANK) ||
            (oamCheckEnabled && state == ScreenState.OAM_SEARCH)) {
            gb.cpu.raiseInterrupt(CPU.Interrupt.LCD);
        }
    }

    public void tick() {
        // TODO: implement state behavior!
        switch (screenState) {
            case OAM_SEARCH:
                // TODO: Scan OAM for first 10 sprites appearing on this scanline
                break;

            case DATA_TRANSFER:
                /* TODO: VRAM accesses (fetch and render tiles to framebuffer)
                   This should be probably implemented on a pixel per pixel basis.
                   If it is not, game effects that occur mid-scanline or mid-tile
                   may not display correctly.

                   Each memory access takes 2 cycles (first the tile number
                   is fetched, then the low and high bit planes). More fetches are
                   needed if the window is being used. */
                break;
        }

        // 456 cycles/scanline, 154 scanlines/frame
        cycle = (char)((cycle + 1) % 456);

        if (cycle == 0) {
            // Just finished the line. Move to the next one
            scanline.data = (byte)((scanline.data + 1) % 154);

            // Did rendering just enter VBlank?
            if (scanline.data == (byte)144)
                setScreenState(ScreenState.VBLANK);

            if (scanlineCheckEnabled && scanline.data == cmpScanline.data)
                gb.cpu.raiseInterrupt(CPU.Interrupt.LCD);
        }

        if ((scanline.data & 0xFF) < 144) {
            // Rendering is on the visible scanlines (not in VBlank). Transition screen states
            switch (cycle) {
                case 0:
                    setScreenState(ScreenState.OAM_SEARCH);
                    break;
                case 80:
                    setScreenState(ScreenState.DATA_TRANSFER);
                    break;
                case 252:
                    setScreenState(ScreenState.HBLANK);
                    break;
            }
        }
    }

    private class LCDControlRegister implements MemoryMappable {
        /* Used to configure LCD settings
        reg: LMWT GZSB
             |||| ||||
             |||| |||+----- BG enabled (0: disabled, 1: enabled)
             |||| ||+------ Sprites enabled (0: disabled, 1: enabled)
             |||| |+------- Sprite size (0: 8x8, 1:8x16)
             |||| +-------- BG tile map (0: $9800-$9BFF, 1: $9C00-$9FFF)
             |||+---------- BG global tileset (applies to window as well; 0: $8800-$97FF, 1: $8000-$8FFF)
             ||+----------- Window enabled (0: disabled, 1: enabled)
             |+------------ Window tile map (0: $9800-$9BFF, 1: $9C00-$9FFF)
             +------------- LCD power (0: disabled, 1: enabled). On CGB this is BG/window master priority */

        @Override
        public byte read(char address) {
            return (byte)(((lcdEnabled ? 1 : 0) << 7) |
                          ((windowTileMapStart == 0x9C00 ? 1 : 0) << 6) |
                          ((windowEnabled ? 1 : 0) << 5) |
                          ((bgTilesetStart == 0x8000 ? 1 : 0) << 4) |
                          ((bgTileMapStart == 0x9C00 ? 1 : 0) << 3) |
                          ((tallSpritesEnabled ? 1 : 0) << 2) |
                          ((spritesEnabled ? 1 : 0) << 1) |
                          ((bgEnabled ? 1 : 0) << 1));
        }

        @Override
        public void write(char address, byte value) {
            lcdEnabled = ((value & 0x80) != 0);
            windowTileMapStart = (char) (0x9800 + ((value & 0x40) * 0x10));
            windowEnabled = ((value & 0x20) != 0);
            bgTilesetStart = (char) (0x8800 - ((value & 0x10) * 0x80));
            bgTileMapStart = (char) (0x9800 + ((value & 8) * 0x80));
            tallSpritesEnabled = ((value & 4) != 0);
            spritesEnabled = ((value & 2) != 0);
            bgEnabled = ((value & 1) != 0);
        }
    }

    private class LCDStatusRegister implements MemoryMappable {
        /* Used to configure the LCD interrupt
        reg: xLOV HCMM
              ||| ||||
              ||| ||++----- Screen mode (0: HBlank, 1: HBlank, 2: Searching OAM, 3: transferring data to LCD)
              ||| ||        Read only. 0 when LCD is off
              ||| |+------- LY=LYC comparison signal (read only)
              ||| +-------- Mode 0 HBLANK check enable (0: disabled, 1: enabled)
              ||+---------- Mode 1 VBLANK check enable (0: disabled, 1: enabled)
              |+----------- Mode 2 OAM check enable (0: disabled, 1: enabled)
              +------------ LY=LYC check enable (0: disabled, 1: enabled) */

        @Override
        public byte read(char address) {
            return (byte)(0x80 |
                          ((scanlineCheckEnabled ? 1 : 0) << 6) |
                          ((oamCheckEnabled ? 1 : 0) << 5) |
                          ((vblankCheckEnabled ? 1 : 0) << 4) |
                          ((hblankCheckEnabled ? 1 : 0) << 3) |
                          ((scanline == cmpScanline ? 1 : 0) << 2) |
                          (lcdEnabled ? (screenState.getStateCode() & 3) : 0));
        }

        @Override
        public void write(char address, byte value) {
            scanlineCheckEnabled = ((value & 0x40) != 0);
            oamCheckEnabled = ((value & 0x20) != 0);
            vblankCheckEnabled = ((value & 0x10) != 0);
            hblankCheckEnabled = ((value & 8) != 0);
        }
    }

    private static class WriteOnlyRegister extends MappableByte {
        @Override
        public byte read(char address) {
            System.err.println(String.format("Read made to write-only location ($%04X)", (int)address));
            return 0;
        }
    }

    private class OAMDMARegister extends WriteOnlyRegister {
        /* Used to write in bulk to OAM */

        @Override
        public void write(char address, byte value) {
            // Copy 160 bytes from (written value * 256) into OAM
            char src = (char) (value << 8);
            for (int i = 0; i < 0xA0; ++i)
                oam.data[i] = (byte)(gb.mmu.read8(src));
        }
    }
}
