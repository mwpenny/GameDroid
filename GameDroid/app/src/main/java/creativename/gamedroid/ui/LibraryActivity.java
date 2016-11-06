package creativename.gamedroid.ui;

import java.io.File;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.content.Intent;
import android.view.ViewGroup;
import android.os.Environment;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.ListView;
import android.widget.AdapterView;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentStatePagerAdapter;

import creativename.gamedroid.R;

/* Main game ROM library view */
public class LibraryActivity extends AppCompatActivity
{

    private AlertDialog romWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_library);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        SectionsPagerAdapter spa = new LibraryActivity.SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager vp = (ViewPager)findViewById(R.id.container);
        vp.setAdapter(spa);

        ((TabLayout)findViewById(R.id.tabs)).setupWithViewPager(vp);

        // No ROMs were found. Instruct user on how to add them
        if (RomCache.getInstance(this).romList.size() == 0) {
            String path = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_roms)).getAbsolutePath();
            romWarning = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_noroms_title))
                    .setMessage(String.format(getString(R.string.dialog_noroms_message), path))
                    .setPositiveButton(android.R.string.ok, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (romWarning != null && romWarning.isShowing())
            romWarning.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_library, menu);
        return true;
    }

   @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // app_bar settings
        // When the app_bar settings button is clicked it will display a dialog box with the
        // settings options for the User regarding preferences, ROM settings, etc...
        if (id == R.id.action_settings)
        {

            System.out.println("Clicked action_settings on app_bar");

            AlertDialog.Builder settingsDialog = new AlertDialog.Builder(this);
            settingsDialog.setTitle("Settings Menu");
            settingsDialog.setCancelable(true);

            // Inflate settings.XML file
            LayoutInflater inflater = this.getLayoutInflater();
            settingsDialog.setView(inflater.inflate(R.layout.settings, null));

            // Handler for Close button
            settingsDialog.setPositiveButton("Close", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    System.out.println("Clicked 'Close' in AlertDialog [Settings]");
                    dialog.cancel();
                }
            });

            // Build the AlertDialog and display it
            AlertDialog displaySettings = settingsDialog.create();
            displaySettings.show();

            return true;

        } // end : settings

        // app_bar search
        // When the app_bar search button is clicked it will displays a dialog box that will
        // prompt the user with a field to search for a specific ROM
        else if (id == R.id.action_search)
        {
            System.out.println("Clicked action_search on app_bar");

            AlertDialog.Builder searchROM = new AlertDialog.Builder(this);
            searchROM.setTitle("Enter ROM name:");
            searchROM.setCancelable(false);

            // Sets button values and handlers within app_search
            final EditText userInput = new EditText(this);
            searchROM.setView(userInput);

            // Handler for Search button
            searchROM.setNegativeButton("Search", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    System.out.println("Clicked 'Search' in AlertDialog [Search]");
                    System.out.println("Input text: " + userInput.getText().toString());
                    // Perform Search functionality here, closes AlertDialog for now
                    dialog.cancel();
                }
            });

            // Handler for Close button
            searchROM.setPositiveButton("Close", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    System.out.println("Clicked 'Close' in AlertDialog [Search]");
                    dialog.cancel();
                }
            });

            // Build the AlertDialog and display it
            AlertDialog my_search = searchROM.create();
            my_search.show();

            return true;

        } // end : search

        return super.onOptionsItemSelected(item);

    }

    /* A list of ROMs with a given sorting */
    public static class RomListFragment extends Fragment {
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

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Populate list with ROM entries from cache
            View rootView = inflater.inflate(R.layout.fragment_library, container, false);
            sortMode = (SortingMode)getArguments().getSerializable("sort_mode");
            ArrayList<RomEntry> romList = RomCache.getInstance(getContext()).romList;

            final ListView listView = (ListView) rootView.findViewById(R.id.library_list);
            listView.setAdapter(new RomListAdapter(getContext(), romList));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    RomEntry rom = (RomEntry)listView.getItemAtPosition(position);
                    if (view.getId() == R.id.favorite) {
                        ViewPager vp = (ViewPager)getActivity().findViewById(R.id.container);

                        rom.isFavorite = !rom.isFavorite;
                        RomCache.getInstance(getContext()).updateRomMetadata(rom);
                        vp.getAdapter().notifyDataSetChanged();
                    } else {
                        Intent i = new Intent(inflater.getContext(), ControllerScreen.class);
                        i.putExtra("rom_path", rom.getPath());
                        i.putExtra("rom_idx", position);
                        startActivityForResult(i, 0);
                    }
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
            if (resultCode == RESULT_OK) {
                View v = getView();
                if (v != null) {
                    // Update "last played" date for the ROM that was just played
                    ListView listView = (ListView) v.findViewById(R.id.library_list);
                    RomEntry rom = (RomEntry)listView.getItemAtPosition(data.getExtras().getInt("rom_idx"));

                    rom.lastPlayed = new Date();
                    RomCache.getInstance(getContext()).updateRomMetadata(rom);
                }
            }

        }
    }

    /* Returns a fragment corresponding to one of the sections/tabs/pages */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Instantiate the fragment for the given page
            RomListFragment.SortingMode sm;
            switch (position) {
                case 0:
                    sm = RomListFragment.SortingMode.RECENT;
                    break;
                case 1:
                    sm = RomListFragment.SortingMode.FAVORITE;
                    break;
                default:
                    sm = RomListFragment.SortingMode.ALPHABETICAL;
            }
            return RomListFragment.newInstance(sm);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Recent";
                case 1:
                    return "Favorite";
                case 2:
                    return "All";
            }
            return null;
        }
    }
}
