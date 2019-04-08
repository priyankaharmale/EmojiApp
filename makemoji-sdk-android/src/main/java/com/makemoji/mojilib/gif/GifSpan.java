package com.makemoji.mojilib.gif;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiSpan;
import com.makemoji.mojilib.R;
import com.makemoji.mojilib.Spanimator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by DouglasW on 4/20/2016.
 */
public class GifSpan extends MojiSpan implements GifConsumer {
    GifProducer producer;
    BitmapFactory.Options options;
    Bitmap bitmap;
    BitmapDrawable bitmapDrawable= new BitmapDrawable();
    static Paint gifPaint = new Paint();
    public static boolean USE_SMALL_GIFS = false;
    public boolean isSmallGif = USE_SMALL_GIFS;
    String viewString;
    /**
     * @param d           The placeholder drawable.
     * @param source      URL of the actual emoji
     * @param w           width
     * @param h           height
     * @param fontSize    pt size of parsed attributes
     * @param simple      if true, scale based on fontSize, otherwise refreshView's size
     * @param link        URL to callback when clicked.
     * @param refreshView view to size against and invalidate after image load.
     */
    public GifSpan(@NonNull Drawable d, String source, int w, int h, int fontSize, boolean simple, String link, TextView refreshView) {
        if (simple){ //scale based on current text size
            if (refreshView!=null)  mFontRatio = refreshView.getTextSize()/BASE_TEXT_PX_SCALED;
            else mFontRatio = 1;
        }
        else{//scale based on font size to be set
            mFontRatio = (fontSize*Moji.density)/BASE_TEXT_PX_SCALED;
        }
        mWidth = (int) (w * Moji.density *BASE_SIZE_MULT * mFontRatio);
        mHeight = (int) (h * Moji.density * BASE_SIZE_MULT * mFontRatio);

        mDrawable = d;
        mPlaceHolder = d;
        mSource = source;
        if (link!=null)mLink = link;

        mViewRef = new WeakReference<>(refreshView);
        viewString = refreshView.toString();
        options = new BitmapFactory.Options();
        load();


    }
    private synchronized void load() {
        producer = GifProducer.getProducerAndSub(this, null, mSource);
        if (producer != null) {
            if (!USE_SMALL_GIFS && !isSmallGif) {
                //mWidth = Math.min(mWidth, producer.getWidth());
                //mHeight = Math.min(mHeight, producer.getHeight());
            }
            return;
        }

            if (!Moji.enableUpdates) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream is = Moji.context.getAssets().open("makemoji/sdkimages/" + mSource);
                            int size = is.available();
                            byte[] buffer = new byte[size];
                            is.read(buffer);
                            is.close();
                            GifProducer.getProducerAndSub(GifSpan.this, buffer, mSource);
                            if (producer != null) {
                                if (!USE_SMALL_GIFS && !isSmallGif) {
                                   // mWidth = Math.max(mWidth, producer.getWidth());
                                   // mHeight = Math.max(mHeight, producer.getHeight());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).run();
            } else
                Moji.okHttpClient.newCall(new Request.Builder().url(mSource).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful())
                            producer = GifProducer.getProducerAndSub(GifSpan.this, response.body().bytes(), mSource);
                        response.body().close();
                        if (producer != null) {
                            if (!USE_SMALL_GIFS && !isSmallGif) {
                               // mWidth = Math.max(mWidth, producer.getWidth());
                               // mHeight = Math.max(mHeight, producer.getHeight());
                            }
                        }
                    }
                });
    }

    @Override
    public void onFrameAvailable(final Bitmap b,String bitmapUrl) {
        final TextView v = mViewRef.get();
        if (v==null){
            if (producer!=null)
            Moji.handler.post(new Runnable() {//prevent concurrent modificication
                @Override
               public void run() {
                    if (producer!=null)producer.unsubscribe(GifSpan.this);
                }
            });
            return;
        }

        Long lastInvalidated = v.getTag(R.id._makemoji_last_invalidated_id)==null?0:
                (long)v.getTag(R.id._makemoji_last_invalidated_id);
        long now = System.currentTimeMillis();
        bitmapDrawable = new BitmapDrawable(Moji.context.getResources(),b);
        if (isSmallGif)
            bitmapDrawable.setBounds(0,0,mWidth,mHeight);
        else
            bitmapDrawable.setBounds(0,0,(int)(mWidth * sizeMultiplier),(int)(mHeight * sizeMultiplier));
        if (lastInvalidated+15>now) return;

            v.setTag(R.id._makemoji_gif_invalidated_id,true);
            Moji.handler.post(new Runnable() {
                @Override
                public void run() {
                    Moji.invalidateTextView(v);
                }
            });

//
    }

    @Override
    public void onStopped() {
                if (producer!=null)producer.unsubscribe(GifSpan.this);
                producer = null;
    }
    @Override
    public void onStarted(GifProducer producer){
        this.producer=producer;
        producer.subscribe(this);
    }
    @Override
    public int getSize(Paint paint, CharSequence text,
                       int start, int end,
                       Paint.FontMetricsInt fm) {
        Drawable d = bitmapDrawable;
        Rect rect = d.getBounds();
        rect.bottom = (int)(mHeight *sizeMultiplier);
        rect.right = (int)(mWidth *sizeMultiplier);



        if (fm != null) {
            fm.ascent = -rect.bottom;
            fm.descent = 0;

            fm.top = fm.ascent;
            fm.bottom = 0;
        }
        //Log.i("gif span","gif span size "+ rect.right);

        //return 100;
        return rect.right;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {
        BitmapDrawable d = bitmapDrawable;
        d.setBounds(0,0,(int)(mWidth * sizeMultiplier),(int)(mHeight * sizeMultiplier));
        Rect size = new Rect(0,0,(int)(mWidth * sizeMultiplier),(int)(mHeight * sizeMultiplier));
        canvas.save();
        //save bounds before applying animation scale. for a size pulse only
        //int oldRight = d.getBounds().right;
        //int oldBottom = d.getBounds().bottom;
        // int newWidth = (int)(oldRight*currentAnimationScale);
        //d.setBounds(d.getBounds().left,d.getBounds().top,newWidth,(int)(oldBottom*currentAnimationScale));
        int transY = bottom - d.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        }

        canvas.translate(x, transY);
        if (d.getBitmap()!=null) canvas.drawBitmap(((BitmapDrawable)d).getBitmap(),null,size,gifPaint);
        else
        d.draw(canvas);
        //if (bitmap!=null)canvas.drawBitmap(bitmap,0,0,paint);
        // d.setBounds(d.getBounds().left,d.getBounds().top,oldRight,oldBottom);
        canvas.restore();
    }

    @Override
    public void onUnsubscribed(int actHash) {
        View v = mViewRef.get();
        if (actHash==hostActHash) {
            if (producer != null) producer.unsubscribe(this);
            producer = null;
        }
    }

    int hostActHash = 0;
    @Override
    public void onSubscribed(int actHash) {
        if (hostActHash==0) hostActHash=actHash;
        if (hostActHash==actHash)//we're resuming the activity associated with this span.
            load();
    }
    @Override
    public void onPaused(){
    }

    @Override
    public String toString() {
        return viewString;
    }
}
