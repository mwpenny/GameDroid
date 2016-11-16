package creativename.gamedroid.ui;

import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import creativename.gamedroid.R;

/* An information dialog for a given ROM */
public class RomInfoFragment extends DialogFragment {
    public RomInfoFragment() {}

    public static RomInfoFragment newInstance(RomEntry rom) {
        RomInfoFragment f = new RomInfoFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("title", rom.getTitle());
        args.putString("licensee", rom.getLicensee());
        args.putInt("version", rom.getVersion());
        args.putString("path", rom.getPath());
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString("title");
        String licensee = args.getString("licensee");
        int version = args.getInt("version");
        String path = args.getString("path");
        View v = inflater.inflate(R.layout.fragment_rom_info, container, false);

        getDialog().setTitle("ROM Details");
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);

        ((TextView)v.findViewById(R.id.dialog_title)).setText("Title: " + title);
        ((TextView)v.findViewById(R.id.dialog_licensee)).setText("Licensee: " + licensee);
        ((TextView)v.findViewById(R.id.dialog_version)).setText("Version: " + version);
        ((TextView)v.findViewById(R.id.dialog_path)).setText("Location: " + path);

        return v;
    }
}
