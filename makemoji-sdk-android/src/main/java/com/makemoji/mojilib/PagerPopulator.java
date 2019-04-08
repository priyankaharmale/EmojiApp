package com.makemoji.mojilib;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Baar on 1/24/2016.
 */
public abstract class PagerPopulator<T> {
    protected List<T> mojiModels = new ArrayList<>();
    protected PopulatorObserver obs;
    public boolean use3pk;

    public interface PopulatorObserver{
       @UiThread void onNewDataAvailable();
    }
    //call when ready to recieve data
    @CallSuper
    public void setup(@NonNull  PopulatorObserver observer) {
        obs = observer;
    }
    //once done, call the next two
    public List<T> populatePage(int count, int offset){
        if (mojiModels.size()<offset)return new ArrayList<>();//return empty
        if (offset+count>mojiModels.size())count = mojiModels.size()-offset;
        return mojiModels.subList(offset,offset+count);
    }
   public int getTotalCount(){
        return mojiModels.size();
    }
    public void teardown(){
        obs = null;
    }
    public void reload(){

    }
}
