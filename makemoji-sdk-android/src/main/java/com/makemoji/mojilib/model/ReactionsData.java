package com.makemoji.mojilib.model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.makemoji.mojilib.IMojiSelected;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiEditText;
import com.makemoji.mojilib.MojiInputLayout;
import com.makemoji.mojilib.MojiSpan;
import com.makemoji.mojilib.PagerPopulator;
import com.makemoji.mojilib.SmallCB;
import com.makemoji.mojilib.wall.MojiWallActivity;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

/**
 * created fields not included
 * Created by s_baa on 7/5/2016.
 */
public class ReactionsData {
    public Content content;
    public List<Reaction> reactions;
    public CurrentUser user;
    static Type ReactionsListType;
    PagerPopulator.PopulatorObserver observer;
    public String id;
    static WeakReference<ReactionsData> selectedData;
    public MojiInputLayout.RNUpdateListener rnUpdateListener;
    public ReactionsData(@NonNull String id){
        this.id = id;
        if (Moji.enableUpdates)
        Moji.mojiApi.getReactionData(getHash(id)).enqueue(new SmallCB<JsonObject>() {
            @Override
            public void done(Response<JsonObject> response, @Nullable Throwable t) {
                if (t!=null){
                    t.printStackTrace();
                    return;
                }
                fromJson(ReactionsData.this,response.body());
                if (observer!=null) observer.onNewDataAvailable();
            }
        });

    }

    public void setObserver(PagerPopulator.PopulatorObserver observer){
        this.observer = observer;
        if (observer!=null && reactions!=null) observer.onNewDataAvailable();
    }
    public void removeObserver(PagerPopulator.PopulatorObserver observerToRemove){
        if (observerToRemove==observer)
            observer =null;
    }
    public List<Reaction> getReactions(){
        if (reactions==null)reactions = new ArrayList<>();
        return reactions;
    }

    static void fromJson(ReactionsData data, JsonObject jo){
        if (ReactionsListType==null)ReactionsListType = new TypeToken<List<Reaction>>() {}.getType();
        try{
            data.content = Moji.gson.fromJson(jo.getAsJsonObject("content").toString(),Content.class);
            data.reactions = Moji.gson.fromJson(jo.getAsJsonArray("reactions").toString(),ReactionsListType);
            if (jo.get("currentUser").isJsonObject())data.user = Moji.gson.fromJson(jo.getAsJsonObject("currentUser").toString(),CurrentUser.class);

            if (data.user!=null){
                for (Reaction r: data.reactions)
                    if (r.emoji_id == data.user.emoji_id) r.selected = true;
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void onClick(int position){
        if (user!=null) {
            for (int i = 0; i < reactions.size(); i++) {
                Reaction r = reactions.get(i);
                if (user.emoji_id == r.emoji_id) {
                    r.total--;
                    if (i==position){
                        Moji.mojiApi.createReaction(getHash(id),user.emoji_id,user.emoji_type).
                                enqueue(new SmallCB<JsonObject>() {
                                    @Override
                                    public void done(Response<JsonObject> response, @Nullable Throwable t) {
                                        if (t!=null) {
                                            t.printStackTrace();
                                        }
                                    }
                                });

                        r.selected = false;
                        user.emoji_id = 0;
                        if (rnUpdateListener!=null) rnUpdateListener.needsUpdate();
                        return;
                    }
                }
                r.selected = false;
            }
        }
        Reaction r = reactions.get(position);
        r.selected = true;
        r.total++;
        if (user ==null) user = new CurrentUser();
        user.emoji_id =r.emoji_id;
        user.emoji_type = r.emoji_type;
        if (position!=0){
            reactions.remove(position);
            reactions.add(0,r);
        }

        Moji.mojiApi.createReaction(getHash(id),user.emoji_id,user.emoji_type).
                enqueue(new SmallCB<JsonObject>() {
            @Override
            public void done(Response<JsonObject> response, @Nullable Throwable t) {
                if (t!=null) {
                    t.printStackTrace();
                }
            }
        });
        if (rnUpdateListener!=null) rnUpdateListener.needsUpdate();
    }
    public class Content{
        public int id;
        public int sdk_id;
        public String content_id;
        public  String title;
    }
    public static class Reaction{
        public int total;
        public int emoji_id;
        public String emoji_type;
        public String character;
        public String image_url;
        public boolean selected;
        public Spanned toSpanned(@Nullable TextView tv){
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            if (character!=null && !character.isEmpty()){
                ssb.append(character);
                return ssb;
            }
            MojiModel model = new MojiModel("unknown",image_url);
            model.id = emoji_id;
            MojiSpan span = MojiSpan.fromModel(model,tv,null);
            ssb.append(MojiEditText.replacementChar);
            ssb.setSpan(span,0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return ssb;
        }
    }
    public class CurrentUser{
        public int id;
        public int sdk_id;
        public int user_id;
        public int content_id;
        public int emoji_id;
        public String emoji_type;
    }

    public static void onNewReactionClicked(ReactionsData data){
        selectedData = new WeakReference<>(data);
    }
    public static boolean onActivityResult(int requestCode, int resultCode, Intent data){
        if (selectedData==null)
            return false;
        ReactionsData reaction = selectedData.get();
        if (reaction==null) return false;

        if (requestCode == IMojiSelected.REQUEST_MOJI_MODEL && resultCode== Activity.RESULT_OK
                && reaction.id.equals(data.getStringExtra(MojiWallActivity.EXTRA_REACTION_ID))){
            try{
                String json = data.getStringExtra(Moji.EXTRA_JSON);
                MojiModel model = MojiModel.fromJson(new JSONObject(json));
                for (Reaction r : reaction.getReactions())
                    if (r.emoji_id==model.id)return true;

                Reaction r = new Reaction();
                r.emoji_id = model.id;
                r.selected=true;
                r.image_url = MojiSpan.extractImageUrl(model);
                r.character = model.character;
                reaction.getReactions().add(0,r);
                reaction.onClick(0);
                if (reaction.observer!=null) reaction.observer.onNewDataAvailable();
                return true;
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String getHash(String str) {
        MessageDigest digest = null;
        byte[] input = null;

        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            input = digest.digest(str.getBytes("UTF-8"));

        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return convertToHex(input);
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }
}
