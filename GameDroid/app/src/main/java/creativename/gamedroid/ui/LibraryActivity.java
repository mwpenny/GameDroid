package creativename.gamedroid.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
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
    ArrayList<RomEntry> roms;

    private void createAppDirs() {
        // Check filesystem access and presence of GameDroid's directories
        File f = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_roms));
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || (!f.exists() && !f.mkdirs())) {
            // Could not create application directories! Can't continue!
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_direrror_title))
                    .setMessage(getString(R.string.dialog_direrror_message))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            finish();
                            dialog.dismiss();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
        }
    }

    /* Removes ROM metadata from the cache for files that are no longer present */
    private static void cleanCache(File[] files, SQLiteDatabase cache) {
        cache.beginTransaction();
        cache.execSQL("CREATE TEMP TABLE IF NOT EXISTS foundFiles (fileName TEXT PRIMARY KEY NOT NULL)");

        ContentValues row = new ContentValues();
        for (File f : files) {
            row.put("fileName", f.getName());
            if (cache.insert("foundFiles", null, row) == -1) {
                // Don't compromise ROM cache if construction of foundFiles table fails
                cache.endTransaction();
                return;
            }
        }

        cache.rawQuery("DELETE FROM roms WHERE fileName NOT IN (SELECT fileName from foundFiles)", null);
        cache.rawQuery("DROP TABLE IF EXISTS foundFiles", null);
        cache.setTransactionSuccessful();
        cache.endTransaction();
    }

    /* Loads game ROM metadata from the cache (database; if available) or the ROM files themselves */
    private void loadRomData() {
        File romDir = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_roms));
        SQLiteDatabase romCache = new SQLiteOpenHelper(this, "romcache.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(getString(R.string.cache_schema));
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
        }.getWritableDatabase();
        romCache.beginTransaction();

        // Search ROM directory for GameBoy and GameBoy color games
        for (File f : romDir.listFiles()) {
            String ext = f.getName().substring(f.getName().lastIndexOf('.')).toLowerCase();
            if (f.isFile() && (ext.equals(".gb") || ext.equals(".gbc"))) {
                try {
                    roms.add(new RomEntry(f, romCache));
                } catch (IOException e) {
                    // Likely due to an invalid ROM file
                    System.err.format("Could not load metadata for '%s': %s.\n", f.getName(), e.getMessage());
                }
            }
        }

        // Remove old cache entries
        cleanCache(romDir.listFiles(), romCache);
        romCache.setTransactionSuccessful();
        romCache.endTransaction();
        romCache.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        SectionsPagerAdapter spa = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager vp = (ViewPager)findViewById(R.id.container);
        vp.setAdapter(spa);

        ((TabLayout)findViewById(R.id.tabs)).setupWithViewPager(vp);
        roms = new ArrayList<>();
        createAppDirs();
        loadRomData();
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
                            return o1.getLastPlayed().compareTo(o2.getLastPlayed());
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
                        if (r.isFavorite())
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
        public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Populate list with ROM entries
            View rootView = inflater.inflate(R.layout.fragment_library, container, false);
            final ArrayList<RomEntry> romList = getArguments().getParcelableArrayList("roms");
            ListView listView = (ListView) rootView.findViewById(R.id.library_list);
            listView.setAdapter(new RomListAdapter(getContext(), romList));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i = new Intent(inflater.getContext(), ControllerScreen.class);
                    Bundle b = new Bundle();
                    b.putString("rom", romList.get(position).getPath());
                    i.putExtras(b);
                    startActivity(i);
                }
            });
            return rootView;
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

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
            return RomListFragment.newInstance(roms, sm);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
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
