package creativename.gamedroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import creativename.gamedroid.R;

import static android.app.Activity.RESULT_OK;

/* A list of ROMs with a given sorting */
public class RomListFragment extends Fragment {

    public enum SortingMode {
        RECENT,
        ALPHABETICAL,
        FAVORITE
    }

    private SortingMode sortMode;

    public RomListFragment() {
    }

    public static RomListFragment newInstance(SortingMode mode) {
        RomListFragment fragment = new RomListFragment();
        Bundle args = new Bundle();
        args.putSerializable("sort_mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

    private void sortRomList() {
        View v = getView();
        if (v != null) {
            RomListAdapter adapter = (RomListAdapter) ((ListView) (getView().findViewById(R.id.library_list))).getAdapter();
            // Sorts the ROM list according to sorting mode (library tab)
            switch (sortMode) {
                case RECENT: {
                    // Sort by last played date
                    adapter.sort(new Comparator<RomEntry>() {
                        @Override
                        public int compare(RomEntry o1, RomEntry o2) {
                            Date d1 = o1.lastPlayed;
                            Date d2 = o2.lastPlayed;

                            if (d1 != null && d2 != null)
                                return d2.compareTo(d1);
                            else if (d1 == null && d2 != null)
                                return 1;
                            else if (d1 != null)
                                return -1;
                            else
                                return 0;
                        }
                    });
                    break;
                }
                case ALPHABETICAL: {
                    // Sort by game title
                    adapter.sort(new Comparator<RomEntry>() {
                        @Override
                        public int compare(RomEntry o1, RomEntry o2) {
                            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
                        }
                    });
                    break;
                }
                case FAVORITE: {
                    // Filter favorites
                    ArrayList<RomEntry> tmp = new ArrayList<>();
                    for (int i = 0; i < adapter.getCount(); ++i) {
                        RomEntry r = adapter.getItem(i);
                        if (r != null && r.isFavorite)
                            tmp.add(r);
                    }
                    adapter.clear();
                    adapter.addAll(tmp);
                    break;
                }
            }
        }
    }

    public void refresh(int removedIdx) {
        // Refresh ListView (and other pages if fragment is inside a ViewPager)
        ViewPager vp = (ViewPager) getActivity().findViewById(R.id.container);
        View v = getView();
        if (vp != null)
            vp.getAdapter().notifyDataSetChanged();
        else if (v != null) {
            /* The ROM list that is displayed is a copy of the ROM cache. This
               is done so that when different ROM lists are sorted, they won't
               affect each other.

               Because of this, if a ROM has been deleted and this fragment is
               not in a viewpager (i.e., it is the search view), refresh() will
               not recreate the fragment, and the deleted ROM will not be removed.

               The ROM is removed manually in these cases */
            ListView listView = (ListView) v.findViewById(R.id.library_list);
            RomListAdapter adapter = ((RomListAdapter)listView.getAdapter());
            if (removedIdx > -1) {
                RomEntry r = adapter.getItem(removedIdx);
                adapter.remove(r);
            }
            adapter.notifyDataSetChanged();
        }
    }

    public void refresh() {
        // Refresh without removing a ROMEntry
        refresh(-1);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Populate list with ROM entries from cache
        View rootView = inflater.inflate(R.layout.fragment_romlist, container, false);
        Bundle args = getArguments();
        if (args != null)
            sortMode = (SortingMode) args.getSerializable("sort_mode");
        else
            sortMode = SortingMode.ALPHABETICAL;
        ArrayList<RomEntry> romList = RomCache.getInstance(getContext()).romList;

        final ListView listView = (ListView) rootView.findViewById(R.id.library_list);
        listView.setAdapter(new RomListAdapter(getContext(), romList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RomEntry rom = (RomEntry) listView.getItemAtPosition(position);
                if (view.getId() == R.id.favorite) {
                    rom.isFavorite = !rom.isFavorite;
                    RomCache.getInstance(getContext()).updateRomMetadata(rom);
                    refresh();
                } else {
                    Intent i = new Intent(inflater.getContext(), EmulatorActivity.class);
                    i.putExtra("rom_path", rom.getPath());
                    i.putExtra("rom_title", rom.getTitle());
                    startActivityForResult(i, 0);
                }
            }
        });
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Show ROM info
                RomEntry rom = (RomEntry) listView.getItemAtPosition(position);
                RomInfoFragment infoDialog = RomInfoFragment.newInstance(rom);
                infoDialog.getArguments().putInt("rom_index", position);
                infoDialog.setTargetFragment(RomListFragment.this, 0);
                infoDialog.show(getFragmentManager(), "fragment_rom_info");
                return true;
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        sortRomList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        View v = getView();
        if (resultCode == RESULT_OK && v != null) {
            /* Update "last played" date for the game that was just played.
               Applies to all RomEntries with the same title (i.e., in the case that
               the user has two software revisions of the same game in their library) */
            String title = data.getStringExtra("rom_title");
            ListView listView = (ListView) v.findViewById(R.id.library_list);
            for (int i = 0; i < listView.getCount(); ++i) {
                RomEntry rom = (RomEntry)listView.getItemAtPosition(i);
                if (rom.getTitle().equals(title)) {
                    rom.lastPlayed = new Date();
                    RomCache.getInstance(getContext()).updateRomMetadata(rom);
                }
            }
        }
    }
}