package la.il.interview;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import la.il.interview.data.ApiManager;
import la.il.interview.data.ApiManager.Callback;

import static la.il.interview.utils.LogUtils.LOGW;
import static la.il.interview.utils.LogUtils.makeLogTag;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = makeLogTag(MainActivity.class);
    private static int page = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content, new ListFragment());
            transaction.commit();
        }
    }

    private void loadItems() {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_load:
                loadItems();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
