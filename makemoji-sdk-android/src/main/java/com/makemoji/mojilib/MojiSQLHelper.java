package com.makemoji.mojilib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.makemoji.mojilib.model.MojiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Baar on 1/25/2016.
 */
public class MojiSQLHelper extends SQLiteOpenHelper {


    public static MojiSQLHelper mInstance;
    public static MojiSQLHelper m3pkInstance;
    private static final int DATABASE_VERSION = 7;
    private static final String DATABASE_NAME = "makemoji.db";

    private static final String FTS_VIRTUAL_TABLE = "FTS";
    public static final String TABLE_MM = "makemoji";
    public static final String TABLE_3PK = "makemoji3pk";

    String table = TABLE_MM;

    public static final String COL_ID = "_ID";
    public static final String COL_ID_INT = "ID";
    public static final  String COL_NAME = "NAME";
    public static final  String COL_IMG_URL = "IMG_URL";
    public static final  String COL_LINK_URL = "LINK_URL";
    public static final  String COL_FLASHTAG = "FLASHTAG";
    public static final  String COL_CHARACTER = "CHARACTER";
    public static final  String COL_GIF = "GIF";
    public static final  String COL_GIF_40 = "GIF_40";
    public static final  String COL_VIDEO = "VIDEO";
    public static final  String COL_VIDEO_URL = "VIDEO_URL";
    public static final  String COL_TAGS = "TAGS";
    public static final  String COL_CATEGORY = "CATEGORY";


    private static String getCreateString(String tableName){
       return "create table "
                + tableName + "(" + COL_ID + " integer primary key AUTOINCREMENT, "
                + COL_ID_INT + " INT, "
                + COL_NAME + " TEXT, "
                + COL_IMG_URL+ " TEXT, "
                + COL_LINK_URL + " TEXT, "
                + COL_FLASHTAG + " TEXT, "
                + COL_CHARACTER + " TEXT, "
                + COL_GIF + " INT, "
                + COL_GIF_40 + " TEXT, "
                + COL_VIDEO + " INT, "
               + COL_VIDEO_URL + " TEXT, "
               + COL_TAGS + " TEXT, "
               + COL_CATEGORY + " TEXT "
                +", UNIQUE( "+COL_ID_INT+ ","+COL_IMG_URL+ ","+ COL_NAME+") ON CONFLICT REPLACE"
                + ");";
    }
    private MojiSQLHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);

    }
    public static synchronized MojiSQLHelper getInstance(Context context,boolean table3pk){
        if (table3pk){
            if (m3pkInstance == null)
                m3pkInstance =  new MojiSQLHelper(context.getApplicationContext());
            m3pkInstance.table = TABLE_3PK;
            return m3pkInstance;
        }
        if (mInstance == null)
            mInstance =  new MojiSQLHelper(context.getApplicationContext());
        return mInstance;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getCreateString(TABLE_MM));
        db.execSQL(getCreateString(TABLE_3PK));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_MM);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_3PK);
        onCreate(db);
    }
    private ContentValues addValues(MojiModel model){
        ContentValues values = new ContentValues();
        values.put(COL_ID_INT, model.id);
        values.put(COL_NAME, model.name);
        values.put(COL_IMG_URL, model.image_url);
        values.put(COL_LINK_URL, model.link_url);
        values.put(COL_FLASHTAG, model.flashtag);
        values.put(COL_CHARACTER, model.character);
        values.put(COL_GIF, model.gif);
        values.put(COL_GIF_40, model.fourtyX40Url);
        values.put(COL_VIDEO, model.video);
        values.put(COL_VIDEO_URL, model.video_url);
        values.put(COL_TAGS, model.tags);
        values.put(COL_CATEGORY, model.categoryName);
        return values;
    }
    public synchronized void insert(List<MojiModel> models){
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.delete(table,null,null);
        for (MojiModel m : models) {
            ContentValues cv = addValues(m);
            long row = db.insert(table, null,cv);
        }
        db.setTransactionSuccessful();
        db.endTransaction();

    }

    //ignores unicode emojis atm
    public @Nullable MojiModel get(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        MojiModel mm = null;
        String raw = "SELECT * FROM "+table + " WHERE "+COL_ID_INT + " = "+id +" AND "+ COL_CHARACTER +" IS NULL";
        Cursor c = db.rawQuery(raw,null);
        try{
            c.moveToFirst();
            mm = new MojiModel();
            mm.id = c.getInt(1);
            mm.name = c.getString(2);
            mm.image_url = c.getString(3);
            mm.link_url = c.getString(4);
            mm.flashtag = c.getString(5);
            mm.character = c.getString(6);
            mm.gif = c.getInt(7);
            mm.fourtyX40Url = c.getString(8);
            mm.video = c.getInt(9);
            mm.video_url = c.getString(10);
            mm.tags = c.getString(11);
            mm.categoryName = c.getString(12);
        }
        catch (Exception e){
            mm = null;
            e.printStackTrace();
        }
        finally {
            c.close();
        }
        return mm;
    }
    public List<MojiModel> search(String query, int limit){
        SQLiteDatabase db = this.getReadableDatabase();
        List<MojiModel> models = new ArrayList<>();
        String raw = "SELECT * FROM "+table + " WHERE "+COL_TAGS + " LIKE '%" + query.replaceAll("'", "''") + "%' LIMIT "+ limit + " COLLATE NOCASE";
        Cursor c = null;
        try{
            c = db.rawQuery(raw,null);
            while (c.moveToNext()) {
                MojiModel mm = new MojiModel();
                mm.id = c.getInt(1);
                mm.name = c.getString(2);
                mm.image_url = c.getString(3);
                mm.link_url = c.getString(4);
                mm.flashtag = c.getString(5);
                mm.character = c.getString(6);
                mm.gif = c.getInt(7);
                mm.fourtyX40Url = c.getString(8);
                mm.video = c.getInt(9);
                mm.video_url = c.getString(10);
                mm.tags = c.getString(11);
                mm.categoryName = c.getString(12);
                models.add(mm);
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
        try{
            if (c!=null)c.close();
        } catch (Exception e){e.printStackTrace();}
        return models;
    }
}
