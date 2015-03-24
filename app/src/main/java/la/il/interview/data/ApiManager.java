package la.il.interview.data;

import android.content.Context;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import la.il.interview.ImagesFragment;
import la.il.interview.R;
import la.il.interview.model.HistoryItem;
import la.il.interview.model.Image;

import static la.il.interview.utils.LogUtils.LOGW;
import static la.il.interview.utils.LogUtils.makeLogTag;

/**
 * @author Amir Lazarovich
 */
public class ApiManager {
    private static final String TAG = makeLogTag(ApiManager.class);
    private static final String KEY_API_KEY = "api_key";

    private static ExecutorService mPool = Executors.newSingleThreadExecutor();

    public static Future getImages(final Context context, final int page, final Callback callback) {
       return mPool.submit(new Runnable() {
            private boolean retry = true;

            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                String key = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_API_KEY, null);
                try {
                    if (key == null) {
                        // scrape an api-key from Flickr website
                        Request request = new Request.Builder()
                                .url(context.getString(R.string.get_flickr_api_key))
                                .build();
                        Response response = client.newCall(request).execute();
                        String html = response.body().string();
                        html = html.substring(html.indexOf("api_key\":") + 10, html.length());
                        key = html.substring(0, html.indexOf(",") - 1);

                        // store api key
                        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_API_KEY, key).apply();
                    }

                    // build request
                    String criteria = PreferenceManager.getDefaultSharedPreferences(context).getString(ImagesFragment.KEY_SEARCH_CRITERIA, context.getString(R.string.default_search_criteria));
                    String url = context.getString(R.string.api_images, page, key,  URLEncoder.encode(criteria, "UTF-8"));
                    Request request = new Request.Builder()
                            .url(url)
                            .build();

                    // issue request
                    Response response = client.newCall(request).execute();
                    String json = response.body().string();
                    json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);

                    // parse
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<Image>>(){}.getType();
                    List<Image> images = gson.fromJson(json, type);

                    // persist images
                    boolean success = Image.insert(context, images);
                    if (success) {
                        // store searched criteria
                        HistoryItem.insert(context, criteria);

                        callback.onSuccess();
                    } else {
                        LOGW(TAG, "Failed to store images");
                        callback.onFailure();
                    }
                } catch (Exception e) {
                    PreferenceManager.getDefaultSharedPreferences(context).edit().remove(KEY_API_KEY).apply();
                    if (retry) {
                        // only attempt once
                        retry = false;
                        run();
                    } else {
                        callback.onFailure();
                    }
                }
            }
        });
    }

    public static void getImagesLocal(final Context context) {
        mPool.submit(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                try {

                    is = context.getAssets().open("data.json");
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();

                    String jsonString = new String(buffer, "UTF-8");

                    // parse
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<Image>>() {
                    }.getType();
                    List<Image> images = gson.fromJson(jsonString, type);

                    // persist images
                    boolean success = Image.insert(context, images);
                    if (!success) {
                        LOGW(TAG, "Failed to store images");
                    }
                } catch (IOException e) {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e1) {
                        }
                    }
                }
            }
        });
    }

    public interface Callback {
        void onSuccess();
        void onFailure();
    }
}
