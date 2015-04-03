package la.il.sample.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import la.il.sample.data.DataContract.History;

/**
 * @author Amir Lazarovich
 */
public class HistoryItem {
    private static final int NOT_DEFINED = -1;
    private static int mTermColumnIndex = NOT_DEFINED;
    private static int mTimestampColumnIndex = NOT_DEFINED;

    public String term;
    public long timestamp;

    public HistoryItem() {
    }

    public HistoryItem(String term, long timestamp) {
        this.term = term;
        this.timestamp = timestamp;
    }

    public static boolean insert(Context context, String term) {
        ContentValues values = new ContentValues();
        values.put(History.HISTORY_TERM, term);
        values.put(History.HISTORY_TIMESTAMP, System.currentTimeMillis());
        Uri inserted = context.getContentResolver().insert(History.CONTENT_URI, values);
        return (inserted != null);
    }

    public static boolean remove(Context context, Cursor cursor) {
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(History._ID);
            long id = cursor.getLong(idIndex);

            int rows = context.getContentResolver().delete(History.buildHistoryUri(id), null, null);
            return (rows == 1);
        } else {
            return false;
        }
    }

    public static String getColumnValue(Cursor cursor, String column) {
        if (cursor != null) {
            return cursor.getString(HistoryItem.getColumnIndex(cursor, column));
        } else {
            return null;
        }
    }

    public static long getColumnValueLong(Cursor cursor, String column) {
        if (cursor != null) {
            return cursor.getLong(HistoryItem.getColumnIndex(cursor, column));
        } else {
            return 0;
        }
    }

    private static int getColumnIndex(Cursor cursor, String column) {
        if (column.equals(History.HISTORY_TERM)) {
            if (mTermColumnIndex == NOT_DEFINED) {
                mTermColumnIndex = cursor.getColumnIndex(column);
            }

            return mTermColumnIndex;
        } else if (column.equals(History.HISTORY_TIMESTAMP)) {
            if (mTimestampColumnIndex == NOT_DEFINED) {
                mTimestampColumnIndex = cursor.getColumnIndex(column);
            }

            return mTimestampColumnIndex;
        } else {
            throw new IllegalArgumentException("Unknown column: " + column);
        }
    }

    public static void clearAll(Context context) {
        context.getContentResolver().delete(History.CONTENT_URI, null, null);
    }
}
