package la.il.interview.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import la.il.interview.R;
import la.il.interview.model.Image;

import static la.il.interview.utils.LogUtils.LOGW;
import static la.il.interview.utils.LogUtils.makeLogTag;

/**
 * @author Amir Lazarovich
 */
public class ApiManager {
    private static final String TAG = makeLogTag(ApiManager.class);

    private static ExecutorService mPool = Executors.newSingleThreadExecutor();

    public static Future getImages(final Context context, final int page, final Callback callback) {
       return mPool.submit(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                String key = context.getString(R.string.api_key);
                String url = context.getString(R.string.api_images, page, key);
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    String json = response.body().string();
                    json = json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);

                    // parse
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<Image>>(){}.getType();
                    List<Image> images = gson.fromJson(json, type);

                    // persist images
                    boolean success = Image.insert(context, images);
                    if (!success) {
                        LOGW(TAG, "Failed to store images");
                        callback.onFailure();
                    } else {
                        callback.onSuccess();
                    }
                } catch (Exception e) {
                    callback.onFailure();
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
