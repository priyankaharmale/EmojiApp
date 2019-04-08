package com.makemoji.mojilib;

import android.support.annotation.CheckResult;
import android.text.Spanned;

/**
 * We need our own text watcher hook to avoid before/after textchanged loops and maintain selection positioning.
 * Set this on a textview .setTag(R.id._makemoji_moji_text_watcher, ...) and it will be called just before the library changes the text.
 * Created by Scott Baar on 1/28/2017.
 */

public interface IMojiTextWatcher {
    // modify the contents of the spanned if needed. return true if changes were made
    @CheckResult
    Spanned textAboutToChange(Spanned spanned);
    IMojiTextWatcher NoChangeWatcher = new IMojiTextWatcher() {
        @Override
        public Spanned textAboutToChange(Spanned spanned) {
            return spanned;
        }
    };
    IMojiTextWatcher BigThreeTextWatcher =  new IMojiTextWatcher() {
        @Override
        public Spanned textAboutToChange(Spanned spanned) {
            MojiSpan spans [] = spanned.getSpans(0,spanned.length(),MojiSpan.class);
            String string = spanned.toString();

            for (int i = 0; i < string.length();i++) {
                if (Character.isLetterOrDigit(string.charAt(i))){//there is something besides emojis: make everything normal
                    for (MojiSpan span : spans)
                        span.setSizeMultiplier(1);
                    return spanned;
                }
            }

            if (spans.length>3){//there are more than three makemojis
                for (MojiSpan span : spans)
                    span.setSizeMultiplier(1);
                return spanned;
            }
            for (MojiSpan span : spans)
                span.setSizeMultiplier(3);
            return spanned;
        }
    };
}
