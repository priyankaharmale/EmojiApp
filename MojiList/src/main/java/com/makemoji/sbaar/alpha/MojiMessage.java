package com.makemoji.sbaar.alpha;

import com.makemoji.mojilib.ParsedAttributes;
import com.makemoji.mojilib.model.ReactionsData;

import org.json.JSONObject;

import java.util.Random;

/**
 * Created by Scott Baar on 12/3/2015.
 */
public class MojiMessage {
    public String html,plainText, id;
    public ParsedAttributes parsedAttributes;
    public ReactionsData reactionsData;
    public MojiMessage(String html){
        this.html = html;
        id = String.valueOf((new Random().nextFloat()));

    }
}
