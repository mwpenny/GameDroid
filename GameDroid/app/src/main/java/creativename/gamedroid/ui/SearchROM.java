package creativename.gamedroid.ui;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.text.Editable;
import android.content.Intent;
import android.widget.EditText;
import android.widget.ListView;
import android.text.TextWatcher;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

import creativename.gamedroid.R;

public class SearchROM extends Activity
{

    ListView list_view;
    EditText search_query;
    RomListAdapter list_handler;

    private ArrayList<RomEntry> search_list = RomCache.getInstance(this).romList;

    @Override
    protected void onDestroy()
    {

        super.onDestroy();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);

        // Sort list by Title
        Collections.sort(search_list, new Comparator<RomEntry>()
        {
            public int compare(RomEntry r1, RomEntry v2)
            {
                return r1.getTitle().compareTo(v2.getTitle());
            }

        });

        // Connect Adapter to ListView
        list_view = (ListView) findViewById(R.id.rom_queries);
        list_handler = new RomListAdapter(this, search_list);
        list_view.setAdapter(list_handler);

        // Add Listener to ListView
        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                System.out.println("ROM entry clicked.");
                RomEntry rom = (RomEntry)list_view.getItemAtPosition(position);
                Intent i = new Intent(view.getContext(), ControllerScreen.class);
                i.putExtra("rom_path", rom.getPath());
                i.putExtra("rom_idx", position);
                startActivity(i);
            }

        });

        // Add TextWatched to the 'search_query' field
        search_query = (EditText) findViewById(R.id.search_query);
        search_query.addTextChangedListener(search_checker);

    }

    // This handles the text in the search_query field being changed and updates the ListView accordingly
    public TextWatcher search_checker = new TextWatcher()
    {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
            // Implementation only uses afterTextChanged() to update the ListView
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
            // Implementation only uses afterTextChanged() to update the ListView
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            updateListView(search_list, search_query.getText().toString());
        }

    };

    // updateListView(ArrayList, String)
    // This function takes the ROM list as input and compares each entry with the query string
    // determining which entries are valid with the current query.
    public void updateListView(ArrayList<RomEntry> curr_list, String query)
    {

        ArrayList<RomEntry> cut_list = new ArrayList<>();

        for (int i = 0; i < curr_list.size(); i++)
        {

            if (search_handler(curr_list.get(i).getTitle(), query))
            {
                cut_list.add(curr_list.get(i));
            }

        }

        list_handler.clear();
        list_handler.addAll(cut_list);
        list_handler.notifyDataSetChanged();

    }

    // search_handler()
    // This function checks the contents of each ROM title for the current query string
    // If there is a match it is left in the list, else it is removed and the list is updated.
    public boolean search_handler(String rom_title, String query)
    {

        for (int i = 0; i < query.length(); i++)
        {

            if (Character.toLowerCase(rom_title.charAt(i)) != Character.toLowerCase(query.charAt(i)))
            {
                return false;
            }

        }

        return true;

    }

}
