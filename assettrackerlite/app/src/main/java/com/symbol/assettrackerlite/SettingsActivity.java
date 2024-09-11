package com.symbol.assettrackerlite;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import static com.symbol.assettrackerlite.MainActivity.FILESAVE_STORAGE;

public class SettingsActivity extends PreferenceActivity {
    private AppCompatDelegate mDelegate;
    private static boolean changeComplete=true;
    private static Set<String> prevAreaList;
    private static int importCount = 0;
    private static int modifyCount = 0;
    private static int totalCount=0;
    protected static SettingsActivity SA;

    public static Handler UIhandler = new Handler(Looper.getMainLooper());
    private static boolean isBackPressed = false;
    private static final String EXPORT_STORAGE = "/data/tmp/public/";
    static String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        SA = this;
        isBackPressed = false;

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public void onBackButton(View v)
    {
        super.onBackPressed();
        Log.d("SettingsActivity","onBackButton"+isBackPressed);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("Settings","OnDestroyed");
        isBackPressed=true;
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        private DatabaseHelper myDb;
        private String outputFormat = "";
        File currentfolder = new File(EXPORT_STORAGE + "AssetTrackerLite/");
        File previousFolder = new File(currentfolder.getAbsolutePath());
        public Vector<AlertDialog> dialogs = new Vector<AlertDialog>();
        private String selectedFile;
        private String fileName;
        private boolean corruptImport = false;
        private FrameLayout progress;
        private boolean mergeflag = true;
        private boolean securedServerFlag = true;
        boolean changeTrack = false;

        private boolean selected;
        private int check =0;

        private void hideLoading(ViewGroup root) {

            if ((root != null) && (progress != null)) {
                root.removeView(progress);
            }
        }

        private void showLoading(ViewGroup root) {
            progress = new FrameLayout(root.getContext());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            progress.addView(new ProgressBar(root.getContext()), lp);
            ViewGroup.LayoutParams lp2 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            root.addView(progress, lp2);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {

            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            Preference importButton = findPreference("importfile");
            importButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                                           @Override
                                                           public boolean onPreferenceClick(Preference preference) {
                                                               Log.d("SettingsActivity", "ImportFile Selector Clicked");
                                                               currentfolder= new File(EXPORT_STORAGE + "AssetTrackerLite/");
                                                               selectInventoryFile(currentfolder);
                                                               return true;
               }
            });
            Preference exportbutton = findPreference("exportDatabase");
            exportbutton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getActivity().getApplicationContext(), "Exporting Database ", Toast.LENGTH_SHORT).show();
                    myDb = new DatabaseHelper(getActivity().getApplicationContext());
                    exportDatabase();
                    return true;
                }
            });

            Preference resetButton = findPreference("dwATLReset");
            resetButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getActivity().getApplicationContext(),"Resetting Data Wedge Profile",Toast.LENGTH_SHORT).show();
                    try {
                        importDW();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
            });

            Preference arealist = findPreference("arealist");
            arealist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    editAreaList();
                    return true;
                }
            });
            Preference fileClean = findPreference("fileClean");
            fileClean.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //TODO pop up window for selecting file to delete
                    selectFile();
                    return true;
                }
            });
        }


        private void selectFile(){
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View myScrollView = inflater.inflate(R.layout.scroll_text, null, false);
            TextView textView = (TextView)myScrollView.findViewById(R.id.custom_title);
            textView.setText(R.string.dbFileListTitle);
            // textViewWithScroll is the name of our TextView on scroll_text.xml
            final LinearLayout ll = (LinearLayout) myScrollView
                    .findViewById(R.id.scrollViewLinear);

            final File folder = new File(EXPORT_STORAGE + "AssetTrackerLite/OutputFiles/");
            if(!folder.exists()){
                folder.mkdir();
                if(!folder.setReadable(true, false))
                {
                    Log.d(TAG,"setReadable failed.");
                }
                if(!folder.setWritable(true,false))
                {
                    Log.d(TAG,"setWritable failed.");
                }
                if(!folder.setExecutable(true,false))
                {
                    Log.d(TAG,"setExecutable failed.");
                }
            }
            final File fileSaveFolder = new File(FILESAVE_STORAGE + "AssetTrackerLite/OutputFiles/");
            if(!fileSaveFolder.exists()){
                fileSaveFolder.mkdir();
                if(!fileSaveFolder.setReadable(true, false))
                {
                    Log.d(TAG,"setReadable failed.");
                }
                if(!fileSaveFolder.setWritable(true,false))
                {
                    Log.d(TAG,"setWritable failed.");
                }
                if(!fileSaveFolder.setExecutable(true,false))
                {
                    Log.d(TAG,"setExecutable failed.");
                }
            }

            final File[] listOfFiles = folder.listFiles();

            if(!(listOfFiles==null)) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        System.out.println("File :" + listOfFiles[i].getName());
                        final CheckBox checkBox = new CheckBox(getActivity().getBaseContext());
                        checkBox.setId(i);
                        checkBox.setText(listOfFiles[i].getName());
                        checkBox.setTextSize(12);
                        checkBox.setTextColor(Color.BLACK);
                        ColorStateList  colorStateList = new ColorStateList(
                                new int[][]{
                                        new int[]{-android.R.attr.state_checked}, // unchecked
                                        new int[]{android.R.attr.state_checked} , // checked
                                },
                                new int[]{
                                        Color.BLACK,
                                        Color.BLUE,
                                }
                        );

                        CompoundButtonCompat.setButtonTintList(checkBox,colorStateList);
                        ll.addView(checkBox);

                        check = 0;
                        checkBox.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(checkBox.isChecked()) {
                                    check++;
                                    Log.d(TAG, "Selected a file"+check);
                                }
                                if(!checkBox.isChecked()){
                                    check--;
                                    Log.d(TAG, "UnChecked a file"+check);
                                }
                            }
                        });

                    } else if (listOfFiles[i].isDirectory()) {
                        System.out.println("Directory :" + listOfFiles[i].getName());
                    }
                }
            }

            final File[] listOfFilesSaved = fileSaveFolder.listFiles();

            if(!(listOfFilesSaved==null)) {
                for (int i = 0; i < listOfFilesSaved.length; i++) {
                    if (listOfFilesSaved[i].isFile()) {
                        System.out.println("File :" + listOfFilesSaved[i].getName());
                        final CheckBox checkBox = new CheckBox(getActivity().getBaseContext());
                        checkBox.setId(i);
                        checkBox.setText(listOfFilesSaved[i].getName());
                        checkBox.setTextSize(12);
                        checkBox.setTextColor(Color.BLACK);
                        ColorStateList  colorStateList = new ColorStateList(
                                new int[][]{
                                        new int[]{-android.R.attr.state_checked}, // unchecked
                                        new int[]{android.R.attr.state_checked} , // checked
                                },
                                new int[]{
                                        Color.BLACK,
                                        Color.BLUE,
                                }
                        );

                        CompoundButtonCompat.setButtonTintList(checkBox,colorStateList);
                        ll.addView(checkBox);

                        check = 0;
                        checkBox.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(checkBox.isChecked()) {
                                    check++;
                                    Log.d(TAG, "Selected a file"+check);
                                }
                                if(!checkBox.isChecked()){
                                    check--;
                                    Log.d(TAG, "UnChecked a file"+check);
                                }
                            }
                        });

                    } else if (listOfFilesSaved[i].isDirectory()) {
                        System.out.println("Directory :" + listOfFilesSaved[i].getName());
                    }
                }
            }

            final AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle).setView(myScrollView)

                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @TargetApi(11)
                        public void onClick(final DialogInterface dialog, int id) {}

                    }).setNeutralButton("Select All", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int id) {

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @TargetApi(11)
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(TAG,"Dialog Dismissed");
                            dialog.cancel();
                        }

                    }).create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int count = ll.getChildCount();
                    Log.d(TAG,"Select All Clicked"+count);
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
                        Log.d(TAG,"Selected"+selected+check);
                    }
                    else {
                        selected=false;
                        Log.d(TAG,"Selected"+selected+check);
                    }
                    if(selected) {
                        //Delete each file after a prompt
                        final AlertDialog.Builder adb = new AlertDialog.Builder(getContext(), R.style.MyAlertDialogStyle);

                        adb.setTitle("Deleting");
                        adb.setMessage("Are you sure you want to delete selected files");
                        adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final int childCount = ll.getChildCount();
                                for (int i = 0; i < childCount; i++) {
                                    View v = ll.getChildAt(i);
                                    CheckBox cb = (CheckBox) v;
                                    if (cb.isChecked()) {
                                        File fullFilename = new File(folder.getAbsolutePath() +"/"+ cb.getText());
                                        File fullFilenameSaved = new File(fileSaveFolder.getAbsolutePath() +"/"+ cb.getText());
                                        Log.d(TAG,"File Deleted"+fullFilename+fullFilename.delete());
                                        Log.d(TAG,"File Deleted"+fullFilename+fullFilenameSaved.delete());
                                    }
                                }

                                dialog.cancel();

                            }
                        });
                        adb.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        adb.show();

                    }
                    else {
                        //No Files Selected to Delete

                    }
                    dialog.cancel();
                }
            });
        }

        private void importDW() throws IOException {

            //NOTE: This Java code is for demo purposes only; it has not been checked for errors.
            FileOutputStream fos = null;
            String autoImportDir = "/enterprise/device/settings/datawedge/autoimport/";
            String temporaryFileName = "dwprofile_AssetTrackerLite.tmp";
            String finalFileName = "dwprofile_AssetTrackerLite.db";
            File outputFile = null;
            File finalFile = null;
            try ( InputStream fis=this.getContext().getAssets().open("dwprofile_AssetTrackerLite.db")) {
                // create a File object for the parent directory
                File outputDirectory = new File(autoImportDir);

                // create a temporary File object for the output file
                outputFile = new File(outputDirectory, temporaryFileName);
                finalFile = new File(outputDirectory, finalFileName);

                // attach the OutputStream to the file object
                fos = new FileOutputStream(outputFile);
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
            }catch (Exception e){
                Log.d (TAG, e.getMessage());
            }
            //release resources
            try {
                if(fos != null) {
                    fos.close();
                }
            }catch (Exception e){
                Log.d (TAG, e.getMessage());
            }finally {
                fos = null;
                //set permission to the file to read, write and exec.
                if (outputFile != null) {
                    if (!outputFile.setExecutable(true, false)) {
                        Log.d(TAG, "setExecutable is failed.");
                    }
                    if (!outputFile.setReadable(true, false)) {
                        Log.d(TAG, "setReadable is failed.");
                    }
                    if (!outputFile.setWritable(true, false)) {
                        Log.d(TAG, "setWritable is failed.");
                    }
                    //rename the file
                    if (!outputFile.renameTo(finalFile)) {
                        Log.d(TAG, "rename failed.");
                    }
                }
            }
        }


        public void importInventory(String inputFormat){

            final String inputformat = inputFormat;
            myDb = new DatabaseHelper(getActivity().getApplicationContext());

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SA);
            if(!Boolean.parseBoolean(sharedPreferences.getString("mergeflag",""))){
                myDb.clearAll();
            }
            Toast.makeText(getActivity(), "Importing..", Toast.LENGTH_SHORT).show();
            totalCount =0;
            importCount = 0;
            modifyCount = 0;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(inputformat.equalsIgnoreCase("csv")) {
                        try ( InputStream instream = new FileInputStream(selectedFile);
                              InputStreamReader inputreader = new InputStreamReader(instream);
                              BufferedReader buffreader = new BufferedReader(inputreader)){
                            // if file the available for reading
                            if (instream != null) {
                                // prepare the file for reading
                                String line;
                                UIhandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showLoading((ViewGroup) getActivity().findViewById(android.R.id.content));
                                    }});
                                // read every line of the file into the line-variable, on line at the time
                                do {
                                    if(!isBackPressed) {
                                        line = buffreader.readLine();
                                        Log.d("Import", "Data Read" + line);
                                        try {
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


                                        } catch (Exception e) {
                                           //e.printStackTrace();
                                        }
                                    }else {
                                        Log.d(TAG,"onBack Pressed Stopped Importing");
                                        break;
                                    }
                                    // do something with the line
                                } while (line != null);
                                if(!isBackPressed) {
                                    Log.d("Import", "Data Read Complete");
                                    File f = getContext().getDatabasePath(myDb.getDatabaseName());
                                    long dbSize = f.length();
                                    Log.d("Settings Inventory", "Size after import:" + dbSize);
                                    Log.d("Finally Called", "Value of ModifiedImport" + modifyCount);
                                }
                            }
                        } catch (Exception ex) {
                            // print stack trace.
                            corruptImport = true;
                            Log.e ("SettingsActivity", ex.getMessage());
                        }
                        finally {
                            UIhandler.post(new Runnable() {
                                @Override
                                public void run() {

                                    if(!isBackPressed) {
                                        setCorruptImport();
                                        hideLoading((ViewGroup) getActivity().findViewById(android.R.id.content));
                                    }
                                }
                            });
                        }
                    }else if(inputformat.equalsIgnoreCase("xml")) {
                        try {

                            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                            factory.setNamespaceAware(true);
                            XmlPullParser parser = factory.newPullParser();

                            InputStream in_s = new FileInputStream(selectedFile);
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
                                        Log.d (TAG, "at the start of the xml. " + barcode);
                                        break;
                                    case XmlPullParser.START_TAG:
                                        if (tagname.equalsIgnoreCase("Item")) {
                                            if(!isBackPressed) {
                                                String text = parser.getAttributeName(0);
                                                barcode = parser.getAttributeValue(0);
                                                value = parser.getAttributeValue(2);
                                                description = parser.getAttributeValue(3);
                                                myDb.insertData(barcode, value, description);
                                                importCount++;
                                                importCount++;
                                                totalCount++;
                                            }
                                            else {
                                                Log.d(TAG,"onBack Pressed Stopped Importing");
                                                break;
                                            }
                                        }
                                        break;

                                    case XmlPullParser.TEXT:
                                        Log.d (TAG, "inside TEXT");
                                        break;

                                    case XmlPullParser.END_TAG:
                                        Log.d (TAG, "inside END TAG");
                                        break;

                                    default:
                                        break;
                                }
                                eventType = parser.next();
                            }
                            in_s.close();
                        }catch (Exception e ){
                            corruptImport = true;
                            Log.e (TAG, e.getMessage());
                        }
                        finally {
                            UIhandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(!isBackPressed)
                                      setCorruptImport();
                                }
                            });
                            Log.d("Finally Called","Value of CorruptImport"+corruptImport);

                        }
                    }
                }
            }).start();

        }

        public void setCorruptImport(){
            String message;
            if(corruptImport) {
                message = "File Corrupted, cannot import product database.";
            }
            else {
                message = "Import Complete.";
            }
            try {
                final AlertDialog.Builder popup = new AlertDialog.Builder(getContext(), R.style.MyAlertDialogStyle);
                popup.setTitle("Import Summary");
                popup.setMessage(message + System.lineSeparator() + "Imported:" + importCount + System.lineSeparator() + "Updated:" + modifyCount + System.lineSeparator() + "Total:" + totalCount);
                popup.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                popup.show();
            }catch (Exception e){
                Log.d (TAG, e.getMessage());
            }
        }

        public void viewAll() {

            Cursor res = myDb.getAllData();
            if(res.getCount() == 0) {
                // show message
                Log.d("Error","Nothing found");
                return;
            }

            StringBuffer buffer = new StringBuffer();
            while (res.moveToNext()) {
                buffer.append("Id :"+ res.getString(0)+"\n");
                buffer.append("Barcode :"+ res.getString(1)+"\n");
                buffer.append("Price :"+ res.getString(2)+"\n");
                buffer.append("Desc :"+ res.getString(3)+"\n\n");
            }

            // Show all data
            Log.d(TAG,buffer.toString());
            res.close();

        }

      public void exportDatabase() {
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity());
            outputFormat = SP.getString("outputformat", "CSV");
            String deviceSerial = SP.getString("Device Serial", "DeviceSerial");

            String sessiondatadirectory = SP.getString("sessiondatadirectory", "sessiondatadirectory");

            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            File path = new File(EXPORT_STORAGE+sessiondatadirectory);
            if(!path.exists())
                path.mkdirs();
            if (!path.setWritable(true,false))
            {
                Log.d(TAG,"setWritable is failed.");
            }
            if (!path.setReadable(true,false))
            {
                Log.d(TAG,"setReadable is failed.");
            }
            if(!path.setExecutable(true,false))
            {
                Log.d(TAG,"setExecutable is failed.");
            }

            Log.d(TAG,"Path Exists"+path.exists()+" "+path);
            if(outputFormat.equalsIgnoreCase("CSV")) {

                File file = new File(EXPORT_STORAGE + sessiondatadirectory +"/"+ timeStamp + deviceSerial+"ProductDatabase.csv");
                Cursor res = myDb.getAllData();
                if (res.getCount() == 0) {
                    // show message
                    Log.d("Error", "Nothing found");
                    res.close();
                    return;
                }
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    if(!file.exists()){
                        if(!file.createNewFile())
                        {
                            Log.d(TAG,"File creation is failed.");
                        }
                    }
                    Log.d(TAG,"File Exist"+file.exists()+file.getPath());
                    while (res.moveToNext()) {
                        stream.write((res.getString(1) + "," + res.getString(2) + "," + res.getString(3) + "\r\n").getBytes());
                    }
                    if(!file.setExecutable(true, false))
                    {
                        Log.d(TAG,"setExecutable is failed.");
                    }
                    if(!file.setReadable(true, false))
                    {
                        Log.d(TAG,"setReadable is failed.");
                    }
                    if(!file.setWritable(true, false))
                    {
                        Log.d(TAG,"setWritable is failed.");
                    }

                    Log.d(TAG,"FileStream Closed");
                    stream.close();
                    res.close();

                    if(file.length()<=0){
                        Log.d(TAG,"File Length="+file.length());
                        Log.d(TAG,"Deleting File"+file.delete());
                        Toast.makeText(getActivity().getApplicationContext(),R.string.file_not_created,Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e (TAG, e.getMessage());
                }
            }
            else //XML format
            {
                File file = new File("/data/tmp/public/" +sessiondatadirectory + timeStamp + deviceSerial+ "ProductDatabase.xml");
                Cursor res = myDb.getAllData();
                if (res.getCount() == 0) {
                    // show message
                    Log.d("Error", "Nothing found");
                    res.close();
                    return;
                }
                try {
                    if (!file.exists()) {
                        if (!file.createNewFile()) {
                            Log.d(TAG, "File creation failed.");
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e(TAG, e.getMessage());
                }
                try (FileOutputStream stream = new FileOutputStream(file)) {

                    Log.d(TAG,"File Exist"+file.exists()+file.getPath());
                    stream.write(("<ProductDatabase>\r\n").getBytes());

                    while (res.moveToNext()) {
                        Log.d(TAG, "Exporting" + res.getString(1));
                        stream.write(("<Item barcode=\"" + res.getString(1) + "\"" + "value =\"" + res.getString(2) + "\" description=\"" + res.getString(3) + "\"/>\r\n").getBytes());
                    }
                    stream.write(("</ProductDatabase>\r\n").getBytes());

                    stream.close();
                    Log.d(TAG,"FileStream Closed");

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

                    if(file.length()<=0){
                        Log.d(TAG,"File Length="+file.length());
                        Log.d(TAG,"Deleting File"+file.delete());
                        Toast.makeText(getActivity().getApplicationContext(),R.string.file_not_created,Toast.LENGTH_SHORT).show();
                    }
                    res.close();
                } catch (Exception e) {
                    Log.d (TAG, e.getMessage());
                }

            }

        }


        public void selectInventoryFile(final File currFolder){
            LayoutInflater inflater  = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View myScrollView = inflater.inflate(R.layout.importfile,null,false);
            final  LinearLayout ll = (LinearLayout)myScrollView.findViewById(R.id.areaListList);
            currentfolder = new File(currFolder.getAbsolutePath());
            final File files[] = currentfolder.listFiles();
            for(final File file : files) {
                Log.d("File Name", file.getName());
                final ImageView im =  new ImageView(getActivity());
                im.setImageResource(R.drawable.file);
                final TextView tv = new TextView(getActivity());
                tv.setText(file.getName());
                tv.setTextColor(Color.BLACK);
                tv.setTextSize(20);
                tv.setBackgroundResource(R.drawable.border);
                tv.setPadding(12, 12, 12, 12);
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG,"Selected "+tv.getText());
                        if(file.isDirectory()){
                            selectedFile=null;
                            previousFolder = new File(currentfolder.getAbsolutePath());
                            currentfolder = new File(currentfolder.getAbsolutePath()+"/"+file.getName()+"/");
                            selectInventoryFile(currentfolder);
                            Log.d(TAG, "FileNames "+previousFolder+" "+currentfolder);
                        }
                        if(file.isFile()){
                            highlightFile(ll,(TextView)v);
                            selectedFile = file.getAbsolutePath();
                            fileName = file.getName();
                            Log.d(TAG,"Selected File"+selectedFile);
                        }
                    }
                });
                if(file.isFile()) {
                    String fileName = file.getName();
                    if(fileName.contains("ProductDatabase")) {
                        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.file, 0);
                        ll.addView(tv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                    }
                }
                if(file.isDirectory()){
                    tv.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_action_name,0);
                    ll.addView(tv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                }
            }
            AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle).setView(myScrollView)
                    .setNeutralButton("Back", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedFile=null;
                            if(previousFolder.getName().equals(currFolder.getName())){
                                removeAllDialog();
                            }
                            else {
                                selectInventoryFile(previousFolder);
                            }
                        }
                    })
                    .setNegativeButton("Import", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(selectedFile==null){
                                Toast.makeText(getActivity().getBaseContext(),"Please Select a file first",Toast.LENGTH_SHORT).show();
                                selectInventoryFile(currFolder);
                            }
                            else {
                                final AlertDialog importConfirmDialog = new AlertDialog.Builder(getContext(), R.style.MyAlertDialogStyle).create();
                                importConfirmDialog.setTitle("Importing");
                                importConfirmDialog.setMessage("Are you sure you want to import :" + fileName);
                                importConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                        outputFormat = SP.getString("outputformat", "CSV");
                                        Log.d("DEBUGGING","FILE IMPORT"+outputFormat+" "+selectedFile);
                                        if (selectedFile.contains(outputFormat.toLowerCase())) {
                                            removeAllDialog();
                                            importInventory(outputFormat);

                                       /* } else if (selectedFile.contains(outputFormat.toUpperCase())) {
                                            removeAllDialog();
                                            importInventory(outputFormat);*/
                                        } else {
                                            Log.d("DEBUGGING","FILE IMPORT"+outputFormat+" "+selectedFile);
                                            Toast.makeText(getContext(), "Wrong File Selected", Toast.LENGTH_SHORT).show();
                                        }

                                        dialog.cancel();

                                    }
                                });
                                importConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeAllDialog();
                                        importConfirmDialog.dismiss();
                                    }
                                });
                                importConfirmDialog.show();
                            }

                        }
                    })
                    .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                        @TargetApi(11)
                        public void onClick(DialogInterface dialog, int id) {
                            selectedFile=null;
                            try {
                                removeAllDialog();
                            }
                            catch (Exception e){
                                Log.d ("SettingsActivity", e.getMessage());
                            }
                        }

                    }).show();
            dialogs.add(dialog);

        }

        public void highlightFile(LinearLayout ll,TextView v){
            int childCount = ll.getChildCount();
            for(int i=0;i<childCount;i++){
                TextView view = (TextView)ll.getChildAt(i);
                if(view.getText()==v.getText()){
                    Log.d("Setting Selected color","For:"+view.getText());
                    v.setBackgroundColor(Color.parseColor("#0077A0"));
                }
                else {
                    Log.d("Setting white color","For:"+view.getText());
                    view.setBackgroundColor(Color.WHITE);
                }

            }
        }

        public void removeAllDialog(){
            try {
                for (Dialog dialog : dialogs) {
                    dialog.cancel();
                }
                dialogs.clear();
            }catch (Exception e){
                Log.d (TAG, e.getMessage());
            }
        }

        public void editAreaList() {

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View myScrollView = inflater.inflate(R.layout.area_list, null, false);

            // textViewWithScroll is the name of our TextView on scroll_text.xml
            final LinearLayout ll = (LinearLayout) myScrollView
                    .findViewById(R.id.areaListList);

            //get shared prefs
            final SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
            Set<String> areas = SP.getStringSet("arealist", null);

            final Set<String> areaList = SP.getStringSet("arealist",null);

            if(areas==null){
                areas = new HashSet<String>();
                areas.add("No areas");
            }
            setAreaListView(ll,areas,SP,areaList);

            final Button addButton = (Button) myScrollView
                    .findViewById(R.id.addAreaList);
            addButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    changeTrack = true;
                    String toAdd = ((TextView)myScrollView.findViewById(R.id.areaToAdd)).getText().toString();
                    if(!toAdd.equals("")){
                        //get shared prefs
                        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                        Set<String> areaList = SP.getStringSet("arealist",null);
                        SharedPreferences.Editor editor = SP.edit();

                        if(areaList == null){
                            areaList = new HashSet<String>();
                        }

                        areaList.add(toAdd);
                        editor.remove("arealist");
                        editor.commit();
                        editor.putStringSet("arealist", areaList);
                        editor.commit();

                        ll.removeAllViews();
                        Set<String> areas = SP.getStringSet("arealist", null);

                        if(areas==null){
                            areas = new HashSet<String>();
                            areas.add("No areas");
                        }

                        setAreaListView(ll,areas,SP,areaList);

                    }
                    ((TextView)myScrollView.findViewById(R.id.areaToAdd)).setText("");
                }
            });



            new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle).setView(myScrollView)
                   .setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // TODO Auto-generated method stub
                            Log.d(TAG,"Dialog Dismissed"+changeTrack+"Area List"+areaList);
                            if(changeTrack){
                                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                                SharedPreferences.Editor editor = SP.edit();
                                editor.putStringSet("arealist",areaList);
                                editor.apply();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @TargetApi(11)
                        public void onClick(DialogInterface dialog, int id) {
                            changeTrack = false;
                            if(!changeComplete){
                                final LinearLayout ll = (LinearLayout) myScrollView
                                        .findViewById(R.id.areaListList);
                                int count = ll.getChildCount();
                                prevAreaList = new HashSet<>();
                                Log.d(TAG,"ChildCount of AreaList View:"+count);
                                String listName;
                                for(int i=0;i<count;i++) {
                                    listName = ((EditText) ll.getChildAt(i)).getText().toString();
                                    Log.d(TAG,"AreaList:"+listName);
                                    if(!listName.equals("")){
                                        prevAreaList.add(listName);
                                    }
                                }
                                final SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                                SharedPreferences.Editor editor = SP.edit();
                                editor.putStringSet("arealist", prevAreaList);
                                editor.commit();

                            }
                            dialog.cancel();
                        }

                    })
                    .show();

        }

        public void setAreaListView(final LinearLayout ll,Set<String> areas, final SharedPreferences SP,final Set<String> areaList){
            for(final String area : areas){
                Log.d("area",area);
                final EditText tv = new EditText(getActivity());
                tv.setText(area);
                tv.setTextColor(Color.BLACK);
                tv.setTextSize(20);
                tv.setBackgroundResource(R.drawable.border);
                tv.setPadding(12, 12, 12, 12);
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeTrack = true;

                        //tv.setVisibility(View.GONE);
                    }
                });
                tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    public void onFocusChange(View v, boolean hasFocus){
                        changeTrack = true;
                        String temp = "";
                        Log.d(TAG,"AreaListSP:"+areaList);
                        if(hasFocus){
                            Log.d(TAG,"Focus Gained");


                            if(areaList!=null){
                                temp = tv.getText().toString();
                                areaList.remove(tv.getText().toString());
                                Log.d(TAG,"Area Changed from"+tv.getText().toString());
                                changeComplete = false;

                            }
                        }
                        else {
                            Log.d(TAG,"Focus Released");
                            if(areaList!=null)
                                areaList.add(tv.getText().toString());
                            Log.d(TAG,"Area Changed to"+tv.getText().toString());
                            changeComplete = true;

                        }
                        if(!changeComplete){
                            if(areaList!=null) {
                                areaList.add(temp);
                            }
                        }

                    }
                });
                ll.addView(tv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
                Preference preference = getPreferenceScreen().getPreference(i);
                if (preference instanceof PreferenceGroup) {
                    PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                    for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                        Preference singlePref = preferenceGroup.getPreference(j);
                        updatePreference(singlePref, singlePref.getKey());
                    }
                } else {
                    updatePreference(preference, preference.getKey());
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updatePreference(findPreference(key), key);
            setSettings();

        }

        private void updatePreference(Preference preference, String key) {
            if (preference == null) return;
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                listPreference.setSummary(listPreference.getEntry());
                Log.d(TAG,"OnConfiguration Changed Called");
                return;
            }else if(preference instanceof EditTextPreference) {
                SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
                if(preference.getTitle().equals("Password")){
                    String pwd = sharedPrefs.getString(key, "Default");
                    StringBuilder strBuff = new StringBuilder();
                    for (int i = 0; i<pwd.length(); i++){
                        strBuff.append("*");
                    }
                    preference.setSummary(strBuff.toString());
                }else{
                    Log.d(TAG,"OnConfiguration Changed Called");
                    preference.setSummary(sharedPrefs.getString(key, "Default"));
                }

            }

        }

        @SuppressLint("SetWorldReadable")
        private void setSettings(){
            try {
                //String dir = Environment.getExternalStorageDirectory()+"/AssetTrackerLite/OutputFiles/";
                String dir = EXPORT_STORAGE + "AssetTrackerLite/OutputFiles/";
                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(SA);
                boolean groupInventory = SP.getBoolean("inventorygrouping", false);
                boolean sendResults = SP.getBoolean("sendresults", false);
                boolean autoImport = SP.getBoolean("autoImport",false);
                String user = SP.getString("username", "user");
                String password = SP.getString("password", "pass");
                String server = SP.getString("server", "server");
                String serverType = SP.getString("serverType" , "serverType");
                String outputformat = SP.getString("outputformat", "CSV");
                String sessiondatadirectory = SP.getString("sessiondatadirectory", dir);
                boolean allowDBUpdates = SP.getBoolean("allowdbupdates",true);
                Set<String> areaList = SP.getStringSet("arealist", null);


                try {
                    File path = new File( EXPORT_STORAGE + "AssetTrackerLite/");
                    path.mkdirs();
                    if(!path.setReadable(true,false))
                    {
                        Log.d(TAG,"setReadable is failed.");
                    }
                    if(!path.setWritable(true,false))
                    {
                        Log.d(TAG,"setWritable is failed.");
                    }
                    if(!path.setExecutable(true,false))
                    {
                        Log.d(TAG,"setExecutable is failed.");
                    }
                    File file = new File(EXPORT_STORAGE + "AssetTrackerLite/settings.xml");
                   if(!file.exists()) {
                       Log.d(TAG, "settings.xml is not yet created.");
                       if(!file.createNewFile())
                       {
                           Log.d(TAG, "Create new file failed..");
                       }
                       if (file.setReadable(true, false)) {
                           Log.d(TAG, "setReadable is successful.");
                       }
                       if (file.setWritable(true, false)) {
                           Log.d(TAG, "setWritable is successful.");
                       }
                       if (file.setExecutable(true, false)) {
                           Log.d(TAG, "setExecutable is successful.");
                       }
                   } else {
                       Log.d(TAG, "settings.xml already created.");
                   }


                    FileOutputStream stream = new FileOutputStream(file);
                    try {
                        String toWrite = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<Settings>\n" +
                                "  <Connection>\n" +
                                "    <Server>" + server + "</Server>\n" +
                                "    <AutomaticSendResults>" + sendResults + "</AutomaticSendResults>\n" +
                                "  </Connection>\n" +
                                "  <Application>\n" +
                                "    <AreaList>\n";
                        if (areaList != null) {
                            for (String s : areaList) {
                                toWrite = toWrite + "      <Item>" + s + "</Item>\n";
                            }
                        }


                        toWrite = toWrite + "    </AreaList>\n" +
                                "    <ItemGrouping>" + groupInventory + "</ItemGrouping>\n" +
                                "  </Application>\n" +
                                "  <Options>\n" +
                                "    <OutputDirectory>" + sessiondatadirectory + "</OutputDirectory>\n" +
                                "    <AutoImport>" + autoImport + "</AutoImport>\n"+
                                "    <SelectedOutput>" + outputformat + "</SelectedOutput>\n" +
                                "    <AllowDBUpdates>" +allowDBUpdates + "</AllowDBUpdates>\n" +
                                "  </Options>\n" +
                                "</Settings>";

                        stream.write((toWrite).getBytes());

                    } finally {
                        stream.close();
                    }
                    if(file.setReadable(true , false))
                    {
                        Log.d(TAG, "setReadable is successful.");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "File write failed: " + e.toString());
                }

            }catch (Exception e){
                Log.d (TAG, e.getMessage());
            }

        }
      }


}
