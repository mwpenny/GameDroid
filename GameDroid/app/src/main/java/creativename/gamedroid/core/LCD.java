package creativename.gamedroid.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;

/* Provides GameBoy graphics data manipulation, processing, and output */
public class LCD implements MemoryMappable, Serializable {
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
            0xFFFFFFFF,  // 0% black
            0xFFC0C0C0,  // 33% black
            0xFF606060,  // 66% black
            0xFF000000   // 100% black
    };

    public transient GameBoy gb;
    public transient int[] framebuffer;
    private ScreenState screenState;
    private ScanlineRegister scanline;
    private MappableByte cmpScanline;
    private char remainingStateCycles;
    private MappableByte scrollX, scrollY;
    private MappableByte windowX, windowY;  // Window = BG layer that can overlay normal BG
    private Sprite[] foundSprites;

    private transient LCDControlRegister lcdControl;
    public boolean lcdEnabled;
    private char windowTileMapOfs;       // i.e., second tile map starts $400 bytes into buffer
    private boolean windowEnabled;
    private char bgTilesetOfs;           // i.e., second tileset starts $800 bytes into buffer
    private char bgTileMapOfs;           // i.e., second tile map starts $400 bytes into buffer
    private boolean tallSpritesEnabled;  // Whether or not to use 8x16 sprites
    private boolean spritesEnabled;
    private boolean bgEnabled;

    private transient LCDStatusRegister lcdStatus;
    private boolean scanlineCheckEnabled;  // Raise LCD interrupt if scanline=cmpScanline?
    private boolean oamCheckEnabled;       // Raise LCD interrupt if PPU is searching OAM?
    private boolean vblankCheckEnabled;    // Raise LCD interrupt if PPU is in VBlank?
    private boolean hblankCheckEnabled;    // Raise LCD interrupt if PPU is in HBlank?

    /* 6144 bytes (384 tiles) in size.

       Each tile is made up of 16 bytes (2 per 8px row,
       each one plane of a bitplane).

       The two planes combine to form a 2-bit index into the
       background/sprite palette (first byte = low bits, second
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

    /* 2 tile maps, each 1024 bytes in size (32x32 tiles). Buffer indices
       correspond to on-screen location. Values are tile numbers */
    private MemoryBuffer bgTileMaps;

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
                          |||+------ Palette number (1: use sprPalette2, 0: use sprPalette1)
                          ||+------- X flip enable
                          |+-------- Y flip enable
                          +--------- Sprite priority (1: in front of bg/window, 0: behind bg/window) */
    private MemoryBuffer oam;
    private transient OAMDMARegister oamdma;

    public LCD(GameBoy gb) {
        this.gb = gb;

        tileBitmaps = new MemoryBuffer(0x1800, 0x8000, ~0);
        bgTileMaps = new MemoryBuffer(0x800, 0x9800, ~0);
        oamdma = new OAMDMARegister();
        oam = new MemoryBuffer(0xA0, 0xFE00, ~0);
        framebuffer = new int[144*160];
        lcdControl = new LCDControlRegister();
        lcdStatus = new LCDStatusRegister();
        scanline = new ScanlineRegister();
        cmpScanline = new MappableByte();
        scrollX = new MappableByte();
        scrollY = new MappableByte();
        windowX = new MappableByte();
        windowY = new MappableByte();
        bgPalette = new WriteOnlyRegister();
        sprPalette1 = new WriteOnlyRegister();
        sprPalette2 = new WriteOnlyRegister();
        foundSprites = new Sprite[10];
        reset();
    }

    public void reset() {
        // Effective bootrom output
        lcdEnabled = true;
        windowTileMapOfs = 0;
        windowEnabled = false;
        bgTilesetOfs = 0;
        bgTileMapOfs = 0;
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

        for (int i = 0; i < foundSprites.length; ++i)
            foundSprites[i] = new Sprite();

        // Start rendering from the top left of the frame
        screenState = ScreenState.OAM_SEARCH;
        remainingStateCycles = 80;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        oamdma = new OAMDMARegister();
        framebuffer = new int[144*160];
        lcdControl = new LCDControlRegister();
        lcdStatus = new LCDStatusRegister();
    }

    private MemoryMappable dispatchAddress(char address) {
        if (address >= 0x8000 && address <= 0x97FF)
            return tileBitmaps;
        else if (address >= 0x9800 && address <= 0x9FFF)
            return bgTileMaps;
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
                return oamdma;
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
        bgTileMaps.setEnabled(vramEnabled);
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

    private void discoverSprites() {
        int sprHeight = tallSpritesEnabled ? 16 : 8;
        int oamPos = 0;
        int spritesFound = 0;

        // Find the first 10 sprites in memory on the current scanline
        while (spritesFound < foundSprites.length) {
            if (oamPos < oam.data.length) {
                int sprY = (oam.data[oamPos++] & 0xFF) - 16;
                int sl = scanline.data & 0xFF;
                // The first byte of sprite data in OAM is the Y coordinate, use it to check position
                if (sprY <= sl && sprY + sprHeight > sl) {
                    // Sprite is on the current scanline. Copy to sprite buffer
                    foundSprites[spritesFound++].update(sprY, (oam.data[oamPos++] & 0xFF) - 8,
                            oam.data[oamPos++], oam.data[oamPos++]);
                } else {
                    // Move to next sprite in OAM
                    oamPos += 3;
                }
            } else {
                // Clear remaining (unused) sprite slots
                foundSprites[spritesFound++].update(-16, -8, (byte)0, (byte)0);
            }
        }
    }

    private char getBitmapSliver(int tileNum, int row, int tableOfs) {
        // Get bitmap for row of tile (tiles are 8x8, and one 8px row is 2 bytes)
        int bitmapIdx = tableOfs + (tileNum * 16) + (2 * row);
        return (char)(((tileBitmaps.data[bitmapIdx] & 0xFF) << 8) |
                      (tileBitmaps.data[bitmapIdx + 1] & 0xFF));
    }

    private void renderLine() {
        /* Render current scanline to framebuffer

           Note: A /very/ small subset of games may use mid-scanline effects, which will
                 not be correctly displayed using this approach. We will disregard them
                 for this project, as its focus is primarily software design. */

        char bgTileRow = 0;
        int bgY = (scanline.data + scrollY.data) & 0xFF;
        int winY = (scanline.data - windowY.data) & 0xFF;

        for (int px = 0; px < 160; ++px) {
            int color = palette[0];
            int bgPaletteIdx = 0;
            int bgX = 0;
            int y = 0;
            char tileMapOfs = 0;
            boolean bgVisible;

            if (windowEnabled && (scanline.data & 0xFF) >= (windowY.data & 0xFF) && px >= ((windowX.data - 7) & 0xFF)) {
                // Show window
                bgX = (px - windowX.data + 7) & 0xFF;
                y = winY;
                tileMapOfs = windowTileMapOfs;
                bgVisible = true;
            } else if (bgEnabled) {
                // Show regular background
                bgX = (px + scrollX.data) & 0xFF;
                y = bgY;
                tileMapOfs = bgTileMapOfs;
                bgVisible = true;
            } else {
                color = palette[0];
                bgPaletteIdx = 0;
                bgVisible = false;
            }

            if (bgVisible) {
                // Latch next background/window tile
                int tileX = bgX % 8;
                if (px == 0 || tileX == 0) {
                    int tileNum = bgTileMaps.data[tileMapOfs + (bgX / 8) + (y / 8 * 32)] & 0xFF;

                    // Tile indices are treated as signed if using the second tile set
                    if (bgTilesetOfs == 0x800)
                        tileNum = (byte) tileNum + 128;

                    bgTileRow = getBitmapSliver(tileNum, y % 8, bgTilesetOfs);
                }

                // Fetch 2-bit palette index to render appropriate color
                bgPaletteIdx = ((bgTileRow >>> (15 - tileX)) & 1) | (((bgTileRow >>> (7 - tileX)) << 1) & 2);
                color = palette[(bgPalette.data >>> (bgPaletteIdx * 2)) & 3];
            }

            if (spritesEnabled) {
                for (Sprite s : foundSprites) {
                    if (px >= s.x && px < s.x + 8) {
                        int x = px - s.x;
                        int pIdx = ((s.sprTileRow >>> (15 - x)) & 1) |
                                    (((s.sprTileRow >>> (7 - x)) << 1) & 2);

                        // First sprite with non-transparent pixel gets drawn
                        if (pIdx != 0 && (bgPaletteIdx == 0 || s.hasFrontPriority)) {
                            color = palette[(s.palette.data >>> (pIdx * 2)) & 3];
                            break;
                        }
                    }
                }
            }
            framebuffer[(scanline.data & 0xFF) * 160 + px] = color;
        }
    }

    public void tick() {
        if (lcdEnabled) {
            if (--remainingStateCycles == 0) {
                // Current screen state ending: time to transition
                switch (screenState) {
                    case OAM_SEARCH:
                        discoverSprites();
                        setScreenState(ScreenState.DATA_TRANSFER);
                        remainingStateCycles = 172;
                        break;
                    case DATA_TRANSFER:
                        renderLine();
                        setScreenState(ScreenState.HBLANK);
                        remainingStateCycles = 204;
                        break;
                    case HBLANK:
                    case VBLANK:
                        // End of line
                        scanline.data = (byte)(((scanline.data & 0xFF) + 1) % 154);

                        if (scanlineCheckEnabled && scanline.data == cmpScanline.data)
                            gb.cpu.raiseInterrupt(CPU.Interrupt.LCD);

                        if ((scanline.data & 0xFF) < 144) {
                            // Rendering visible frame: move to next visible scanline
                            setScreenState(ScreenState.OAM_SEARCH);
                            remainingStateCycles = 80;
                        } else if (scanline.data == (byte) 144) {
                            // Rendering just entered VBlank
                            setScreenState(ScreenState.VBLANK);
                            gb.renderTarget.frameReady(framebuffer);
                        }

                        if (screenState == ScreenState.VBLANK)
                            remainingStateCycles = 456;
                        break;
                }
            }
        }
    }

    private static int flip(int b) {
        // Reverse the bits in an 8-bit integer
        b = ((b & 0xAA) >>> 1) | ((b & 0x55) << 1);
        b = ((b & 0xCC) >>> 2) | ((b & 0x33) << 2);
        return ((b >>> 4) | (b << 4)) & 0xFF;
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
                          ((windowTileMapOfs == 0x400 ? 1 : 0) << 6) |
                          ((windowEnabled ? 1 : 0) << 5) |
                          ((bgTilesetOfs == 0 ? 1 : 0) << 4) |
                          ((bgTileMapOfs == 0x400 ? 1 : 0) << 3) |
                          ((tallSpritesEnabled ? 1 : 0) << 2) |
                          ((spritesEnabled ? 1 : 0) << 1) |
                          ((bgEnabled ? 1 : 0)));
        }

        @Override
        public void write(char address, byte value) {
            windowTileMapOfs = (char) ((value & 0x40) * 0x10);
            windowEnabled = ((value & 0x20) != 0);
            bgTilesetOfs = (char) (0x800 - ((value & 0x10) * 0x80));
            bgTileMapOfs = (char) ((value & 8) * 0x80);
            tallSpritesEnabled = ((value & 4) != 0);
            spritesEnabled = ((value & 2) != 0);
            bgEnabled = ((value & 1) != 0);

            if (((value & 0x80) == 0)) {
                lcdEnabled = false;
                oam.setEnabled(true);
                tileBitmaps.setEnabled(true);
                bgTileMaps.setEnabled(true);
                screenState = ScreenState.HBLANK;
                scanline.data = 0;

                // Blank screen when disabled
                Arrays.fill(framebuffer, palette[0]);
                gb.renderTarget.frameReady(framebuffer);
            } else if (!lcdEnabled) {
                // LCD transitioning from disabled to enabled
                lcdEnabled = true;
                setScreenState(ScreenState.OAM_SEARCH);
                remainingStateCycles = 76;
            }
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
                           (screenState.getStateCode() & 3));
        }

        @Override
        public void write(char address, byte value) {
            scanlineCheckEnabled = ((value & 0x40) != 0);
            oamCheckEnabled = ((value & 0x20) != 0);
            vblankCheckEnabled = ((value & 0x10) != 0);
            hblankCheckEnabled = ((value & 8) != 0);
        }
    }

    private class ScanlineRegister extends MappableByte {
        @Override
        public void write(char address, byte value) {
            // Writing resets the scanline
            data = 0;
        }
    }

    private static class WriteOnlyRegister extends MappableByte {
        @Override
        public byte read(char address) {
            System.err.format("Read made to write-only location ($%04X)\n", (int)address);
            return (byte)0xFF;
        }
    }

    private class OAMDMARegister extends WriteOnlyRegister {
        /* Used to write in bulk to OAM */

        @Override
        public void write(char address, byte value) {
            // Copy 160 bytes from (written value * 256) into OAM
            char src = (char) (value << 8);
            for (int i = 0; i < 0xA0; ++i)
                oam.data[i] = (byte)(gb.mmu.read8((char) (src + i)));
        }
    }

    private class Sprite implements Serializable {
        public int x;
        public boolean hasFrontPriority;
        public char sprTileRow;
        public MappableByte palette;

        public void update(int y, int x, byte tileNum, byte flags) {
            this.x = x;
            hasFrontPriority = ((flags & 0x80) == 0);
            palette = ((flags & 0x10) == 0) ? sprPalette1 : sprPalette2;
            sprTileRow = getTileRow(tileNum, y, ((flags & 0x40) != 0), ((flags & 0x20) != 0));
        }

        private char getTileRow(byte tileNum, int y, boolean yflip, boolean xflip) {
            int row = (scanline.data & 0xFF) - y;
            char bmp;

            /* 8x16 mode. Bits 1-7 of the tile number select the top
               tile. The bottom tile is the next one in the tileset. */
            if (tallSpritesEnabled) {
                if (yflip)
                    row = 15 - row;
                tileNum = (byte)((tileNum & 0xFE) + row/8);
                row &= 7;
            } else if (yflip) {
                row = 7 - row;
            }
            bmp = getBitmapSliver(tileNum & 0xFF, row, 0);

            if (xflip)
                return (char)((flip((bmp >>> 8) & 0xFF) << 8) | flip(bmp & 0xFF));
            return bmp;
        }
    }
}
