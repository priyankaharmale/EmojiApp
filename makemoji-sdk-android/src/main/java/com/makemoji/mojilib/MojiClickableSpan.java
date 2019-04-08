package com.makemoji.mojilib;

import android.text.style.ClickableSpan;

/**
 * override equals to facilitate equality tests
 * Created by Scott Baar on 3/21/2016.
 */
public abstract class MojiClickableSpan extends ClickableSpan {
    @Override
    public boolean equals (Object o){
        return (o instanceof MojiClickableSpan);
    }
}
