package com.trackobill.www.models;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.trackobill.www.database.AppDatabase;

public class AddBillEntryViewModelFactory extends ViewModelProvider.NewInstanceFactory {
    private final AppDatabase mDb;
    private final int mBillEntryId;

    public AddBillEntryViewModelFactory(AppDatabase database, int billEntryId) {
        mDb = database;
        mBillEntryId = billEntryId;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        //noinspection unchecked
        return (T) new AddBillEntryViewModel(mDb, mBillEntryId);
    }
}
