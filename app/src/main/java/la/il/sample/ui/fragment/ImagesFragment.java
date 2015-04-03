package la.il.sample.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import la.il.sample.R;
import la.il.sample.ui.activity.ImageActivity;
import la.il.sample.data.DataContract.Images;
import la.il.sample.model.Image;
import la.il.sample.ui.widget.CursorRecyclerViewAdapter;
import la.il.sample.ui.widget.MutableCursor;
import la.il.sample.ui.widget.SwipeDismissRecyclerViewTouchListener;
import la.il.sample.ui.widget.SwipeDismissTouchListener;

import static la.il.sample.utils.LogUtils.makeLogTag;


/**
 * @author Amir Lazarovich
 */
public class ImagesFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(ImagesFragment.class);
    public static final String KEY_SEARCH_CRITERIA = "search_criteria";
    private RecyclerCursorAdapter mAdapter;
    private String mSearchCriteria;

    public ImagesFragment() {
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
        mAdapter = new RecyclerCursorAdapter(context, mSearchCriteria);
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSearchCriteria = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(mSearchCriteria, getString(R.string.default_search_criteria));
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


    public static class RecyclerCursorAdapter extends CursorRecyclerViewAdapter<ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private String mSearchCriteria;
        SparseArray<Swatch> mSwatches;

        public RecyclerCursorAdapter(Context context, String searchCriteria) {
            super(context, null);
            enableHeader(true);
            mSwatches = new SparseArray<>();
            mSearchCriteria = searchCriteria;
        }

        @Override
        public int getItemViewType(int position) {
            return (position <= 0) ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header, parent, false);
                return new HeaderViewHolder(itemView, mSearchCriteria);
            } else {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
                return new ItemViewHolder(itemView);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor, int position) {
            int type = getItemViewType(position);
            if (cursor == null && type == TYPE_HEADER) {
                ((HeaderViewHolder) viewHolder).bind();
            } else {
//                Image image = Image.fromCursor(cursor);
                ((ItemViewHolder) viewHolder).bind(cursor, position);
            }
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {
            public EditText mEditText;

            public HeaderViewHolder(View view, String searchCriteria) {
                super(view);
                mEditText = (EditText) view;
                mEditText.setText(searchCriteria);
                mEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mEditText.getContext());
                        prefs.edit().putString(ImagesFragment.KEY_SEARCH_CRITERIA, s.toString()).apply();
                    }
                });
            }

            public void bind() {
                String criteria = PreferenceManager.getDefaultSharedPreferences(mEditText.getContext()).getString(ImagesFragment.KEY_SEARCH_CRITERIA, mEditText.getText().toString());
                mEditText.setText(criteria);
            }
        }

        public class ItemViewHolder extends ViewHolder implements Callback {
            public TextView mTextView;
            public ImageView mImageView;
            String mUrl;
            String mTitle;
            int mPosition;

            public ItemViewHolder(View view) {
                super(view);
                mTextView = (TextView) view.findViewById(R.id.text);
                mImageView = (ImageView) view.findViewById(R.id.image);

                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Activity activity = (Activity) v.getContext();
                        final Intent intent = new Intent(activity, ImageActivity.class);
                        intent.putExtra(ImageActivity.EXTRA_IMAGE_URL, mUrl);

                        if (VERSION.SDK_INT >= 21) {
                            final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, mImageView, "image");
                            activity.startActivity(intent, options.toBundle());
                        } else {
                            activity.startActivity(intent);
                        }
                    }
                });

                view.setOnTouchListener(new SwipeDismissTouchListener(view, null, new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view, Object token) {
                        Cursor cursor = RecyclerCursorAdapter.this.getCursor();
                        cursor.moveToPosition(mPosition);
                        Image.remove(view.getContext(), cursor);
                        Cursor newCursor = new MutableCursor(cursor, mPosition);
                        RecyclerCursorAdapter.this.swapCursor(newCursor);
                    }
                }));
            }

            public void bind(final Cursor cursor, final int position) {
                // cache values
                mUrl = Image.getColumnValue(cursor, Images.IMAGE_URL);
                mTitle = Image.getColumnValue(cursor, Images.IMAGE_TITLE);
                mPosition = position;

                // set text background
                boolean loadPalette = true;
                Swatch swatch = mSwatches.get(mUrl.hashCode());
                if (swatch == null) {
                    mTextView.setVisibility(View.INVISIBLE);
                    mTextView.setText(null);
                } else {
                    loadPalette = false;
                    if (mTextView.getVisibility() != View.VISIBLE) {
                        mTextView.setVisibility(View.VISIBLE);
                    }

                    mTextView.setText(mTitle);
                    mTextView.setTextColor(swatch.getBodyTextColor());
                    mTextView.setBackgroundColor(Color.HSVToColor(200, swatch.getHsl()));
                }

                // load cover image
                Context context = mTextView.getContext();
                Picasso.with(context)
                        .load(mUrl)
                        .placeholder(R.drawable.placeholder)
                        .into(mImageView, loadPalette ? this : null);
            }

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
                            mSwatches.put(mUrl.hashCode(), swatch);

                            // change text color and background
                            mTextView.setText(mTitle);
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
        }

    }
}