// #################################################################################################

package creativename.gamedroid;

// #################################################################################################

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.content.Context;

import java.util.ArrayList;

// #################################################################################################

// RomListAdapter Class
// --------------------
// This class handles the translation of data wrt formatting the ListView entries of the ROM list

// #################################################################################################

public class RomListAdapter extends ArrayAdapter<RomEntry>
{

    public RomListAdapter(Context context, ArrayList<RomEntry> curr_list)
    {
        super(context, 0, curr_list);
    }

    // #################################################################################################

    @Override
    public View getView(int pos, View curr_view, ViewGroup parent)
    {

        RomEntry curr_rom = getItem(pos);

        if (curr_view == null)
        {
            curr_view = LayoutInflater.from(getContext()).inflate(R.layout.rom_layout, parent, false);
        }

        // Assign data to fields [@+id/rom_title && @+id/last_played]
        TextView curr_title = (TextView) curr_view.findViewById(R.id.rom_title);
        TextView curr_play_date = (TextView) curr_view.findViewById(R.id.last_played);

        // Send data to ViewObject from Object
        curr_title.setText(curr_rom.get_title());
        curr_play_date.setText(curr_rom.get_last_play_date());

        return curr_view;

    } // end : getView

    // #################################################################################################

} // end : RomListAdapter

// #################################################################################################