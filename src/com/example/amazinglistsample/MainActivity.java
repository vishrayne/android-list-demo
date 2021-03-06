package com.example.amazinglistsample;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amazinglistsample.db.TodoContentProvider;
import com.example.amazinglistsample.db.TodoTable;

public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // private static final int ACTIVITY_CREATE = 0;

    private SectionedCursorAdapter adapter;
    private ListView listView;
    private EditText noteSummaryET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.list);
        noteSummaryET = (EditText) findViewById(R.id.note_edit_text);

        fillData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onAddTodo(View v) {
        String summary = noteSummaryET.getText().toString();
        String status;
        if (TextUtils.isEmpty(summary)) {
            status = "Empty!";
        } else {
            ContentValues values = new ContentValues();
            values.put(TodoTable.COLUMN_CATEGORY, "sample category");
            values.put(TodoTable.COLUMN_SUMMARY, summary);
            values.put(TodoTable.COLUMN_DESCRIPTION, "sample description");
            Uri todoUri = getContentResolver().insert(TodoContentProvider.CONTENT_URI, values);
            status = "Saved as " + todoUri;
            noteSummaryET.setText("");
        }
        Toast.makeText(getBaseContext(), status, Toast.LENGTH_SHORT).show();
    }

    private void fillData() {
        getSupportLoaderManager().initLoader(0, null, this);
        adapter = new SectionedCursorAdapter(this, null, false);
        listView.setAdapter(adapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { TodoTable.COLUMN_ID, TodoTable.COLUMN_SUMMARY, TodoTable.COLUMN_NAME_CREATE_DATE };
        CursorLoader cursorLoader = new CursorLoader(this, TodoContentProvider.CONTENT_URI, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    /**
     * Custom sectioned Adapter. Source:
     * http://kmansoft.com/2010/11/16/adding-group-headers-to-listview/
     */
    class SectionedCursorAdapter extends CursorAdapter {
        private static final int VIEW_TYPE_GROUP_START = 0;
        private static final int VIEW_TYPE_GROUP_CONT = 1;
        private static final int VIEW_TYPE_COUNT = 2;

        private LayoutInflater mInflater;

        public int mColSummary = -1;
        public int mColCreatedAt = -1;

        private final SimpleDateFormat gDateFormatDataItem = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.US);

        public SectionedCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {
            super(context, cursor, autoRequery);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public int getItemViewType(int position) {
            // There is always a group header for the first data item
            if (position == 0) {
                return VIEW_TYPE_GROUP_START;
            }

            // For other items, decide based on current data
            Cursor cursor = getCursor();
            cursor.moveToPosition(position);
            boolean newGroup = isNewGroup(cursor, position);

            // Check item grouping
            if (newGroup) {
                return VIEW_TYPE_GROUP_START;
            } else {
                return VIEW_TYPE_GROUP_CONT;
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.itemLabel.setText(cursor.getString(mColSummary));
            holder.itemDate.setText("Position: " + cursor.getPosition());

            if (holder.itemHeader.getVisibility() == View.VISIBLE) {
                Date date = new Date(cursor.getLong(mColCreatedAt));
                holder.itemHeader.setText(gDateFormatDataItem.format(date));
            }

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            if (mColSummary == -1) {
                mColSummary = cursor.getColumnIndexOrThrow(TodoTable.COLUMN_SUMMARY);
            }
            if (mColCreatedAt == -1) {
                mColCreatedAt = cursor.getColumnIndexOrThrow(TodoTable.COLUMN_NAME_CREATE_DATE);
            }

            int position = cursor.getPosition();
            

            View rowView = mInflater.inflate(R.layout.item, parent, false);
            View header = rowView.findViewById(R.id.todo_header);

            if (getItemViewType(position) == VIEW_TYPE_GROUP_START) {
                // Ignore clicks on the list header
                header.setVisibility(View.VISIBLE);
                header.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // nop
                    }
                });
            } else {
                header.setVisibility(View.GONE);
            }

            ViewHolder holder = new ViewHolder();
            holder.itemLabel = (TextView) rowView.findViewById(R.id.todo_item);
            holder.itemDate = (TextView) rowView.findViewById(R.id.todo_date);
            holder.itemHeader = (TextView) header;
            rowView.setTag(holder);

            return rowView;
        }

        private boolean isNewGroup(Cursor cursor, int position) {
            // Get date values for current and previous data items
            long nWhenThis = cursor.getLong(mColCreatedAt);

            cursor.moveToPosition(position - 1);
            long nWhenPrev = cursor.getLong(mColCreatedAt);

            // Restore cursor position
            cursor.moveToPosition(position);

            // Compare date values, ignore time values
            Calendar calThis = Calendar.getInstance();
            calThis.setTimeInMillis(nWhenThis);

            Calendar calPrev = Calendar.getInstance();
            calPrev.setTimeInMillis(nWhenPrev);

            int nDayThis = calThis.get(Calendar.DAY_OF_YEAR);
            int nDayPrev = calPrev.get(Calendar.DAY_OF_YEAR);

            if (nDayThis != nDayPrev || calThis.get(Calendar.YEAR) != calPrev.get(Calendar.YEAR)) {
                return true;
            }

            return false;
        }

    }

    static class ViewHolder {
        public TextView itemLabel;
        public TextView itemDate;
        public TextView itemHeader;
    }

}
