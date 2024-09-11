package com.symbol.assettrackerlite;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.zebra.symbolsecurity.TrustedDevice;


public class MainActivity extends Activity {
    boolean selected;
    int check =0;
    static int LowMemThreshold_KB =1024;    // Memory Check for available memory
    static int WarmMemThreshold_KB = 1024*10; // Memory check for min available mem warning
    String TAG="AssetTrakerLite:MainActivity";
    private final static int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1;
    CheckBox dontShowAgain;

    public static final String EXPORT_STORAGE = "/data/tmp/public/";
    File currentfolder = new File(EXPORT_STORAGE + "AssetTrackerLite/");
    public static String FILESAVE_STORAGE = "";


    int importCount = 0;
    int modifyCount = 0;
    int totalCount=0;
    boolean corruptImport = false;
    protected static Context mCtx;

    public int GetFreeStorageMemKb() {
        long freestorageKb =0;
        try {
            StatFs statFs = new StatFs(EXPORT_STORAGE);
            //return free storage in  of KB
            freestorageKb = statFs.getAvailableBytes() / 1024;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }
        return (int) freestorageKb;
    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FILESAVE_STORAGE = getFilesDir().getPath() + "/";
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"Language"+Locale.getDefault().getDisplayLanguage());
        mCtx = getApplicationContext();
        Log.d(TAG, "at the start of onCreate.");

        try {
            if (!currentfolder.exists()){
                if(!currentfolder.mkdir()){
                    Log.d(TAG, "currentfolder not created.");
                }
                if (!currentfolder.setWritable(true, false)) {
                    Log.d(TAG, "setWritable is failed.");
                }
                if (!currentfolder.setReadable(true, false)) {
                    Log.d(TAG, "setReadable is failed.");
                }
                if (!currentfolder.setExecutable(true, false)) {
                    Log.d(TAG, "setExecutable is failed.");
                }
            }

        }catch (Exception e)
        {
            Log.e(TAG, e.getMessage());
        }

       // modelHandle();
        getDeviceSerial();

        int freeStorageMemKb = GetFreeStorageMemKb();

