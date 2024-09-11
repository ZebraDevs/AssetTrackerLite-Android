package com.symbol.assettrackerlite;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by MMondal on 2/15/2018.
 */

public class AboutActivity extends Activity{
    TextView mAbout;
    String version;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        mAbout = (TextView) findViewById(R.id.version);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e ("AboutActivity", e.getMessage());
        }
        String text ="Asset Tracker Lite Version: "+version;
        mAbout.setText(text);

    }
    /***********************************************************************************************************************/
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        finish();
        overridePendingTransition(0, 0);
    }
    /***********************************************************************************************************************/
    public void onBackButton(View v)
    {
        onBackPressed();
    }
}
