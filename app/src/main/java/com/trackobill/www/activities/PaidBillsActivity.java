package com.trackobill.www.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdView;
import com.trackobill.www.R;
import com.trackobill.www.adapters.BillsAdapter;
import com.trackobill.www.database.AppDatabase;
import com.trackobill.www.database.BillEntry;
import com.trackobill.www.models.BillsViewModel;
import com.trackobill.www.utils.MainUtil;
import com.trackobill.www.utils.RecurringBillUtil;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.trackobill.www.activities.ViewBillsActivity.BILL_ITEM_EXTRA;

public class PaidBillsActivity extends AppCompatActivity implements BillsAdapter.ItemClickListener {
    private static final int LAUNCH_NEW_BILL_ACTIVITY = 500;
    private MainUtil mMainUtil;
    private RecurringBillUtil mRecurringBillUtil;

    private AppDatabase mDb;
    private List<BillEntry> mBillEntries;
    private ArrayList<BillEntry> mPaidBillEntries;
    private RecyclerView mPaidBillsRecyclerView;
    private BillsAdapter mBillsAdapter;

    @BindView(R.id.progress_circular_view) ProgressBar mProgressBar;
    @BindView(R.id.no_paid_bills_found_tv) TextView mNoPaidBillsFoundTv;
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paid_bills);
        ButterKnife.bind(this);

        ActionBar actionBar = this.getSupportActionBar();
        // Set the action bar back button to look like an up button
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mMainUtil = new MainUtil(this);
        mRecurringBillUtil = new RecurringBillUtil(this);

        // load banner ad
        AdView mAdView = findViewById(R.id.adView);
        mMainUtil.loadBannerAdFromADMOB(mAdView);

        // Set the RecyclerView to its corresponding view
        mPaidBillsRecyclerView = findViewById(R.id.paid_bills_recycler_view);

        // Set the layout for the RecyclerView to be a linear layout, which measures and
        // positions items within a RecyclerView into a linear list
        mPaidBillsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mBillEntries = new ArrayList<>();
        mPaidBillEntries = new ArrayList<>();

        // Initialize the adapter and attach it to the RecyclerView
        mBillsAdapter = new BillsAdapter(this,mPaidBillEntries,this);
        mPaidBillsRecyclerView.setAdapter(mBillsAdapter);

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(),DividerItemDecoration.VERTICAL);
        decoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.divider)));
        mPaidBillsRecyclerView.addItemDecoration(decoration);

        mDb = AppDatabase.getInstance(getApplicationContext());
        setupViewModel();
    }

    private void setupViewModel() {
        BillsViewModel billsViewModel = new ViewModelProvider(this).get(BillsViewModel.class);
        mProgressBar.setVisibility(View.VISIBLE);
        billsViewModel.getListLiveBillEntriesData().observe(this, billEntries -> {
            mProgressBar.setVisibility(View.INVISIBLE);
            mBillEntries = billEntries;
            if (!mBillEntries.isEmpty()) {
                int j = 0;
                mPaidBillEntries.clear();
                for (int i = 0; i < mBillEntries.size(); i++) {
                    if (mBillEntries.get(i).isIs_paid()) {
                        j++;
                        mPaidBillEntries.add(mBillEntries.get(i));
                    }
                }
                if (j > 0) {
                    mPaidBillsRecyclerView.setVisibility(View.VISIBLE);
                    mNoPaidBillsFoundTv.setVisibility(View.INVISIBLE);
                    mBillsAdapter.setBillEntries(mPaidBillEntries);
                } else {
                    mPaidBillsRecyclerView.setVisibility(View.INVISIBLE);
                    mNoPaidBillsFoundTv.setVisibility(View.VISIBLE);
                }
            } else {
                mPaidBillsRecyclerView.setVisibility(View.INVISIBLE);
                mNoPaidBillsFoundTv.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LAUNCH_NEW_BILL_ACTIVITY) {
            if(resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                if (result.equalsIgnoreCase("inserted")) {
                    mMainUtil.showToastMessage(getString(R.string.new_bill_added_successfully_text));
                } else if (result.equalsIgnoreCase("updated")) {
                    mMainUtil.showToastMessage(getString(R.string.recurring_bill_updated));
                } else if (result.equalsIgnoreCase("deleted")) {
                    mMainUtil.showToastMessage(getString(R.string.recurring_bill_deleted));
                }
                finish();
                startActivity(getIntent());
            }
        }
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClickListener(int itemId) {
        BillEntry billEntry = mPaidBillEntries.get(itemId);
        Parcelable wrappedBillItem = Parcels.wrap(billEntry);
        mIntent = new Intent(this, RecurringBillActivity.class);
        mIntent.putExtra(BILL_ITEM_EXTRA,wrappedBillItem);
        startActivityForResult(mIntent,LAUNCH_NEW_BILL_ACTIVITY);
    }

    @Override
    public void onCheckAsPaidListener(int itemId) {
        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(PaidBillsActivity.this);

        // Set a title for alert dialog
        builder.setTitle(R.string.mark_bill_as_paid_text);

        // Ask the final question
        builder.setMessage(R.string.next_month_bill_due_date);

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            mRecurringBillUtil.markRecurringBillAsPaid(itemId,mPaidBillEntries);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(PaidBillsActivity.this);

        // Set a title for alert dialog
        builder.setTitle(R.string.mark_bill_on_hold_title_text);

        // Ask the final question
        builder.setMessage(R.string.not_notified_bill_on_hold_text);

        // Set the alert dialog yes button click listener
        builder.setPositiveButton(R.string.yes_button_text, (dialog, which) -> {
            // Do something when user clicked the Yes button
            mRecurringBillUtil.putRecurringBillOnHold(itemId,mPaidBillEntries);
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
        mRecurringBillUtil.undoRecurringBillOnHold(itemId,mPaidBillEntries);
    }
}