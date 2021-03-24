package com.trackobill.www.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.trackobill.www.database.AppDatabase;
import com.trackobill.www.database.BillEntry;

public class AddBillEntryViewModel extends ViewModel {
    private final LiveData<BillEntry> mBillEntryLiveData;

    public AddBillEntryViewModel(AppDatabase database, int billEntryId) {
        mBillEntryLiveData = database.mBillEntryDao().loadRecurringBillDataById(billEntryId);
    }

    public LiveData<BillEntry> getBillEntryLiveData() {
        return mBillEntryLiveData;
    }
}
