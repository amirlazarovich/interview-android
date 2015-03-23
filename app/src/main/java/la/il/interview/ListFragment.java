package la.il.interview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import la.il.interview.data.DataContract.Images;
import la.il.interview.model.Image;
import la.il.interview.widget.CursorRecyclerViewAdapter;
import la.il.interview.widget.MutableCursor;
import la.il.interview.widget.SwipeDismissRecyclerViewTouchListener;
import la.il.interview.widget.SwipeDismissTouchListener;

import static la.il.interview.utils.LogUtils.makeLogTag;


/**
 * @author Amir Lazarovich
 */
public class ListFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(ListFragment.class);
    private RecyclerCursorAdapter mAdapter;

    public ListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_list, container, false);
        Context context = container.getContext();
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        // set adapter
        mAdapter = new RecyclerCursorAdapter(context, null);
        recyclerView.setAdapter(mAdapter);

        // set swipe-to-remove
        SwipeDismissRecyclerViewTouchListener touchListener = new SwipeDismissRecyclerViewTouchListener(
                recyclerView,
                new SwipeDismissRecyclerViewTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(int position) {
                        return true;
                    }

                    @Override
                    public void onDismiss(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions) {
                            Cursor cursor = mAdapter.getCursor();
                            cursor.moveToPosition(position);
                            Image.remove(recyclerView.getContext(), cursor);
                            Cursor newCursor = new MutableCursor(cursor, position);
                            mAdapter.swapCursor(newCursor);
                        }
                    }
                });

        recyclerView.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        recyclerView.setOnScrollListener(touchListener.makeScrollListener());

        // load images in a background thread
        getLoaderManager().initLoader(0, null, this);
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Images.CONTENT_URI, null, null, null, Images.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


    public static class RecyclerCursorAdapter extends CursorRecyclerViewAdapter<RecyclerCursorAdapter.ViewHolder> {
        SparseArray<Swatch> mSwatches;

        public RecyclerCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            mSwatches = new SparseArray<>();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public View mRoot;
            public TextView mTextView;
            public ImageView mImageView;

            public ViewHolder(View view) {
                super(view);
                mRoot = view;
                mTextView = (TextView) view.findViewById(R.id.text);
                mImageView = (ImageView) view.findViewById(R.id.image);
            }

            public void bind(final Image image, final RecyclerCursorAdapter adapter, final int position) {
                mRoot.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Activity activity = (Activity) v.getContext();
                        final Intent intent = new Intent(activity, ImageActivity.class);
                        intent.putExtra(ImageActivity.EXTRA_IMAGE_URL, image.url);

                        if (VERSION.SDK_INT >= 21) {
                            final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, mImageView, "image");
                            activity.startActivity(intent, options.toBundle());
                        } else {
                            activity.startActivity(intent);
                        }
                    }
                });

                mRoot.setOnTouchListener(new SwipeDismissTouchListener(mRoot, null, new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view, Object token) {
                        Cursor cursor = adapter.getCursor();
                        cursor.moveToPosition(position);
                        Image.remove(view.getContext(), cursor);
                        Cursor newCursor = new MutableCursor(cursor, position);
                        adapter.swapCursor(newCursor);
                    }
                }));

                // set text background
                final SparseArray<Swatch> swatches = adapter.mSwatches;
                boolean loadPalette = true;
                Swatch swatch = swatches.get(image.url.hashCode());
                if (swatch == null) {
                    mTextView.setVisibility(View.INVISIBLE);
                    mTextView.setText(null);
                } else {
                    loadPalette = false;
                    if (mTextView.getVisibility() != View.VISIBLE) {
                        mTextView.setVisibility(View.VISIBLE);
                    }

                    mTextView.setText(image.title);
                    mTextView.setTextColor(swatch.getBodyTextColor());
                    mTextView.setBackgroundColor(Color.HSVToColor(200, swatch.getHsl()));
                }

                // load cover image
                Context context = mTextView.getContext();
                Picasso.with(context)
                        .load(image.url)
                        .placeholder(R.drawable.placeholder)
                        .into(mImageView, loadPalette ? new Callback() {
                            @Override
                            public void onSuccess() {
                                // choose an appropriate color based on the bitmap color
                                Drawable drawable = mImageView.getDrawable();
                                if (drawable instanceof BitmapDrawable) {
                                    final Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                    Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                                        public void onGenerated(Palette palette) {
                                            // get swatch colors
                                            Swatch swatch = palette.getLightMutedSwatch();
                                            if (swatch == null) {
                                                swatch = new Swatch(mTextView.getResources().getColor(R.color.header_bright_opaque), 1);
                                            }

                                            // store swatch
                                            swatches.put(image.url.hashCode(), swatch);

                                            // change text color and background
                                            mTextView.setText(image.title);
                                            mTextView.setTextColor(swatch.getBodyTextColor());
                                            mTextView.setBackgroundColor(Color.HSVToColor(200, swatch.getHsl()));

                                            // animate title: fade-in
                                            ObjectAnimator animator = ObjectAnimator.ofFloat(mTextView, "Alpha", 0f, 1f);
                                            animator.setDuration(250).addListener(new AnimatorListenerAdapter() {
                                                                                      @Override
                                                                                      public void onAnimationStart(Animator animator) {
                                                                                          mTextView.setVisibility(View.VISIBLE);
                                                                                      }

                                                                                      @Override
                                                                                      public void onAnimationEnd(Animator animator) {

                                                                                      }
                                                                                  }
                                            );

                                            animator.start();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onError() {

                            }
                        } : null);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor, int position) {
            Image image = Image.fromCursor(cursor);
            viewHolder.bind(image, this, position);
        }
    }
}