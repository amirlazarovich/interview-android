package la.il.interview.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import la.il.interview.data.DataContract;

import static la.il.interview.utils.LogUtils.LOGD;
import static la.il.interview.utils.LogUtils.LOGI;
import static la.il.interview.utils.LogUtils.makeLogTag;

/**
 * @author Amir Lazarovich
 */
public class SyncHelper {
    private static final String TAG = makeLogTag(SyncHelper.class);

    public static final String ACCOUNT_TYPE = "office.media.mit.edu";
    public static final String ACCOUNT = "Phoenix";

    private static final String KEY_REGISTERED_ACCOUNT = "registered_account";
    private static Account sAccount;

    public static Account registerAccount(Context context) {
        Account account = getActiveAccount(context);
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(KEY_REGISTERED_ACCOUNT, false)) {
            return account;
        } else if (accountManager.addAccountExplicitly(account, null, null)) {
            prefs.edit().putBoolean(KEY_REGISTERED_ACCOUNT, true).apply();
            ContentResolver.setIsSyncable(account, DataContract.CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, DataContract.CONTENT_AUTHORITY, true);
            return account;
        } else {
            return null;
        }
    }

    public static ContentObserver registerContentObserver(Context context, Uri uri) {
        SyncContentObserver observer = new SyncContentObserver(null);
        context.getContentResolver().registerContentObserver(uri, true, observer);
        return observer;
    }

    public static void unregisterContentObserver(Context context, ContentObserver observer) {
        context.getContentResolver().unregisterContentObserver(observer);
    }

    public static Account getActiveAccount(Context context) {
        // TODO Use account name stored in shared preferences
        if (sAccount == null) {
            sAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        }

        return sAccount;
    }

    private static void requestManualSync(Account account, boolean manual) {
        if (account != null) {
            LOGD(TAG, "Requesting" + (manual ? " manual" : "") + " sync for account " + account.name);
            Bundle bundle;
            if (manual) {
                bundle = new Bundle();
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            } else {
                bundle = Bundle.EMPTY;
            }

            ContentResolver.setSyncAutomatically(account, DataContract.CONTENT_AUTHORITY, true);
            ContentResolver.setIsSyncable(account, DataContract.CONTENT_AUTHORITY, 1);

            boolean pending = ContentResolver.isSyncPending(account, DataContract.CONTENT_AUTHORITY);
            if (pending) {
                LOGD(TAG, "Warning: sync is PENDING. Will cancel.");
            }

            boolean active = ContentResolver.isSyncActive(account, DataContract.CONTENT_AUTHORITY);
            if (active) {
                LOGD(TAG, "Warning: sync is ACTIVE. Will cancel.");
            }

            if (pending || active) {
                LOGD(TAG, "Cancelling previously pending/active sync.");
                ContentResolver.cancelSync(account, DataContract.CONTENT_AUTHORITY);
            }

            LOGD(TAG, "Requesting sync now.");
            ContentResolver.requestSync(account, DataContract.CONTENT_AUTHORITY, bundle);
        } else {
            LOGD(TAG, "Can't request" + (manual ? " manual" : "") + " sync -- no chosen account.");
        }
    }

    public static void requestManualSync(Context context) {
        Account account = getActiveAccount(context);
        requestManualSync(account, true);
    }

    public static void cancelSync(Context context) {
        Account account = getActiveAccount(context);
        if (account != null) {
            LOGI(TAG, "Cancelling any pending syncs for account");
            ContentResolver.cancelSync(account, DataContract.CONTENT_AUTHORITY);
        }
    }

    private static class SyncContentObserver extends ContentObserver {

        private SyncContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            SyncHelper.requestManualSync(sAccount, false);
        }
    }
}
