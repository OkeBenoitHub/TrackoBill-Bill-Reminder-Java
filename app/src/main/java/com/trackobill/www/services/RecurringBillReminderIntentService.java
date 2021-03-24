package com.trackobill.www.services;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.trackobill.www.sync.ReminderBillTask;

public class RecurringBillReminderIntentService extends IntentService {

    public RecurringBillReminderIntentService() {
        super("RecurringBillReminderIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String action = intent.getAction();
        int bill_id = intent.getExtras().getInt("bill_id");
        ReminderBillTask.executeBillTask(this, action, bill_id);
    }
}
