package creativename.gamedroid.ui;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/* Data structure for a game ROM's metadata */
public class RomEntry {
    private String path;
    private String title;
    private String licensee;
    private String locale;
    private int version;
    public Date lastPlayed;
    public boolean isFavorite;

    /* Accessors */
    public String getPath() { return path; }
    public String getTitle() { return title; }
    public String getLicensee() { return licensee; }
    public String getLocale() { return locale; }
    public int getVersion() { return version; }

    public RomEntry(String path, String title, String licensee, String locale, int version, Date lastPlayed, boolean favorite) {
        this.path = path;
        this.title = title;
        this.licensee = licensee;
        this.locale = locale;
        this.version = version;
        this.lastPlayed = lastPlayed;
        this.isFavorite = favorite;
    }
}