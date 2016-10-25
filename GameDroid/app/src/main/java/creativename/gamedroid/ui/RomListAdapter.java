package creativename.gamedroid.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import creativename.gamedroid.R;

/* Handles the translation of data wrt formatting the ListView entries of the ROM list */

public class RomListAdapter extends ArrayAdapter<RomEntry> {

    public RomListAdapter(Context context, ArrayList<RomEntry> roms) {
        super(context, 0, roms);
    }

    @Override
    public View getView(int pos, View v, ViewGroup parent) {
        // Use ROM metadata for list entry
        RomEntry rom = getItem(pos);

        // Recycle view
        if (v == null)
            v = LayoutInflater.from(getContext()).inflate(R.layout.entry_rom, parent, false);

        TextView title = (TextView) v.findViewById(R.id.rom_title);
        TextView date = (TextView) v.findViewById(R.id.last_played);
        title.setText(rom.getTitle());
        date.setText(
            String.format(getContext().getString(R.string.rom_date_format),
                    new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US).format(rom.getLastPlayed()))
        );

        return v;
    }
}