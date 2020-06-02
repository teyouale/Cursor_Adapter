package com.et.cursor_adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

class ContactsAccessHelper {
    private static final String TAG = ContactsAccessHelper.class.getName();
    private static volatile ContactsAccessHelper sInstance = null;
    private ContentResolver contentResolver = null;

    private ContactsAccessHelper(Context context) {
        contentResolver = context.getApplicationContext().getContentResolver();
    }

    public static ContactsAccessHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (ContactsAccessHelper.class) {
                if (sInstance == null) {
                    sInstance = new ContactsAccessHelper(context);
                }
            }

        }
        return sInstance;
    }
    private boolean validate(Cursor cursor) {
        if (cursor == null || cursor.isClosed()) return false;
        if (cursor.getCount() == 0) {
            cursor.close();
            return false;
        }
        return true;
    }

    // SMS data URIs
    private static final Uri URI_CONTENT_SMS = Uri.parse("content://sms");
    private static final Uri URI_CONTENT_SMS_INBOX = Uri.parse("content://sms/inbox");
    private static final Uri URI_CONTENT_SMS_CONVERSATIONS = Uri.parse("content://sms/conversations");
    private static final Uri URI_CONTENT_CALLS = Uri.parse("content://call_log/calls");

    // SMS data columns
    public static final String ID = "_id";
    public static final String ADDRESS = "address";
    public static final String BODY = "body";
    public static final String PERSON = "person";
    public static final String DATE = "date";
    public static final String DATE_SENT = "date_sent";
    public static final String PROTOCOL = "protocol";
    public static final String REPLY_PATH_PRESENT = "reply_path_present";
    public static final String SERVICE_CENTER = "service_center";
    public static final String SUBJECT = "subject";
    public static final String READ = "read";
    public static final String SEEN = "seen";
    public static final String TYPE = "type";
    public static final String STATUS = "status";
    public static final String DELIVERY_DATE = "delivery_date";
    public static final String THREAD_ID = "thread_id";
    public static final String MSG_COUNT = "msg_count";
    public static final String NAME = "name";


    // SMS conversation cursor wrapper
    public class SMSConversationWrapper extends CursorWrapper {
        private final int _THREAD_ID;

        private SMSConversationWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            _THREAD_ID = cursor.getColumnIndex(THREAD_ID);
        }

        @Nullable
        public SMSConversation getConversation(Context context) {
            int threadId = getInt(_THREAD_ID);
            return getSMSConversationByThreadId(context, threadId);
        }
    }
    public class SMSMessage {
        public final long id;
        public final int type;
        public final int status;
        public final long date;
        public final long deliveryDate;
        public final String person;
        public final String number;
        public final String body;

        SMSMessage(long id, int type, int status, long date, long deliveryDate,
                   String person, String number, String body) {
            this.id = id;
            this.type = type;
            this.status = status;
            this.date = date;
            this.deliveryDate = deliveryDate;
            this.person = person;
            this.number = number;
            this.body = body;
        }
    }


    private class SMSMessageCursorWrapper extends CursorWrapper {
        private final int _ID;
        private final int _TYPE;
        private final int _STATUS;
        private final int _DATE;
        private final int _DATE_SENT;
        private final int _DELIVERY_DATE;
        private final int _PERSON;
        private final int _NUMBER;
        private final int _BODY;

        private SMSMessageCursorWrapper(Cursor cursor) {
            super(cursor);

            cursor.moveToFirst();
            _ID = cursor.getColumnIndex(ID);
            _TYPE = cursor.getColumnIndex(TYPE);
            _STATUS = cursor.getColumnIndex(STATUS);
            _DATE = cursor.getColumnIndex(DATE);
            _DATE_SENT = cursor.getColumnIndex(DATE_SENT);
            _DELIVERY_DATE = cursor.getColumnIndex(DELIVERY_DATE);
            _NUMBER = cursor.getColumnIndex(ADDRESS);
            _PERSON = cursor.getColumnIndex(PERSON);
            _BODY = cursor.getColumnIndex(BODY);
        }

        SMSMessage getSMSMessage(boolean withContact) {
            long id = getLong(_ID);
            int type = getInt(_TYPE);
            int status = getInt(_STATUS);
            long date = getLong(_DATE);
            long date_sent = 0;
            if (_DATE_SENT >= 0) {
                date_sent = getLong(_DATE_SENT);
            } else if (_DELIVERY_DATE >= 0) {
                date_sent = getLong(_DELIVERY_DATE);
            }
            String number = getString(_NUMBER);
            number = normalizePhoneNumber(number);
            String body = getString(_BODY);
            String person = null;

            return new SMSMessage(id, type, status, date, date_sent, person, number, body);
        }
    }


    // Returns SMS conversation cursor wrapper
    @Nullable
    public SMSConversationWrapper getSMSConversations(Context context) {

        Log.d("TAG", "12");
        // select available conversation's data
        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS_CONVERSATIONS,
                new String[]{THREAD_ID + " as " + ID, THREAD_ID},
                null,
                null,
                DATE + " DESC");

        return (validate(cursor) ? new SMSConversationWrapper(cursor) : null);
    }

    private SMSMessageCursorWrapper getSMSMessagesByThreadId(Context context, int threadId,
                                                             boolean desc, int limit) {

        String orderClause = (desc ? DATE + " DESC " : DATE + " ASC ");
        String limitClause = (limit > 0 ? " LIMIT " + limit : "");
        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS,
                null,
                THREAD_ID + " = ? " +
                        // we don't support drafts yet
                        " AND " + ADDRESS + " NOT NULL ",
                new String[]{String.valueOf(threadId)},
                orderClause + limitClause);

        return (validate(cursor) ? new SMSMessageCursorWrapper(cursor) : null);
    }
    // Returns SMS conversation by thread id
    @Nullable
    private SMSConversation getSMSConversationByThreadId(Context context, int threadId) {

        SMSConversation smsConversation = null;

        // get the count of unread SMS in the thread
        int unread = getSMSMessagesUnreadCountByThreadId(context, threadId);
        // get date and address from the last SMS of the thread
        SMSMessageCursorWrapper cursor = getSMSMessagesByThreadId(context, threadId, true, 1);
        if (cursor != null) {
            SMSMessage sms = cursor.getSMSMessage(true);
            smsConversation = new SMSConversation(threadId, sms.date,
                    sms.person, sms.number, sms.body, unread);
            cursor.close();
        }

        return smsConversation;
    }
    public int getSMSMessagesUnreadCountByThreadId(Context context, int threadId) {
        Cursor cursor = contentResolver.query(
                URI_CONTENT_SMS_INBOX,
                new String[]{"COUNT(" + ID + ")"},
                THREAD_ID + " = ? AND " +
                        READ + " = ? ",
                new String[]{
                        String.valueOf(threadId),
                        String.valueOf(0)
                },
                null);

        int count = 0;
        if (validate(cursor)) {
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
        }

        return count;
    }

    public class SMSConversation {
        public final int threadId;
        public final long date;
        public final String person;
        public final String number;
        public final String snippet;
        public final int unread;

        SMSConversation(int threadId, long date, String person,
                        String number, String snippet, int unread) {
            this.threadId = threadId;
            this.date = date;
            this.person = person;
            this.number = number;
            this.snippet = snippet;
            this.unread = unread;
        }
    }

    // For the sake of performance we don't use comprehensive phone number pattern.
    // We just want to detect whether a phone number is digital but not symbolic.
    private static final Pattern digitalPhoneNumberPattern = Pattern.compile("[+]?[0-9-() ]+");
    // Is used for normalizing a phone number, removing from it brackets, dashes and spaces.
    private static final Pattern normalizePhoneNumberPattern = Pattern.compile("[-() ]");

    /**
     * If passed phone number is digital and not symbolic then normalizes
     * it, removing brackets, dashes and spaces.
     */
    public static String normalizePhoneNumber(@NonNull String number) {
        number = number.trim();
        if (digitalPhoneNumberPattern.matcher(number).matches()) {
            number = normalizePhoneNumberPattern.matcher(number).replaceAll("");
        }
        return number;
    }

    /**
     * Checks whether passed phone number is private
     */
    public static boolean isPrivatePhoneNumber(@Nullable String number) {
        try {
            if (number == null) {
                return true;
            }
            number = number.trim();
            if (number.isEmpty() || Long.valueOf(number) < 0) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

//---------------------------------------------------------------------

    private static void debug(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String s = cursor.getString(i);
            String n = cursor.getColumnName(i);
            sb.append("[").append(n).append("]=").append(s);
        }
        Log.d("TAG2", sb.toString());
    }

}
