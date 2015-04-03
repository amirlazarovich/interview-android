package la.il.sample.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * @author Amir Lazarovich
 */
public class SyncService extends Service {

    private static final Object sLock = new Object();
    private static SyncAdapter mSyncAdapter;

    @Override
    public void onCreate() {
        super.onCreate();

        synchronized (sLock) {
            if (mSyncAdapter == null) {
                mSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mSyncAdapter.getSyncAdapterBinder();
    }
}
