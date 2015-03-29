package la.il.interview.ui.fragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import la.il.interview.R;
import la.il.interview.data.DataContract.History;
import la.il.interview.model.HistoryItem;
import la.il.interview.ui.widget.CursorRecyclerViewAdapter;
import la.il.interview.ui.widget.MutableCursor;
import la.il.interview.ui.widget.SwipeDismissRecyclerViewTouchListener;
import la.il.interview.ui.widget.SwipeDismissTouchListener;

import static la.il.interview.utils.LogUtils.makeLogTag;

/**
 * @author Amir Lazarovich
 */
public class HistoryFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(HistoryFragment.class);
    private RecyclerCursorAdapter mAdapter;

    public HistoryFragment() {
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
        mAdapter = new RecyclerCursorAdapter(context);
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
                            HistoryItem.remove(recyclerView.getContext(), cursor);
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
        return new CursorLoader(getActivity(), History.CONTENT_URI, null, null, null, History.DEFAULT_SORT);
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
        static final String DATE_FORMAT = "%Y-%m-%d %H:%M";
        Time time;

        public RecyclerCursorAdapter(Context context) {
            super(context, null);
            time = new Time();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_history_item, parent, false);
            return new ItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor, int position) {
            ((ItemViewHolder) viewHolder).bind(cursor, position);
        }

        public class ItemViewHolder extends ViewHolder {
            public TextView mTimestamp;
            public TextView mTerm;
            int mPosition;

            public ItemViewHolder(View view) {
                super(view);
                mTimestamp = (TextView) view.findViewById(R.id.timestamp);
                mTerm = (TextView) view.findViewById(R.id.term);

                view.setOnTouchListener(new SwipeDismissTouchListener(view, null, new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view, Object token) {
                        Cursor cursor = RecyclerCursorAdapter.this.getCursor();
                        cursor.moveToPosition(mPosition);
                        HistoryItem.remove(view.getContext(), cursor);
                        Cursor newCursor = new MutableCursor(cursor, mPosition);
                        RecyclerCursorAdapter.this.swapCursor(newCursor);
                    }
                }));
            }

            public void bind(final Cursor cursor, final int position) {
                // cache position
                mPosition = position;

                String term = HistoryItem.getColumnValue(cursor, History.HISTORY_TERM);
                long timestamp = HistoryItem.getColumnValueLong(cursor, History.HISTORY_TIMESTAMP);

                // parse timestamp
                time.set(timestamp);

                // set values
                mTerm.setText(term);
                mTimestamp.setText(time.format(DATE_FORMAT));
            }
        }

    }
}
