package creativename.gamedroid.ui;

import android.os.Bundle;
import android.graphics.Color;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.AppCompatActivity;

import creativename.gamedroid.R;

public class SettingsActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        Toolbar settings_bar = (Toolbar) findViewById(R.id.settings_toolbar);
        settings_bar.setTitle("Settings");
        settings_bar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(settings_bar);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

}
