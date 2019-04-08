package com.makemoji.mojilib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ClickableSpan;
import android.text.style.ReplacementSpan;
import android.util.Log;
import android.widget.TextView;

import com.makemoji.mojilib.gif.GifSpan;
import com.makemoji.mojilib.model.MojiModel;
import com.squareup.okhttp.internal.Util;
import com.squareup.picasso252.MemoryPolicy;
import com.squareup.picasso252.NetworkPolicy;
import com.squareup.picasso252.Picasso;
import com.squareup.picasso252.RequestCreator;
import com.squareup.picasso252.Target;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * Fork of ImageSpan for custom emojis.
 * A note about resolutions: MojiImageView and MojiSpan both use getDefaultSpanDimension to determine the width/height to store the downloaded
 * bitmap in memory. This prevents storing two copies of the bitmap in memory. However, if the text size is a lot larger than BASE_TEXT_PT, such
 * as by a factor of 2 or 4, then MojiSpan will appear to be a low resolution. If this is an issue, change resize(size,size) in the picasso request
 * to use mWidth and mHeight to get a bitmap at the actual resolution it will be displayed, or change BASE_TEXT_PT to a larger number to always
 * get a bigger image in all cases.
 * Created by Scott Baar on 12/3/2015.
 */
    public class MojiSpan extends ReplacementSpan implements Spanimatable {

    protected Drawable mDrawable;
    protected String mSource;
    protected int mWidth;
    protected int mHeight;

    // the baseline "normal" font size in sp.
    static final int BASE_TEXT_PT = 16;

    //most incoming images will be at this size in px. Use it to calculate default span dimension
    public static int DEFAULT_INCOMING_IMG_WH = 20;

    //the text size in pixels, determined by BASE_TEXT_PT and screen density
    public static float BASE_TEXT_PX_SCALED;
    protected float mFontRatio;

    // to make mojis stand out from text, always multiply the size by this
    public static float BASE_SIZE_MULT = 1.0f;

    //proportion to size the moji on next frame when being animated;
    private float currentAnimationScale = 1f;

    protected float sizeMultiplier = 1f;


    protected SoftReference<Drawable> mDrawableRef;
    protected WeakReference<TextView> mViewRef;
    protected String mLink = "";
    boolean shouldAnimate;
    protected Drawable mPlaceHolder;
    private static final String TAG = "MojiSpan";
    private static boolean LOG = false;
    protected String name;
    public MojiModel model;
    int id = -1;
    Runnable loadRunnable;

    public MojiSpan() {
    }
     //static constructors might need to be merged in the future
     static MojiSpan createMojiSpan
            (@NonNull Drawable d, String source, int w, int h, int fontSize, boolean simple, String link, TextView refreshView,Bitmap b){
        if (source!=null && source.toLowerCase().endsWith(".gif")){
            return new GifSpan(d,source,w,h,fontSize,simple,link,refreshView);
        }
        else return new MojiSpan(d,source,w,h,fontSize,simple,link,refreshView,b);
    }

    /**
     *
     * @param d The placeholder drawable.
     * @param source URL of the actual emoji
     * @param w width, usually 20
     * @param h height, usually 20
     * @param fontSize pt size of parsed attributes
     * @param simple if true, scale based on fontSize, otherwise refreshView's size
     * @param link URL to callback when clicked.
     * @param refreshView view to size against and invalidate after image load.
     * @param b bitmap to use instead of picasso load
     */
    public MojiSpan(@NonNull Drawable d, String source, int w, int h, int fontSize, boolean simple, String link, final TextView refreshView, Bitmap b) {
        //scale based on font size
        if (simple) { //scale based on current text size
            if (refreshView != null) mFontRatio = refreshView.getTextSize() / BASE_TEXT_PX_SCALED;
            else mFontRatio = 1;
        } else {//scale based on font size to be set
            mFontRatio = (fontSize * Moji.density) / BASE_TEXT_PX_SCALED;
        }

        mWidth = (int) (w * Moji.density * BASE_SIZE_MULT * mFontRatio);
        mHeight = (int) (h * Moji.density * BASE_SIZE_MULT * mFontRatio);

        mDrawable = d;
        mPlaceHolder = d;
        mSource = source;
        if (link != null) mLink = link;
        shouldAnimate = (link != null && !link.isEmpty());
        if (shouldAnimate) {
            currentAnimationScale = Spanimator.getValue(Spanimator.HYPER_PULSE);
        }

        mViewRef = new WeakReference<>(refreshView);
        if (LOG) Log.d(TAG, "starting load " + name + " " + System.currentTimeMillis());
        final int size = mWidth;
        if (b != null && b.getWidth() >= size) {
            t.onBitmapLoaded(b, null);
            return;
        }
        if (mSource != null && !mSource.isEmpty()) {
            loadRunnable = new Runnable() {
                @Override
                public void run() {

                    Bitmap cache = Moji.picasso.quickMemoryCacheCheckStartsWith(Moji.uriImage(mSource).toString());
                    if (cache != null) {
                        Log.d("moji span","mojispan cache hit  " +mSource);
                        t.onBitmapLoaded(cache, null);
                        return;
                    }
                    Log.d("moji span","mojispan cache miss " +mSource);
                    //if load exact has not been set or is true, load at given size, otherwise load raw image.
                    //lets disable this for now since we're resizing mojispans so much.
                //    boolean loadExact = (refreshView==null || refreshView.getTag(R.id._makemoji_load_exact_size)==null
                  //          || Boolean.TRUE.equals(refreshView.getTag(R.id._makemoji_load_exact_size)));
                    boolean loadExact= false;
                    RequestCreator requestCreator = Moji.picasso.load(Moji.uriImage(mSource));
                            if (loadExact)requestCreator = requestCreator.resize(size, size).onlyScaleDown();
                            requestCreator.into(t);
                }
            };
            if (Moji.isMain()) {
                loadRunnable.run();
            } else {
                Moji.handler.post(loadRunnable);
            }
        }
    }

    public static int getDefaultSpanDimension(float textSize){
        float ratio = (textSize)/BASE_TEXT_PX_SCALED;
        return (int) (DEFAULT_INCOMING_IMG_WH * Moji.density * BASE_SIZE_MULT * ratio);
    }
    public static MojiSpan fromModel(MojiModel model, @Nullable TextView tv, @Nullable BitmapDrawable bitmapDrawable){
        Drawable d = bitmapDrawable!=null? bitmapDrawable: Moji.resources.getDrawable(R.drawable.mm_placeholder);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        MojiSpan span = createMojiSpan(d,extractImageUrl(model),20,20,14,true,model.link_url,tv,model.bitmapRef!=null? model.bitmapRef.get():null);
        if (GifSpan.USE_SMALL_GIFS && model.fourtyX40Url!=null && !model.fourtyX40Url.isEmpty() && span instanceof GifSpan)
            ((GifSpan)span).isSmallGif=true;
        span.name = model.name;
        span.id = model.id;
        span.model = model;
        return span;
    }
    public static String extractImageUrl(MojiModel model){
        if (model.image_url==null)return null;
        if (model.image_url.toLowerCase().endsWith(".gif")){
            if (GifSpan.USE_SMALL_GIFS && model.fourtyX40Url!=null && !model.fourtyX40Url.isEmpty())
                return model.fourtyX40Url;
        }
        return model.image_url;
    }

    Target t = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (LOG)Log.d(TAG,"loaded "+name + " " + System.currentTimeMillis());
            mDrawable = new BitmapDrawable(Moji.resources,bitmap);
            mDrawable.setBounds(0,0,mWidth,mHeight);
            mDrawableRef = new SoftReference<>(mDrawable);
            TextView tv = mViewRef.get();
            if (tv!=null) tv.setTag(R.id._makemoji_tv_has_new_load,true);
            Moji.invalidateTextView(tv);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };




    /**
     * Returns the source string that was saved during construction.
     */
    public String getSource() {
        return mSource;
    }



    /**
     * A constant indicating that the bottom of this span should be aligned
     * with the bottom of the surrounding text, i.e., at the same level as the
     * lowest descender in the text.
     */
    public static final int ALIGN_BOTTOM = 0;

    /**
     * A constant indicating that the bottom of this span should be aligned
     * with the baseline of the surrounding text.
     */
    public static final int ALIGN_BASELINE = 1;

    protected final int mVerticalAlignment = ALIGN_BOTTOM;

    public int getVerticalAlignment() {
        return mVerticalAlignment;
    }


    public void setSizeMultiplier(float multiplier){
        sizeMultiplier = multiplier;
    }

    @Override
    public int getSize(Paint paint, CharSequence text,
                       int start, int end,
                       Paint.FontMetricsInt fm) {
        Drawable d = getCachedDrawable();
        Rect rect = d.getBounds();
        rect.bottom = (int)(mHeight *sizeMultiplier);
        rect.right = (int)(mWidth * sizeMultiplier);
        //rect.bottom=100;

        if (fm != null) {
            fm.ascent = -rect.bottom;
            fm.descent = 0;

            fm.top = fm.ascent;
            fm.bottom = 0;
        }

        return rect.right;
    }


    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {
        if (LOG)Log.d(TAG,"draw "+name);
        Drawable d = getCachedDrawable();
        d.setBounds(0,0,(int)(mWidth * sizeMultiplier),(int)(mHeight * sizeMultiplier));
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

        //d.setAlpha((int)(255 * currentAnimationScale));
        paint.setAlpha((int)(255 * currentAnimationScale));
        canvas.translate(x, transY);
        //d.draw(canvas);
        canvas.drawBitmap(((BitmapDrawable)d).getBitmap(),null,d.getBounds(),paint);
       // d.setBounds(d.getBounds().left,d.getBounds().top,oldRight,oldBottom);
        canvas.restore();
    }

    private Drawable getCachedDrawable() {
        SoftReference<Drawable> wr = mDrawableRef;
        Drawable d = null;

        if (wr != null)
            d = wr.get();

        if (d == null) {
            d = mPlaceHolder;
        }

        return d;
    }

    public boolean shouldAnimate(){
        return shouldAnimate;

    }
    public String getLink(){
        return mLink;
    }
    @Override
    public void onAnimationUpdate(@Spanimator.Spanimation int spanimation, float progress, float min, float max) {
        if (shouldAnimate) {
            currentAnimationScale = progress;
            TextView tv = mViewRef.get();
            if (tv != null) {
                Moji.invalidateTextView(tv);
            }

            else
                Spanimator.unsubscribe(Spanimator.HYPER_PULSE, this);//no longer attatched to view.
        }
    }

    @Override
    public void onPaused() {

    }
    @Override
    public void onUnsubscribed(int actHash){
        mDrawable = null;
    }

    @Override
    public void onKbStart() {

    }

    @Override
    public void onKbStop() {

    }

    @Override
    public void onSubscribed(int actHash){
        if (mDrawableRef!= null)
            mDrawable = mDrawableRef.get();
        if (mDrawable==null && mSource!=null && !mSource.isEmpty()) //if bitmap was gced, get it again. don't bother refetching for a new size.
        {
            if (Moji.isMain())
                loadRunnable.run();
            else
                Moji.handler.post(loadRunnable);
        }
    }
    public void setTextView(TextView tv){
        mViewRef = new WeakReference<>(tv);
    }

    public String toHtml() {
        return "<img style=\"vertical-align:text-bottom;width:"+ ((int) sizeMultiplier *20)+"px;height:"+ ((int) sizeMultiplier *20)+"px;\""
                +
        (id == -1 ? " " : " id=\"" + id + "\"")//insert id if this came from a model
                + "src=\"" + mSource + "\" "
                + "name=\"" + name + "\" "
                + "link=\""+getLink()+"\""
                + ">";
    }
    public String toPlainText(){

        String b62Id = Moji.base62.encodeBase10(id);
        return  "[" + ((this instanceof GifSpan)?"gif":name)
                + '.' + b62Id + (mLink == null ||mLink.isEmpty() ? "]" :
                " " + mLink+"]");
    }
    public boolean equivelant (Object o){
        MojiSpan other;
        if (o instanceof MojiSpan)
            other = (MojiSpan) o;
        else
            return false;
        if (id!=other.id)return false;
        if (name!=null && !name.equals(other.name)) return false;
        if (sizeMultiplier!=other.sizeMultiplier) return false;
        if (other.name!=null && !other.name.equals(name)) return false;

        if (mLink!=null && !mLink.equals(other.mLink)) return false;
        if (other.mLink!=null && !other.mLink.equals(mLink)) return false;

        return true;
    }
    @Override
    public boolean equals(Object o){
        return equivelant(o);
    }
    @Override public int hashCode() {
        return (int)(41 * (41 + id) *(41 + sizeMultiplier) *(41*(""+name).hashCode())) + (""+mLink).hashCode();
    }

}
