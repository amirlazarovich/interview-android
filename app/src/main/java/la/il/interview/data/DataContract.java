package la.il.interview.data;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;

/**
 * @author Amir Lazarovich
 */
public class DataContract {
    public static final String QUERY_PARAMETER_DISTINCT = "distinct";

    interface ImagesColumns {
        String IMAGE_ID = "image_id";
        String IMAGE_TITLE = "image_title";
        String IMAGE_URL = "image_url";
    }

    public static final String CONTENT_AUTHORITY = "la.il.interview";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_IMAGES = "images";

    /**
     * Images
     */
    public static class Images implements ImagesColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_IMAGES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.interview.image";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.interview.image";

        /**
         * "ORDER BY" clauses.
         */
        public static final String DEFAULT_SORT = ImagesColumns.IMAGE_TITLE + " ASC";

        /**
         * Build {@link Uri} for requested {@link #IMAGE_ID}.
         */
        public static Uri buildImageUri(String imageId) {
            return CONTENT_URI.buildUpon().appendPath(imageId).build();
        }

        /**
         * Read {@link #IMAGE_ID} from {@link Images} {@link Uri}.
         */
        public static String getImageId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    private DataContract() {
    }

    public static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return TextUtils.equals("true",uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }
}