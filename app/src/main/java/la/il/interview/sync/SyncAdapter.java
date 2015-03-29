package la.il.interview.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import static la.il.interview.utils.LogUtils.LOGD;
import static la.il.interview.utils.LogUtils.makeLogTag;

/**
 * @author Amir Lazarovich
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = makeLogTag(SyncAdapter.class);
    private Context mContext;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        LOGD(TAG, "sync for account: " + account.name);

        // get the timestamp of last upload event
        //PreferenceManager.getDefaultSharedPreferences(mContext) ...

        // check if there were any updates in between

        // upload delta

        // fetch data from the server

        // update local storage
    }
}
