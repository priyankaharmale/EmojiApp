package com.makemoji.mojilib;

import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;

import com.makemoji.mojilib.model.MojiModel;

/**
 * Created by Scott Baar on 4/16/2016.
 */
public interface IMojiSelected {
    public static final int REQUEST_MOJI_MODEL = 2341;
    void mojiSelected(MojiModel model, @Nullable BitmapDrawable bd);
}
