package com.makemoji.sbaar.alpha;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.multidex.MultiDex;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.makemoji.keyboard.MMKB;
import com.makemoji.mojilib.KBCategory;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiSQLHelper;
import com.makemoji.mojilib.SmallCB;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;
import com.squareup.leakcanary.LeakCanary;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Scott Baar on 12/14/2015.
 */
public class App extends Application {
    public static Context context;
    String test="";
    SharedPreferences prefUser;

    SharedPreferences.Editor editorUser;

    ArrayList<MojiModel> mojiModels = new ArrayList<MojiModel>();

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        prefUser = getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
        test = prefUser.getString("enable", "");
        MultiDex.install(this);

       /* if (test.equals("true") || test.equals("")) {
            Moji.initialize(this, "");
            LeakCanary.install(this);

            KBCategory.categoryDrawables.put("Sports", R.drawable.custom_kb_tab);
            checkpermission();

        }else
        {*/
            Moji.initialize(this, "17898532df22db35a9b1ff14a95e5bcbcf273cb9");
       //    LeakCanary.install(this);

            KBCategory.categoryDrawables.put("Sports", R.drawable.custom_kb_tab);

      //  }
       /* Moji.initialize(this, "");
        LeakCanary.install(this);

        KBCategory.categoryDrawables.put("Sports", R.drawable.custom_kb_tab);*/
       /* Moji.initialize(this, "17898532df22db35a9b1ff14a95e5bcbcf273cb9");
        LeakCanary.install(this);

        KBCategory.categoryDrawables.put("Sports", R.drawable.custom_kb_tab);
*/
       /* prefUser = getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
        test = prefUser.getString("enable", "");

        Toast.makeText(getApplicationContext(), "Demo" + test, Toast.LENGTH_SHORT).show();*/



/*
        if (test.equals("true")) {
            //Toast.makeText(getApplicationContext(), test, Toast.LENGTH_SHORT).show();
           *//* Moji.initialize(this, "17898532df22db35a9b1ff14a95e5bcbcf273cb9");
            LeakCanary.install(this);

            KBCategory.categoryDrawables.put("Sports", R.drawable.custom_kb_tab);*//*
            Moji.initialize(this, "");
            LeakCanary.install(this);

            KBCategory.categoryDrawables.put("Sports", R.drawable.custom_kb_tab);

        } else {
            Moji.initialize(this, "17898532df22db35a9b1ff14a95e5bcbcf273cb9");
            LeakCanary.install(this);

            KBCategory.categoryDrawables.put("Sports", R.drawable.custom_kb_tab);

        }*/
        //  checkpermission();

        //MMKB.showLockedEmojis(false);
        /*MMKB.setCategoryListener(new MMKB.ICategorySelected() {
            View v;
            @Override
            public void categorySelected(String category,boolean locked, final FrameLayout parent) {
                if (v!=null) {
                    parent.removeView(v);
                    v=null;
                    return;
                }
                if (!"Sports".equalsIgnoreCase(category))return;
                v = new View(context);
                v.setAlpha(.5f);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setVisibility(View.GONE);
                        parent.removeView(v);
                    }
                });
                v.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(210*Moji.density), Gravity.FILL));
                v.setBackgroundColor(ContextCompat.getColor(context,R.color.colorPrimary));

                parent.addView(v);
            }
        });*/
        //Moji.setEnableUpdates(false);
        // Moji.loadOfflineFromAssets();//call only when new assets are in the app after an update




    }



}
