package com.trackobill.www.utils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.github.thunder413.datetimeutils.DateTimeUtils;

import com.trackobill.www.R;
import com.trackobill.www.RecurringBillAppWidget;
import com.trackobill.www.database.AppDatabase;
import com.trackobill.www.database.BillEntry;

import java.util.Date;
import java.util.List;

/**
 * Recurring Bill Util
 */
public class RecurringBillUtil {
    private final AppDatabase mDb;
    private final Context mContext;
    private final MainUtil mMainUtil;

    public RecurringBillUtil(Context context) {
        mContext = context;
        mDb = AppDatabase.getInstance(context);
        mMainUtil = new MainUtil(context);
    }

    // Mark recurring bill as paid
    public void markRecurringBillAsPaid(int billId, List<BillEntry> billEntries) {
        final BillEntry billEntry = billEntries.get(billId);
        String bill_due_date_String = billEntry.getDue_date();
        String[] bill_due_date_Array = bill_due_date_String.split("/");
        int bill_due_date_day = Integer.parseInt(bill_due_date_Array[0]);
        int bill_due_date_month = Integer.parseInt(bill_due_date_Array[1]);
        int bill_due_date_year = Integer.parseInt(bill_due_date_Array[2]);
        if (bill_due_date_month >= 12) {
            bill_due_date_month = 1;
            bill_due_date_year++;
        } else {
            bill_due_date_month++;
        }
        String next_bill_due_date_String = bill_due_date_day + "/" + bill_due_date_month + "/" + bill_due_date_year;
        Date next_bill_due_date_Date = DateTimeUtils.formatDate(bill_due_date_day+1 + "/" + bill_due_date_month + "/" + bill_due_date_year);
        mMainUtil.showToastMessage(mContext.getString(R.string.recurring_bill_as_paid));
        AppExecutors.getInstance().diskIO().execute(() -> {
            mDb.mBillEntryDao().markRecurringBillDataAsPaidById(billEntry.getId(),true);
            mDb.mBillEntryDao().updateRecurringBillDueDateById(billEntry.getId(),next_bill_due_date_String,next_bill_due_date_Date);
            forceUpdateRecurringBillWidget();
        });
    }

    // Put recurring bill on hold
    public void putRecurringBillOnHold(int billId, List<BillEntry> billEntries) {
        final BillEntry billEntry = billEntries.get(billId);
        mMainUtil.showToastMessage(mContext.getString(R.string.recurring_bill_as_hold));
        AppExecutors.getInstance().diskIO().execute(() -> {
            mDb.mBillEntryDao().markRecurringBillDataAsOnHoldById(billEntry.getId(), true);
            forceUpdateRecurringBillWidget();
        });
    }

    // Undo recurring bill on hold
    public void undoRecurringBillOnHold(int billId, List<BillEntry> billEntries) {
        final BillEntry billEntry = billEntries.get(billId);
        mMainUtil.showToastMessage(mContext.getString(R.string.undo_bill_marked_hold));
        AppExecutors.getInstance().diskIO().execute(() -> {
            mDb.mBillEntryDao().markRecurringBillDataAsOnHoldById(billEntry.getId(), false);
            forceUpdateRecurringBillWidget();
        });
    }

    public void forceUpdateRecurringBillWidget() {
        Intent forceWidgetUpdate = new Intent(mContext, RecurringBillAppWidget.class);
        forceWidgetUpdate.setAction(RecurringBillAppWidget.FORCE_WIDGET_UPDATE);
        mContext.sendBroadcast(forceWidgetUpdate);
    }

    public void allowRecurringBillWidgetToHomeScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager mAppWidgetManager = mContext.getSystemService(AppWidgetManager.class);

            ComponentName myProvider = new ComponentName(mContext, RecurringBillAppWidget.class);

            Bundle b = new Bundle();
            b.putString("ggg", "ggg");
            if (mAppWidgetManager != null && mAppWidgetManager.isRequestPinAppWidgetSupported()) {
                Intent pinnedWidgetCallbackIntent = new Intent(mContext, RecurringBillAppWidget.class);
                PendingIntent successCallback = PendingIntent.getBroadcast(mContext, 0,
                        pinnedWidgetCallbackIntent, 0);

                mAppWidgetManager.requestPinAppWidget(myProvider, b, successCallback);
            }
        }
    }
}
