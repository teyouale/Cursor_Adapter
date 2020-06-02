package com.et.cursor_adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class SMSConversationsListFragment extends Fragment {

    private SMSConversationsListCursorAdapter cursorAdapter = null;
    private ListView listView = null;
    private int listPosition = 0;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SMSConversationsListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            listPosition = savedInstanceState.getInt("LIST_POSITION", 0);
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sms_conversations_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // cursor adapter
        cursorAdapter = new SMSConversationsListCursorAdapter(getContext());

        listView = (ListView) view.findViewById(R.id.rows_list);
        listView.setAdapter(cursorAdapter);

        TextView textEmptyView = (TextView) view.findViewById(R.id.text_empty);
        listView.setEmptyView(textEmptyView);
        loadListViewItems(listPosition, true, true);
    }

    private void loadListViewItems(int listPosition, boolean markSeen, boolean showProgress) {
        if (!isAdded()) {
            return;
        }
        int loaderId = 0;
        ConversationsLoaderCallbacks callbacks =
                new ConversationsLoaderCallbacks(getContext(), listView,
                        listPosition, cursorAdapter, markSeen, showProgress);

        LoaderManager manager = getLoaderManager();
        Loader<?> loader = manager.getLoader(loaderId);
        if (loader == null) {
            // init and run the items loader
            manager.initLoader(loaderId, null, callbacks);
        } else {
            // restart loader
            manager.restartLoader(loaderId, null, callbacks);
        }
    }

    private static class ConversationsLoader extends CursorLoader {
        public ConversationsLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            // get all SMS conversations
            ContactsAccessHelper db = ContactsAccessHelper.getInstance(getContext());
            Log.d("TAGA", "20: ");
            return db.getSMSConversations(getContext());
        }
    }
    private static class ConversationsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private SMSConversationsListCursorAdapter cursorAdapter;
        private Context context;
        private ListView listView;
        private int listPosition;
        private boolean markSeen;
        private boolean showProgress;

        public ConversationsLoaderCallbacks(Context context, ListView listView, int listPosition, SMSConversationsListCursorAdapter cursorAdapter, boolean markSeen, boolean showProgress) {
            this.context = context;
            this.listView = listView;
            this.listPosition = listPosition;
            this.cursorAdapter = cursorAdapter;
            this.markSeen = markSeen;
            this.showProgress = showProgress;

        }

        @NonNull
        @Override
        public Loader onCreateLoader(int id, @Nullable Bundle args) {
            return new ConversationsLoader(context);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
            cursorAdapter.changeCursor(cursor);

            // scroll list to the saved position
            listView.post(new Runnable() {
                @Override
                public void run() {
                    Cursor cursor = cursorAdapter.getCursor();
                    if (cursor != null && !cursor.isClosed() && cursor.getCount() > 0) {
                        listView.setSelection(listPosition);
                        listView.setVisibility(View.VISIBLE);
                    }
                }
            });
            Log.d("Finshed","Finshed");
        }


        @Override
        public void onLoaderReset(@NonNull Loader loader) {

        }
    }
}
