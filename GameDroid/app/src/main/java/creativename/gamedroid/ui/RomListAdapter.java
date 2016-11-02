package creativename.gamedroid.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import creativename.gamedroid.R;

/* Handles the translation of data wrt formatting the ListView entries of the ROM list */
public class RomListAdapter extends ArrayAdapter<RomEntry> {
    private static final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public RomListAdapter(Context context, ArrayList<RomEntry> roms) {
        super(context, 0, roms);
    }

    @Override
    public View getView(final int pos, View v, final ViewGroup parent) {
        // Use ROM metadata for list entry
        final RomEntry rom = getItem(pos);

        // Recycle view
        if (v == null)
            v = LayoutInflater.from(getContext()).inflate(R.layout.entry_rom, parent, false);

        ImageView favBtn = (ImageView)v.findViewById(R.id.favorite);
        favBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rom != null) {
                    // Propogate favorite button click up to list entry
                    ((ListView)parent).performItemClick(v, pos, getItemId(pos));
                }
            }
        });

        // Use ROM metadata in view
        if (rom != null) {
            TextView title = (TextView) v.findViewById(R.id.rom_title);
            TextView date = (TextView) v.findViewById(R.id.last_played);
            title.setText(rom.getTitle());

            if (rom.getLastPlayed() != null) {
                date.setText(
                        String.format(getContext().getString(R.string.rom_date_format),
                                iso8601DateFormat.format(rom.getLastPlayed()))
                );
            } else {
                date.setText(
                        String.format(getContext().getString(R.string.rom_date_format), "Never")
                );
            }

            // Apply color if favorite
            int color = rom.isFavorite ? R.color.favorite_selected : R.color.favorite_unselected;
            favBtn.getDrawable().setColorFilter(ContextCompat.getColor(getContext(), color), PorterDuff.Mode.SRC_IN);
        }

        return v;
    }
}