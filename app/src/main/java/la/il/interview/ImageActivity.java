package la.il.interview;

import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;


public class ImageActivity extends ActionBarActivity {

    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getStringExtra(EXTRA_IMAGE_URL);
            if (url != null) {
                ImageView imageView = (ImageView) findViewById(R.id.image);
                imageView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        closeActivity();
                    }
                });

                Picasso.with(this)
                        .load(url)
                        .into(imageView);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                closeActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void closeActivity() {
        if (VERSION.SDK_INT >= 21) {
            finishAfterTransition();
        } else {
            finish();
        }
    }
}
