package com.trackobill.www.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdView;
import com.trackobill.www.R;
import com.trackobill.www.utils.MainUtil;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private MainUtil mMainUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mMainUtil = new MainUtil(this);
        // load banner ad
        AdView mAdView = findViewById(R.id.adView);
        mMainUtil.loadBannerAdFromADMOB(mAdView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // share app
        if (item.getItemId() == R.id.action_share_app) {
            String shareAboutAppTitle = getString(R.string.share_app_about_title);
            String message = getString(R.string.share_app_message_1) + "\n";
            message += getString(R.string.share_app_message_2) + "\n";
            message += getString(R.string.app_play_store_link);
            mMainUtil.shareTextData(shareAboutAppTitle,message);
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.viewBillsButton)
    public void viewBillsButtonTapped(View view) {
        // go to View Bills Activity
        Intent intent = new Intent(this,ViewBillsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}