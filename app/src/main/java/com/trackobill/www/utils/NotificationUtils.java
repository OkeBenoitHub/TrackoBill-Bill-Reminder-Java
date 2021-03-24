package com.trackobill.www.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.text.Html;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.github.thunder413.datetimeutils.DateTimeStyle;
import com.github.thunder413.datetimeutils.DateTimeUtils;
import com.trackobill.www.R;
import com.trackobill.www.activities.RecurringBillActivity;
import com.trackobill.www.activities.ViewBillsActivity;
import com.trackobill.www.database.BillEntry;
import com.trackobill.www.services.RecurringBillReminderIntentService;
import com.trackobill.www.sync.ReminderBillTask;

import org.parceler.Parcels;

import java.util.Random;

/**
 * Notification Utils
 */
public class NotificationUtils {
    /**
     * This notification channel id is used to link notifications to this channel
     */
    private static final String BILL_DUE_DATE_REMINDER_NOTIFICATION_CHANNEL_ID = "reminder_bill_notification_channel";
    private static int ACTION_BILL_PENDING_INTENT_ID = 1;

    /**
     * Clear all visible notifications
     */
    public static void clearAllNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    /*
      Clear a notification by its ID
     */
    public static void clearNotificationById(Context context,int notificationId) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
        }
    }

    /**
     * Remind user about a specific Bill due date
     * @param context :: context
     * @param billEntry :: Specific bill entry object
     */
    public static void remindUserBillDueDateOnTime(Context context, BillEntry billEntry) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        /*
         * This notification ID can be used to access our notification after we've displayed it. This
         * can be handy when we need to cancel the notification, or perhaps update it. This number is
         * arbitrary and can be set to whatever you like. 1138 is in no way significant.
         */
        int BILL_DUE_DATE_REMINDER_NOTIFICATION_ID = billEntry.getId();
        Uri alarmSound;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    BILL_DUE_DATE_REMINDER_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.main_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.petals);
            mChannel.enableLights(true);
            mChannel.enableVibration(true);
            mChannel.setSound(alarmSound, attributes); // This is IMPORTANT

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }
        }
        alarmSound = Uri.parse("android.resource://"+context.getPackageName()+"/" + R.raw.petals);//Here is FILE_NAME is the name of file that you want to play
        String billAmountText = String.valueOf(billEntry.getAmount()).replace(".0","");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,BILL_DUE_DATE_REMINDER_NOTIFICATION_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSmallIcon(R.drawable.baseline_check_circle_white_36dp)
                .setLargeIcon(largeIcon(context))
                .setContentTitle(context.getString(R.string.bill_due_date_reminder_notification_title))
                .setContentText(billEntry.getName() + "\n" + "$" + billAmountText + "\n" + context.getString(R.string.due_on_text) + " " + DateTimeUtils.formatWithStyle(billEntry.getDue_date(), DateTimeStyle.FULL))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(billEntry.getName() + ": " + "$" + billAmountText + "<br/>" + context.getString(R.string.due_on_text) + " " + DateTimeUtils.formatWithStyle(billEntry.getDue_date(), DateTimeStyle.FULL))))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(contentIntent(context,billEntry))
                .setSound(alarmSound)
                .addAction(markRecurringBillAsPaidAction(context,billEntry))
                .addAction(putRecurringBillOnHoldAction(context,billEntry))
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }
        if (notificationManager != null) {
            notificationManager.notify(BILL_DUE_DATE_REMINDER_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    // Notification put bill on Hold action button
    private static NotificationCompat.Action putRecurringBillOnHoldAction(Context context, BillEntry billEntry) {
        Intent putRecurringBillOnHoldIntent = new Intent(context, RecurringBillReminderIntentService.class);
        putRecurringBillOnHoldIntent.putExtra("bill_id",billEntry.getId());
        putRecurringBillOnHoldIntent.setAction(ReminderBillTask.ACTION_PUT_BILL_ON_HOLD);
        ACTION_BILL_PENDING_INTENT_ID = billEntry.getId();
        PendingIntent putRecurringBillOnHoldPendingIntent = PendingIntent.getService(
                context,
                ACTION_BILL_PENDING_INTENT_ID,
                putRecurringBillOnHoldIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        return new NotificationCompat.Action(null,
                context.getString(R.string.put_on_hold_button_notification),
                putRecurringBillOnHoldPendingIntent);
    }

    // Notification mark bill as paid action button
    private static NotificationCompat.Action markRecurringBillAsPaidAction(Context context, BillEntry billEntry) {
        Intent markRecurringBillAsPaidIntent = new Intent(context, RecurringBillReminderIntentService.class);
        markRecurringBillAsPaidIntent.putExtra("bill_id",billEntry.getId());
        markRecurringBillAsPaidIntent.setAction(ReminderBillTask.ACTION_MARK_BILL_AS_PAID);
        ACTION_BILL_PENDING_INTENT_ID = billEntry.getId();
        PendingIntent markRecurringBillAsPaidPendingIntent = PendingIntent.getService(
                context,
                ACTION_BILL_PENDING_INTENT_ID,
                markRecurringBillAsPaidIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        return new NotificationCompat.Action(null,
                context.getString(R.string.mark_as_paid_button_notification),
                 markRecurringBillAsPaidPendingIntent);
    }

    /**
     * Pending Intent tap bill notification go to RecurringBillActivity
     * @param context :: context
     * @param billEntry :: bill entry object
     */
    private static PendingIntent contentIntent(Context context, BillEntry billEntry) {
        Intent startActivityIntent = new Intent(context, RecurringBillActivity.class);
        Random rand = new Random();
        Parcelable wrappedBillItem = Parcels.wrap(billEntry);
        startActivityIntent.putExtra(ViewBillsActivity.BILL_ITEM_EXTRA,wrappedBillItem);
        /*
         * This pending intent id is used to uniquely reference the pending intent
         */
        int BILL_DUE_DATE_REMINDER_PENDING_INTENT_ID = rand.nextInt(1000);
        return PendingIntent.getActivity(
                context,
                BILL_DUE_DATE_REMINDER_PENDING_INTENT_ID,
                startActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Bitmap largeIcon(Context context) {
        Resources res = context.getResources();
        return BitmapFactory.decodeResource(res, R.drawable.trackobill_128);
    }
}
