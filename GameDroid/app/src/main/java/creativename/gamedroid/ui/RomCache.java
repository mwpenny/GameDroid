package creativename.gamedroid.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import creativename.gamedroid.core.Cartridge;

/* Singleton for ROM metadata cache database access */
public class RomCache {
    private static final String DB_NAME = "romcache.db";
    private static final String TABLE_NAME = "roms";

    private static final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");;
    private static RomCache instance;

    private SQLiteOpenHelper opener;
    public ArrayList<RomEntry> romList;

    /* Converts a string to title case (e.g., "pokemon gold" => "Pokemon Gold") */
    private static String toTitleCase(String input) {
        StringBuilder title = new StringBuilder();
        boolean nextWord = true;
        input = input.replace('_', ' ');

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c) || c == '.' || c == '-' || c == ':') {
                nextWord = true;
            } else if (nextWord) {
                c = Character.toUpperCase(c);
                nextWord = false;
            } else {
                c = Character.toLowerCase(c);
            }
            title.append(c);
        }
        return title.toString();
    }

    /* Creates a Date object from an ISO-8601 formatted string */
    private static Date dateFromString(String dateString) {
        if (!dateString.isEmpty()) {
            try {
                return iso8601DateFormat.parse(dateString);
            } catch (ParseException ex) {
                System.err.format("Date could not be parsed: %s\n", dateString);
            }
        }
        return null;
    }

    private RomCache(Context context) {
        opener = new SQLiteOpenHelper(context.getApplicationContext(), DB_NAME, null, 2) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n" +
                            "fileName	    TEXT PRIMARY KEY NOT NULL,\n" +
                            "title		    TEXT,\n" +
                            "licensee	    TEXT,\n" +
                            "locale		    TEXT,\n" +
                            "version		INT,\n" +
                            "lastPlayed	    TEXT,\n" +
                            "isFavorite	    INT)");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }

            @Override
            public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                onUpgrade(db, oldVersion, newVersion);
            }
        };
    }

    public static synchronized RomCache getInstance(Context context) {
        if (instance == null)
            instance = new RomCache(context.getApplicationContext());
        return instance;
    }

    /* Removes ROM metadata from the cache for files that are no longer present */
    private static void clean(File[] files, SQLiteDatabase cache) {
        cache.beginTransaction();

        if (files.length == 0) {
            // All ROM files were removed
            cache.execSQL("DELETE FROM " + TABLE_NAME);
        } else {
            cache.execSQL("CREATE TEMP TABLE IF NOT EXISTS foundFiles (fileName TEXT PRIMARY KEY NOT NULL)");
            ContentValues row = new ContentValues();
            for (File f : files) {
                row.put("fileName", f.getName());
                if (cache.insert("foundFiles", null, row) == -1) {
                    // Don't compromise ROM cache if construction of foundFiles table fails
                    cache.endTransaction();
                    return;
                }
            }
            cache.execSQL("DELETE FROM " + TABLE_NAME + " WHERE fileName NOT IN (SELECT fileName from foundFiles)");
            cache.execSQL("DROP TABLE IF EXISTS foundFiles");
        }
        cache.setTransactionSuccessful();
        cache.endTransaction();
    }

    /* Updates or inserts a ROM metadata entry */
    private static boolean upsertMetadata(RomEntry rom, SQLiteDatabase cache) {
        ContentValues row = new ContentValues();
        boolean success;
        String dateString = (rom.lastPlayed != null) ? iso8601DateFormat.format(rom.lastPlayed) : "";
        String fileName = (new File(rom.getPath())).getName();

        row.put("fileName", fileName);
        row.put("title", rom.getTitle());
        row.put("licensee", rom.getLicensee());
        row.put("locale", rom.getLocale());
        row.put("version", rom.getVersion());
        row.put("lastPlayed", dateString);
        row.put("isFavorite", rom.isFavorite);

        // Insert or update depending on what's in the cache already
        if (cache.query(TABLE_NAME, null, "fileName=?", new String[]{fileName},
                null, null, null).getCount() == 0) {
            success = (cache.insert(TABLE_NAME, null, row) != -1);
        } else {
            success = (cache.update(TABLE_NAME, row, "fileName=?", new String[]{fileName}) == 1);
        }

        if (!success)
            System.err.format("Could not update metadata for '%s'\n", fileName);

        return success;
    }

    /* Retrieves ROM metadata from the cache. If not present, metadata is loaded
       from disk and then cached for next time */
    private static RomEntry getRomMetadata(File f, SQLiteDatabase cache) throws IOException {
        // Search cache for metadata (use filename as key)
        Cursor c = cache.query(TABLE_NAME, null, "fileName=?", new String[]{f.getName()},
                               null, null, null);

        String title, licensee, locale;
        Date lastPlayed;
        boolean favorite;
        int version;
        RomEntry rom;

        try {
            if (c.getCount() == 0) {
                // ROM metadata is not in the cache: parse file for it
                Cartridge game = new Cartridge(f.getAbsolutePath(), Cartridge.LoadMode.PARSE_ONLY);
                title = toTitleCase(game.getTitle());
                licensee = game.getLicensee();
                locale = game.getLocale() == Cartridge.GameLocale.JAPAN ? "Japan" : "World";
                version = game.getGameVersion();
                lastPlayed = null;
                favorite = false;
            } else {
                // Load ROM metadata from cache
                c.moveToFirst();
                title = c.getString(c.getColumnIndex("title"));
                licensee = c.getString(c.getColumnIndex("licensee"));
                locale = c.getString(c.getColumnIndex("locale"));
                version = c.getInt(c.getColumnIndex("version"));
                lastPlayed = dateFromString(c.getString(c.getColumnIndex("lastPlayed")));
                favorite = (c.getInt(c.getColumnIndex("isFavorite")) != 0);
            }
            if (title.isEmpty())
                title = f.getName();
            rom = new RomEntry(f.getAbsolutePath(), title, licensee, locale, version, lastPlayed, favorite);

            // Cache parsed data for next time
            if (c.getCount() == 0)
                upsertMetadata(rom, cache);
        } finally {
            c.close();
        }

        return rom;
    }

    /* Updates a ROM's metadata in the cache */
    public boolean updateRomMetadata(RomEntry rom) {
        SQLiteDatabase cache = opener.getWritableDatabase();
        boolean success = upsertMetadata(rom, cache);
        cache.close();
        return success;
    }

    /* Retrieves ROM metadata for every ROM file present in a directory */
    public void populateCache(File romDir) {
        ArrayList<RomEntry> romList = new ArrayList<>();
        SQLiteDatabase cache = opener.getWritableDatabase();
        cache.beginTransaction();

        // Search ROM directory for GameBoy and GameBoy color games
        for (File f : romDir.listFiles()) {
            String name = f.getName();
            String ext = name.substring(name.lastIndexOf('.')).toLowerCase();
            if (f.isFile() && (ext.equals(".gb") || ext.equals(".gbc"))) {
                try {
                    romList.add(getRomMetadata(f, cache));
                } catch (IOException e) {
                    // Likely due to an invalid ROM file
                    System.err.format("Could not load metadata for '%s': %s.\n", name, e.getMessage());
                }
            }
        }

        // Remove old cache entries
        clean(romDir.listFiles(), cache);
        cache.setTransactionSuccessful();
        cache.endTransaction();
        cache.close();
        this.romList = romList;
    }
}
