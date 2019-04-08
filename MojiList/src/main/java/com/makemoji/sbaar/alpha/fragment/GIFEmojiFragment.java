package com.makemoji.sbaar.alpha.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.SmallCB;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;
import com.makemoji.sbaar.alpha.LoadingDialog;
import com.makemoji.sbaar.alpha.R;
import com.makemoji.sbaar.alpha.adaptor.GIFAdaptor;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class GIFEmojiFragment extends Fragment {


    RecyclerView androidGridView;
    ArrayList<MojiModel> mojiModels;
    LoadingDialog loadingDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_gridlayout, container, false);

        androidGridView = ( RecyclerView ) view.findViewById(R.id.gridview_android_example);
        androidGridView = view.findViewById(R.id.gridview_android_example);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        androidGridView.setLayoutManager(layoutManager);
        Gson gson = new Gson();
        SharedPreferences prefUser = getActivity().getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
        loadingDialog = new LoadingDialog(getActivity());
        loadingDialog.show();
        /*String jsonText = prefUser.getString("key", null);
        mojiModels = gson.fromJson(jsonText, ArrayList.class);*/


        Moji.mojiApi.getEmojiWallData3pk().enqueue(new SmallCB<Map<String, List<MojiModel>>>() {
            @Override
            public void done(final retrofit2.Response<Map<String, List<MojiModel>>> wallData, @Nullable Throwable t) {
                mojiModels = new ArrayList<MojiModel>();
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                Moji.mojiApi.getCategories().enqueue(new SmallCB<List<Category>>() {
                    @Override
                    public void done(final retrofit2.Response<List<Category>> categories, @Nullable Throwable t) {
                        if (t != null) {
                            t.printStackTrace();
                            return;
                        }

                        for (Category c : categories.body()) {
                            if (c.name.equalsIgnoreCase("GIF")) {
                                c.models = wallData.body().get(c.name);
                                mojiModels.addAll(c.models);
                            }
                        }

                        Log.e("Size", String.valueOf(mojiModels.size()));
                        loadingDialog.dismiss();
                        androidGridView.setAdapter(new GIFAdaptor(getActivity(), mojiModels, "2"));


                    }
                });
            }
        });
        return view;
    }


}
