package com.trackobill.www.activities;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.thunder413.datetimeutils.DateTimeStyle;
import com.github.thunder413.datetimeutils.DateTimeUtils;
import com.google.android.gms.ads.AdView;
import com.trackobill.www.R;
import com.trackobill.www.database.AppDatabase;
import com.trackobill.www.database.BillEntry;
import com.trackobill.www.models.AddBillEntryViewModel;
import com.trackobill.www.models.AddBillEntryViewModelFactory;
import com.trackobill.www.utils.AppExecutors;
import com.trackobill.www.utils.MainUtil;
import com.trackobill.www.utils.RecurringBillUtil;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;

public class RecurringBillActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {
    private static final String BILL_NAME_VALUE_EXTRA = "bill_name_value_extra";
    private static final String BILL_AMOUNT_VALUE_EXTRA = "bill_amount_value_extra";
    private static final String BILL_DUE_DATE_VALUE_EXTRA = "bill_due_date_value_extra";
    private static final String BILL_INTENT_ID_EXTRA = "bill_intent_extra_id";
    private MainUtil mMainUtil;
    private RecurringBillUtil mRecurringBillUtil;
    private String mBillDueDateValue;

    private DatePickerDialog mDatePickerDialog;

    private String mBillNameValue;
    private double mBillAmountValue;

    // Member variable for the Database
    private AppDatabase mDb;
    private int mBillIntentId;

