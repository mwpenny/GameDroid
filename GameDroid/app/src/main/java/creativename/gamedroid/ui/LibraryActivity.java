package creativename.gamedroid.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import creativename.gamedroid.R;

/* Main game ROM library view */
public class LibraryActivity extends AppCompatActivity {
    private ArrayList<RomEntry> romList;
    private AlertDialog romWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_library);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        romList = getIntent().getExtras().getParcelableArrayList("roms");
        SectionsPagerAdapter spa = new LibraryActivity.SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager vp = (ViewPager)findViewById(R.id.container);
        vp.setAdapter(spa);

        ((TabLayout)findViewById(R.id.tabs)).setupWithViewPager(vp);

        // No ROMs were found. Instruct user on how to add them
        if (romList.size() == 0) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // TODO: handle these
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /* A list of ROMs with a given sorting */
    public static class RomListFragment extends Fragment {
        public enum SortingMode {
            RECENT,
            ALPHABETICAL,
            FAVORITE
        }

        public RomListFragment() {
        }

        public static RomListFragment newInstance(ArrayList<RomEntry> romList, SortingMode mode) {
            RomListFragment fragment = new RomListFragment();
            Bundle args = new Bundle();
            switch (mode) {
                case RECENT: {
                    // Sort by last played date
                    ArrayList<RomEntry> copy = new ArrayList<>(romList);
                    Collections.sort(copy, new Comparator<RomEntry>() {
                        @Override
                        public int compare(RomEntry o1, RomEntry o2) {
                            Date d1 = o1.getLastPlayed();
                            Date d2 = o2.getLastPlayed();

                            if (d1 != null && d2 != null)
                                return d1.compareTo(d2);
                            else if (d1 == null && d2 != null)
                                return 1;
                            else if (d1 != null)
                                return -1;
                            else
                                return 0;
                        }
                    });
                    romList = copy;
                    break;
                }
                case ALPHABETICAL: {
                    // Sort by game title
                    ArrayList<RomEntry> copy = new ArrayList<>(romList);
                    Collections.sort(copy, new Comparator<RomEntry>() {
                        @Override
                        public int compare(RomEntry o1, RomEntry o2) {
                            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
                        }
                    });
                    romList = copy;
                    break;
                }

                case FAVORITE: {
                    // Filter favorites
                    ArrayList<RomEntry> tmp = new ArrayList<>();
                    for (RomEntry r : romList) {
                        if (r.isFavorite)
                            tmp.add(r);
                    }
                    romList = tmp;
                    break;
                }
            }
            args.putParcelableArrayList("roms", romList);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {
            // Populate list with ROM entries
            View rootView = inflater.inflate(R.layout.fragment_library, container, false);
            final ArrayList<RomEntry> romList = getArguments().getParcelableArrayList("roms");
            final RomListAdapter adapter = new RomListAdapter(getContext(), romList);

            ListView listView = (ListView) rootView.findViewById(R.id.library_list);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (romList != null) {
                        RomEntry rom = romList.get(position);
                        if (view.getId() == R.id.favorite) {
                            ViewPager vp = (ViewPager)getActivity().findViewById(R.id.container);

                            rom.isFavorite = !rom.isFavorite;
                            RomCache.getInstance(getContext()).updateRomMetadata(rom);
                            adapter.notifyDataSetChanged();

                            // Update fragments
                            vp.getAdapter().notifyDataSetChanged();
                        } else {
                            Intent i = new Intent(inflater.getContext(), ControllerScreen.class);
                            Bundle b = new Bundle();
                            b.putString("rom", rom.getPath());
                            i.putExtras(b);
                            startActivity(i);
                        }
                    }
                }
            });
            return rootView;
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
            return RomListFragment.newInstance(romList, sm);
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
