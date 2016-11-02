package creativename.gamedroid.ui;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/* Data structure for a game ROM's metadata */
public class RomEntry implements Parcelable {
    private String path;
    private String title;
    private String licensee;
    private String locale;
    private int version;
    public Date lastPlayed;
    public boolean isFavorite;

    private static final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* Accessors */
    public String getPath() { return path; }
    public String getTitle() { return title; }
    public String getLicensee() { return licensee; }
    public String getLocale() { return locale; }
    public int getVersion() { return version; }

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

    public RomEntry(Parcel in) {
        path = in.readString();
        title = in.readString();
        licensee = in.readString();
        locale = in.readString();
        version = in.readInt();
        lastPlayed = dateFromString(in.readString());
        isFavorite = (in.readByte() != 0);
    }

    public RomEntry(String path, String title, String licensee, String locale, int version, Date lastPlayed, boolean favorite) {
        this.path = path;
        this.title = title;
        this.licensee = licensee;
        this.locale = locale;
        this.version = version;
        this.lastPlayed = lastPlayed;
        this.isFavorite = favorite;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(title);
        dest.writeString(licensee);
        dest.writeString(locale);
        dest.writeInt(version);

        String dateString = (lastPlayed != null) ? iso8601DateFormat.format(lastPlayed) : "";
        dest.writeString(dateString);

        dest.writeByte((byte)(isFavorite ? 1: 0));
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
