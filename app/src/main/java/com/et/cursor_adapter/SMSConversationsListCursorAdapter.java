package com.et.cursor_adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.cursoradapter.widget.CursorAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

class SMSConversationsListCursorAdapter extends CursorAdapter {
    private final DateFormat timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
    private final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
    private final Calendar calendar = Calendar.getInstance();
    private Date datetime = new Date();
    private View.OnClickListener outerOnClickListener = null;
    private View.OnLongClickListener outerOnLongClickListener = null;
    private LongSparseArray<ContactsAccessHelper.SMSConversation> smsConversationCache = new LongSparseArray<>();
    private final int currentYear;
    private final int currentDay;

    public SMSConversationsListCursorAdapter(Context context) {
        super(context, null, 0);

        calendar.setTimeInMillis(System.currentTimeMillis());
        currentYear = calendar.get(Calendar.YEAR);
        currentDay = calendar.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_sms_conversations_list, parent, false);
        // view holder for the row
        ViewHolder viewHolder = new ViewHolder(view);
        // add view holder to the row
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // try to get a model from the cache
        long itemId = getItemId(cursor.getPosition());
        ContactsAccessHelper.SMSConversation model = smsConversationCache.get(itemId);
        if (model == null) {
            // get cursor wrapper
            ContactsAccessHelper.SMSConversationWrapper cursorWrapper = (ContactsAccessHelper.SMSConversationWrapper) cursor;
            // get model
            model = cursorWrapper.getConversation(context);
            // put it to the cache
            smsConversationCache.put(itemId, model);
        }
        // get view holder from the row
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // update the view holder with new model
        viewHolder.setModel(model);

    }

    // Holder of the view data
    private class ViewHolder {
        private ContactsAccessHelper.SMSConversation model;
        private View rowView;
        private TextView addressTextView;
        private TextView snippetTextView;
        private TextView dateTextView;
        private TextView unreadTextView;

        ViewHolder(View rowView) {
            this(rowView,
                    (TextView) rowView.findViewById(R.id.address),
                    (TextView) rowView.findViewById(R.id.snippet),
                    (TextView) rowView.findViewById(R.id.date),
                    (TextView) rowView.findViewById(R.id.unread_sms));
        }

        ViewHolder(View rowView,
                   TextView addressTextView,
                   TextView snippetTextView,
                   TextView dateTextView,
                   TextView unreadTextView) {
            this.model = null;
            this.rowView = rowView;
            this.addressTextView = addressTextView;
            this.snippetTextView = snippetTextView;
            this.dateTextView = dateTextView;
            this.unreadTextView = unreadTextView;
        }

        void setModel(@Nullable ContactsAccessHelper.SMSConversation model) {
            this.model = model;
            if (model == null) {
                rowView.setVisibility(View.GONE);
                return;
            }
            rowView.setVisibility(View.VISIBLE);
            String address;
            if (model.person != null) {
                address = model.person + "\n" + model.number;
            } else {
                address = model.number;
            }
            addressTextView.setText(address);
            snippetTextView.setText(model.snippet);

            String text;
            Date date = toDate(model.date);
            calendar.setTimeInMillis(model.date);
            // if current year
            if(calendar.get(Calendar.YEAR) == currentYear) {
                // if current day
                if(calendar.get(Calendar.DAY_OF_YEAR) == currentDay) {
                    // day-less format
                    text = timeFormat.format(date);
                } else {
                    // year-less format
                    //text = yearLessDateFormat.format(date);
                    text=null;
                }
            } else {
                // full format
                text = dateFormat.format(date);
            }
            dateTextView.setText(text);

            if (model.unread > 0) {
                unreadTextView.setText(String.valueOf(model.unread));
                unreadTextView.setVisibility(View.VISIBLE);
            } else {
                unreadTextView.setVisibility(View.GONE);
            }
        }

        private Date toDate(long time) {
            datetime.setTime(time);
            return datetime;
        }
    }
}
