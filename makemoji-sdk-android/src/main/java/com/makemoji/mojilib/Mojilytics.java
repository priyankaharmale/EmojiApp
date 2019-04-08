package com.makemoji.mojilib;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.makemoji.mojilib.model.MojiModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * Analytics for emojis, of course
 * Created by Scott Baar on 2/2/2016.
 */
public class Mojilytics {
    static Map<Integer,Data> viewed = new ConcurrentHashMap<>();
    static final List<MojiModel> clickList = new ArrayList<>();
    static Handler handler = new Handler(Looper.getMainLooper());
    static boolean runnablePosted;

    static int MAX_SIZE = 200;
    static int MAX_CLICK_SIZE = 25;
    static int TRACK_INTERVAL = 30;

    static DateFormat sdf = SimpleDateFormat.getInstance();
    static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static void trackView(int id) {
        Data d = viewed.get(id);
        if (d != null) {
            if (System.currentTimeMillis() > d.lastViewTime + 300) {
                d.lastViewTime = System.currentTimeMillis();
                d.viewCount++;
            }
        } else {
            d = new Data(id);
            viewed.put(id, d);
        }
        if (viewed.size()>MAX_SIZE){
            handler.removeCallbacks(sendRunnable);
            sendRunnable.run();
            runnablePosted=false;
        }
        else if (!runnablePosted) {
            handler.postDelayed(sendRunnable,TRACK_INTERVAL *1000);
            runnablePosted = true;
        }
    }
    public static void trackClick(MojiModel model){
        synchronized (clickList){
            clickList.add(model);
        }
        if (clickList.size()>MAX_CLICK_SIZE){
            handler.removeCallbacks(sendRunnable);
            sendRunnable.run();
            runnablePosted=false;
        }
        else if (!runnablePosted) {
            handler.postDelayed(sendRunnable,TRACK_INTERVAL *1000);
            runnablePosted = true;
        }



    }
    static void forceSend(){
        sendRunnable.run();
    }
    private static Runnable sendRunnable = new Runnable() {
        @Override
        public void run() {
            runnablePosted=false;
            if (viewed.isEmpty())return;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer,Data> e : viewed.entrySet())    {
                Data d = e.getValue();
                sb.append(d.id).append("[emoji_id]=").append(d.id).append("&").append(d.id).append("[views]=").append(d.viewCount).append("&");
            }
            viewed.clear();
            sb.append("data=").append(sdf.format(new Date()));
            if (Moji.enableUpdates)
            Moji.mojiApi.trackViews(RequestBody.create(MediaType.parse("text/plain"),sb.toString())).enqueue(new SmallCB<Void>() {
                @Override
                public void done(Response<Void> response, @Nullable Throwable t) {
                    if (t!=null){
                        t.printStackTrace();
                        return;
                    }
                }
            });
            try {
                JSONArray ja = new JSONArray();
                synchronized (clickList){
                    for (MojiModel model: clickList){
                        JSONObject jo = new JSONObject();
                        jo.put("click", df.format(new Date()));
                        jo.put("id",model.id);
                        ja.put(jo);
                    }
                    clickList.clear();
                }
                String s = ja.toString();
                //Log.d("click","click "+s);
                if (Moji.enableUpdates)
                Moji.mojiApi.trackClicks(s).enqueue(new SmallCB<Void>() {
                    @Override
                    public void done(Response<Void> response, @Nullable Throwable t) {
                        if (t!=null) t.printStackTrace();
                    }
                });
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    static class Data{
        int id;
        long lastViewTime;
        int viewCount;
        Data(int id){
            this.id = id;
            viewCount = 1;
            lastViewTime = System.currentTimeMillis();
        }
    }

}
