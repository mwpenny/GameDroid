package creativename.gamedroid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

import creativename.gamedroid.R;

/* An information dialog for a given ROM */
public class RomInfoFragment extends DialogFragment {
    private AlertDialog yesNoPrompt;

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

        // Format ROM metadata
        float version = 1 + args.getInt("version")/10f;
        String title = String.format(getString(R.string.rom_title_format), args.getString("title"), version);
        String licensee = String.format(getString(R.string.rom_licensee_format), args.getString("licensee"));
        String region = String.format(getString(R.string.rom_region_format), args.getString("locale"));
        final String path = args.getString("path");
        String formattedPath = String.format(getString(R.string.rom_path_format), path);

        final Button deleteSaveBtn = (Button)v.findViewById(R.id.button_delete_save);
        final Button deleteStateBtn = (Button)v.findViewById(R.id.button_delete_state);
        deleteSaveBtn.setEnabled(getSaveFile().exists());
        deleteStateBtn.setEnabled(getStateFile().exists());

        getDialog().setTitle(title);
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);

        ((TextView)v.findViewById(R.id.dialog_licensee)).setText(licensee);
        ((TextView)v.findViewById(R.id.dialog_locale)).setText(region);
        ((TextView)v.findViewById(R.id.dialog_path)).setText(formattedPath);

        /* Add handlers */
        deleteSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Confirm and delete save
                promptYesNo(getString(R.string.dialog_delete_save_title),
                        getString(R.string.dialog_delete_save_message),
                        new Runnable() {
                            @Override
                            public void run() {
                                deleteSaveBtn.setEnabled(!deleteSave());
                            }
                        }, false);
            }
        });

        deleteStateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Confirm and delete state
                promptYesNo(getString(R.string.dialog_delete_state_title),
                        getString(R.string.dialog_delete_state_message),
                        new Runnable() {
                            @Override
                            public void run() {
                                deleteStateBtn.setEnabled(!deleteState());
                            }
                        }, false);
            }
        });

        v.findViewById(R.id.button_delete_rom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Confirm and delete ROM and associated data
                promptYesNo(getString(R.string.dialog_delete_rom_title),
                        getString(R.string.dialog_delete_rom_message),
                        new Runnable() {
                            @Override
                            public void run() {
                                if (deleteRom()) {
                                    // Remove from lists to update view
                                    RomCache.getInstance(getContext()).removeRom(path);
                                    ((RomListFragment)getTargetFragment()).refresh(getArguments().getInt("rom_index", -1));

                                    deleteSave();
                                    deleteState();
                                }
                            }
                        }, true);
            }
        });

        return v;
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

    private File getSaveFile() {
        File path = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_saves));
        return new File(path, getArguments().getString("title") + ".sav");
    }

    private File getStateFile() {
        File path = new File(Environment.getExternalStorageDirectory(), getString(R.string.path_states));
        return new File(path, getArguments().getString("title") + ".st");
    }

    private boolean deleteSave() {
        return getSaveFile().delete();
    }

    private boolean deleteState() {
        return getStateFile().delete();
    }

    private boolean deleteRom() {
        // Metadata will be removed from the cache on app restart
        String path = getArguments().getString("path");
        return (path != null && (new File(path)).delete());
    }
}
