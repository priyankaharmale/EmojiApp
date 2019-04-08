package com.makemoji.mojilib;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;

import java.util.List;

import retrofit2.Response;

/**
 * populates a page based on a given category
 * Created by Scott Baar on 1/22/2016.
 */
public class CategoryPopulator extends PagerPopulator<MojiModel>  {
    Category category;
    public CategoryPopulator(Category category){
        this.category = category;
    }


    @Override
    public void setup(PopulatorObserver o) {
        super.setup(o);
        onNewDataAvailable();
    }
    public void onNewDataAvailable(){
        mojiModels = MojiModel.getList(category.name + (use3pk?"3pk":""));
        if (!mojiModels.isEmpty()) {
            if (obs != null) obs.onNewDataAvailable();
        }
    }
    @Override
    public void reload(){
        mojiModels = MojiModel.getList(category.name);
    }

}