        if(freeStorageMemKb< WarmMemThreshold_KB && freeStorageMemKb > LowMemThreshold_KB) {
            final AlertDialog.Builder adb = new             AlertDialog.Builder(MainActivity.this);
            LayoutInflater adbInflater = LayoutInflater.from(MainActivity.this);
            View eulaLayout = adbInflater.inflate(R.layout.error_dialog_content, null);

            dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.notShowAgain);
            adb.setView(eulaLayout);
            adb.setTitle("Warning");
            adb.setMessage("System Strorage is limited, Asset Tracker Lite may not be able to save a large inventory file");
            adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences settings = getSharedPreferences("Warning", 0);
                    dialog.cancel();
                }
            });
            SharedPreferences settings = getSharedPreferences("Warning", 0);
            Boolean skipMessage = settings.getBoolean("skipMessage", false);
            if (skipMessage.equals(false)) {
                adb.show();
            }
        }
        else if (freeStorageMemKb < LowMemThreshold_KB) {
            Log.d(TAG, "Free Storage Mem:" + LowMemThreshold_KB);
            final AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this, R.style.MyAlertDialogStyle);
            LayoutInflater adbInflater = LayoutInflater.from(MainActivity.this);
            View eulaLayout = adbInflater.inflate(R.layout.error_dialog_content, null);
            adb.setView(eulaLayout);

            adb.setTitle("Alert");
            adb.setMessage("Insufficient Storage space available to save inventory files.");

            adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences settings = getSharedPreferences("Warning", 0);
                    dialog.cancel();
                    finish();
                }

            });
            adb.show();
        }
        else {
            Log.d(TAG, "Free Storage Mem (Greater than 100):" + freeStorageMemKb);
        }

        File[] files = getExternalFilesDirs(null);

        for (File file : files)
            Log.d("MainActivity","Storage Available"+file);

        readSettings();
        refreshSites();

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), InventoryActivity.class);
                i.putExtra("user", retrieveUser());
                i.putExtra("site", getSite());
                startActivity(i);
            }
        });
        button.setEnabled(false);

        final EditText personName = (EditText)findViewById(R.id.editText);
        personName.setImeActionLabel("Start", EditorInfo.IME_ACTION_GO);
        personName.setSingleLine();
        personName.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_GO) {
                            if(personName.getText().toString().length()>0){
                                Intent i = new Intent(getApplicationContext(), InventoryActivity.class);
                                i.putExtra("user", retrieveUser());
                                i.putExtra("site", getSite());
                                startActivity(i);
                            }
                            return true;
                        }
                        // Return true if you have consumed the action, else false.
                        return false;
                    }
                });
        personName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(personName.getText().toString().length() > 0){
                    button.setEnabled(true);
                }else{
                    button.setEnabled(false);
                }
            }
        });

        readSettings();

        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction("com.symbol.datawedge.api.RESULT_ACTION");
        registerReceiver(myBroadcastReceiver, filter);

        Intent i =new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.GET_PROFILES_LIST","");
        this.sendBroadcast(i);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean autoImportFlag = sharedPreferences.getBoolean("autoImport",false);
        String inputFormat = sharedPreferences.getString("outputformat","xml");

        if(autoImportFlag){
            autoImportInventory(inputFormat);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "in request permission.");
            if (!checkPermissions()) return;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myBroadcastReceiver);
    }

    public void importComplete(String filename){
        Log.d(TAG,"Auto Import Completed");
        Log.d(TAG, "Upload complete " + filename);

        File f = new File(currentfolder+"/"+filename);
        File nf = new File(currentfolder+"/imported_"+filename);
        if (!f.renameTo(nf))
        {
            Log.d(TAG,"rename failed.");
        }
    }

    public void autoImportInventory(String inputFormat){
        final Handler UIhandler = new Handler(Looper.getMainLooper());

        Log.d(TAG,"Auto Import Triggered");
        final String inputformat = inputFormat;

        try(DatabaseHelper myDb=new DatabaseHelper(getApplicationContext())) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            if (!Boolean.parseBoolean(sharedPreferences.getString("mergeflag", ""))) {
                myDb.clearAll();
            }
            SharedPreferences settings = getSharedPreferences("Warning", 0);
            Boolean skipMessage = settings.getBoolean("skipImportMessage", false);
            if (skipMessage.equals(false)) {
                Toast.makeText(getBaseContext(), "Importing..", Toast.LENGTH_SHORT).show();
            }
            totalCount = 0;
            importCount = 0;
            modifyCount = 0;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (inputformat.equalsIgnoreCase("csv")) {
                        InputStream instream = null;
                        try {
                            if (new File(currentfolder + "/autoImportInventory.csv").exists()) {
                                // open the file for reading
                                instream = new FileInputStream(currentfolder + "/autoImportInventory.csv");
                                // if file the available for reading
                                if (instream != null) {
                                    // prepare the file for reading
                                    InputStreamReader inputreader = new InputStreamReader(instream);
                                    try (BufferedReader buffreader = new BufferedReader(inputreader)) {
                                        // buffreader = new BufferedReader(inputreader);
                                        String line;
                                        // read every line of the file into the line-variable, on line at the time
                                        do {
                                            line = buffreader.readLine();
                                            Log.d(TAG + "Import", "Data Read" + line);

                                            String[] params = line.split(",");

                                            if (params[0].equals("")) {
                                                Log.d(TAG, "Reading CSV Null Data");
                                                corruptImport = true;
                                            }
                                            if (params[1].equals("")) {
                                                Log.d(TAG, "Reading CSV Null Data");
                                                corruptImport = true;
                                            }
                                            if (!corruptImport) {
                                                if (myDb.insertData(params[0], params[1], params[2])) {
                                                    importCount++;
                                                } else {
                                                    modifyCount++;
                                                }
                                            }
                                            totalCount++;

                                        } while (line != null);

                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception: " + e.getMessage());
                                    }

                                    Log.d(TAG + "Import", "Data Read Complete");
                                    File f = getDatabasePath(myDb.getDatabaseName());
                                    long dbSize = f.length();
                                    Log.d(TAG, "Size after import:" + dbSize);
                                    Log.d(TAG + "Finally Called", "Value of ModifiedImport" + modifyCount);

                                }
                            } else {
                                Log.d(TAG + "Import", "File not found for AutoImport");
                                corruptImport = true;
                            }
                        } catch (Exception ex) {
                            // print stack trace.
                            corruptImport = true;
                            Log.e(TAG, ex.getMessage());
                        } finally {
                            UIhandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setCorruptImport();
                                }
                            });
                            if(!corruptImport) {
                                importComplete("autoImportInventory.csv");
                            }
                            try {
                                if (instream != null) {
                                    instream.close();
                                }

                            } catch (IOException e) {
                                Log.e(TAG, "Exception: " + e.getMessage());
                            }


                        }
                    } else if (inputformat.equalsIgnoreCase("xml")) {
                        InputStream in_s = null;
                        try {
                            if (new File(currentfolder + "/autoImportInventory.xml").exists()) {
                                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                                factory.setNamespaceAware(true);
                                XmlPullParser parser = factory.newPullParser();

                                in_s = new FileInputStream(currentfolder + "/autoImportInventory.xml");
                                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                                parser.setInput(in_s, null);

                                int eventType = parser.getEventType();

                                while (eventType != XmlPullParser.END_DOCUMENT) {
                                    String tagname = parser.getName();
                                    String barcode = "";
                                    String value = "";
                                    String description = "";
                                    switch (eventType) {
                                        case XmlPullParser.START_DOCUMENT:
                                            Log.d(TAG, "at the start of the xml. " + barcode);
                                            break;
                                        case XmlPullParser.START_TAG:
                                            if (tagname.equalsIgnoreCase("Item")) {
                                                String text = parser.getAttributeName(0);
                                                barcode = parser.getAttributeValue(0);
                                                value = parser.getAttributeValue(2);
                                                description = parser.getAttributeValue(3);
                                                myDb.insertData(barcode, value, description);
                                                importCount++;
                                                totalCount++;
                                            }
                                            break;

                                        case XmlPullParser.TEXT:
                                            Log.d(TAG, "inside TEXT");
                                            break;

                                        case XmlPullParser.END_TAG:
                                            Log.d(TAG, "inside END TAG");
                                            break;

                                        default:
                                            break;
                                    }
                                    eventType = parser.next();
                                }
                            }
                            else {
                                Log.d(TAG + "Import", "File not found for AutoImport");
                                corruptImport = true;
                            }
                        } catch (Exception e) {
                            corruptImport = true;
                            Log.d(TAG, e.getMessage());
                        } finally {
                            UIhandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setCorruptImport();
                                }
                            });
                            Log.d(TAG + "Finally Called", "Value of CorruptImport" + corruptImport);
                            if (!corruptImport) {
                                importComplete("autoImportInventory.xml");
                            }
                            try {
                                if(in_s != null) {
                                    in_s.close();
                                }
                            } catch (IOException e) {
                                Log.e (TAG , "Exception: " + e.getMessage());
                            }

                        }
                    }
                }
            }).start();
        }
        catch (Exception e){
            Log.e (TAG, e.getMessage());
        }

    }

    public void setCorruptImport(){
        String message;
        if(corruptImport) {
            message = "File Corrupted/Cannot be found, cannot import product database.";
        }
        else {
            message = "Import Complete.";
        }

        final AlertDialog.Builder popup = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater adbInflater = LayoutInflater.from(MainActivity.this);
        View eulaLayout = adbInflater.inflate(R.layout.warning_dialog_content, null);

        dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.notShowAgain);
        popup.setView(eulaLayout);
        popup.setTitle("Import Summary");
        popup.setMessage(message+System.lineSeparator()+"Imported:"+importCount+System.lineSeparator()+"Updated:"+modifyCount+System.lineSeparator()+"Total:"+totalCount);
        popup.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences settings = getSharedPreferences("Warning", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("skipImportMessage", dontShowAgain.isChecked());
                editor.commit();
                dialog.cancel();
            }
        });
        SharedPreferences settings = getSharedPreferences("Warning", 0);
        Boolean skipMessage = settings.getBoolean("skipImportMessage", false);
        if (skipMessage.equals(false)) {
            popup.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);//Menu Resource, Menu
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.menu_addItem:
                i = new Intent(getApplicationContext(), AddModifyData.class);
                startActivity(i);
                return true;

            case R.id.menu_upload:
                return true;

            case R.id.menu_license:
                return true;

            case R.id.menu_about:
                return true;

            case R.id.menu_exit:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void importDW() {

        //NOTE: This Java code is for demo purposes only; it has not been checked for errors.
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG,"importDW called, ProgressBar Visible");

        String autoImportDir = "/enterprise/device/settings/datawedge/autoimport/";
        String temporaryFileName = "dwprofile_AssetTrackerLite.tmp";
        String finalFileName = "dwprofile_AssetTrackerLite.db";
        File outputFile ;
        File finalFile = null;
        File outputDirectory = new File(autoImportDir);
        outputFile = new File(outputDirectory, temporaryFileName);

        try(
                FileOutputStream fos = new FileOutputStream(outputFile);
                InputStream fis=getApplicationContext().getAssets().open("dwprofile_AssetTrackerLite.db");
        ) {
            finalFile = new File(outputDirectory, finalFileName);
            // transfer bytes from the input file to the output file
            byte[] buffer = new byte[1024];
            int length;
            int tot = 0;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
                tot += length;
            }
            Log.d("DEMO", tot + " bytes copied");
            //flush the buffers
            fos.flush();
        }catch (Exception e) {
            Log.e (TAG, "This is the DWImport Issue "+ e.getMessage());
        }
        //release resources
        finally {
            if (outputFile != null) {         //set permission to the file to read, write and exec.
                if (!outputFile.setExecutable(true, false)) {
                    Log.d(TAG, "setExecutable failed.");
                }
                if (!outputFile.setReadable(true, false)) {
                    Log.d(TAG, "setReadable failed.");
                }
                if (!outputFile.setWritable(true, false)) {
                    Log.d(TAG, "setWritable failed.");
                }
                //rename the file
                Log.d(TAG, "Output File Status Before" + outputFile.exists());
                if (!outputFile.renameTo(finalFile)) {
                    Log.d(TAG, "File rename failed.");
                }
                Log.d(TAG, "Output File Status After" + outputFile.exists());
                Log.d(TAG, "Finally Called, Progress Bar invisible");

                progressBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSites();

    }

   /* private boolean isTrustedDevice() {
        boolean trustedDevice = false;
        try {
            TrustedDevice aTrusted = new TrustedDevice();
            trustedDevice = aTrusted.isCallerNotAGoat(mCtx);
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e));
        }
        return trustedDevice;
    }*/

 /*   public void modelHandle(){
        String device_name = android.os.Build.MODEL;
        String manufacturer_name = Build.MANUFACTURER;
        Log.d("MainActivity","Device Name:"+!device_name.contains("TC2"));

        if(!isTrustedDevice()){
            final AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this, R.style.MyAlertDialogStyle);
            adb.setTitle(R.string.warning_title);
            adb.setMessage("This application will only work on Zebra Devices!");
            adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });

        }
        else {
            if(!(device_name.contains("TC2")|| device_name.contains("EC3")||device_name.contains("TC1")||device_name.contains("MC2"))){
                final AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater adbInflater = LayoutInflater.from(MainActivity.this);
                View eulaLayout = adbInflater.inflate(R.layout.warning_dialog_content, null);

                dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.notShowAgain);
                adb.setView(eulaLayout);
                adb.setTitle(R.string.warning_title);
                adb.setMessage(R.string.warning_message);
                adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences settings = getSharedPreferences("Warning", 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("skipMessage", dontShowAgain.isChecked());
                        editor.commit();
                        dialog.cancel();
                    }
                });
                SharedPreferences settings = getSharedPreferences("Warning", 0);
                Boolean skipMessage = settings.getBoolean("skipMessage", false);
                if (skipMessage.equals(false)) {
                    adb.show();
                }
            }
        }
    } */

   private String getDeviceSerial(){
        String myUri = "content://oem_info/oem.zebra.secure/build_serial";
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(Uri.parse(myUri),null, null, null, null);
        // Read the cursor
        String serialNumber = "";
        if ((cursor!=null)&& (cursor.getCount()>0)) {
            cursor.moveToFirst();
            serialNumber = cursor.getString(0);
        }

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = SP.edit();
        editor.putString("Device Serial",serialNumber);
        editor.commit();
        return serialNumber;
    }

    public void refreshSites(){
        Spinner s = (Spinner)findViewById(R.id.spinner);
        ArrayList<String> list = new ArrayList<String>();
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Set<String> areas = SP.getStringSet("arealist", null);
        if(areas == null || areas.size() == 0){
            list.add("Sales Floor");
        }else{
            for(String str : areas){
                list.add(str);
            }
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(dataAdapter);
    }

    private String getSite(){
        try {
            return ((Spinner) findViewById(R.id.spinner)).getSelectedItem().toString();
        }catch (Exception e){
            return "";
        }
    }

    private String retrieveUser(){
        return ((EditText)findViewById(R.id.editText)).getText().toString();
    }

    public void displayMenu(View v)
    {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                switch (item.getItemId()) {
                    case R.id.menu_settings:
                        Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.menu_upload:
                        alertScrollView();
                        return true;
                    case R.id.menu_addItem:
                        Intent j = new Intent(getApplicationContext(), AddModifyData.class);
                        j.putExtra("user", retrieveUser());
                        j.putExtra("site", getSite());
                        startActivity(j);
                        return true;
                    case R.id.menu_license:
                        Intent k = new Intent(getApplicationContext(), LicenseActivity.class);
                        startActivity(k);
                        return true;

                    case R.id.menu_about:
                        Intent l = new Intent(getApplicationContext(), AboutActivity.class);
                        startActivity(l);
                        return true;

                    case R.id.menu_exit:
                        finish();
                        return true;

                }
                return true;
            }
        });
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean allowupdates = SP.getBoolean("allowdbupdates",true);
        MenuInflater inflater = getMenuInflater();
        if(allowupdates) {
            inflater.inflate(R.menu.menu, popup.getMenu());
        }
        else {
            inflater.inflate(R.menu.associate_menu, popup.getMenu());
        }
        popup.show();
    }

    public class CustomEditTextNormal extends android.support.v7.widget.AppCompatEditText
    {
        public CustomEditTextNormal(Context context) {
            super(context);
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                onBackPressed();
                return false;
            }
            return super.dispatchKeyEvent(event);
        }

    }
    /*
     * Show AlertDialog with ScrollView.
     *
     * We use a TextView as ScrollView's child/host
     */
    public void alertScrollView() {

        /*
         * Inflate the XML view.
         *
         * @activity_main is in res/layout/scroll_text.xml
         */
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View myScrollView = inflater.inflate(R.layout.scroll_text, null, false);

        // textViewWithScroll is the name of our TextView on scroll_text.xml
        final LinearLayout ll = (LinearLayout) myScrollView
                .findViewById(R.id.scrollViewLinear);

        final LinearLayout ll2 = (LinearLayout) myScrollView
                .findViewById(R.id.scrollViewLinear2);


        File folder = new File(FILESAVE_STORAGE + "AssetTrackerLite/OutputFiles/");
        if(!folder.exists()){
            folder.mkdir();
            if (!folder.setReadable(true,false))
            {
                Log.d("InventoryActivity","setReadable is failed.");
            }
            if(!folder.setWritable(true, false))
            {
                Log.d("InventoryActivity","setWritable is failed.");
            }
            if(!folder.setExecutable(true,false))
            {
                Log.d("InventoryActivity","setExecutable is failed.");
            }
        }
        File folderExport = new File(EXPORT_STORAGE + "AssetTrackerLite/OutputFiles/");
        if(!folderExport.exists()){
            folderExport.mkdir();
            if (!folderExport.setReadable(true,false))
            {
                Log.d("InventoryActivity","setReadable is failed.");
            }
            if(!folderExport.setWritable(true, false))
            {
                Log.d("InventoryActivity","setWritable is failed.");
            }
            if(!folderExport.setExecutable(true,false))
            {
                Log.d("InventoryActivity","setExecutable is failed.");
            }
        }

        File[] listOfFiles = folder.listFiles();
        File[] listOfExportedFiles = folderExport.listFiles();

        if(listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    System.out.println("File " + listOfFiles[i].getName());
                    final CheckBox checkBox = new CheckBox(this);
                    checkBox.setId(i);
                    checkBox.setText(listOfFiles[i].getName());
                    checkBox.setTextSize(12);
                    boolean isCompleted = false;

                    try {
                        isCompleted = ((listOfFiles[i].getName().startsWith("uploaded")));
                    } catch (Exception e) {
                        Log.d (TAG, e.getMessage());
                    }

                    if (isCompleted) {
                        checkBox.setTextColor(Color.parseColor("#0077a0"));
                        ll2.addView(checkBox);
                    } else {
                        ll.addView(checkBox);
                    }
                    check = 0;
                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(checkBox.isChecked()) {
                                check++;
                                Log.d("MainActivity", "Selected a file"+check);
                            }
                            if(!checkBox.isChecked()){
                                check--;
                                Log.d("MainActivity", "UnChecked a file"+check);
                            }
                        }
                    });

                } else if (listOfFiles[i].isDirectory()) {
                    System.out.println("Directory " + listOfFiles[i].getName());
                }
            }
        }

        if(listOfExportedFiles != null) {
            for (int i = 0; i < listOfExportedFiles.length; i++) {
                if (listOfExportedFiles[i].isFile()) {
                    System.out.println("File " + listOfExportedFiles[i].getName());
                    final CheckBox checkBox = new CheckBox(this);
                    checkBox.setId(i);
                    checkBox.setText(listOfExportedFiles[i].getName());
                    checkBox.setTextSize(12);
                    boolean isCompleted = false;

                    try {
                        isCompleted = ((listOfExportedFiles[i].getName().startsWith("uploaded")));
                    } catch (Exception e) {
                        Log.d (TAG, e.getMessage());
                    }

                    if (isCompleted) {
                        checkBox.setTextColor(Color.parseColor("#0077a0"));
                        ll2.addView(checkBox);
                    } else {
                        ll.addView(checkBox);
                    }
                    check = 0;
                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(checkBox.isChecked()) {
                                check++;
                                Log.d("MainActivity", "Selected a file"+check);
                            }
                            if(!checkBox.isChecked()){
                                check--;
                                Log.d("MainActivity", "UnChecked a file"+check);
                            }
                        }
                    });

                } else if (listOfExportedFiles[i].isDirectory()) {
                    System.out.println("Directory " + listOfFiles[i].getName());
                }
            }
        }



        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this, R.style.MyAlertDialogStyle).setView(myScrollView)
                //.setTitle("Inventory Files\r\n ")
                .setPositiveButton("Upload", new DialogInterface.OnClickListener() {
                    @TargetApi(11)
                    public void onClick(final DialogInterface dialog, int id) {

                    }

                }).setNeutralButton("Select All", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @TargetApi(11)
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d("MainActivity","Dialog Dismissed");
                        dialog.cancel();
                    }

                }).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = ll.getChildCount();
                Log.d("MainActivity","Select All Clicked"+count);
                for(int i =0;i<count;i++){
                    View view = ll.getChildAt(i);
                    ((CheckBox)view).setChecked(true);
                    check++;
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(check>0){
                    selected=true;
                    Log.d("MainActivity","Selected"+selected+check);
                }
                else {
                    selected=false;
                    Log.d("MainActivity","Selected"+selected+check);
                }
                if(selected) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Uploading", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.d("MainActivity","Dialog Dismissed");
                    dialog.cancel();
                    new Thread(new Runnable() {
                        public void run() {
                            addNotification();
                            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                            String username = SP.getString("username","");
                            String password = SP.getString("password","");
                            String server = SP.getString("server","");
                            String serverType = SP.getString("serverType" , "");
                            String dir = FILESAVE_STORAGE + "AssetTrackerLite/OutputFiles/";
                            String dirExport = EXPORT_STORAGE + "AssetTrackerLite/OutputFiles/";
                            boolean secureServerFlag = SP.getBoolean("servertype",false);
                            Log.d("AssetTrackerLite", "connecting to  " + server+" ");
                            try {
                                if (serverType.equalsIgnoreCase("WebDav")) {
                                    //WebDavUploader webDavUploader = new WebDavUploader(server,username,password);
                                    final int childCount = ll.getChildCount();
                                    for (int i = 0; i < childCount; i++) {
                                        View v = ll.getChildAt(i);
                                        CheckBox cb = (CheckBox) v;
                                        if (cb.isChecked()) {
                                            File fullFilename = new File(dir + cb.getText());
                                            Log.d("AssetTrackerLite", "Uploading " + fullFilename);
                                            //webDavUploader.readData(fullFilename);
                                            //String result = webDavUploader.push("/"+cb.getText().toString());
                                            //Log.d("AssetTrackerLite", "Pushed" + result);
                                            uploadComplete(cb.getText().toString());
                                        }
                                    }
                                    removeNotification();
                                }
                                else {
                                    FTPUploader ftpu = new FTPUploader(server, username, password);
                                    final int childCount = ll.getChildCount();
                                    for (int i = 0; i < childCount; i++) {
                                        View v = ll.getChildAt(i);
                                        CheckBox cb = (CheckBox) v;
                                        if (cb.isChecked()) {
                                            String fullfilename= dir+cb.getText().toString();
                                            String filename = cb.getText().toString();
                                            if (filename.contains("ProductDatabase")){
                                                String fullFileNameExport = dirExport+cb.getText().toString();
                                                ftpu.uploadFile(fullFileNameExport, cb.getText().toString(), "AssetTrackerLite/");
                                                uploadComplete(cb.getText().toString());
                                            }
                                            else {
                                                ftpu.uploadFile(fullfilename, cb.getText().toString(), "AssetTrackerLite/");
                                                uploadComplete(cb.getText().toString());
                                            }
                                            Log.d("AssetTrackerLite", "Uploading " + fullfilename);
                                        }
                                    }

                                    ftpu.disconnect();
                                    removeNotification();
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Dialog uploadComplete = new Dialog(MainActivity.this);
                                        uploadComplete.setContentView(R.layout.upload_complete);
                                        uploadComplete.setTitle("System Message");

                                        TextView message = (TextView) uploadComplete.findViewById(R.id.uploadCompleteMsg);
                                        message.setText("Upload Complete");


                                        Button dialogButton = (Button) uploadComplete.findViewById(R.id.dialogButtonOK);
                                        dialogButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                uploadComplete.dismiss();
                                            }
                                        });

                                        uploadComplete.show();

                                    }
                                });
                            }catch (Exception e){
                                removeNotification();
                                Log.d (TAG, e.getMessage());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Upload Error", Toast.LENGTH_SHORT).show();
                                        final Dialog uploadComplete = new Dialog(MainActivity.this);
                                        uploadComplete.setContentView(R.layout.upload_complete);
                                        uploadComplete.setTitle("System Message");

                                        TextView message = (TextView) uploadComplete.findViewById(R.id.uploadCompleteMsg);
                                        message.setText("Upload Failed");

                                        Button dialogButton = (Button) uploadComplete.findViewById(R.id.dialogButtonOK);
                                        dialogButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                uploadComplete.dismiss();
                                            }
                                        });

                                        uploadComplete.show();
                                    }
                                });
                            }
                        }
                    }).start();
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "No Files Selected", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        });


    }

    public void onBackButton(View v)
    {
        onBackPressed();
    }

    public void uploadComplete(String fullfilename){
        Log.d("AssetTrackerLite", "Upload complete " + fullfilename);
        File path = new File(EXPORT_STORAGE + "AssetTrackerLite/OutputFiles/");
        File fileSavePath = new File(FILESAVE_STORAGE + "AssetTrackerLite/OutputFiles/");


        if(!path.exists()){
            path.mkdirs();
            if(!path.setReadable(true,false))
            {
                Log.d("InventoryActivity","setReadable is failed.");
            }
            if (!path.setWritable(true, false))
            {
                Log.d("InventoryActivity","setWritable is failed.");
            }
            if(!path.setExecutable(true,false))
            {
                Log.d("InventoryActivity","setExecutable is failed.");
            }
        }
        File f = new File(EXPORT_STORAGE + "AssetTrackerLite/OutputFiles/"+fullfilename);
        File nf = new File(EXPORT_STORAGE + "AssetTrackerLite/OutputFiles/uploaded_"+fullfilename);
        if(!f.renameTo(nf))
        {
            Log.d("InventoryActivity","rename is failed.");
        }

        if(!fileSavePath.exists()){
            fileSavePath.mkdirs();
            if(!fileSavePath.setReadable(true,false))
            {
                Log.d("InventoryActivity","setReadable is failed.");
            }
            if (!fileSavePath.setWritable(true, false))
            {
                Log.d("InventoryActivity","setWritable is failed.");
            }
            if(!fileSavePath.setExecutable(true,false))
            {
                Log.d("InventoryActivity","setExecutable is failed.");
            }
        }
        File fs = new File(FILESAVE_STORAGE + "AssetTrackerLite/OutputFiles/"+fullfilename);
        File nfs = new File(FILESAVE_STORAGE + "AssetTrackerLite/OutputFiles/uploaded_"+fullfilename);
        if(!fs.renameTo(nfs))
        {
            Log.d("InventoryActivity","rename is failed.");
        }
    }

    private void addNotification() {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_sys_upload);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(12883, builder.build());
    }

    // Remove notification
    private void removeNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(12883);
    }


    private void readSettings()  {

        XmlPullParserFactory pullParserFactory;
        //InputStream in_s = null;

        try (InputStream in_s = new FileInputStream(EXPORT_STORAGE + "AssetTrackerLite/settings.xml")){
            pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullParserFactory.newPullParser();

            //in_s = new FileInputStream(STORAGE + "AssetTrackerLite/settings.xml");
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in_s, null);

            parseXML(parser);

        } catch (XmlPullParserException e) {

            Log.d (TAG, "readSettings() "+e.getMessage());
        } catch (IOException e) {
            resetSettings();
            Log.d (TAG, "readSettings() "+e.getMessage());
        }
    }

    private void resetSettings(){

        try {

            File path = new File(EXPORT_STORAGE + "AssetTrackerLite/OutputFiles");
            path.mkdirs();
            if (!path.setReadable(true,false))
            {
                Log.d("InventoryActivity","setReadable is failed.");
            }
            if(!path.setWritable(true, false))
            {
                Log.d("InventoryActivity","setWritable is failed.");
            }
            if(!path.setExecutable(true,false))
            {
                Log.d("InventoryActivity","setExecutable is failed.");
            }
            File file = new File(EXPORT_STORAGE + "AssetTrackerLite/settings.xml");


            FileOutputStream stream = new FileOutputStream(file);
            try {
                String toWrite="<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<Settings>\n" +
                        "  <Connection>\n" +
                        "    <Server>server</Server>\n" +
                        "    <ServerType>servertype</ServerType>\n" +
                        "    <AutomaticSendResults>True</AutomaticSendResults>\n" +
                        "  </Connection>\n" +
                        "  <Application>\n" +
                        "    <AreaList>\n" +
                        "      <Item>Sales Floor</Item>\n" +
                        "      <Item>Stock Room</Item>\n" +
                        "    </AreaList>\n" +
                        "    <ItemGrouping>False</ItemGrouping>\n" +
                        "  </Application>\n" +
                        "  <Options>\n" +
                        "    <OutputDirectory>/AssetTrackerLite/OutputFiles/</OutputDirectory>\n" +
                        "    <AutoImport>false</AutoImport>\n"+
                        "    <SelectedOutput>CSV</SelectedOutput>\n" +
                        "    <AllowDBUpdates>True</AllowDBUpdates>\n" +
                        "  </Options>\n" +
                        "</Settings>";

                stream.write((toWrite).getBytes());
                if(!file.setExecutable(true, false))
                {
                    Log.d(TAG,"setExecutable is failed.");
                }
                if(!file.setWritable(true, false))
                {
                    Log.d(TAG,"setWritable is failed.");
                }
                if(!file.setReadable(true, false))
                {
                    Log.d(TAG,"setReadable is failed.");
                }

            } finally {
                stream.close();
            }
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    private void parseXML(XmlPullParser parser) throws XmlPullParserException,IOException
    {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Set<String> areaList = new HashSet<>();
        String user = "";
        String password = "";
        String server = "";
        String serverType = "";
        boolean automaticsendresults=false;
        boolean itemgrouping = false;
        boolean autoImport = false;
        String outputdirectory = "";
        String selectedOutput = "";
        boolean allowDBupdates = true;

        SharedPreferences.Editor editor = SP.edit();

        int eventType = parser.getEventType();
        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:

                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("server")) {
                            server = parser.nextText();
                            Log.d("AssetTrackerLite", "server = " + server);
                        } else if (name.equalsIgnoreCase("serverType")) {
                            serverType = parser.nextText();
                            Log.d("AssetTrackerLite", "serverType = " + serverType);
                        } else if (name.equalsIgnoreCase("automaticsendresults")) {
                            String text = parser.nextText();
                            if(text.equalsIgnoreCase("true")){
                                automaticsendresults = true;
                            }else{
                                automaticsendresults = false;
                            }
                            Log.d("AssetTrackerLite", "automaticsendresults = " + automaticsendresults);
                        } else if (name.equalsIgnoreCase("user")) {
                            user = parser.nextText();
                        } else if (name.equalsIgnoreCase("allowdbupdates")) {
                            String text = parser.nextText();
                            if(text.equalsIgnoreCase("true")){
                                allowDBupdates = true;
                            }else{
                                allowDBupdates = false;
                            }
                            Log.d("AssetTrackerLite", "allowDBupdates = " + text);
                        } else if (name.equalsIgnoreCase("password")) {
                            password = parser.nextText();
                        } else if (name.equalsIgnoreCase("item")) {
                            String text = parser.nextText();
                            areaList.add(text);
                            Log.d("AssetTrackerLite", "saw new area = " + text);
                        } else if (name.equalsIgnoreCase("itemgrouping")) {
                            String text = parser.nextText();
                            if(text.equalsIgnoreCase("true")){
                                itemgrouping = true;
                            }else{
                                itemgrouping = false;
                            }
                            Log.d("AssetTrackerLite", "itemgrouping = " + itemgrouping);
                        } else if (name.equalsIgnoreCase("outputdirectory")) {
                            outputdirectory = parser.nextText();
                            Log.d("AssetTrackerLite", "outputdirectory = " + outputdirectory);
                        } else if (name.equalsIgnoreCase("AutoImport")) {
                            String text = parser.nextText();
                            Log.d(TAG,"AutoImport"+text);
                            if(Boolean.parseBoolean(text)) {
                                autoImport = true;
                            }
                            Log.d("AssetTrackerLite", "autoimport = " + autoImport);
                        } else if (name.equalsIgnoreCase("selectedoutput")) {
                            selectedOutput = parser.nextText();
                            Log.d("AssetTrackerLite", "selectedoutput = " + selectedOutput);
                            if(selectedOutput.equalsIgnoreCase("csv")){
                                selectedOutput = "CSV";
                            }else if(selectedOutput.equalsIgnoreCase("xml")){
                                selectedOutput = "XML";
                            }else{
                                selectedOutput = "XML";
                            }

                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                }
                eventType = parser.next();
            }

            //set new prefs here**

            if(areaList!=null){
                editor.remove("arealist");
                editor.commit();
                editor.putStringSet("arealist", areaList);
                editor.commit();
            }
            editor.remove("server");
            editor.commit();
            editor.putString("server", server);
            editor.commit();

            editor.remove("serverType");
            editor.commit();
            editor.putString("serverType", serverType);
            editor.commit();

            editor.remove("username");
            editor.commit();
            editor.putString("username", user);
            editor.commit();

            editor.remove("password");
            editor.commit();
            editor.putString("password", password);
            editor.commit();

            editor.remove("outputformat");
            editor.commit();
            editor.putString("outputformat", selectedOutput);
            editor.commit();

            editor.remove("autoImport");
            editor.commit();
            editor.putBoolean("autoImport", autoImport);
            editor.commit();

            editor.remove("sessiondatadirectory");
            editor.commit();
            editor.putString("sessiondatadirectory", outputdirectory);
            editor.commit();

            editor.remove("inventorygrouping");
            editor.commit();
            editor.putBoolean("inventorygrouping", itemgrouping);
            editor.commit();

            editor.remove("sendresults");
            editor.commit();
            editor.putBoolean("sendresults", automaticsendresults);
            editor.commit();

            editor.remove("allowdbupdates");
            editor.commit();
            editor.putBoolean("allowdbupdates", allowDBupdates);
            editor.commit();


        }catch (Exception e){
            Log.d("AssetTrackerLite", "Settings file ill formed, resetting to default settings");
            resetSettings();
            Log.d (TAG, e.getMessage());
        }

    }
    @SuppressLint("NewApi")
    private boolean checkPermissions()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            //Get Permissions
            String[] requestedPermissions = packageInfo.requestedPermissions;
            List<String> neededPermissions = new ArrayList<String>();

            neededPermissions.clear();
            if(requestedPermissions == null) return true;
            for (int i = 0; i < requestedPermissions.length; i++)
            {
                if (checkSelfPermission(requestedPermissions[i]) == PackageManager.PERMISSION_DENIED)
                {
                    if(requestedPermissions[i].equals(Manifest.permission.POST_NOTIFICATIONS) && Build.VERSION_CODES.TIRAMISU > Build.VERSION.SDK_INT){
                        continue;
                    }
                    neededPermissions.add(requestedPermissions[i]);
                }
            }
            if (neededPermissions.size() == 0 ) return true;

            requestPermissions(neededPermissions.toArray(new String[neededPermissions.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

        } catch (Exception e)
        {
            Log.e (TAG, e.getMessage());
        }
        return false;
    }

    //Restart Activity based on new Permission changes
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults)
    {
        int REQUEST_PERMISSION = 1;
        int ctr=0;
        Log.d("OnReuquestCalled","");

        if (requestCode == REQUEST_PERMISSION) {
            // for each permission check if the user granted/denied them
            // you may want to group the rationale in a single dialog,
            // this is just an example
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission
                        ctr++;
                }
            }
        }
        if(ctr>0) {
           Toast.makeText(mCtx, "This app requires notification access to function properly. Please enable it in the settings.", Toast.LENGTH_LONG).show();
            }
        else {
            Log.d(TAG, "in else repeating oncreate.");
            Intent intent=new Intent();
            intent.setClass(this, this.getClass());
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        }

    }

    public BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"Intent :"+action);
            if(action.equals("com.symbol.datawedge.api.RESULT_ACTION"))
            {
                Bundle extras = getIntent().getExtras();
                //    Log.d(TAG,"Extras :"+extras.toString());
                Log.d(TAG,"Intent Action "+intent.getAction());
                if (intent.hasExtra("com.symbol.datawedge.api.RESULT_GET_PROFILES_LIST"))
                {
                    String[] profileList = intent.getStringArrayExtra("com.symbol.datawedge.api.RESULT_GET_PROFILES_LIST");
                    if ((profileList != null) && (profileList.length > 0)) {
                        boolean profileExists = false;
                        for (String s : profileList) {
                            Log.d(TAG,"Profile List"+s);
                            if (s.equals("AssetTrackerLite")) {
                                profileExists = true;
                                break;
                            }

                        }
                        if (!profileExists) {
                            Log.d(TAG,"Profile Doesn't exist");
                            try {
                                importDW();
                            }
                            catch(Exception e){
                                Log.e(TAG, "ImportDW Issue "+ e.getMessage());
                            }
                        }
                    }
                }
                else
                {
                    Log.d(TAG,"No DataWedge Profiles defined ");
                }
            }

        }
    };

}