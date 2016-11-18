package creativename.gamedroid.ui;

import java.io.File;
import creativename.gamedroid.R;

import android.Manifest;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.content.Intent;
import android.os.Environment;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.content.ContextCompat;

/* Splash screen for performing app initialization and loading ROM metadata */
public class SplashActivity extends AppCompatActivity {
    AlertDialog fsWarning;
    FindRomsTask findRomsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
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
        String[] paths = {
                getString(R.string.path_roms),
                getString(R.string.path_saves),
                getString(R.string.path_states),
                getString(R.string.path_screenshots)
        };
        boolean success = true;

        for (String path : paths) {
            File f = new File(Environment.getExternalStorageDirectory(), path);
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || (!f.exists() && !f.mkdirs())) {
                success = false;
                break;
            }
        }

        if (!success) {
            // Could not create application directories! Can't continue!
            fsWarning = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_direrror_title))
                    .setMessage(getString(R.string.dialog_direrror_message))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            finish();
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
        return success;
    }

    private void gdinit() {
        if (createAppDirs()) {
            findRomsTask = new FindRomsTask();
            findRomsTask.execute();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            // Prompt user for filesytem write permission if not present (needed for accessing ROMs)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        else
            gdinit();
    }

    @Override
    public void onRequestPermissionsResult(int request, @NonNull String[] permissions, @NonNull int[] results) {
        if (results.length > 0)
            gdinit();
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