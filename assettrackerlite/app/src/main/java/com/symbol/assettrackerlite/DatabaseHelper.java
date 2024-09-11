package com.symbol.assettrackerlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by ProgrammingKnowledge on 4/3/2015.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Inventory.db";
    public static final String TABLE_NAME = "inventory_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "BARCODE";
    public static final String COL_3 = "PRICE";
    public static final String COL_4 = "DESC";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String command = "create table " + TABLE_NAME +" (ID INTEGER PRIMARY KEY AUTOINCREMENT,BARCODE TEXT,PRICE TEXT,'"+COL_4+"' TEXT)";
        Log.d("DatabaseHelper","Creating Table"+command);
        db.execSQL(command);
    }

    //understand when onUpgrade is called
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public void clearAll(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }
    public boolean insertData(String barcode,String price,String desc) {
        boolean insertStatus = false;
        Log.d("DB","Data to be inserted"+barcode+" "+price);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2,barcode);
        contentValues.put(COL_3,price);
        contentValues.put(COL_4,desc);

        Log.d("DATABASEHELPER","Insert Response:"+CheckIsDataAlreadyInDBorNot(barcode));
        //check CONFLICT IGNORE
        //MAKE SURE UPDATE HAPPENS INSTEAD OF INSERT
        if(CheckIsDataAlreadyInDBorNot(barcode)) {
            Log.d("DBHelper","Updating"+barcode);
            db.update(TABLE_NAME,contentValues,COL_1+"=?",new String[]{barcode});
            insertStatus = false;
        }
        else {
            Log.d("DBHelper","Inserting"+barcode);
            db.insert(TABLE_NAME,null,contentValues);
            insertStatus = true;
        }
        db.close();
        return insertStatus;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from "+TABLE_NAME,null);
        return res;
    }

    public String getItemPrice(String barcode){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM inventory_table WHERE BARCODE = ?", new String[]{barcode});
        c.moveToFirst();
        if(c.getCount() == 0){
            c.close();
            return "0.00";
        }else{
            String returnVal = c.getString(2);
            c.close();
            return returnVal;
        }
    }

    public String getItemDesc(String barcode){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM inventory_table WHERE BARCODE = ?", new String[]{barcode});
        c.moveToFirst();
        if(c.getCount() == 0){
            c.close();
            return "unknown";
        }else{
            String returnVal = c.getString(3);
            c.close();
            return returnVal;
        }

    }

    public boolean CheckIsDataAlreadyInDBorNot( String barcode) {
        SQLiteDatabase sqldb = this.getWritableDatabase();
        String Query = "Select * from inventory_table  where BARCODE =?";
        Cursor cursor = sqldb.rawQuery(Query, new String[]{barcode});
        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }
        cursor.close();
        sqldb.close();
        return true;
    }

    public String getID(String barcode){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM inventory_table WHERE BARCODE = ?", new String[]{barcode});
        c.moveToFirst();
        if(c.getCount() == 0){
            c.close();
            db.close();
            return "unknown";
        }else{
            String returnVal = c.getString(0);
            c.close();
            db.close();
            return returnVal;
        }

    }


    public boolean updateData(String id,String barcode,String price,String desc) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1,id);
        contentValues.put(COL_2,barcode);
        contentValues.put(COL_3,price);
        contentValues.put(COL_4,desc);
        db.update(TABLE_NAME, contentValues, "ID = ?",new String[] { id });
        db.close();
        return true;
    }

    public Integer deleteData (String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int returnVal = db.delete(TABLE_NAME, "ID = ?",new String[] {id});
        db.close();
        return returnVal;
    }
}