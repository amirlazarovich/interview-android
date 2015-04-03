package la.il.sample.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import la.il.sample.data.DataContract.HistoryColumns;
import la.il.sample.data.DataContract.ImagesColumns;
import la.il.sample.sync.SyncHelper;

import static la.il.sample.utils.LogUtils.LOGD;
import static la.il.sample.utils.LogUtils.LOGW;
import static la.il.sample.utils.LogUtils.makeLogTag;

/**
 * @author Amir Lazarovich
 */
public class DataDatabase extends SQLiteOpenHelper {
    private static final String TAG = makeLogTag(DataDatabase.class);

    private static final String DATABASE_NAME = "sample.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int VER_BASE = 2;
    private static final int CUR_DATABASE_VERSION = VER_BASE;

    // TODO we'll need this when using a sync adapter
    private final Context mContext;

    interface Tables {
        String IMAGES = "images";
        String HISTORY = "history";
    }

    public DataDatabase(Context context) {
        super(context, DATABASE_NAME, null, CUR_DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.IMAGES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ImagesColumns.IMAGE_ID + " TEXT NOT NULL,"
                + ImagesColumns.IMAGE_TITLE + " TEXT DEFAULT 'Unknown',"
                + ImagesColumns.IMAGE_URL + " INTEGER NOT NULL,"
                + "UNIQUE (" + ImagesColumns.IMAGE_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.HISTORY + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + HistoryColumns.HISTORY_TERM + " TEXT NOT NULL,"
                + HistoryColumns.HISTORY_TIMESTAMP + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LOGD(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // Cancel any sync currently in progress
        SyncHelper.cancelSync(mContext);

        // Current DB version. We update this variable as we perform upgrades to reflect
        // the current version we are in.
        int version = oldVersion;

        // Indicates whether the data we currently have should be invalidated as a
        // result of the db upgrade. Default is true (invalidate); if we detect that this
        // is a trivial DB upgrade, we set this to false.
        boolean dataInvalidated = true;

        // at this point, we ran out of upgrade logic, so if we are still at the wrong
        // version, we have no choice but to delete everything and create everything again.
        if (version != CUR_DATABASE_VERSION) {
            LOGW(TAG, "Upgrade unsuccessful -- destroying old data during upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.IMAGES);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.HISTORY);

            onCreate(db);
            version = CUR_DATABASE_VERSION;
        }

        if (dataInvalidated) {
            LOGD(TAG, "Data invalidated; resetting our data timestamp.");
            SyncHelper.requestManualSync(mContext);
        }
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}