package creativename.gamedroid.ui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import creativename.gamedroid.R;
import creativename.gamedroid.core.Cartridge;

public class LibraryActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

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

    /* Converts a string to title case (e.g., "pokemon gold" => "Pokemon Gold") */
    private static String toTitleCase(String input) {
        StringBuilder title = new StringBuilder();
        boolean nextWord = true;
        input = input.replace('_', ' ');

        for (char c : input.toCharArray()) {
            if (nextWord) {
                c = Character.toUpperCase(c);
                nextWord = false;
            } else if (Character.isSpaceChar(c)) {
                nextWord = true;
            } else {
                c = Character.toLowerCase(c);
            }
            title.append(c);
        }
        return title.toString();
    }

    /* Loads game ROM metadata from the cache (database; if available) or the ROM files themselves */
    private void loadROMData() {
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
        romCache.execSQL(getString(R.string.cache_schema));

        // Search ROM directory
        for (File f : romDir.listFiles()) {
            String ext = f.getName().substring(f.getName().lastIndexOf('.')).toLowerCase();
            if (f.isFile() && (ext.equals(".gb") || ext.equals(".gbc"))) {
                // Search cache for metadata (use filename as key)
                Cursor c = romCache.query("roms", null, "fileName=?", new String[]{f.getName()},
                                          null, null, null);
                String title, manufacturer, licensee, locale;
                int version;

                try {
                    if (c.getCount() == 0) {
                        // ROM metadata is not in the cache: parse file for it
                        Cartridge game = new Cartridge(f.getAbsolutePath(), Cartridge.LoadMode.PARSE_ONLY);
                        title = toTitleCase(game.getTitle());
                        manufacturer = game.getManufacturer();
                        licensee = game.getLicensee();
                        locale = game.getLocale() == Cartridge.GameLocale.JAPAN ? "Japan" : "World";
                        version = game.getGameVersion();

                        // Cache parsed data for next time
                        ContentValues row = new ContentValues();
                        row.put("fileName", f.getName());
                        row.put("title", title);
                        row.put("manufacturer", manufacturer);
                        row.put("licensee", licensee);
                        row.put("locale", locale);
                        row.put("version", version);
                        romCache.insert("roms", null, row);
                    } else {
                        // Load ROM metadata from cache
                        c.moveToFirst();
                        title = c.getString(c.getColumnIndex("title"));
                        manufacturer = c.getString(c.getColumnIndex("manufacturer"));
                        licensee = c.getString(c.getColumnIndex("licensee"));
                        locale = c.getString(c.getColumnIndex("locale"));
                        version = c.getInt(c.getColumnIndex("version"));
                    }
                    roms.add(new RomEntry(f.getAbsolutePath(), title, manufacturer, licensee, locale, version));
                } catch (IOException e) {
                    // Likely due to an invalid ROM file
                    System.err.format("Could not load metadata for '%s': %s.\n", f.getName(), e.getMessage());
                }
                c.close();
            }
        }

        romCache.setTransactionSuccessful();
        romCache.endTransaction();
        romCache.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        //setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager)findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        ((TabLayout)findViewById(R.id.tabs)).setupWithViewPager(mViewPager);
        roms = new ArrayList<>();
        createAppDirs();
        loadROMData();
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


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_library, container, false);
            //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            //textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
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
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
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
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }
}
