package creativename.gamedroid.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

        float version = 1 + args.getInt("version")/10f;
        String title = String.format(getString(R.string.rom_title_format), args.getString("title"), version);
        String licensee = String.format(getString(R.string.rom_licensee_format), args.getString("licensee"));
        String region = String.format(getString(R.string.rom_region_format), args.getString("locale"));
        String path = String.format(getString(R.string.rom_path_format), args.getString("path"));

        Button deleteSave = (Button)v.findViewById(R.id.button_delete_save);
        Button deleteState = (Button)v.findViewById(R.id.button_delete_state);

        // TODO: enable delete save/state buttons if their corresponding files exist

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
                                deleteSave();
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
                                deleteSave();
                                deleteState();
                                deleteRom();
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

    private void deleteSave() {
        // TODO: delete game save
    }

    private void deleteState() {
        // TODO: delete game state
    }

    private void deleteRom() {
        // TODO: delete game ROM
    }
}
