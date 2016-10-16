// #################################################################################################

package com.example.brendanmarko.gamedroid_ui;

// #################################################################################################

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.text.Layout;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

// #################################################################################################

public class SecondScreen extends AppCompatActivity
{

    // #################################################################################################

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_screen);
        build_rom_list();
    } // end : onCreate

    // #################################################################################################

    // This function builds entries for the ListView by loading ROM data from loaded ROMs
    public void build_rom_list()
    {

        System.out.println("Inside build_rom_list()");
        System.out.println("Building List...");

        ArrayList<RomEntry> rom_list = new ArrayList<RomEntry>();

        RomEntry e1 = new RomEntry("Pokemon XY",  "06/27/1951");
        RomEntry e2 = new RomEntry("Super Smash Bros Melee", "12/12/2012");
        RomEntry e3 = new RomEntry("Mario", "20/12/1988");
        RomEntry e4 = new RomEntry("Paper Mario", "20/14/2015");
        RomEntry e5 = new RomEntry("Crash Bandicoot", "11/09/1998");
        RomEntry e6 = new RomEntry("Spyro the Dragon", "2/15/1997");
        RomEntry e7 = new RomEntry("Duckhunt", "3/19/1976");
        RomEntry e8 = new RomEntry("Duckhunt", "3/19/1976");
        RomEntry e9 = new RomEntry("Duckhunt", "3/19/1976");

        rom_list.add(e1);
        rom_list.add(e2);
        rom_list.add(e3);
        rom_list.add(e4);
        rom_list.add(e5);
        /*
        rom_list.add(e6);
        rom_list.add(e7);
        rom_list.add(e8);
        rom_list.add(e9);*/

        // Attach ArrayAdapter to ListView
        RomListAdapter list_handler = new RomListAdapter(this, rom_list);

        // Connect Adapter to ListView
        ListView list_view = (ListView) findViewById(R.id.scrollable_list);
        list_view.setAdapter(list_handler);


        System.out.println("Exiting build_rom_list()");

    } // end : build_rom_list

    // #################################################################################################

    public void rom_execute_handler(View currView)
    {

        System.out.println("Inside rom_execute_handler");
        Intent next_page = new Intent(getApplicationContext(), ControllerScreen.class);
        startActivity(next_page);

    } // end : execute_rom_handler

    // #################################################################################################

} // end : SecondScreen

// #################################################################################################