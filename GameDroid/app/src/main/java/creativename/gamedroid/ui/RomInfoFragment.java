package creativename.gamedroid.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

import creativename.gamedroid.R;

/* An information dialog for a given ROM */
public class RomInfoFragment extends DialogFragment {
    private AlertDialog yesNoPrompt;
    private String romTitle;
    private String romPath;
    private Button deleteSave;
    private Button deleteState;
    private Button deleteRom;
    private ListView listView;
    public RomInfoFragment() {}

    public static RomInfoFragment newInstance(RomEntry rom) {
        RomInfoFragment f = new RomInfoFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("title", rom.getTitle());
        args.putString("licensee", rom.getLicensee());
        args.putString("locale", rom.getLocale());
        args.putInt("version", rom.getVersion());
        args.putString("path", rom.getPath());
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        View v = inflater.inflate(R.layout.fragment_rom_info, container, false);
        View rootView = inflater.inflate(R.layout.fragment_romlist, container, false);
        listView = (ListView) rootView.findViewById(R.id.library_list);
        float version = 1 + args.getInt("version")/10f;
        String title = String.format(getString(R.string.rom_title_format), args.getString("title"), version);
        String licensee = String.format(getString(R.string.rom_licensee_format), args.getString("licensee"));
        String region = String.format(getString(R.string.rom_region_format), args.getString("locale"));
        String path = String.format(getString(R.string.rom_path_format), args.getString("path"));

        romTitle = args.getString("title");

        deleteSave = (Button)v.findViewById(R.id.button_delete_save);
        deleteState = (Button)v.findViewById(R.id.button_delete_state);
        deleteRom = (Button)v.findViewById(R.id.button_delete_rom);

        boolean stateExists = fileExist(getString(R.string.path_states) + "/" + romTitle + ".st");
        boolean saveExists = fileExist(getString(R.string.path_saves) + "/" + romTitle + ".sav");
        boolean romExist = fileExist(path);

        deleteSave.setEnabled(saveExists);
        deleteState.setEnabled(stateExists);
        deleteRom.setEnabled(romExist);

        getDialog().setTitle(title);
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);

        ((TextView)v.findViewById(R.id.dialog_licensee)).setText(licensee);
        ((TextView)v.findViewById(R.id.dialog_locale)).setText(region);
        ((TextView)v.findViewById(R.id.dialog_path)).setText(path);

        /* Add handlers */
        deleteSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Confirm and delete save
                promptYesNo(getString(R.string.dialog_delete_save_title),
                        getString(R.string.dialog_delete_save_message),
                        new Runnable() {
                            @Override
                            public void run() {
                                deleteSave();
                            }
                        }, false);
            }
        });

        deleteState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Confirm and delete state
                promptYesNo(getString(R.string.dialog_delete_state_title),
                        getString(R.string.dialog_delete_state_message),
                        new Runnable() {
                            @Override
                            public void run() {
                                deleteState();
                            }
                        }, false);
            }
        });

        deleteRom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Confirm and delete ROM and associated data
                promptYesNo(getString(R.string.dialog_delete_rom_title),
                        getString(R.string.dialog_delete_rom_message),
                        new Runnable() {
                            @Override
                            public void run() {
                                //deleteSave();
                                //deleteState();
                                deleteRom();
                            }
                        }, true);
            }
        });

        return v;
    }

    private boolean fileExist(String path) {
        return new File(Environment.getExternalStorageDirectory(), path).exists();
    }

    private void deleteFile(String path) {
        if(fileExist(path)) {
            try {
                File f = new File(path);
                f.setWritable(true);
                System.gc();
                boolean result = f.delete();
                //System.out.println("Value of result is: " + result);
            } catch(SecurityException e) {
                //Failed to delete file
                System.out.println("Security Exception caught - Do not have permission");
                throw e;
            } catch(NullPointerException e) {
                //Couldn't reference the file
                System.out.println("Lost reference to file");
                throw e;
            } catch(Exception e) {
                //Something happened
                System.out.println("File deleting: Something else happened");
                throw e;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeYesNoPrompt();
    }

    private void promptYesNo(String title, String message, final Runnable action, final boolean finish) {
        // Prompt the user to perform some action (i.e., deletion)
        closeYesNoPrompt();
        yesNoPrompt = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (action != null)
                            action.run();
                        if (finish)
                            dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void closeYesNoPrompt() {
        if (yesNoPrompt != null && yesNoPrompt.isShowing())
            yesNoPrompt.dismiss();
    }

    private void deleteSave() {
        deleteFile(getString(R.string.path_saves) + "/" + romTitle + ".sav");
        deleteSave.setEnabled(false);
    }

    private void deleteState() {
        deleteFile(getString(R.string.path_states) + "/" + romTitle + ".st");
        deleteState.setEnabled(false);
    }

    private void deleteRom() {
        deleteFile(romPath);
        deleteRom.setEnabled(false);
    }
}
