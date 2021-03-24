package com.trackobill.www.sync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.github.thunder413.datetimeutils.DateTimeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.trackobill.www.R;
import com.trackobill.www.activities.BillsOnHoldActivity;
import com.trackobill.www.activities.PaidBillsActivity;
import com.trackobill.www.database.BillEntry;
import com.trackobill.www.services.WorkerService;
import com.trackobill.www.utils.NotificationUtils;
import com.trackobill.www.utils.RecurringBillUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReminderBillTask {
    public static final String ACTION_MARK_BILL_AS_PAID = "mark-bill-as-paid";
    public static final String ACTION_PUT_BILL_ON_HOLD = "put_bill-on-old";
    public static final String ACTION_DISMISS_NOTIFICATION = "dismiss-notification";
    public static final String ACTION_BILL_DUE_DATE_REMINDER = "bill-due-date-reminder";

    public static void executeBillTask(Context context, String action, int billId) {
        DateTimeUtils.setTimeZone("UTC");
        if (ACTION_MARK_BILL_AS_PAID.equals(action)) {
            markRecurringBillAsPaid(context,billId);
        } else if (ACTION_PUT_BILL_ON_HOLD.equals(action)) {
            putRecurringBillOnHold(context,billId);
        } else if (ACTION_DISMISS_NOTIFICATION.equals(action)) {
            NotificationUtils.clearAllNotifications(context);
        } else if (ACTION_BILL_DUE_DATE_REMINDER.equals(action)) {
            issueBillDueDateReminder(context);
        }
    }

    public static List<BillEntry> getBillEntriesFromPreferences(Context context) {
        List<BillEntry> billEntriesPref;
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                context.getString(R.string.package_name_text), Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("bill_items", "");
        if (!json.equals("")) {
            Type type = new TypeToken<List<BillEntry>>() {
            }.getType();
            billEntriesPref = gson.fromJson(json, type);
            if (billEntriesPref.size() > 0) {
                List<BillEntry> billEntries = new ArrayList<>();
                for (int i = 0; i < billEntriesPref.size(); i++) {
                    if (!billEntriesPref.get(i).isIs_on_hold()) {
                        billEntries.add(billEntriesPref.get(i));
                    }
                }
                if (billEntries.size() > 0) {
                    return billEntries;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private static void issueBillDueDateReminder(Context context) {
        List<BillEntry> billEntries = getBillEntriesFromPreferences(context);
        if (billEntries != null) {
            for (int i = 0; i < billEntries.size(); i++) {
                if (!billEntries.get(i).isIs_on_hold()) {
                    Date dateF = DateTimeUtils.formatDate(billEntries.get(i).getDue_date());
                    // Notify user as bill due date reach 24h :: user will be able to choose own settings
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    String reminderTimeLimitPref = sharedPreferences.getString(context.getString(R.string.pref_notify_prior_bill_due_date_key), context.getResources().getString(R.string.pref_notify_prior_bill_due_date_24h));
                    int reminderTimeLimit = 48 * 60 * 60 * 6; // 24h default
                    if (reminderTimeLimitPref.equals("48h")) {
                        reminderTimeLimit = 96 * 60 * 60 * 6; // 2 days prior
                    } else if (reminderTimeLimitPref.equals("168h")) {
                        reminderTimeLimit = 336 * 60 * 60 * 6;
                    }
                    if (dateF.getTime() - new Date().getTime() <= reminderTimeLimit) {
                        NotificationUtils.remindUserBillDueDateOnTime(context, billEntries.get(i));
                    }
                }
            }
        }
    }

    private static void putRecurringBillOnHold(Context context, int billId) {
        RecurringBillUtil recurringBillUtil = new RecurringBillUtil(context);
        List<BillEntry> billEntries = getBillEntriesFromPreferences(context);
        if (billEntries != null) {
            List<BillEntry> billEntry = new ArrayList<>();
            for (int j = 0; j < billEntries.size(); j++) {
                if (billEntries.get(j).getId() == billId) {
                    billEntry.add(billEntries.get(j));
                    recurringBillUtil.putRecurringBillOnHold(0,billEntry);
                    NotificationUtils.clearNotificationById(context,billId);
                    Intent intent = new Intent(context, BillsOnHoldActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        }
    }

    private static void markRecurringBillAsPaid(Context context, int billId) {
        RecurringBillUtil recurringBillUtil = new RecurringBillUtil(context);
        List<BillEntry> billEntries = getBillEntriesFromPreferences(context);
        if (billEntries != null) {
            List<BillEntry> billEntry = new ArrayList<>();
            for (int j = 0; j < billEntries.size(); j++) {
                if (billEntries.get(j).getId() == billId) {
                    billEntry.add(billEntries.get(j));
                    recurringBillUtil.markRecurringBillAsPaid(0,billEntry);
                    NotificationUtils.clearNotificationById(context,billId);
                    Intent intent = new Intent(context, PaidBillsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        }
    }
}
