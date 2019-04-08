package com.makemoji.mojilib;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/**
 * Created by Scott Baar on 1/25/2016.
 */
public class SearchPopulator extends PagerPopulator<MojiModel> {
    MojiSQLHelper mojiSQLHelper;
    String currentQuery = "";
    boolean getWallData;
    public SearchPopulator(boolean getWallData){
        this.getWallData = getWallData;
    }
    @Override
    public void setup(final PopulatorObserver observer) {
        super.setup(observer);
        mojiSQLHelper = MojiSQLHelper.getInstance(Moji.context,use3pk);
        if (Moji.enableUpdates && getWallData)
            Moji.mojiApi.getEmojiWallData().enqueue(new SmallCB<Map<String,List<MojiModel>>>() {
                @Override
                public void done(final Response<Map<String,List<MojiModel>>> response, @Nullable Throwable t) {
                    if (t!=null){
                        t.printStackTrace();
                        return;
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            List<MojiModel> accumulated = new ArrayList<MojiModel>();
                            for (Map.Entry<String,List<MojiModel>> entry:response.body().entrySet()) {
                                for (MojiModel m : entry.getValue())
                                    m.categoryName = entry.getKey();

                                if (!"osemoji".equalsIgnoreCase(entry.getKey()))accumulated.addAll(entry.getValue());
                                MojiModel.saveList(entry.getValue(),entry.getKey());
                            }
                            Moji.handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    observer.onNewDataAvailable();
                                }
                            });
                           if (!use3pk) mojiSQLHelper.insert(accumulated);
                            Moji.context.getSharedPreferences("emojiWall",0).edit().putString("data",Moji.gson.toJson(response.body())).apply();
                        }
                    }).start();
                }
            });
    }

    @Override
    public List<MojiModel> populatePage(int count, int offset) {
        if (mojiModels.size()<offset)return new ArrayList<>();
        if (offset+count>mojiModels.size())count = mojiModels.size()-offset;
        return mojiModels.subList(offset,offset+count);
    }
    //search off thread. If query is still relevant then return results.
    public void search(@NonNull String query) {
        final String runQuery = query;
        currentQuery = query;

        if (!query.isEmpty() && query.length() > 1 && mojiSQLHelper!=null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final List<MojiModel> models = mojiSQLHelper.search(runQuery, 50);
                    Moji.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (runQuery.equals(currentQuery)) {
                                mojiModels = models;
                                if (obs != null) obs.onNewDataAvailable();
                            }
                        }
                    });

                }
            }).start();

        }
    }
}
