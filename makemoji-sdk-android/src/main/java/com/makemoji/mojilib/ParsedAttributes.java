package com.makemoji.mojilib;

import android.text.Spanned;

/**
 * Spanned and attributes parsed from html message. Margins are in raw unscaled px. Font in unscaled pt.
 * Created by Scott Baar on 12/15/2015.
 */
public class ParsedAttributes {
    public Spanned spanned;
    public int marginBottom;
    public int marginTop;
    public int marginLeft;
    public int marginRight;
    public String fontFamily;
    public int fontSizePt = -1;//unscaled font size in pt. -1 if not set.
    public int color = -1;//color parsed from hex. -1 if not set.
}
