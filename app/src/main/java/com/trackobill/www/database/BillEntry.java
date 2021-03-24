package com.trackobill.www.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.parceler.Parcel;

import java.util.Date;

@Entity(tableName = "bills_table")
@Parcel
public class BillEntry implements Comparable<BillEntry> {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private double amount;
    private String due_date;
    @ColumnInfo(name = "due_date_long")
    private Date dueDateLong;

    private boolean is_paid;
    private boolean is_on_hold;

    public BillEntry() {
    }

    @Ignore
    public BillEntry(int id, String name, double amount, String due_date, boolean is_paid, boolean is_on_hold) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.due_date = due_date;
        this.is_paid = is_paid;
        this.is_on_hold = is_on_hold;
    }

    public BillEntry(String name, double amount, String due_date, boolean is_paid, boolean is_on_hold) {
        this.name = name;
        this.amount = amount;
        this.due_date = due_date;
        this.is_paid = is_paid;
        this.is_on_hold = is_on_hold;
    }

    public BillEntry(String name, double amount, String due_date, Date dueDateLong, boolean is_paid, boolean is_on_hold) {
        this.name = name;
        this.amount = amount;
        this.due_date = due_date;
        this.dueDateLong = dueDateLong;
        this.is_paid = is_paid;
        this.is_on_hold = is_on_hold;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDue_date() {
        return due_date;
    }

    public void setDue_date(String due_date) {
        this.due_date = due_date;
    }

    public boolean isIs_paid() {
        return is_paid;
    }

    public void setIs_paid(boolean is_paid) {
        this.is_paid = is_paid;
    }

    public boolean isIs_on_hold() {
        return is_on_hold;
    }

    public void setIs_on_hold(boolean is_on_hold) {
        this.is_on_hold = is_on_hold;
    }

    public Date getDueDateLong() {
        return dueDateLong;
    }

    public void setDueDateLong(Date dueDateLong) {
        this.dueDateLong = dueDateLong;
    }

    @Override
    public int compareTo(BillEntry billEntry) {
        if (this.amount != billEntry.getAmount()) {
            return (int) (this.amount - billEntry.getAmount());
        }
        return this.name.compareTo(billEntry.getName());
    }
}
