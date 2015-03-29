package la.il.interview.ui.activity;

import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import la.il.interview.R;
import la.il.interview.data.ApiManager;
import la.il.interview.data.ApiManager.Callback;
import la.il.interview.data.DataContract.History;
import la.il.interview.model.HistoryItem;
import la.il.interview.model.Image;
import la.il.interview.sync.SyncHelper;
import la.il.interview.ui.fragment.HistoryFragment;
import la.il.interview.ui.fragment.ImagesFragment;

import static la.il.interview.utils.LogUtils.LOGW;
import static la.il.interview.utils.LogUtils.makeLogTag;

/**
 * @author Amir Lazarovich
 */
public class MainActivity extends ActionBarActivity implements OnItemClickListener {
    private static final String TAG = makeLogTag(MainActivity.class);

    private static int page = 1;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String[] mSectionTitles;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ContentObserver mObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SyncHelper.registerAccount(this);

        // set main content
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content, new ImagesFragment());
            transaction.commit();
        }

        mTitle = mDrawerTitle = getTitle();
        mSectionTitles = getResources().getStringArray(R.array.sections);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        mDrawerList = (ListView) findViewById(R.id.drawer_content);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.list_drawer_item, mSectionTitles));

        // Set the list's click listener
        mDrawerList.setOnItemClickListener(this);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }



    private void loadItems() {
        if (getResources().getBoolean(R.bool.localhost)) {
            ApiManager.getImagesLocal(this);
        } else {
            ApiManager.getImages(this, page, new Callback() {
                @Override
                public void onSuccess() {
                    page++;
                }

                @Override
                public void onFailure() {
                    LOGW(TAG, "Failed to fetch images, maybe need to change Flickr API key");
                }
            });
        }
    }

    private void clearItems() {
        switch (mDrawerList.getCheckedItemPosition()) {
            case -1:
            case 0:
                Image.clearAll(this);
                break;

            case 1:
                HistoryItem.clearAll(this);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_load).setVisible(mDrawerList.getCheckedItemPosition() <= 0 && !drawerOpen);
        menu.findItem(R.id.action_clear).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }


        switch (item.getItemId()) {
            case R.id.action_load:
                loadItems();
                return true;

            case R.id.action_clear:
                clearItems();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.content);
        switch (position) {
            case 0:
                if (! (fragment instanceof ImagesFragment)) {
                    fragmentManager.beginTransaction()
                            .replace(R.id.content, new ImagesFragment())
                            .commit();
                }
                break;

            case 1:
                if (! (fragment instanceof HistoryFragment)) {
                    fragmentManager.beginTransaction()
                            .replace(R.id.content, new HistoryFragment())
                            .commit();
                }
                break;
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mSectionTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mObserver = SyncHelper.registerContentObserver(this, History.CONTENT_URI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mObserver != null) {
            SyncHelper.unregisterContentObserver(this, mObserver);
            mObserver = null;
        }
    }
}