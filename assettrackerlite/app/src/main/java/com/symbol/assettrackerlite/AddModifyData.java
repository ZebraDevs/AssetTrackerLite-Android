package com.symbol.assettrackerlite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.icu.text.NumberFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AddModifyData extends Activity {
    private boolean hasData = false;
    public boolean do_print = false;

    private List<ArrayList<String>> inventory = null;
    private DatabaseHelper myDb;
    private String startDate = "";
    private String endDate = "";
    private int totalUnits = 0;
    private double totalValue = 0;
    private NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
    String toUpload = "";
    String TAG ="ATL_AddNewInventory";

    String scanData;
    TextView barcode;
    EditText Value;
    EditText Units;
    EditText Description;
    String price="",units,description;
    @SuppressLint({"SuspiciousIndentation", "MissingInflatedId", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_modify_data);
        Intent in = getIntent();
        scanData = String.valueOf(in.getSerializableExtra("ScanData"));
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        barcode = (TextView)findViewById(R.id.scanData);
        if(scanData!=null)
        barcode.setText(scanData);
        Value = (EditText) findViewById(R.id.itemValue);
        Units = (EditText) findViewById(R.id.itemUnits);
        Description = (EditText) findViewById(R.id.itemDescription);
        inventory = new ArrayList<ArrayList<String>>();
        myDb = new DatabaseHelper(this.getApplicationContext());


        Button add = (Button)findViewById(R.id.addData);
        final Button delete = (Button)findViewById(R.id.deleteData);
        Button cancel = (Button) findViewById(R.id.cancelData);

        Log.d("Check if empty",""+barcode.getText().toString().equals("null")+Value.getText().toString().equals(""));

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(Value.getText().toString().equals(""))) {
                    Toast.makeText(getApplicationContext(), "Updating DB", Toast.LENGTH_SHORT).show();
                    price = String.valueOf(Value.getText());
                    units = "1";
                    description = String.valueOf(Description.getText());
                    Log.d("Data Added", "" + price + units + description);
                    hasData = true;
                    saveData();
                    clearData();
                }
                else {
                    Toast.makeText(getApplicationContext(), "No Data to Entered", Toast.LENGTH_SHORT).show();
                }
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(Value.getText().toString().equals(""))) {
                    Toast.makeText(getApplicationContext(), "Updating DB", Toast.LENGTH_SHORT).show();
                    price = String.valueOf(Value.getText());
                    description = String.valueOf(Description.getText());
                    Log.d("Data Deleted", "" + price + units + description);
                    hasData = true;
                    deleteItem(String.valueOf(barcode.getText()));
                    clearData();
                }
                else {
                    Toast.makeText(getApplicationContext(), "No Data", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearData();
            }
        });

    }

    public void comfirmToPrint() {
        final  boolean do_print = false;
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                             printBarcode();
                             break;

                        case DialogInterface.BUTTON_NEGATIVE:
                             break;
                    }
                }
            };
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        builder.setMessage("Press Continue to print label").setNegativeButton("Cancel",
                dialogClickListener).setPositiveButton("Continue", dialogClickListener).show();

    }
    public void printBarcode()
    {
        price = String.valueOf(Value.getText());
        description = String.valueOf(Description.getText());
        String bc = String.valueOf(barcode.getText());
        Toast.makeText(getApplicationContext(),"Barcode Send to Printer",Toast.LENGTH_SHORT).show();
    }

    public void loadData(final String data)
    {
        if(myDb.getItemDesc(data).equals("unknown"))
        {
            Log.d(TAG,"NewItem");
            return;
        }
        Log.d(TAG,"Desc"+myDb.getItemDesc(data));
        Description.setText(myDb.getItemDesc(data));
        Value.setText(myDb.getItemPrice(data));
    }
    public void clearData(){
        Value.setText("");
        Description.setText("");
        barcode.setText("");
    }
    public void deleteItem(final String data)
    {
        String id = myDb.getID(data);
        int sts = myDb.deleteData(id);
        Log.d(TAG,"Delete item status : "+sts);
    }
    public void updateItem(String barcode,String value, String description)
    {
        myDb.updateData(myDb.getID(barcode),barcode,value,description);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onResume(){
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("symbol.action.AssetTrackerLite");
        intentFilter.addCategory("android.intent.category.DEFAULT");
        registerReceiver(myBroadcastReceiver, intentFilter);

    }
    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(myBroadcastReceiver);
    }

    private BroadcastReceiver myBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d("Add new","onResume Intent Registered");
                    try{
                        String data = intent.getStringExtra("com.motorolasolutions.emdk.datawedge.data_string");
                        scanData = data;
                        loadData(data);
                        Log.d(TAG,"New Scan"+scanData);
                        barcode.setText(scanData);
                    }catch (Exception e){
                        Log.d (TAG, e.getMessage());
                        Toast.makeText(context, "Warning: Insufficient memory, please clear up space.", Toast.LENGTH_SHORT).show();
                    }
                }
            };

    public void onBackButton(View v)
    {
        onBackPressed();
    }
    public void addRow(){

        String itemBarcode = String.valueOf(barcode.getText());
        String itemValue="";
        Log.d(TAG,"addRow Price"+(price.equals("")));
        if(!(price.equals(""))) {
            itemValue = convertStringToDecmil(price);
            String itemDescription = description;
            try {
                myDb.insertData(itemBarcode, itemValue, itemDescription);
            }catch (Exception e){
                Log.d(TAG,"Insert Data failed : "+e.toString());
            }
            Log.d(TAG, "Added"+inventory);
            hasData = false;
        }
        else {
            Toast.makeText(getApplicationContext(),"No Data Entered",Toast.LENGTH_SHORT).show();
        }

    }

    public void saveData(){
        String itemBarcode = String.valueOf(barcode.getText());
        String id = myDb.getID(itemBarcode);
        if(id.equalsIgnoreCase("unknown")){
            addRow();
        }
        else
        {
            String itemValue = convertStringToDecmil(String.valueOf(Value.getText()) );
            String itemDescription = String.valueOf(Description.getText());
            myDb.updateData(id,itemBarcode,itemValue,itemDescription);
        }

    }

private String convertStringToDecmil(String inputValue)
    {
        String newValue = "0.0";
        if(!(inputValue.equals(""))) {

            if (inputValue.contains(",")) {
                for (int i = inputValue.length() - 1; i >= 0; i--) {
                    char at = inputValue.charAt(i);
                    if (at == ',') {
                        Log.d(TAG, "Replacing " + String.valueOf(at) + "at" + i);
                        newValue = inputValue.substring(0, i).replace(",", "") + "," + inputValue.substring(i + 1, inputValue.length());
                        newValue = newValue.replace(",", ".");
                        break;
                        }
                }
            }
            else if(inputValue.contains(".")) // check if there are multiple decimals ie 33.22.11 -> 3322.11
            {
                for (int i = inputValue.length() - 1; i >= 0; i--) {
                    char at = inputValue.charAt(i);
                    if (at == '.') {
                        Log.d(TAG, "Replacing " + String.valueOf(at) + "at" + i);
                        newValue = inputValue.substring(0, i).replace(".", "") + "." + inputValue.substring(i + 1, inputValue.length());
                        newValue = newValue.replace(".", ".");
                        break;
                    }
                }
            }
            else
                {
                newValue = "" + (Double.parseDouble(inputValue));
            }

        }
        return newValue;
    }

}

