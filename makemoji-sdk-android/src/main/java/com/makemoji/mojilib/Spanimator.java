package com.makemoji.mojilib;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.makemoji.mojilib.gif.GifProducer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Coordinates and syncs the animation of MojiSpans. Implement Spanimatable to listen and sync your own animations
 * Currently only supports animation #HYPER_PULSE
 * Created by Scott Baar on 12/18/2015.
 */
public class Spanimator {


    public static final int HYPER_PULSE = 0;
    public static final float HYPER_PULSE_MAX = 1f;
    public static final float HYPER_PULSE_MIN = .25f;


    static final Map<Spanimatable,Boolean> subscribers = Collections.synchronizedMap(new WeakIdentityHashMap<Spanimatable, Boolean>());
    static ValueAnimator hyperAnimation;
    static Handler mainHandler = new Handler(Looper.getMainLooper());
    static boolean mPaused =false, kbPaused;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({HYPER_PULSE})
    public @interface Spanimation {}

    /**
     * Adds a spanimatable to the list of subscribers to be updated on each animation frame
     * @param spanimation the animation to subscribe to
     * @param spanimatable the subscriber to add.
     */
    public static synchronized void subscribe(@Spanimation int spanimation, Spanimatable spanimatable){
        subscribers.put(spanimatable,true);
        spanimatable.onSubscribed(actHash);
        setupStartAnimation(spanimation);
    }

    /**
     * Removes a spanimatable from the list of subscribers to be updated on each animation frame
     * @param spanimation
     * @param spanimatable
     */
    public static synchronized void unsubscribe(@Spanimation int spanimation, Spanimatable spanimatable ){
        subscribers.remove(spanimatable);
        spanimatable.onUnsubscribed(actHash);
    }
    private static synchronized void setupStartAnimation(@Spanimation int spanimation){
        if (mPaused)return;
        if (hyperAnimation!=null) {
            if (!hyperAnimation.isRunning())
                Moji.handler.post(new Runnable() {//sometimes the old thread dies on a rotate
                    @Override
                    public void run() {
                        hyperAnimation.start();
                    }
                });
            return;
        }
        int duration = Moji.resources.getInteger(R.integer._mm_pulse_duration);
        Interpolator interpolator = new DecelerateInterpolator();
        hyperAnimation = ValueAnimator.ofFloat(HYPER_PULSE_MAX,HYPER_PULSE_MIN).setDuration(duration);
        hyperAnimation.setInterpolator(interpolator);
        hyperAnimation.setRepeatCount(ValueAnimator.INFINITE);
        hyperAnimation.setRepeatMode(ValueAnimator.REVERSE);
        hyperAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = (float) animation.getAnimatedValue();
                Set<Spanimatable> set = subscribers.keySet();
                //Log.d("Spanimator","spanimator subscruber size "+ set.size());
                synchronized (subscribers) {
                    if (set.size() == 0 && animation.getAnimatedFraction() != 1f)
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (hyperAnimation != null) hyperAnimation.end();
                            }
                        });
                    for (Spanimatable spanimatable : set) {
                        if (spanimatable != null) {
                            spanimatable.onAnimationUpdate(HYPER_PULSE, progress, HYPER_PULSE_MIN, HYPER_PULSE_MAX);
                        }
                    }
                }
            }
        });
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
            if (hyperAnimation!=null && !mPaused)    hyperAnimation.start();
            }
        });


    }
    public static float getValue(@Spanimation int animation){
        if (hyperAnimation!=null)return (float)hyperAnimation.getAnimatedValue();
        else return HYPER_PULSE_MIN;
    }
    public static void onResume(int actHash){
        mPaused=false;
        //Log.d("Spanimator","spanimator lifecycle resume");
        Spanimator.actHash = actHash;
        if (hyperAnimation!=null && !hyperAnimation.isRunning()){
            hyperAnimation.start();

        }

        synchronized (subscribers) {
            for (Spanimatable spanimatable : subscribers.keySet()) {
                if (spanimatable != null) {
                    spanimatable.onSubscribed(actHash);
                }
            }
        }

        GifProducer.onStart();
    }
   public static void onPause(int actHash){
        mPaused=true;
        //Log.d("Spanimator","spanimator lifecycle pause");
        if (hyperAnimation!=null){

            hyperAnimation.end();
        }

       synchronized (subscribers) {
           for (Spanimatable spanimatable : subscribers.keySet()) {
               if (spanimatable != null) {
                   spanimatable.onUnsubscribed(actHash);
               }
           }
       }
       GifProducer.onStop();

    }
    public static void onKbStart(){
        kbPaused=false;

        synchronized (subscribers) {
            for (Spanimatable spanimatable : subscribers.keySet()) {
                if (spanimatable != null) {
                    spanimatable.onKbStart();
                }
            }
        }

        GifProducer.onStart();
    }

    public static void onKbStop(){
        kbPaused=true;

        synchronized (subscribers) {
            for (Spanimatable spanimatable : subscribers.keySet()) {
                if (spanimatable != null) {
                    spanimatable.onKbStop();
                }
            }
        }

        GifProducer.onStop();
    }
    public static boolean isGifRunning(){
        return !mPaused || !kbPaused;
    }
    public static int actHash;

}
