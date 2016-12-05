package creativename.gamedroid.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;

import creativename.gamedroid.R;

/* Handles the translation of data wrt formatting the ListView entries of the ROM list */
public class RomListAdapter extends ArrayAdapter<RomEntry> {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
    private Filter filter;
    private ArrayList<RomEntry> romList;

    public RomListAdapter(Context context, ArrayList<RomEntry> roms) {
        // Make shallow copy so sorting has no effect on other instances
        super(context, 0, new ArrayList<>(roms));
        filter = new RomEntryFilter();
        romList = new ArrayList<>(roms);
    }

    @Override
    public void remove(RomEntry object) {
        super.remove(object);
        romList.remove(object);
    }

    @NonNull
    @Override
    public View getView(final int pos, View v, @NonNull final ViewGroup parent) {
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
                    // Propagate favorite button click up to list entry
                    ((ListView)parent).performItemClick(v, pos, getItemId(pos));
                }
            }
        });

        // Use ROM metadata in view
        if (rom != null) {
            TextView title = (TextView) v.findViewById(R.id.rom_title);
            TextView date = (TextView) v.findViewById(R.id.last_played);
            title.setText(rom.getTitle());

            if (rom.lastPlayed != null) {
                date.setText(
                        String.format(getContext().getString(R.string.rom_date_format),
                                dateFormat.format(rom.lastPlayed))
                );
            } else {
                date.setText(
                        String.format(getContext().getString(R.string.rom_date_format), "Never")
                );
            }

            // Apply color if favorite
            int favColor;
            int favImage;
            if (rom.isFavorite) {
                favColor = R.color.favorite_selected;
                favImage = R.drawable.ic_favorite;
            } else {
                favColor = R.color.favorite_unselected;
                favImage = R.drawable.ic_favorite_border;
            }
            favBtn.setImageResource(favImage);
            favBtn.getDrawable().setColorFilter(ContextCompat.getColor(getContext(), favColor), PorterDuff.Mode.SRC_IN);
        }

        return v;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }

    /* For searching through ROMs. Will search ALL ROMs (does not work with
       favorite view, but should never have to) */
    private class RomEntryFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence query) {
            FilterResults results = new FilterResults();

            // Filter ROM list by query string
            ArrayList<RomEntry> matched = new ArrayList<>();
            if (query != null && !query.toString().trim().isEmpty() && query.length() > 0) {
                for (int i = 0; i < romList.size(); ++i) {
                    RomEntry rom = romList.get(i);
                    if (rom.getTitle().toLowerCase().contains(query.toString().toLowerCase()))
                        matched.add(rom);
                }
            }
            results.count = matched.size();
            results.values = matched;
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            addAll((ArrayList<RomEntry>)results.values);
            sort(new Comparator<RomEntry>() {
                @Override
                public int compare(RomEntry o1, RomEntry o2) {
                    return o1.getTitle().compareToIgnoreCase(o2.getTitle());
                }
            });
            notifyDataSetChanged();
        }
    }
}