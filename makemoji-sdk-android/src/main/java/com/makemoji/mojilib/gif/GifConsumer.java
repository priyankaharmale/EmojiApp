package com.makemoji.mojilib.gif;

import android.graphics.Bitmap;

/**
 * Created by DouglasW on 4/19/2016.
 */
public interface GifConsumer {
    void onFrameAvailable(Bitmap b,String bitmapUrl);
    void onStopped();
    void onStarted(GifProducer producer);
}
