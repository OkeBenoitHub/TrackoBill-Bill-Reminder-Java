package com.trackobill.www.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface BillEntryDao {
    @Query("SELECT * FROM bills_table ORDER BY due_date_long ASC")
    LiveData<List<BillEntry>> loadAllRecurringBillsData();

    @Query("SELECT * FROM bills_table ORDER BY amount DESC")
    LiveData<List<BillEntry>> loadAllRecurringBillsDataByAmountUp();

    @Query("SELECT * FROM bills_table ORDER BY amount ASC")
    LiveData<List<BillEntry>> loadAllRecurringBillsDataByAmountDown();

    @Query("SELECT * FROM bills_table WHERE is_paid = :is_paid ORDER BY due_date_long ASC")
    LiveData<List<BillEntry>> loadAllRecurringPaidBillsData(boolean is_paid);

    @Query("SELECT * FROM bills_table WHERE is_on_hold = :is_on_hold ORDER BY due_date_long ASC")
    LiveData<List<BillEntry>> loadAllRecurringOnHoldBillsData(boolean is_on_hold);

    @Insert
    void insertNewRecurringBillData(BillEntry billEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateRecurringBillData(BillEntry billEntry);

    @Query("UPDATE bills_table SET is_paid = :is_paid WHERE id = :id")
    public void markRecurringBillDataAsPaidById(int id, boolean is_paid);

    @Query("UPDATE bills_table SET due_date = :due_date, due_date_long = :due_date_long WHERE id = :id")
    public void updateRecurringBillDueDateById(int id, String due_date, Date due_date_long);

    @Query("UPDATE bills_table SET is_on_hold = :is_on_hold WHERE id = :id")
    public void markRecurringBillDataAsOnHoldById(int id, boolean is_on_hold);

    @Query("DELETE FROM bills_table WHERE id = :id")
    public void deleteRecurringBillDataById(int id);

    @Query("SELECT * FROM bills_table WHERE id = :id")
    LiveData<BillEntry> loadRecurringBillDataById(int id);
}
