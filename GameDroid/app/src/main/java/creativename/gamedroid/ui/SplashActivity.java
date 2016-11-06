package creativename.gamedroid.ui;

import java.io.File;
import creativename.gamedroid.R;

import android.Manifest;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.Toast;
import android.content.Intent;
import android.os.Environment;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.content.ContextCompat;

// Splash screen for performing app initialization and loading ROM metadata
public class SplashActivity extends AppCompatActivity
{

    // Request Code for Callback
    private int EXT_STORAGE_REQUEST = 100;

    AlertDialog fsWarning;
    FindRomsTask findRomsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);

        // Asks the User for permissions to access their file system for ROMs
        userFilePermissions();

    }

    // userFilePermissions()
    // This function performs a check for access privileges to the file system to access ROMs here.
    // If improper privileges are not given the app cannot access ROM files and will not function on Android 6.0 and above!
    private void userFilePermissions()
    {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        {

            // If User previously rejected the request this block executes...
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                System.out.println("User previously rejected request and this block is running...");
            }

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXT_STORAGE_REQUEST);

        }

        else
        {

            if (createAppDirs())
            {
                findRomsTask = new FindRomsTask();
                findRomsTask.execute();
            }

        }

    } // end : userFilePermissions

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] results)
    {

        if (request == EXT_STORAGE_REQUEST)
        {

            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Toast.makeText(this, "Enjoy your GameDroid experience!", Toast.LENGTH_LONG).show();

                if (createAppDirs())
                {
                    findRomsTask = new FindRomsTask();
                    findRomsTask.execute();
                }
            }

            else
            {
                Toast.makeText(this, "Please accept the Storage Request to allow GameDroid to access ROM files!", Toast.LENGTH_LONG).show();
                this.finish();
            }

        }

    } // end : onRequestPermissionsResult

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