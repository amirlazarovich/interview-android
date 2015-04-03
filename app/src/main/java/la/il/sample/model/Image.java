package la.il.sample.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import la.il.sample.data.DataContract.Images;
import la.il.sample.utils.Lists;

/**
 * @author Amir Lazarovich
 */
public class Image {
    private static final int NOT_DEFINED = -1;
    private static int mIdColumnIndex = NOT_DEFINED;
    private static int mUrlColumnIndex = NOT_DEFINED;
    private static int mTitleColumnIndex = NOT_DEFINED;

    public String id;
    @SerializedName("url_l") public String url;
    public String title;

    public Image() {
    }

    public Image(String id, String url, String title) {
        this.id = id;
        this.url = url;
        this.title = title;
    }

    public static boolean insert(Context context, List<Image> images) {
        List<ContentValues> values = Lists.newArrayList();
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            if (image.url == null || image.title == null) {
                continue;
            }

            ContentValues value = new ContentValues();
            value.put(Images.IMAGE_ID, image.id);
            value.put(Images.IMAGE_URL, image.url);
            value.put(Images.IMAGE_TITLE, image.title);

            values.add(value);
        }

        ContentValues[] result = new ContentValues[values.size()];
        values.toArray(result);
        int inserted = context.getContentResolver().bulkInsert(Images.CONTENT_URI, result);
        return (inserted > 0);
    }

    public static Cursor getAll(Context context) {
        return context.getContentResolver().query(Images.CONTENT_URI, null, null, null, Images.DEFAULT_SORT);
    }

    public static Image fromCursor(Cursor cursor) {
        Image image = null;
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(Images.IMAGE_ID);
            int urlIndex = cursor.getColumnIndex(Images.IMAGE_URL);
            int titleIndex = cursor.getColumnIndex(Images.IMAGE_TITLE);

            String id = cursor.getString(idIndex);
            String url = cursor.getString(urlIndex);
            String title = cursor.getString(titleIndex);
            image = new Image(id, url, title);
        }

        return image;
    }

    public static boolean remove(Context context, Cursor cursor) {
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(Images.IMAGE_ID);
            String id = cursor.getString(idIndex);

            int rows = context.getContentResolver().delete(Images.CONTENT_URI, Images.IMAGE_ID + "=" + id, null);
            return (rows == 1);
        } else {
            return false;
        }
    }

    public static void clearAll(Context context) {
        context.getContentResolver().delete(Images.CONTENT_URI, null, null);
    }

    public static String getColumnValue(Cursor cursor, String column) {
        if (cursor != null) {
            return cursor.getString(Image.getColumnIndex(cursor, column));
        } else {
            return null;
        }
    }

    private static int getColumnIndex(Cursor cursor, String column) {
        if (column.equals(Images.IMAGE_ID)) {
            if (mIdColumnIndex == NOT_DEFINED) {
                mIdColumnIndex = cursor.getColumnIndex(column);
            }

            return mIdColumnIndex;
        } else if (column.equals(Images.IMAGE_TITLE)) {
            if (mTitleColumnIndex == NOT_DEFINED) {
                mTitleColumnIndex = cursor.getColumnIndex(column);
            }

            return mTitleColumnIndex;
        } else if (column.equals(Images.IMAGE_URL)) {
            if (mUrlColumnIndex == NOT_DEFINED) {
                mUrlColumnIndex = cursor.getColumnIndex(column);
            }

            return mUrlColumnIndex;
        } else {
            throw new IllegalArgumentException("Unknown column: " + column);
        }
    }
}