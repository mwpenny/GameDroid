// #################################################################################################

package com.example.brendanmarko.gamedroid_ui;

// #################################################################################################

import android.os.Bundle;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;

// #################################################################################################

public class MainScreen extends AppCompatActivity
{

    // #################################################################################################

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

    } // end : onCreate

    // #################################################################################################

    public void start_button(View currView)
    {

        System.out.println("Inside start_button()");
        Intent next_page = new Intent(getApplicationContext(), SecondScreen.class);
        startActivity(next_page);

    } // end : start_button

    // #################################################################################################

    public void settings_button(View currView)
    {

        System.out.println("Inside settings_button()");

    } // end : settings_button

    // #################################################################################################

    public void help_button(View currView)
    {

        System.out.println("Inside help_button()");

    } // end : help_button

    // #################################################################################################

} // end : MainScreen

// #################################################################################################