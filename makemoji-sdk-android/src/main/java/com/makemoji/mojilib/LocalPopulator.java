package com.makemoji.mojilib;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;

import java.util.List;

import retrofit2.Response;

/**
 * populates a page based on static models.
 * Created by Scott Baar on 1/22/2016.
 */
public class LocalPopulator extends PagerPopulator<MojiModel>  {
    Category category;
    SharedPreferences sp;
    public LocalPopulator(Category category,List<MojiModel> models){
        this.category = category;
        mojiModels = models;
    }


    @Override
    public void setup(@NonNull PopulatorObserver o) {
        super.setup(o);
        if (!mojiModels.isEmpty())
            if (obs != null) obs.onNewDataAvailable();

    }


}
