package creativename.gamedroid.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init
        createAppDirs();
        new FindRomsTask(this).execute();
    }

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

    /* Async task for loading ROM metadata from cache or disk */
    private static class FindRomsTask extends AsyncTask<Void, Void, ArrayList<RomEntry>> {
        private Context context;
        private ProgressDialog pd;

        FindRomsTask(Context context) {
            this.context = context;
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

            File romDir = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.path_roms));
            SQLiteDatabase romCache = new SQLiteOpenHelper(context, "romcache.db", null, 1) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    db.execSQL(context.getString(R.string.cache_schema));
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
            pd = ProgressDialog.show(context, null, context.getString(R.string.dialog_romcache_message), true);
        }

        @Override
        protected void onPostExecute(ArrayList<RomEntry> roms) {
            pd.dismiss();

            // No ROMs were found. Instruct user on how to add them
            if (roms.size() == 0) {
                String path = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.path_roms)).getAbsolutePath();
                new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.dialog_noroms_title))
                        .setMessage(String.format(context.getString(R.string.dialog_noroms_message), path))
                        .setPositiveButton(android.R.string.ok, null)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .show();
            }

            Intent intent = new Intent(context, LibraryActivity.class);
            Bundle b = new Bundle();
            b.putParcelableArrayList("roms", roms);
            intent.putExtras(b);
            context.startActivity(intent);
            ((AppCompatActivity)context).finish();  // So user can't return to splash screen
        }
    }
}