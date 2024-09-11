package com.symbol.assettrackerlite;

import static com.symbol.assettrackerlite.MainActivity.EXPORT_STORAGE;
import static com.symbol.assettrackerlite.MainActivity.FILESAVE_STORAGE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InventoryActivity extends Activity {
    private boolean groupInventory = false;
    private boolean sendResults = false;
    private boolean hasData = false;
    private String outputFormat = "";
    private String site = "";
    private String user = "";
    private DatabaseHelper myDb;
    private String startDate = "";
    private String endDate = "";
    private int totalUnits = 0;
    private double totalValue = 0;
    private int rowCount = 0;
    private boolean newSKUadded = false;
    private String savedSKU ="";
    private boolean memoryFlag=false;
    private boolean noDataentered = false;
    private boolean warningShow = true;
    static String TAG = "InventoryActivity";

    private List<ArrayList<String>> inventory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_inventory);

        myDb = new DatabaseHelper(getApplicationContext());
        //parse name and location
        Intent i = getIntent();

        startDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());

        try {
            site = i.getStringExtra("site");
            user = i.getStringExtra("user");
        }catch (Exception e){
            Log.e (TAG, e.getMessage());
        }

        hasData = false;
        //get shared prefs
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        groupInventory = SP.getBoolean("inventorygrouping",false);
        sendResults = SP.getBoolean("sendresults",false);
        outputFormat = SP.getString("outputformat", "CSV");

        Button button = (Button) findViewById(R.id.stopButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stop();
            }
        });

        inventory = new ArrayList<ArrayList<String>>();
        Log.d(TAG, outputFormat);

        EditText val = (EditText)findViewById(R.id.itemUnits);
        val.setImeActionLabel("Add", EditorInfo.IME_ACTION_GO);
        val.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                        if (actionId == EditorInfo.IME_ACTION_GO) {
                            if(hasData){
                                addRow();
                            }
                            // Check if no view has focus:
                            View view = getCurrentFocus();
                            if (view != null) {
                                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }
                            return true;
                        }
                        // Return true if you have consumed the action, else false.
                        return false;
                    }
                });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("symbol.action.AssetTrackerLite");
        intentFilter.addCategory("android.intent.category.DEFAULT");
        registerReceiver(myBroadcastReceiver, intentFilter);
        if (newSKUadded) {
            newSKUadded = false;
            ((TextView) findViewById(R.id.itemDescription)).setText(myDb.getItemDesc(savedSKU));
            ((EditText) findViewById(R.id.itemUnits)).setText("1");
            ((TextView) findViewById(R.id.itemBarcode)).setText(savedSKU);
            ((TextView) findViewById(R.id.itemValue)).setText(myDb.getItemPrice(savedSKU));
        }
    }

    @Override
    public void onPause() {
        unregisterReceiver(myBroadcastReceiver);
        super.onPause();
    }


    public String itemPrice(String barcode){
        return "0.0";
    }

    public void addRow(){
        String itemBarcode = "" ;
        String itemUnits ="";
        String itemValue = "0.00" ;
        String itemDescription = "";

        try {
            itemBarcode = ((TextView) findViewById(R.id.itemBarcode)).getText().toString();
            itemUnits = ((TextView) findViewById(R.id.itemUnits)).getText().toString();
            itemValue = "" + (Double.parseDouble(((TextView) findViewById(R.id.itemValue)).getText().toString()) * Integer.parseInt(itemUnits));
            itemDescription = ((TextView) findViewById(R.id.itemDescription)).getText().toString();
        }
        catch (Exception e)
        {
            Log.d (TAG, e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.incompleteEntry, Toast.LENGTH_SHORT).show();
            return;
        }

        if(Integer.valueOf(itemUnits) == 0 )
        {
            return;
        }

        if(groupInventory){
            boolean didChange = false;
            for(ArrayList<String> list : inventory){
                if(list.get(1).equals(itemBarcode)){
                    int additional = 1;
                    try {
                        additional = Integer.parseInt(itemUnits);
                    }catch (Exception e){
                        Log.d (TAG, e.getMessage());
                    }
                    double additional2 = 1;
                    try {
                        additional2 = Double.parseDouble(itemValue);
                    }catch (Exception e){
                        Log.d (TAG, e.getMessage());
                    }
                    int current = Integer.parseInt(list.get(3));
                    int newVal = current+additional;
                    double current2 = Double.parseDouble(list.get(2));
                    double newVal2 = current2+additional2;
                    DecimalFormat df = new DecimalFormat("#.00");
                    list.set(2, ""+df.format(newVal2));
                    list.set(3, ""+newVal);
                    didChange = true;
                }
            }
            if(!didChange){
                ArrayList<String> lst = new ArrayList<String>();
                lst.add(itemDescription);
                lst.add(itemBarcode);
                DecimalFormat df = new DecimalFormat("#.00");
                double itemVal = Double.parseDouble(itemValue);
                lst.add(""+df.format(itemVal));
                lst.add(itemUnits);
                inventory.add(lst);
            }
        }else{
            ArrayList<String> lst = new ArrayList<String>();
            lst.add(itemDescription);
            lst.add(itemBarcode);
            DecimalFormat df = new DecimalFormat("#.00");
            double itemVal = Double.parseDouble(itemValue);
            lst.add(""+df.format(itemVal));
            lst.add(itemUnits);
            inventory.add(lst);
            //Toast.makeText(getApplicationContext(), "no grouping", Toast.LENGTH_SHORT).show();
        }

        final ListView lv=(ListView) findViewById(R.id.listView);
        final CustomAdapter c = new CustomAdapter(this, inventory);
        lv.setAdapter(c);
        lv.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                lv.setSelection(c.getCount() - 1);
            }
        });

        Log.d("InventoryActivity", "Added");

        ((TextView)findViewById(R.id.itemBarcode)).setText("");
        ((TextView)findViewById(R.id.itemValue)).setText("");
        ((TextView)findViewById(R.id.itemUnits)).setText("");
        ((TextView)findViewById(R.id.itemDescription)).setText("");
        hasData = false;

        updateTotals();
    }

    @Override
    public void onBackPressed() {
        askToExit();
    }

    private void dismissKeyboard(){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    private BroadcastReceiver myBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d("ScanReceiver","Scanned Received");
                    try{
                        String data = intent.getStringExtra("com.motorolasolutions.emdk.datawedge.data_string");
                        getScan(data);
                    }catch (Exception e){
                        Log.d (TAG, e.getMessage());
                    }
                }
            };

    private void getScan(final String data){
        if(hasData){
            addRow();
        }
        Log.i(TAG,"scanEvent"+data);
        rowCount++;
        Log.d(TAG,"Scanned Item"+rowCount);
        if(rowCount>100){
            final AlertDialog.Builder adb = new             AlertDialog.Builder(InventoryActivity.this);
            LayoutInflater adbInflater = LayoutInflater.from(InventoryActivity.this);
            View eulaLayout = adbInflater.inflate(R.layout.warning_dialog_content, null);
            if(warningShow) {
                final CheckBox dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.notShowAgain);
                adb.setView(eulaLayout);
                adb.setTitle("Attention");
                adb.setMessage("It is recommended that you save the scanned session before continue");
                adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences settings = getSharedPreferences("Warning", 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("savePrompt", dontShowAgain.isChecked());
                        editor.commit();
                        warningShow = true;
                        dialog.cancel();
                    }
                });
                SharedPreferences settings = getSharedPreferences("Warning", 0);
                Boolean skipMessage = settings.getBoolean("savePrompt", false);
                if (skipMessage.equals(false)) {
                    adb.show();
                    warningShow = false;
                }
            }
        }
        final Dialog dialog = new Dialog(InventoryActivity.this,R.style.cust_dialog);
        dialog.setContentView(R.layout.newitemfound);
        dialog.setTitle(R.string.dialogTitle);
        TextView message = (TextView)dialog.findViewById(R.id.tdescription);
        Button add = (Button)dialog.findViewById(R.id.add);
        Button cancel = (Button)dialog.findViewById(R.id.cancel);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean allowupdates = SP.getBoolean("allowdbupdates",true);

        message.setText(R.string.dialogTextViewMessage);
        if(myDb.getItemDesc(data).equals("unknown") && allowupdates == true){
            dialog.show();
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    dialog.dismiss();
                }
            });
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent out  = new Intent(InventoryActivity.this,AddModifyData.class);
                    out.putExtra("ScanData",data);
                    out.putExtra("site",site);
                    startActivity(out);
                    dialog.dismiss();
                }
            });
            newSKUadded = true;
            savedSKU = data;

        }
        ((TextView) findViewById(R.id.itemDescription)).setText(myDb.getItemDesc(data));
        ((EditText) findViewById(R.id.itemUnits)).setText("1");
        ((TextView) findViewById(R.id.itemBarcode)).setText(data);
        ((TextView) findViewById(R.id.itemValue)).setText(myDb.getItemPrice(data));
        hasData = true;

    }

    public void saveData(){
        if(hasData){
            addRow();
        }

        String toUpload = "";
        Log.d(TAG,"saveData Inventory Size:"+inventory.size());
        if(inventory.size()>0) {
            if (outputFormat.equals("CSV")) {
                toUpload = saveDataCSV();
            } else if (outputFormat.equals("XML")) {
                toUpload = saveDataXML();
            }

            final String uploadFile = toUpload;
            if (sendResults) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                            String username = SP.getString("username", "");
                            String password = SP.getString("password", "");
                            String server = SP.getString("server", "");
                            FTPUploader ftpu = new FTPUploader(server, username, password);
                            String name = uploadFile.split("/")[uploadFile.split("/").length - 1];
                            ftpu.uploadFile(uploadFile, name, "/AssetTrackerLite/");
                            ftpu.disconnect();
                            uploadComplete(name);
                        } catch (Exception e) {
                            Log.d ("InventoryActivity", e.getMessage());
                        }
                    }
                }).start();

            }
        }else {
            noDataentered = true;
        }
    }

    public void uploadComplete(String fullfilename){
        Log.d(TAG, "Upload complete " + fullfilename);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String sessiondatadirectory = SP.getString("sessiondatadirectory", "sessiondatadirectory");
        String[] storageFolder = sessiondatadirectory.split("/");
        String outputfolder = storageFolder[2]+"/"+storageFolder[3]+"/";
        Log.d(TAG,outputfolder);

        File f = new File(FILESAVE_STORAGE + outputfolder+fullfilename);
        File nf = new File(FILESAVE_STORAGE + "uploaded_"+fullfilename);

        if(!(f.renameTo(nf)))
        {
            Log.d(TAG,"File rename failed.");
        }
    }
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
        return serialNumber;
    }

    public String saveDataCSV(){
        String name = "";
        try {
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            String deviceSerial = getDeviceSerial();
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String sessiondatadirectory = SP.getString("sessiondatadirectory", "");
            Log.d(TAG,sessiondatadirectory);
            File path = new File(FILESAVE_STORAGE + sessiondatadirectory);
            path.mkdirs();

            if(!path.setReadable(true,false))
            {
                Log.d(TAG,"setReadable Failed...");
            }
            if(!path.setWritable(true, false))
            {
                Log.d(TAG,"setWritable failed.");
            }
            if(!path.setExecutable(true,false))
            {
                Log.d(TAG,"setExecutable failed.");
            }
            File file = new File(FILESAVE_STORAGE + sessiondatadirectory+timeStamp+"_"+deviceSerial+"_"+user+"_"+site.replace(" ","")+".csv");
            name = file.getAbsolutePath();
            Log.d (TAG, "CSV File Name" + file.toString());
            endDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());

            FileOutputStream stream = new FileOutputStream(file);
            try {
                for(ArrayList<String> item : inventory){
                    Log.d("Inventory","Size:"+inventory.size());
                    stream.write((version+","+deviceSerial+","+user+","+site+","+startDate+","+endDate+","+totalUnits+","+totalValue+","+item.get(1)+","+item.get(3)+","+item.get(2)+","+item.get(0)+","+"\r\n").getBytes());
                }
                       if(!file.setReadable(true, false))
                       {
                           Log.d(TAG, "setReadable failed.");
                       }
                        if(!file.setWritable(true, false))
                        {
                            Log.d(TAG, "setWritable failed.");
                        }
                        if(!file.setExecutable(true, false))
                        {
                            Log.d(TAG, "setExecutable failed.");
                        }

            }catch (IOException e) {
                memoryFlag = true;
                Log.e("Exception", "File create failed: " + e.toString());
                Log.d("SettingsActivity","File Length="+file.length()+" FileName:"+file.getName());
                Log.d("SettingsActivity","Deleting File"+file.delete());
            }finally {
                stream.close();
            }
        }catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }catch(PackageManager.NameNotFoundException e2){
            Log.e (TAG, e2.getMessage());
        }
        return name;
    }

    public String saveDataXML(){
       String name = "";
        try {
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            String deviceSerial = getDeviceSerial();

            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String sessiondatadirectory = SP.getString("sessiondatadirectory", "");
            Log.d(TAG,sessiondatadirectory);
            File path = new File(FILESAVE_STORAGE + sessiondatadirectory);
            path.mkdirs();
            if(!path.setReadable(true,false))
            {
                Log.d(TAG,"setReadable is failed.");
            }
            if(!path.setWritable(true, false))
            {
                Log.d(TAG,"setWritable is failed.");
            }
            if(!path.setExecutable(true,false))
            {
                Log.d(TAG,"setExecutable is failed.");
            }
            File file = new File(FILESAVE_STORAGE + sessiondatadirectory+timeStamp+"_"+deviceSerial+"_"+user+"_"+site.replace(" ","")+".xml");
            name = file.getAbsolutePath();
            endDate = new SimpleDateFormat("yyyy/MM/dd HH.mm.ss").format(new Date());

            FileOutputStream stream = new FileOutputStream(file);
            try {
                stream.write("<ZebraRetail>\r\n".getBytes());
                stream.write(("<Application version=\""+version+"\"/>\r\n").getBytes());
                stream.write(("<Device serialNumber=\""+deviceSerial+"\"/>\r\n").getBytes());
                stream.write(("<Login identifier=\""+user+"\" date=\""+timeStamp+"\"/>\r\n").getBytes());
                stream.write(("<Inventory area=\""+site+"\" startDate=\""+startDate+"\" endDate=\""+endDate+"\" totalUnits=\""+totalUnits+"\" totalValue=\""+totalValue+"\">\r\n").getBytes());
                for(ArrayList<String> item : inventory){
                    Log.d("Inventory","Size:"+inventory.size());
                    stream.write(("<Item barcode=\""+item.get(1)+"\" units=\""+item.get(3)+"\" value=\""+item.get(2)+"\" description=\""+item.get(0)+"\"/>\r\n").getBytes());
                }
                stream.write(("</Inventory>\r\n").getBytes());
                stream.write(("</ZebraRetail>").getBytes());

                if(!file.setWritable(true, false))
                {
                    Log.d(TAG,"setWritable is failed.");
                }
                if(!file.setReadable(true, false))
                {
                    Log.d(TAG,"setReadable is failed.");
                }
                if(!file.setExecutable(true, false))
                {
                    Log.d(TAG,"setExecutable is failed.");
                }
            }catch (IOException e) {
                memoryFlag = true;
                Log.e("Exception", "File create failed: " + e.toString());
                Log.d("SettingsActivity","File Length="+file.length());
                Log.d("SettingsActivity","Deleting File"+file.delete());
            }finally {
                stream.close();
            }
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }catch(PackageManager.NameNotFoundException e2){
            Log.d (TAG, e2.getMessage());
        }
        return name;
    }

    public void saveConfirm(){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        myFinish();
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        if(memoryFlag) {
            builder.setMessage("Session data was not saved, not enough space").setPositiveButton("OK", dialogClickListener).show();
        }
        else if(noDataentered){
            builder.setMessage("Session data was not saved, no data entered").setPositiveButton("OK", dialogClickListener).show();
        }
        else {
            builder.setMessage("Session data has been saved").setPositiveButton("OK", dialogClickListener).show();
        }

    }

    public void exitConfirm(){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        myFinish();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        builder.setMessage("All session data will be lost").setPositiveButton("Continue", dialogClickListener).setNeutralButton("Cancel", dialogClickListener).show();
    }

    public void askToExit(){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        saveData();
                        saveConfirm();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        exitConfirm();
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        builder.setMessage("Would you like to save this inventory session?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener)
                .setNeutralButton("Cancel", dialogClickListener).show();

    }

    public void myFinish(){
        finish();
    }
    public void stop(){
        askToExit();
    }
    public void onBackButton(View v)
    {
        onBackPressed();
    }
    public void updateTotals(){
        totalValue = 0;
        totalUnits = 0;
        DecimalFormat df = new DecimalFormat("#.00");
        for(ArrayList<String> list : inventory){
            totalValue += Double.parseDouble(list.get(2));
            totalUnits += Integer.parseInt(list.get(3));
        }
        ((TextView)findViewById(R.id.totalUnits)).setText(""+totalUnits);
        ((TextView)findViewById(R.id.totalValue)).setText(df.format(totalValue));
    }
    public static double round(double value) {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }


}
