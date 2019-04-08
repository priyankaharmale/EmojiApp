package com.makemoji.mojilib.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.IntegerRes;

import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiUnlock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Baar on 1/9/2016.
 */
public class Category {
    public final String name;
    public String image_url;
    public @DrawableRes int drawableRes;
    public int locked;
    public int gif;
    public List<MojiModel> models;


    public boolean isLocked(){ return locked==1;}
    public boolean isgif(){ return gif==1;}
    public Category(String name, String image_url) {
        this.name = name;
        this.image_url = image_url;
    }


    public static void saveCategories(List<Category> categoryList){
        if (categoryList==null) return;
        JSONArray ja =new JSONArray();
        try {
            for (Category category : categoryList) {
                if (category.isLocked()) MojiUnlock.getLockedGroups().add(category.name);
                JSONObject jo = new JSONObject();
                jo.putOpt("name", category.name);
                jo.putOpt("locked", category.locked);
                jo.putOpt("gif", category.gif);
                jo.putOpt("image_url", category.image_url);
                ja.put(ja.length(), jo);
            }
            Moji.context.getSharedPreferences("_mm_categories",0).edit().putString("categories",ja.toString()).apply();
        } catch (Exception e){e.printStackTrace();}
    }
    public static List<Category> getCategories(){
        List<Category> categories = new ArrayList<>();
        try{
            JSONArray ja = new JSONArray(Moji.context.getSharedPreferences("_mm_categories",0).getString("categories","[]"));
            for (int i = 0; i<ja.length();i++){
                JSONObject jo = ja.getJSONObject(i);
                Category c = new Category(jo.optString("name"),jo.optString("image_url"));
                c.locked = jo.optInt("locked");
                c.gif = jo.optInt("gif");
                if (c.isLocked()) MojiUnlock.getLockedGroups().add(c.name);
                categories.add(c);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return categories;
    }
    @Override
    public String toString(){
        return "" + name + " " + image_url + " " + drawableRes;
    }
}
