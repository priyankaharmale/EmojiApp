package com.makemoji.mojilib;

import android.support.annotation.Nullable;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Response;

/**
 * Created by Scott Baar on 5/18/2016.
 */
public class MojiUnlock {
    private static Set<String> unlocked;
    private static Set<String> locked = new HashSet<>();
    public interface ICategoryUnlock{
        void unlockChange();
    }
    public interface ILockedCategoryClicked{
        void lockedCategoryClick(String name);
    }
    public static Set<String> getUnlockedGroups(){
        if (unlocked == null)
           unlocked= new HashSet<>(Moji.context.getSharedPreferences("mm_unlock",0).getStringSet("groupUnlocks",new HashSet<String>()));
        return unlocked;
    }
    public static void addGroup(String name){
        getUnlockedGroups().add(name);
        Moji.context.getSharedPreferences("mm_unlock",0).edit().putStringSet("groupUnlocks",getUnlockedGroups()).apply();
        alertListeners();
    }
    public static void removeGroup(String name){
        getUnlockedGroups().remove(name);
        Moji.context.getSharedPreferences("mm_unlock",0).edit().putStringSet("groupUnlocks",getUnlockedGroups()).apply();
        alertListeners();
    }
    public static void clearGroups(){
        getUnlockedGroups().clear();
        Moji.context.getSharedPreferences("mm_unlock",0).edit().putStringSet("groupUnlocks",getUnlockedGroups()).apply();
        alertListeners();
    }
    public static void unlockCategory(final String name){

        addGroup(name);
        if (Moji.enableUpdates)
        Moji.mojiApi.unlockGroup(name).enqueue(new SmallCB<JsonObject>() {
            @Override
            public void done(Response<JsonObject> response, @Nullable Throwable t) {
                if (t!=null){
                    t.printStackTrace();
                }
            }
        });
    }
    static void alertListeners(){
        for (Map.Entry<ICategoryUnlock,Boolean> entry : listeners.entrySet())
            entry.getKey().unlockChange();
    }
    static Map<ICategoryUnlock,Boolean> listeners = new ConcurrentHashMap<ICategoryUnlock,Boolean>();
    public static void addListener(ICategoryUnlock unlock){
        listeners.put(unlock,true);
    }
    public static void removeListener(ICategoryUnlock unlock){
        listeners.remove(unlock);

    }
    public static Set<String> getLockedGroups(){
        return locked;
    }


}
