package com.trackobill.www.models;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;

import com.trackobill.www.R;
import com.trackobill.www.database.AppDatabase;
import com.trackobill.www.database.BillEntry;

import java.util.List;

public class BillsViewModel extends AndroidViewModel {
    // Constant for logging
    private static final String TAG = BillsViewModel.class.getSimpleName();

    private LiveData<List<BillEntry>> mListLiveBillEntriesData;
    private LiveData<List<BillEntry>> mListLiveBillEntriesDataByAmountUp;
    private LiveData<List<BillEntry>> mListLiveBillEntriesDataByAmountDown;

    public BillsViewModel(Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(this.getApplication());
        Log.d(TAG, "Actively retrieving the bills data from the DataBase");
        mListLiveBillEntriesData = database.mBillEntryDao().loadAllRecurringBillsData();
        mListLiveBillEntriesDataByAmountDown = database.mBillEntryDao().loadAllRecurringBillsDataByAmountDown();
        mListLiveBillEntriesDataByAmountUp = database.mBillEntryDao().loadAllRecurringBillsDataByAmountUp();
    }

    public LiveData<List<BillEntry>> getListLiveBillEntriesData() {
        return mListLiveBillEntriesData;
    }

    public LiveData<List<BillEntry>> getListLiveBillEntriesDataByAmountUp() {
        return mListLiveBillEntriesDataByAmountUp;
    }

    public LiveData<List<BillEntry>> getListLiveBillEntriesDataByAmountDown() {
        return mListLiveBillEntriesDataByAmountDown;
    }
}
