package com.trackobill.www.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.trackobill.www.sync.ReminderBillTask;

public class WorkerService extends Worker {

    public WorkerService(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Do your work here.
        // COMPLETED (7) Use ReminderTasks to execute the new charging reminder task you made, use
        // this service as the context (WaterReminderFirebaseJobService.this) and return null
        // when finished.
        Context context = this.getApplicationContext();
        ReminderBillTask.executeBillTask(context, ReminderBillTask.ACTION_BILL_DUE_DATE_REMINDER, 0);

        // Return a ListenableWorker.Result
        Data outputData = getInputData();
        return Result.success(outputData);
    }

    @Override
    public void onStopped() {
        // Cleanup because you are being stopped.
    }
}
