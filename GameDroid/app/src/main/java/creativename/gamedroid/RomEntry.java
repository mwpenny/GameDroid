// ##############################################################################################

package creativename.gamedroid;

// ##############################################################################################

public class RomEntry
{

    private String title = "";
    private String last_play_date = "";
    private String image_source = "";

    RomEntry(String new_title, String play_date)
    {

        title = new_title;
        last_play_date = play_date;

    }

    public String get_title()
    {
        return title;
    }

    public String get_last_play_date()
    {
        return last_play_date;
    }

    public void set_source(String new_source)
    {
        image_source = new_source;
    }

} // end : RomEntry

// ##############################################################################################
