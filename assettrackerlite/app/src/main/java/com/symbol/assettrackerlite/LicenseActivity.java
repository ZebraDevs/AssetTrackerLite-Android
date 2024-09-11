package com.symbol.assettrackerlite;

/**
 * Created by MMondal on 1/12/2018.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.BufferType;


public class LicenseActivity extends Activity {
    private TextView mAbout;
    protected void onCreate(Bundle savedInstanceState) {
        StringBuilder mAboutString = new StringBuilder();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        mAbout = (TextView) findViewById(R.id.about);
        BufferedReader br = null;

        try {
            InputStreamReader in = new InputStreamReader(getAssets().open("eula.txt"));
            br = new BufferedReader(in);
            String line;
            while ((line = br.readLine()) != null)
            {
                mAboutString.append(line);
                mAboutString.append("\n");
            }
          SpannableString res = new SpannableString(Html.fromHtml(mAboutString.toString()));
            mAbout.setText(res,BufferType.SPANNABLE);
        } catch (IOException e) {}
        finally
        {
            if (br != null) {
               try {
                    br.close();
                } catch (IOException e) {
                   Log.d ("LicenseActivity", e.getMessage());
                }
            }
        }
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
