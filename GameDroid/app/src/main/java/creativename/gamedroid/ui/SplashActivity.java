package creativename.gamedroid.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

import creativename.gamedroid.R;

/* Splash screen for performing app initialization and loading ROM metadata */
public class SplashActivity extends AppCompatActivity {
    AlertDialog fsWarning;
    FindRomsTask findRomsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init
        if (createAppDirs()) {
            findRomsTask = new FindRomsTask();
            findRomsTask.execute();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (findRomsTask != null) {
            findRomsTask.cancel(true);
            findRomsTask.destroyProgressDialog();
        }
        if (fsWarning != null && fsWarning.isShowing())
            fsWarning.dismiss();
    }

    private boolean createAppDirs() {
        // Check filesystem access and presence of GameDroid's directories
        File f = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_roms));
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || (!f.exists() && !f.mkdirs())) {
            // Could not create application directories! Can't continue!
            fsWarning = new AlertDialog.Builder(this)
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
            return false;
        }
        return true;
    }

    /* Async task for loading ROM metadata from cache or disk */
    private class FindRomsTask extends AsyncTask<Void, Void, ArrayList<RomEntry>> {
        private ProgressDialog pd;

        public void destroyProgressDialog() {
            if (pd != null && pd.isShowing())
                pd.dismiss();
            pd = null;
        }

        /* Removes ROM metadata from the cache for files that are no longer present */
        private void cleanCache(File[] files, SQLiteDatabase cache) {
            cache.beginTransaction();
            cache.execSQL("CREATE TEMP TABLE IF NOT EXISTS foundFiles (fileName TEXT PRIMARY KEY NOT NULL)");

            ContentValues row = new ContentValues();
            if (files.length == 0) {
                cache.execSQL("DELETE FROM roms");
            } else {
                for (File f : files) {
                    row.put("fileName", f.getName());
                    if (cache.insert("foundFiles", null, row) == -1) {
                        // Don't compromise ROM cache if construction of foundFiles table fails
                        cache.endTransaction();
                        return;
                    }
                }
                cache.execSQL("DELETE FROM roms WHERE fileName NOT IN (SELECT fileName from foundFiles)");
            }

            cache.execSQL("DROP TABLE IF EXISTS foundFiles");
            cache.setTransactionSuccessful();
            cache.endTransaction();
        }

        @Override
        /* Loads game ROM metadata from the cache (database; if available) or the ROM files themselves */
        protected ArrayList<RomEntry> doInBackground(Void... params) {
            ArrayList<RomEntry> romList = new ArrayList<>();

            File romDir = new File(Environment.getExternalStorageDirectory(), getApplicationContext().getString(R.string.path_roms));
            SQLiteDatabase romCache = new SQLiteOpenHelper(getApplicationContext(), "romcache.db", null, 1) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    db.execSQL(getApplicationContext().getString(R.string.cache_schema));
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
                        romList.add(new RomEntry(f, romCache));
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
            return romList;
        }

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(SplashActivity.this, null, getApplicationContext().getString(R.string.dialog_romcache_message), true);
        }

        @Override
        protected void onPostExecute(ArrayList<RomEntry> roms) {
            destroyProgressDialog();
            Intent intent = new Intent(SplashActivity.this, LibraryActivity.class);
            Bundle b = new Bundle();
            b.putParcelableArrayList("roms", roms);
            intent.putExtras(b);
            SplashActivity.this.startActivity(intent);
            SplashActivity.this.finish();  // So user can't return to splash screen
        }
    }
}