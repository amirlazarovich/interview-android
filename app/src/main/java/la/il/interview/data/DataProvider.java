package la.il.interview.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;

import la.il.interview.data.DataContract.Images;
import la.il.interview.data.DataDatabase.Tables;
import la.il.interview.utils.SelectionBuilder;

import static la.il.interview.utils.LogUtils.*;

public class DataProvider extends ContentProvider {
    private static final String TAG = makeLogTag(DataProvider.class);
    private DataDatabase mOpenHelper;
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final int IMAGES = 100;
    private static final int IMAGES_ID = 101;

    public DataProvider() {
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DataContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "images", IMAGES);
        matcher.addURI(authority, "images/*", IMAGES_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DataDatabase(getContext());
        return true;
    }

    private void deleteDatabase() {
        // TODO: wait for content provider operations to finish, then tear down
        mOpenHelper.close();
        Context context = getContext();
        DataDatabase.deleteDatabase(context);
        mOpenHelper = new DataDatabase(getContext());
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        // avoid the expensive string concatenation below if not loggable
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            LOGD(TAG, "uri=" + uri + " proj=" + Arrays.toString(projection) +
                    " selection=" + selection + " args=" + Arrays.toString(selectionArgs) + ")");
        }

        final SelectionBuilder builder = buildSelection(uri);

        boolean distinct = !TextUtils.isEmpty(uri.getQueryParameter(DataContract.QUERY_PARAMETER_DISTINCT));

        Cursor cursor = builder
                .where(selection, selectionArgs)
                .query(db, distinct, projection, sortOrder, null);

        Context context = getContext();
        if (null != context) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case IMAGES: {
                db.insertOrThrow(Tables.IMAGES, null, values);
                notifyChange(uri);
                return Images.buildImageUri(values.getAsString(Images.IMAGE_ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        LOGV(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        final SelectionBuilder builder = buildSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        notifyChange(uri);
        return retVal;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LOGV(TAG, "delete(uri=" + uri + ")");
        if (uri.equals(DataContract.BASE_CONTENT_URI)) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        return retVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case IMAGES:
                return Images.CONTENT_TYPE;
            case IMAGES_ID:
                return Images.CONTENT_ITEM_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Build a {@link SelectionBuilder} to match the requested {@link Uri}
     */
    private SelectionBuilder buildSelection(Uri uri) {
        final int match = sUriMatcher.match(uri);
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case IMAGES: {
                return builder.table(Tables.IMAGES);
            }
            case IMAGES_ID: {
                final String imageId = Images.getImageId(uri);
                return builder.table(Tables.IMAGES)
                        .where(Images.IMAGE_ID + "=?", imageId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private void notifyChange(Uri uri) {
        // We only notify changes if the caller is not the sync adapter.
        // The sync adapter has the responsibility of notifying changes (it can do so
        // more intelligently than we can -- for example, doing it only once at the end
        // of the sync instead of issuing thousands of notifications for each record).
        if (!DataContract.hasCallerIsSyncAdapterParameter(uri)) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);

            // TODO Widgets can't register content observers so we refresh widgets separately.
        }
    }
}
