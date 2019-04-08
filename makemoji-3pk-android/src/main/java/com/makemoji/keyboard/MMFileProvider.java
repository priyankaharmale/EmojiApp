package com.makemoji.keyboard;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.makemoji.mojilib.Moji;

import java.util.Arrays;

/**
 * Created by DouglasW on 4/23/2016.
 */
public class MMFileProvider extends FileProvider {

    public MMFileProvider(){}
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        ContentResolver cR = Moji.context.getContentResolver();
        String type = cR.getType(uri);
        Cursor source = new LegacyCompatCursorWrapper(super.query(uri, projection, selection, selectionArgs, sortOrder),type,uri);
        return source;
    }
}
