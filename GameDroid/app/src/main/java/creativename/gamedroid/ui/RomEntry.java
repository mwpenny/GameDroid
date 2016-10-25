package creativename.gamedroid.ui;

import java.util.Date;

/* Data structure for a game ROM's metadata */
public class RomEntry {
    private String path;
    private String title;
    private String manufacturer;
    private String licensee;
    private String locale;
    private int version;
    private Date lastPlayed;
    private String imagePath;

    /* Getters */
    public String getPath() { return path; }
    public String getTitle() { return title; }
    public String getManufacturer() { return manufacturer; }
    public String getLicensee() { return licensee; }
    public String getLocale() { return locale; }
    public int getVersion() { return version; }
    public Date getLastPlayed() { return lastPlayed; }
    public String getImagePath() { return imagePath; }

    RomEntry(String path, String title, String manufacturer, String licensee, String locale, int version) {
        this.path = path;
        this.title = title;
        this.manufacturer = manufacturer;
        this.licensee = licensee;
        this.locale = locale;
        this.version = version;

        // TODO: actual values for these
        lastPlayed = new Date();
        imagePath = "";
    }
}
