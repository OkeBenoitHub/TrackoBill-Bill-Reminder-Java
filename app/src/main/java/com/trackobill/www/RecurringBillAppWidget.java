package com.trackobill.www;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.widget.RemoteViews;

import com.github.thunder413.datetimeutils.DateTimeStyle;
import com.github.thunder413.datetimeutils.DateTimeUtils;
import com.trackobill.www.activities.RecurringBillActivity;
import com.trackobill.www.activities.ViewBillsActivity;
import com.trackobill.www.database.BillEntry;
import com.trackobill.www.sync.ReminderBillTask;

import org.parceler.Parcels;

import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class RecurringBillAppWidget extends AppWidgetProvider {
    public static String FORCE_WIDGET_UPDATE = "com.trackobill.www.FORCE_WIDGET_UPDATE";

    static void updateAppWidgets(Context context, final AppWidgetManager appWidgetManager,
                                final int[] appWidgetIds, final PendingResult pendingResult) {

        // create a thread to asynchronously load data to show on the widgets
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                List<BillEntry> billEntries = ReminderBillTask.getBillEntriesFromPreferences(context);
                if (billEntries != null) {
                    String bill_name_widget_value = billEntries.get(0).getName();
                    String billAmountText = String.valueOf(billEntries.get(0).getAmount()).replace(".0","");
                    String bill_paid_status_widget_value = "";
                    if (billEntries.get(0).isIs_paid()) {
                        bill_paid_status_widget_value = " [ " + context.getString(R.string.paid_text) + " ] ";
                    }
                    String bill_on_hold_widget_status_value = "";
                    if (billEntries.get(0).isIs_on_hold()) {
                        bill_on_hold_widget_status_value = " [ " + context.getString(R.string.on_hold_text) + " ] ";
                    }
                    String billDueDateShortDescription = DateTimeUtils.formatWithStyle(billEntries.get(0).getDue_date(), DateTimeStyle.MEDIUM);
                    String bill_due_date_widget_value = context.getString(R.string.next_bill_text) + " " + billDueDateShortDescription;

                    // Construct the RemoteViews object
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.recurring_bill_app_widget);
                    views.setTextViewText(R.id.bill_name_text_widget, bill_name_widget_value);
                    views.setTextViewText(R.id.bill_amount_text_widget, "$" + billAmountText);
                    views.setTextViewText(R.id.bill__paid_status_text_widget, bill_paid_status_widget_value);
                    views.setTextViewText(R.id.bill__on_hold_status_text_widget, bill_on_hold_widget_status_value);
                    views.setTextViewText(R.id.bill_due_date_text_widget, bill_due_date_widget_value);
                    views.setTextViewText(R.id.widget_upcoming_bill_text,context.getString(R.string.upcoming_bill_widget_text));

                    // Create an intent to launch RecurringBillActivity
                    Intent intent = new Intent(context, RecurringBillActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    Parcelable wrappedBillItem = Parcels.wrap(billEntries.get(0));
                    intent.putExtra(ViewBillsActivity.BILL_ITEM_EXTRA,wrappedBillItem);

                    // wrap it in a Pending Intent so another application can fire it by default
                    PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

                    // Assign the pending intent to be triggered when the assigned view is clicked
                    views.setOnClickPendingIntent(R.id.widget_main_layout,pendingIntent);

                    // Instruct the widget manager to update the widget
                    for (int appWidgetId: appWidgetIds) {
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }

                    if (pendingResult != null)
                        pendingResult.finish();
                }
            }
        };
        thread.start();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (FORCE_WIDGET_UPDATE.equals(intent.getAction())) {
            // update widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName recurringBillAppWidget = new ComponentName(context, RecurringBillAppWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(recurringBillAppWidget);

            final PendingResult pendingResult = goAsync();
            updateAppWidgets(context,appWidgetManager,appWidgetIds,pendingResult);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        PendingResult pendingResult = goAsync();
        updateAppWidgets(context,appWidgetManager,appWidgetIds,pendingResult);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName recurringBillAppWidget = new ComponentName(context, RecurringBillAppWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(recurringBillAppWidget);

        final PendingResult pendingResult = goAsync();
        updateAppWidgets(context,appWidgetManager,appWidgetIds,pendingResult);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

