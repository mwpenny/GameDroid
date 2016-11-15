package creativename.gamedroid.ui;

import android.os.Bundle;
import android.graphics.Color;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

import creativename.gamedroid.R;

public class SettingsActivity extends AppCompatActivity
{

    String size = "";
    String orientation = "";
    boolean invert_buttons = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // Toolbar
        Toolbar settings_bar = (Toolbar) findViewById(R.id.settings_toolbar);
        settings_bar.setTitle("Settings");
        settings_bar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(settings_bar);

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // Screen Size
        RadioGroup size_selector = (RadioGroup)findViewById(R.id.size_selector);
        size = button_text_handler(size_selector);

        System.out.println("size_selection " + size);

        // Invert A/B
        ToggleButton inversion = (ToggleButton)findViewById(R.id.invert_switch);

            if (inversion.isChecked())
            {
                invert_buttons = true;
            }

        System.out.println("Invert A/B: " + invert_buttons);

        // Orientation
        RadioGroup orientation_selector = (RadioGroup)findViewById(R.id.orient_selector);
        orientation = button_text_handler(orientation_selector);

        System.out.println("orientation " + orientation);

    }

    // button_text_handler()
    // Given a RadioGroup, this function returns the text value associated with the selected
    // RadioButton with the RadioGroup.
    public String button_text_handler(RadioGroup curr_group)
    {
        View curr_size = curr_group.findViewById(curr_group.getCheckedRadioButtonId());
        RadioButton r = (RadioButton) curr_group.getChildAt(curr_group.indexOfChild(curr_size));
        return r.getText().toString();
    }

}