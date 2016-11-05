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
import android.Manifest;
import android.os.Environment;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.content.ContextCompat;

import creativename.gamedroid.R;

/* Splash screen for performing app initialization and loading ROM metadata */
public class SplashActivity extends AppCompatActivity {
    AlertDialog fsWarning;
    FindRomsTask findRomsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);

        // Asks the User for permissions to access their file system for ROMs
        if (userFilePermissions())
        {

            System.out.println("User has provided proper file access to GameDroid.");

            // Continues with GameDroid initialization since User has given file access
            if (createAppDirs())
            {
                findRomsTask = new FindRomsTask();
                findRomsTask.execute();
            }

        }

        else
        {
            System.out.println("User has NOT provided proper file access to GameDroid");
            // App should terminate gracefully here since the User will not be able to access ROMS
            // rather than crashing.
        }

    }

    // userFilePermissions()
    // This function performs a check for access privileges to the file system to access ROMs here.
    // If improper privileges are not given the app cannot access ROM files and will not work.
    // GameDroid will require access to READ_EXTERNAL_STORAGE && WRITE_EXTERNAL_STORAGE
    private boolean userFilePermissions()
    {

        int perm_w = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int perm_r = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (perm_r == PackageManager.PERMISSION_DENIED || perm_w == PackageManager.PERMISSION_DENIED)
        {
            System.out.println("Permissions not granted currently, do...");
            return true;
        }


        return true;

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
    private class FindRomsTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog pd;

        public void destroyProgressDialog() {
            if (pd != null && pd.isShowing())
                pd.dismiss();
            pd = null;
        }

        @Override
        /* Loads game ROM metadata from the cache (database; if available) or the ROM files themselves */
        protected Void doInBackground(Void... params) {
            File romDir = new File(Environment.getExternalStorageDirectory(), getApplicationContext().getString(R.string.path_roms));
            RomCache.getInstance(SplashActivity.this).populateCache(romDir);
            return null;
        }

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(SplashActivity.this, null, getApplicationContext().getString(R.string.dialog_romcache_message), true);
        }

        @Override
        protected void onPostExecute(Void v) {
            destroyProgressDialog();
            Intent intent = new Intent(SplashActivity.this, LibraryActivity.class);
            SplashActivity.this.startActivity(intent);
            SplashActivity.this.finish();  // So user can't return to splash screen
        }
    }
}