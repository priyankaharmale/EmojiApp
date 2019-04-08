package com.makemoji.mojilib;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Scott Baar on 4/5/2016.
 */
public class BackSpaceDelegate {
    boolean isPressed;
    public BackSpaceDelegate(final View backspace, final Runnable action){
        backspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        backspace.setOnTouchListener(new View.OnTouchListener() {
            private Runnable backSpaceRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isPressed){
                        action.run();
                        backspace.postDelayed(this,120);
                    }
                }
            };
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        isPressed = true;
                        backspace.postDelayed(backSpaceRunnable,200);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isPressed = false;

                }
                return false;
            }
        });
    }
}
