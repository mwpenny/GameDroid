package creativename.gamedroid.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import creativename.gamedroid.R;

/* View for searching the library */
public class SearchActivity extends AppCompatActivity
{
    private SearchView searchView;
    private String lastQuery;

    private void filter(String query) {
        ListView listView = (ListView)findViewById(R.id.library_list);
        ((RomListAdapter)listView.getAdapter()).getFilter().filter(query);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setSupportActionBar((Toolbar)findViewById(R.id.search_bar));
        getSupportActionBar().setTitle(getString(R.string.action_search));

        if (savedInstanceState != null)
            lastQuery = savedInstanceState.getString("query");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (searchView != null)
            outState.putString("query", searchView.getQuery().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Leave activity when the search field is closed
                finish();
                return false;
            }
        });

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView)MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter results in real-time
                filter(newText);
                return false;
            }
        });
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchItem.expandActionView();
        if (lastQuery != null)
            searchView.setQuery(lastQuery, false);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (searchView != null)
                searchView.setQuery(query, false);
            filter(query);
        }
    }
}
