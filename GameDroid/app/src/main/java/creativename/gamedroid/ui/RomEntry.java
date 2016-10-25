package creativename.gamedroid.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import creativename.gamedroid.core.Cartridge;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/* Data structure for a game ROM's metadata */
public class RomEntry implements Parcelable {
    private String path;
    private String title;
    private String manufacturer;
    private String licensee;
    private String locale;
    private int version;
    private Date lastPlayed;
    private String imagePath;
    private boolean favorite;

    /* Getters */
    public String getPath() { return path; }
    public String getTitle() { return title; }
    public String getManufacturer() { return manufacturer; }
    public String getLicensee() { return licensee; }
    public String getLocale() { return locale; }
    public int getVersion() { return version; }
    public Date getLastPlayed() { return lastPlayed; }
    public String getImagePath() { return imagePath; }
    public boolean isFavorite() { return favorite; }

    /* Converts a string to title case (e.g., "pokemon gold" => "Pokemon Gold") */
    private static String toTitleCase(String input) {
        StringBuilder title = new StringBuilder();
        boolean nextWord = true;
        input = input.replace('_', ' ');

        for (char c : input.toCharArray()) {
            if (nextWord) {
                c = Character.toUpperCase(c);
                nextWord = false;
            } else if (Character.isSpaceChar(c)) {
                nextWord = true;
            } else {
                c = Character.toLowerCase(c);
            }
            title.append(c);
        }
        return title.toString();
    }

    RomEntry(Parcel in) {
        path = in.readString();
        title = in.readString();
        manufacturer = in.readString();
        licensee = in.readString();
        locale = in.readString();
        version = in.readInt();
        lastPlayed = new Date(in.readLong());
        imagePath = in.readString();
        favorite = (in.readByte() != 0);
    }

    public RomEntry (File rom, SQLiteDatabase cache) throws IOException {
        // Search cache for metadata (use filename as key)
        Cursor c = cache.query("roms", null, "fileName=?", new String[]{rom.getName()},
                               null, null, null);
        try {
            if (c.getCount() == 0) {
                // ROM metadata is not in the cache: parse file for it
                Cartridge game = new Cartridge(rom.getAbsolutePath(), Cartridge.LoadMode.PARSE_ONLY);
                title = toTitleCase(game.getTitle());
                manufacturer = game.getManufacturer();
                licensee = game.getLicensee();
                locale = game.getLocale() == Cartridge.GameLocale.JAPAN ? "Japan" : "World";
                version = game.getGameVersion();

                // Cache parsed data for next time
                ContentValues row = new ContentValues();
                row.put("fileName", rom.getName());
                row.put("title", title);
                row.put("manufacturer", manufacturer);
                row.put("licensee", licensee);
                row.put("locale", locale);
                row.put("version", version);
                cache.insert("roms", null, row);
            } else {
                // Load ROM metadata from cache
                c.moveToFirst();
                title = c.getString(c.getColumnIndex("title"));
                manufacturer = c.getString(c.getColumnIndex("manufacturer"));
                licensee = c.getString(c.getColumnIndex("licensee"));
                locale = c.getString(c.getColumnIndex("locale"));
                version = c.getInt(c.getColumnIndex("version"));
            }
        } finally {
            c.close();
        }

        // TODO: actual values for these
        path = rom.getAbsolutePath();
        lastPlayed = new Date();
        imagePath = "";
        favorite = false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(title);
        dest.writeString(manufacturer);
        dest.writeString(licensee);
        dest.writeString(locale);
        dest.writeInt(version);
        dest.writeLong(lastPlayed.getTime());
        dest.writeString(imagePath);
        dest.writeByte((byte)(favorite ? 1: 0));
    }

    public static final Creator CREATOR = new Parcelable.Creator() {
        public RomEntry createFromParcel(Parcel in) {
            return new RomEntry(in);
        }

        public RomEntry[] newArray(int size) {
            return new RomEntry[size];
        }
    };
}