    private boolean should_add_new_bill_data; // whether to add or update new bill data
    private BillEntry mBillEntry;
    private boolean mIsPaidBill,mIsOnHoldBill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_bill);
        ButterKnife.bind(this);

        /*
        ActionBar actionBar = this.getSupportActionBar();
        // Set the action bar back button to look like an up button
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }*/

        mMainUtil = new MainUtil(this);
        mRecurringBillUtil = new RecurringBillUtil(this);
        mDb = AppDatabase.getInstance(getApplicationContext());

        // load banner ad
        AdView mAdView = findViewById(R.id.adView);
        mMainUtil.loadBannerAdFromADMOB(mAdView);

        Calendar now = Calendar.getInstance();
        mDatePickerDialog = DatePickerDialog.newInstance(RecurringBillActivity.this,
                now.get(Calendar.YEAR), // Initial year selection
                now.get(Calendar.MONTH), // Initial month selection
                now.get(Calendar.DAY_OF_MONTH)
        );
        mBillDueDateValue = "";

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BILL_NAME_VALUE_EXTRA)) {
                mBillNameValue = savedInstanceState.getString(BILL_NAME_VALUE_EXTRA);
            }

            if (savedInstanceState.containsKey(BILL_AMOUNT_VALUE_EXTRA)) {
                mBillAmountValue = savedInstanceState.getDouble(BILL_AMOUNT_VALUE_EXTRA);
            }

            if (savedInstanceState.containsKey(BILL_DUE_DATE_VALUE_EXTRA)) {
                mBillDueDateValue = savedInstanceState.getString(BILL_DUE_DATE_VALUE_EXTRA);
                String billDueDateLongDescription = null;
                if (mBillDueDateValue != null) {
                    billDueDateLongDescription = DateTimeUtils.formatWithStyle(mBillDueDateValue, DateTimeStyle.FULL);
                }
                TextView billDueDateTV = findViewById(R.id.bill_due_date_tv);
                String bill_due_date_str = getString(R.string.bill_due_date_text);
                String full_bill_due_date = bill_due_date_str + "\n" + billDueDateLongDescription;
                billDueDateTV.setText(full_bill_due_date);
            }

            if (savedInstanceState.containsKey(BILL_INTENT_ID_EXTRA)) {
                mBillIntentId = savedInstanceState.getInt(BILL_INTENT_ID_EXTRA);
            }
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ViewBillsActivity.BILL_ITEM_EXTRA)){
            // update bill data
            should_add_new_bill_data = false;
            mBillEntry = Parcels.unwrap(getIntent().getParcelableExtra(ViewBillsActivity.BILL_ITEM_EXTRA));

            AddBillEntryViewModelFactory addBillEntryViewModelFactory = null;
            if (mBillEntry != null) {
                addBillEntryViewModelFactory = new AddBillEntryViewModelFactory(mDb, mBillEntry.getId());
            }
            final AddBillEntryViewModel addBillEntryViewModel;
            if (addBillEntryViewModelFactory != null) {
                addBillEntryViewModel = new ViewModelProvider(this, addBillEntryViewModelFactory).get(AddBillEntryViewModel.class);
                //viewModel.getTask().removeObserver(this);
                addBillEntryViewModel.getBillEntryLiveData().observe(this, this::updateRecurringBillUI);
            }
        } else {
            // add new bill data
            should_add_new_bill_data = true;
            mIsPaidBill = false;
            mIsOnHoldBill = false;
        }

        RelativeLayout bill_extra_actions_layout = findViewById(R.id.bill_action_box);
        if (should_add_new_bill_data) {
            bill_extra_actions_layout.setVisibility(View.GONE);
        }
    }

    private void updateRecurringBillUI(BillEntry billEntry) {
        if (billEntry != null) {
            mBillIntentId = billEntry.getId();
            mIsPaidBill = billEntry.isIs_paid();
            mIsOnHoldBill = billEntry.isIs_on_hold();
            // bill entry found :: update ui
            EditText billNameEditText = findViewById(R.id.bill_name_edit_text);
            billNameEditText.setText(billEntry.getName());

            EditText billAmountValueEditText = findViewById(R.id.bill_amount_edit_text);
            String billAmountText = String.valueOf(billEntry.getAmount()).replace(".0","");
            billAmountValueEditText.setText(billAmountText);

            mBillDueDateValue = billEntry.getDue_date();
            TextView billDueDateTV = findViewById(R.id.bill_due_date_tv);
            String bill_due_date_str = getString(R.string.bill_due_date_text);
            String billDueDateLongDescription = DateTimeUtils.formatWithStyle(mBillDueDateValue, DateTimeStyle.FULL);
            String full_bill_date = bill_due_date_str + "\n" + billDueDateLongDescription;
            billDueDateTV.setText(full_bill_date);

            ImageButton recurringBillCheckOnHold = findViewById(R.id.check_on_hold);
            ImageButton undoRecurringBillOnHold = findViewById(R.id.undo_on_hold);
            if (mIsOnHoldBill) {
                recurringBillCheckOnHold.setVisibility(View.GONE);
                undoRecurringBillOnHold.setVisibility(View.VISIBLE);
            } else {
                recurringBillCheckOnHold.setVisibility(View.VISIBLE);
                undoRecurringBillOnHold.setVisibility(View.GONE);
            }
        } else {
            mBillIntentId = -1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_bill_menu,menu);
        MenuItem saveRecurringBillItem = menu.findItem(R.id.save_recurring_bill);
        MenuItem deleteRecurringBillItem = menu.findItem(R.id.delete_recurring_bill);

        if (saveRecurringBillItem != null) {
            tintMenuIcon(RecurringBillActivity.this, saveRecurringBillItem,R.color.greenish_bg_color_dark);
        }
        if (deleteRecurringBillItem != null) {
            tintMenuIcon(RecurringBillActivity.this, deleteRecurringBillItem,R.color.greenish_bg_color_dark);
        }

        if (should_add_new_bill_data) {
            // hide delete menu item if add new bill entry
            if (deleteRecurringBillItem != null) {
                deleteRecurringBillItem.setVisible(false);
            }
        }
        return true;
    }

    public static void tintMenuIcon(Context context, MenuItem item, @ColorRes int color) {
        Drawable normalDrawable = item.getIcon();
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, context.getResources().getColor(color));
        item.setIcon(wrapDrawable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                Intent i = new Intent(this, ViewBillsActivity.class);
                i.putExtra("exit", true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                break;
            case R.id.save_recurring_bill:
                // save recurring bill
                saveRecurringBill();
                break;
            case R.id.delete_recurring_bill:
                // delete recurring bill
                deleteRecurringBill();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Delete recurring bill
     */
    private void deleteRecurringBill() {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(RecurringBillActivity.this);

        // Set a title for alert dialog
        builder.setTitle(R.string.delete_bill_title_box);

        // Ask the final question
        builder.setMessage(R.string.are_u_sure_delete_bill);

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    mDb.mBillEntryDao().deleteRecurringBillDataById(mBillIntentId);
                    mRecurringBillUtil.forceUpdateRecurringBillWidget();
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", "deleted");
                    setResult(Activity.RESULT_OK,returnIntent);
                    finish();
                }
            });
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.no_button_text, (dialog, which) -> {
            // Do something when No button clicked
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    /**
     * Save recurring bill
     */
    private void saveRecurringBill() {
        EditText billNameEditText = findViewById(R.id.bill_name_edit_text);
        mBillNameValue = billNameEditText.getText().toString().trim();
        // check for bill name
        if (mBillNameValue.isEmpty()) {
            billNameEditText.setError(getString(R.string.empty_bill_name_error_text));
            return;
        }
        // check for bill amount
        EditText billAmountValueEditText = findViewById(R.id.bill_amount_edit_text);
        if (billAmountValueEditText.getText().toString().trim().isEmpty()) {
            billAmountValueEditText.setError(getString(R.string.empty_bill_amount_error_text));
            return;
        }
        mBillAmountValue = Double.parseDouble(billAmountValueEditText.getText().toString().trim());
        if (mBillAmountValue <= 0) {
            billAmountValueEditText.setError(getString(R.string.invalid_bill_amount_error_text));
            return;
        }
        // check for bill due date
        if (mBillDueDateValue.equals("")) {
            mMainUtil.showToastMessage(getString(R.string.empty_bill_due_date_error_text));
            return;
        }
        mBillEntry = new BillEntry(mBillNameValue, mBillAmountValue, mBillDueDateValue, DateTimeUtils.formatDate(mBillDueDateValue), mIsPaidBill, mIsOnHoldBill);
        Intent returnIntent = new Intent();
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (!should_add_new_bill_data) {
                // update recurring bill data
                //update task
                if (mBillIntentId != -1) {
                    mBillEntry.setId(mBillIntentId);
                    mDb.mBillEntryDao().updateRecurringBillData(mBillEntry);
                    returnIntent.putExtra("result", "updated");
                } else {
                    mMainUtil.showToastMessage(getString(R.string.failed_update_bill));
                }
            } else {
                // add new recurring bill data
                mDb.mBillEntryDao().insertNewRecurringBillData(mBillEntry);
                returnIntent.putExtra("result","inserted");
            }
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        });
    }

    public void addEvent(String title, String location, long begin, long end) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void specifyBillDueDate(View view) {
        mDatePickerDialog.show(getSupportFragmentManager(), "dialogTag");
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        String date = dayOfMonth+"/"+(monthOfYear+1)+"/"+year;
        TextView billDueDateTV = findViewById(R.id.bill_due_date_tv);
        String bill_due_date_str = getString(R.string.bill_due_date_text);
        Date dateF = DateTimeUtils.formatDate(date);
        if (dateF.getTime() < new Date().getTime()) {
            mMainUtil.showToastMessage(getString(R.string.bill_date_future_error_text));
            return;
        }
        mBillDueDateValue = date;
        String billDueDateShortDescription = DateTimeUtils.formatWithStyle(mBillDueDateValue, DateTimeStyle.MEDIUM);
        String billDueDateLongDescription = DateTimeUtils.formatWithStyle(mBillDueDateValue, DateTimeStyle.FULL);
        mMainUtil.showToastMessage(bill_due_date_str + " " + billDueDateShortDescription);
        String full_bill_date = bill_due_date_str + "\n" + billDueDateLongDescription;
        billDueDateTV.setText(full_bill_date);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BILL_NAME_VALUE_EXTRA,mBillNameValue);
        outState.putDouble(BILL_AMOUNT_VALUE_EXTRA,mBillAmountValue);
        if (!mBillDueDateValue.equals("")) {
            outState.putString(BILL_DUE_DATE_VALUE_EXTRA, mBillDueDateValue);
        }
        outState.putInt(BILL_INTENT_ID_EXTRA,mBillIntentId);
    }

    public void markRecurringAsPaid(View view) {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(RecurringBillActivity.this);

        // Set a title for alert dialog
        builder.setTitle(R.string.mark_bill_as_paid_text);

        // Ask the final question
        builder.setMessage(R.string.next_month_bill_due_date);

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            //markRecurringBillAsPaid(itemId);
            List<BillEntry> billEntry = new ArrayList<>();
            billEntry.add(mBillEntry);
            mRecurringBillUtil.markRecurringBillAsPaid(0,billEntry);
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.no_button_text, (dialog, which) -> {
            // Do something when No button clicked
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    public void putRecurringBillOnHold(View view) {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(RecurringBillActivity.this);

        // Set a title for alert dialog
        builder.setTitle(R.string.mark_bill_on_hold_title_text);

        // Ask the final question
        builder.setMessage(R.string.not_notified_bill_on_hold_text);

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            List<BillEntry> billEntry = new ArrayList<>();
            billEntry.add(mBillEntry);
            mRecurringBillUtil.putRecurringBillOnHold(0,billEntry);
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.no_button_text, (dialog, which) -> {
            // Do something when No button clicked
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    public void undoRecurringBillOnHold(View view) {
        List<BillEntry> billEntry = new ArrayList<>();
        billEntry.add(mBillEntry);
        mRecurringBillUtil.undoRecurringBillOnHold(0,billEntry);
    }
}