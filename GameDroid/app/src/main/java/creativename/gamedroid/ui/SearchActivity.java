package creativename.gamedroid.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.ListView;

import creativename.gamedroid.R;

public class SearchActivity extends FragmentActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        final ImageButton clearBtn = (ImageButton)findViewById(R.id.clear);
        final ListView listView = (ListView)findViewById(R.id.library_list);
        final EditText searchField = (EditText) findViewById(R.id.search_query);

        // Update ROM list when search query is changed
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ((RomListAdapter)listView.getAdapter()).getFilter().filter(s.toString());
                clearBtn.setVisibility((s.length() == 0) ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchField.setText("");
            }
        });
    }
}
