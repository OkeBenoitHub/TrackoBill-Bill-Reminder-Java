package com.trackobill.www.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.thunder413.datetimeutils.DateTimeStyle;
import com.github.thunder413.datetimeutils.DateTimeUtils;
import com.trackobill.www.R;
import com.trackobill.www.database.BillEntry;
import com.trackobill.www.utils.MainUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BillsAdapter extends RecyclerView.Adapter<BillsAdapter.BillsViewHolder> {
    // Member variable to handle item clicks
    final private ItemClickListener mItemClickListener;
    // Class variables for the List that holds task data and the Context
    private List<BillEntry> mBillEntries;
    private Context mContext;
    private MainUtil mMainUtil;
    private boolean isPaidBillsActivity;

    public void setPaidBillsActivity(boolean paidBillsActivity) {
        isPaidBillsActivity = paidBillsActivity;
    }

    public BillsAdapter(Context context, List<BillEntry> billEntries, ItemClickListener itemClickListener) {
        mItemClickListener = itemClickListener;
        mBillEntries = billEntries;
        mContext = context;
        mMainUtil = new MainUtil(mContext);
    }

    @NonNull
    @Override
    public BillsAdapter.BillsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.bill_item, parent, false);
        return new BillsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillsAdapter.BillsViewHolder holder, int position) {
        BillEntry billEntry = mBillEntries.get(position);

        holder.mBillNameTV.setText(billEntry.getName());
        String billDueDateShortDescription = DateTimeUtils.formatWithStyle(billEntry.getDue_date(), DateTimeStyle.MEDIUM);
        String billAmountText = String.valueOf(billEntry.getAmount()).replace(".0","");
        holder.mBillAmountTV.setText("$" + billAmountText);

        if (billEntry.isIs_paid()) {
            holder.mBillPaidStatusTV.setText(" [ " + mContext.getString(R.string.paid_text) + " ] ");
            holder.mBillPaidStatusTV.setTextColor(mContext.getResources().getColor(R.color.greenish_bg_color));
        }
        if (billEntry.isIs_on_hold()){
            holder.mBillOnHoldStatusTV.setText(" [ " + mContext.getString(R.string.on_hold_text) + " ] ");
            holder.mBillOnHoldStatusTV.setTextColor(mContext.getResources().getColor(R.color.reddish_color));
            holder.mBillCheckAsHold.setVisibility(View.GONE);
            holder.mBillUndoCheckAsHold.setVisibility(View.VISIBLE);
        } else {
            holder.mBillOnHoldStatusTV.setText("");
            holder.mBillCheckAsHold.setVisibility(View.VISIBLE);
            holder.mBillUndoCheckAsHold.setVisibility(View.GONE);
        }

        holder.mBillDueDateTV.setText(mContext.getString(R.string.next_bill_text) + " " + billDueDateShortDescription);
    }

    @Override
    public int getItemCount() {
        if (mBillEntries == null) {
            return 0;
        }
        return mBillEntries.size();
    }

    public List<BillEntry> getBillEntries() {
        return mBillEntries;
    }

    /**
     * When data changes, this method updates the list of data
     * and notifies the adapter to use the new values on it
     */
    public void setBillEntries(List<BillEntry> billEntries) {
        mBillEntries = billEntries;
        notifyDataSetChanged();
    }

    public interface ItemClickListener {
        void onItemClickListener(int itemId);
        void onCheckAsPaidListener(int itemId);
        void onCheckAsHoldListener(int itemId);
        void onUndoCheckAsHoldListener(int itemId);
    }

    public class BillsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.bill_name_tv) TextView mBillNameTV;
        @BindView(R.id.bill_amount_tv) TextView mBillAmountTV;
        @BindView(R.id.check_paid) ImageButton mBillCheckAsPaid;
        @BindView(R.id.check_on_hold) ImageButton mBillCheckAsHold;
        @BindView(R.id.undo_on_hold) ImageButton mBillUndoCheckAsHold;
        @BindView(R.id.bill__paid_status_tv) TextView mBillPaidStatusTV;
        @BindView(R.id.bill__on_hold_status_tv) TextView mBillOnHoldStatusTV;
        @BindView(R.id.bill_due_date_tv) TextView mBillDueDateTV;

        public BillsViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
            itemView.setOnClickListener(this);
            mBillCheckAsPaid.setOnClickListener(this);
            mBillCheckAsHold.setOnClickListener(this);
            mBillUndoCheckAsHold.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int elementId = getAdapterPosition();
            if (view.getId() == R.id.check_paid) {
                mItemClickListener.onCheckAsPaidListener(elementId);
            } else if (view.getId() == R.id.check_on_hold) {
                mItemClickListener.onCheckAsHoldListener(elementId);
            } else if(view.getId() == R.id.undo_on_hold) {
                mItemClickListener.onUndoCheckAsHoldListener(elementId);
            } else {
                mItemClickListener.onItemClickListener(elementId);
            }
        }
    }
}
