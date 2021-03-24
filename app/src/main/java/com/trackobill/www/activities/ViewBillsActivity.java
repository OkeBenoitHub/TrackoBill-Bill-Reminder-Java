package com.trackobill.www.activities;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.trackobill.www.R;
import com.trackobill.www.adapters.BillsAdapter;
import com.trackobill.www.database.AppDatabase;
import com.trackobill.www.database.BillEntry;
import com.trackobill.www.models.BillsViewModel;
import com.trackobill.www.utils.MainUtil;
import com.trackobill.www.utils.RecurringBillUtil;
import com.trackobill.www.utils.ReminderWorkUtil;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ViewBillsActivity extends AppCompatActivity implements BillsAdapter.ItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String BILL_ITEM_EXTRA = "bill_item_extra";
    private static final int LAUNCH_NEW_BILL_ACTIVITY = 500;
    private static final String IS_SEARCH_BILL_LAYOUT_VISIBLE_EXTRA = "is_search_bill_layout_visible_extra";
    private MainUtil mMainUtil;
    private RecurringBillUtil mRecurringBillUtil;
    private Intent mIntent;

    private AppDatabase mDb;
    private List<BillEntry> mBillEntries;
    private RecyclerView mBillsRecyclerView;
    private BillsAdapter mBillsAdapter;

    @BindView(R.id.progress_circular_view) ProgressBar mProgressBar;
    @BindView(R.id.no_bills_found_tv) TextView mNoBillsFoundTv;
    @BindView(R.id.search_for_bill) FloatingActionButton mFloatingActionButtonSearch;
    @BindView(R.id.fab_add_bill_widget) FloatingActionButton mFabAddBillActionButtonWidget;
    private RelativeLayout mSearch_bill_layout;
    private boolean isSearchLayoutVisible;
    private String mBillNameSearchValue;
    private ArrayList<BillEntry> mSearchBillEntriesResults;
    private RecyclerView mSearchBillsRecyclerView;
    @BindView(R.id.top_progress_circular_view) ProgressBar mTopProgressBar;
    @BindView(R.id.no_search_bills_found_tv) TextView mNoSearchBillsFoundTv;
    private EditText mBillNameSearchEditText;
    private BillsAdapter mSearchBillsAdapter;
    private boolean mHideBillsOnHoldFromMainBills;
    private SharedPreferences sharedPreferences;
    private String sort_recurring_bills_by;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bills);
        ButterKnife.bind(this);

        mMainUtil = new MainUtil(this);
        mRecurringBillUtil = new RecurringBillUtil(this);
        mSearch_bill_layout = findViewById(R.id.search_bill_layout);
        mSearch_bill_layout.setVisibility(View.INVISIBLE);
        isSearchLayoutVisible = false;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(IS_SEARCH_BILL_LAYOUT_VISIBLE_EXTRA)) {
                isSearchLayoutVisible = savedInstanceState.getBoolean(IS_SEARCH_BILL_LAYOUT_VISIBLE_EXTRA);
            }
        }

        if (isSearchLayoutVisible) {
            mSearch_bill_layout.setVisibility(View.VISIBLE);
        }

        // Set the RecyclerView to its corresponding view
        mBillsRecyclerView = findViewById(R.id.my_bills_recycler_view);

        // Set the layout for the RecyclerView to be a linear layout, which measures and
        // positions items within a RecyclerView into a linear list
        mBillsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mBillEntries = new ArrayList<>();

        // Initialize the adapter and attach it to the RecyclerView
        mBillsAdapter = new BillsAdapter(this,mBillEntries,this);
        mBillsRecyclerView.setAdapter(mBillsAdapter);

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(),DividerItemDecoration.VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.divider)));
        mBillsRecyclerView.addItemDecoration(decoration);

        /* Bill search properties **
         */
        mBillNameSearchEditText = findViewById(R.id.bill_search_edit_text);
        mSearchBillsRecyclerView = findViewById(R.id.search_bills_recycler_view);

        // Set the layout for the RecyclerView to be a linear layout, which measures and
        // positions items within a RecyclerView into a linear list
        mSearchBillsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mSearchBillEntriesResults = new ArrayList<>();

        // Initialize the adapter and attach it to the RecyclerView
        mSearchBillsAdapter = new BillsAdapter(this,mSearchBillEntriesResults,this);
        mSearchBillsRecyclerView.setAdapter(mSearchBillsAdapter);

        mSearchBillsRecyclerView.addItemDecoration(decoration);

        mSearchBillsRecyclerView.setVisibility(View.INVISIBLE);
        mNoSearchBillsFoundTv.setText(R.string.find_bill_by_name_text);
        mNoSearchBillsFoundTv.setVisibility(View.VISIBLE);

        //ReminderWorkUtil.cancelRecurringWork(this,"bill_due_date_reminder_work_tag");
        ReminderWorkUtil.scheduleRecurringBillReminder(this,"bill_due_date_reminder_work_tag");

        mDb = AppDatabase.getInstance(getApplicationContext());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setupViewModel();
    }

    private void setupViewModel() {
        BillsViewModel billsViewModel = new ViewModelProvider(this).get(BillsViewModel.class);
        mProgressBar.setVisibility(View.VISIBLE);
        sort_recurring_bills_by = sharedPreferences.getString(getString(R.string.pref_sort_recurring_bills_by_key), getResources().getString(R.string.pref_sort_recurring_bills_by_due_date));
        LiveData<List<BillEntry>> billEntriesData = billsViewModel.getListLiveBillEntriesData();
        if (sort_recurring_bills_by.equals("amount up")) {
            billEntriesData = billsViewModel.getListLiveBillEntriesDataByAmountDown();
        } else if (sort_recurring_bills_by.equals("amount down")) {
            billEntriesData = billsViewModel.getListLiveBillEntriesDataByAmountUp();
        }
        billEntriesData.observe(this, billEntries -> {
            mProgressBar.setVisibility(View.INVISIBLE);
            mBillEntries = billEntries;
            ArrayList<BillEntry> billEntriesPref = new ArrayList<>();
            mHideBillsOnHoldFromMainBills = sharedPreferences.getBoolean(getString(R.string.pref_hide_bills_on_hold_from_main_bills_key), getResources().getBoolean(R.bool.pref_default_hide_bills_on_hold_from_main_bills));
            if (!mBillEntries.isEmpty()) {
                mBillsRecyclerView.setVisibility(View.VISIBLE);
                mNoBillsFoundTv.setVisibility(View.INVISIBLE);
                mMainUtil.writeDataArrayListObjectToSharedPreferences("bill_items",mBillEntries);
                if (mHideBillsOnHoldFromMainBills) {
                    for (int i = 0; i < mBillEntries.size(); i++) {
                         if (!mBillEntries.get(i).isIs_on_hold()) {
                             billEntriesPref.add(mBillEntries.get(i));
                         }
                    }
                    mBillsAdapter.setBillEntries(billEntriesPref);
                } else {
                    mBillsAdapter.setBillEntries(mBillEntries);
                }

                mFloatingActionButtonSearch.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mFabAddBillActionButtonWidget.setVisibility(View.VISIBLE);
                }
            } else {
                mBillsRecyclerView.setVisibility(View.INVISIBLE);
                mNoBillsFoundTv.setVisibility(View.VISIBLE);
                mFloatingActionButtonSearch.setVisibility(View.INVISIBLE);
                mFabAddBillActionButtonWidget.setVisibility(View.INVISIBLE);
            }
            if (isSearchLayoutVisible) {
                if (mSearchBillEntriesResults.size() > 0) {
                    mBillNameSearchEditText.append(" ");
                    mBillNameSearchValue = mBillNameSearchEditText.getText().toString().trim();
                    mBillNameSearchEditText.setText(mBillNameSearchValue);
                    mBillNameSearchEditText.setSelection(mBillNameSearchEditText.getText().length());
                }
                searchForBillBasedOnName();
            }
        });
    }

    private void searchForBillBasedOnName() {
        // perform search for bill
        mBillNameSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mBillNameSearchValue = editable.toString().toLowerCase().trim();
                if (!mBillNameSearchValue.isEmpty()) {
                    if (!mBillEntries.isEmpty()) {
                        int billsFound = 0;
                        mSearchBillEntriesResults.clear();
                        for (int i = 0; i < mBillEntries.size(); i++) {
                            if (mBillEntries.get(i).getName().toLowerCase().contains(mBillNameSearchValue)) {
                                billsFound++;
                                mSearchBillEntriesResults.add(mBillEntries.get(i));
                            }
                        }
                        if (billsFound > 0) {
                            mSearchBillsRecyclerView.setVisibility(View.VISIBLE);
                            mNoSearchBillsFoundTv.setVisibility(View.INVISIBLE);
                            mSearchBillsAdapter.setBillEntries(mSearchBillEntriesResults);
                        } else {
                            // no results
                            mSearchBillsRecyclerView.setVisibility(View.INVISIBLE);
                            mNoSearchBillsFoundTv.setText(R.string.no_search_bills_found_tv);
                            mNoSearchBillsFoundTv.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // no results
                        mSearchBillsRecyclerView.setVisibility(View.INVISIBLE);
                        mNoSearchBillsFoundTv.setText(R.string.no_search_bills_found_tv);
                        mNoSearchBillsFoundTv.setVisibility(View.VISIBLE);
                    }
                } else {
                    mSearchBillsRecyclerView.setVisibility(View.INVISIBLE);
                    mNoSearchBillsFoundTv.setText(R.string.find_bill_by_name_text);
                    mNoSearchBillsFoundTv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bills_menu,menu);
        MenuItem billsOnHoldItem = menu.findItem(R.id.action_mark_as_bills_on_hold);
        MenuItem paidBillsItem = menu.findItem(R.id.action_mark_as_paid_bills);

        if (billsOnHoldItem != null) {
            tintMenuIcon(ViewBillsActivity.this, billsOnHoldItem,R.color.greenish_bg_color_dark);
        }
        if (paidBillsItem != null) {
            tintMenuIcon(ViewBillsActivity.this, paidBillsItem,R.color.greenish_bg_color_dark);
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
        String[] subjects = {};
        switch (id) {
            case R.id.action_mark_as_paid_bills:
                mIntent = new Intent(this, PaidBillsActivity.class);
                startActivity(mIntent);
                // paid bills
                break;
            case R.id.action_mark_as_bills_on_hold:
                // important bills
                mIntent = new Intent(this, BillsOnHoldActivity.class);
                startActivity(mIntent);
                break;
            case R.id.action_add_new_bill:
                // add new bill
                goToAddNewBillActivity(item.getActionView());
                break;
            case R.id.action_settings:
                // settings
                mIntent = new Intent(this, SettingsActivity.class);
                startActivity(mIntent);
                break;
            case R.id.feedback:
                // feedback
                mMainUtil.composeEmail(subjects,getString(R.string.feedback_menu_text),getString(R.string.give_us_feedback_text),getString(R.string.give_us_feedback_text_by));
                break;
            case R.id.report_issue:
                // report an issue
                mMainUtil.composeEmail(subjects,getString(R.string.report_an_issue_menu_text),getString(R.string.what_went_wrong_text),getString(R.string.report_an_issue_by));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void goToAddNewBillActivity(View view) {
        mIntent = new Intent(this, RecurringBillActivity.class);
        startActivityForResult(mIntent,LAUNCH_NEW_BILL_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LAUNCH_NEW_BILL_ACTIVITY) {
            if(resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                if (result != null) {
                    if (result.equalsIgnoreCase("inserted")) {
                        mMainUtil.showToastMessage(getString(R.string.new_bill_added_successfully_text));
                    } else if (result.equalsIgnoreCase("updated")) {
                        mMainUtil.showToastMessage(getString(R.string.recurring_bill_updated));
                    } else if (result.equalsIgnoreCase("deleted")) {
                        mMainUtil.showToastMessage(getString(R.string.recurring_bill_deleted));
                    }
                }
                finish();
                startActivity(getIntent());
            }
        }
    }

    @Override
    public void onItemClickListener(int itemId) {
        BillEntry billEntry = mBillEntries.get(itemId);
        if (isSearchLayoutVisible) {
            billEntry = mSearchBillEntriesResults.get(itemId);
        }
        Parcelable wrappedBillItem = Parcels.wrap(billEntry);
        mIntent = new Intent(this, RecurringBillActivity.class);
        mIntent.putExtra(BILL_ITEM_EXTRA,wrappedBillItem);
        startActivityForResult(mIntent,LAUNCH_NEW_BILL_ACTIVITY);
    }

    @Override
    public void onCheckAsPaidListener(int itemId) {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(ViewBillsActivity.this);

        // Set a title for alert dialog
        builder.setTitle(R.string.mark_bill_as_paid_text);

        // Ask the final question
        builder.setMessage(R.string.next_month_bill_due_date);

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            //markRecurringBillAsPaid(itemId);
            if (isSearchLayoutVisible) {
                mRecurringBillUtil.markRecurringBillAsPaid(itemId, mSearchBillEntriesResults);
            } else {
                mRecurringBillUtil.markRecurringBillAsPaid(itemId, mBillEntries);
            }
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.no_button_text, (dialog, which) -> {
            // Do something when No button clicked
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    @Override
    public void onCheckAsHoldListener(int itemId) {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(ViewBillsActivity.this);

        // Set a title for alert dialog
        builder.setTitle(R.string.mark_bill_on_hold_title_text);

        // Ask the final question
        builder.setMessage(R.string.not_notified_bill_on_hold_text);

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            if (isSearchLayoutVisible) {
                mRecurringBillUtil.putRecurringBillOnHold(itemId,mSearchBillEntriesResults);
            } else {
                mRecurringBillUtil.putRecurringBillOnHold(itemId,mBillEntries);
            }
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.no_button_text, (dialog, which) -> {
            // Do something when No button clicked
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    @Override
    public void onUndoCheckAsHoldListener(int itemId) {
        if (isSearchLayoutVisible) {
            //undoRecurringBillOnHold(itemId);
            mRecurringBillUtil.undoRecurringBillOnHold(itemId,mSearchBillEntriesResults);
        } else {
            mRecurringBillUtil.undoRecurringBillOnHold(itemId,mBillEntries);
        }
    }

    public void searchForBill(View view) {
        mSearch_bill_layout.setVisibility(View.VISIBLE);
        isSearchLayoutVisible = true;
        setupViewModel();
    }

    public void hideSearchBillLayout(View view) {
        mSearch_bill_layout.setVisibility(View.INVISIBLE);
        isSearchLayoutVisible = false;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_SEARCH_BILL_LAYOUT_VISIBLE_EXTRA,isSearchLayoutVisible);
    }

    public void addNextRecurringToHomeScreen(View view) {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(ViewBillsActivity.this);

        // Set a title for alert dialog
        builder.setTitle(R.string.add_widget_home_screen);

        // Ask the final question
        builder.setMessage(R.string.add_home_screen_widget_message);

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            mRecurringBillUtil.allowRecurringBillWidgetToHomeScreen();
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.no_button_text, (dialog, which) -> {
            // Do something when No button clicked
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister ViewBillsActivity as an OnPreferenceChangedListener to avoid any memory leaks.
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_hide_bills_on_hold_from_main_bills_key))) {
            mHideBillsOnHoldFromMainBills = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_default_hide_bills_on_hold_from_main_bills));
            setupViewModel();
        } else if (key.equals(getString(R.string.pref_sort_recurring_bills_by_key))) {
            sort_recurring_bills_by = sharedPreferences.getString(key,getResources().getString(R.string.pref_sort_recurring_bills_by_due_date));
            setupViewModel();
        }
    }
}