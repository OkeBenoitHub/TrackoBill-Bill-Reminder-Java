package com.trackobill.www.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.trackobill.www.services.WorkerService;

import java.util.concurrent.TimeUnit;

public class ReminderWorkUtil {

    synchronized public static void scheduleRecurringBillReminder(@NonNull final Context context, String work_tag) {

        Data input = new Data.Builder()
                .putString("some_key", "some_value")
                .build();

        Constraints constraints;
            constraints = new Constraints.Builder()
                    // The Worker needs Network connectivity
                    //.setRequiredNetworkType(NetworkType.UNMETERED)
                    // Needs the device to be charging
                    .setRequiresBatteryNotLow(true)
                    //.setRequiresCharging(true)
                    .build();

        PeriodicWorkRequest request =
                // Executes MyWorker every 6h
                new PeriodicWorkRequest.Builder(WorkerService.class, 6, TimeUnit.HOURS)
                        // Sets the input data for the ListenableWorker
                        .setConstraints(constraints)
                        .setInputData(input)
                        .build();

        WorkManager.getInstance(context)
                // Use ExistingWorkPolicy.REPLACE to cancel and delete any existing pending
                // (uncompleted) work with the same unique name. Then, insert the newly-specified
                // work.
                .enqueueUniquePeriodicWork(work_tag, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void cancelRecurringWork(Context context, String work_tag) {
        WorkManager.getInstance(context).cancelUniqueWork(work_tag);
    }
}
