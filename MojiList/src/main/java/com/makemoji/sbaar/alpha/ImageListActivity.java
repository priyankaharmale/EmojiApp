package com.makemoji.sbaar.alpha;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.SmallCB;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;
import com.makemoji.sbaar.alpha.adaptor.GIFAdaptor;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageListActivity extends AppCompatActivity {
    Toolbar toolbar;
    RecyclerView androidGridView;
    LoadingDialog loadingDialog;
    String categoryName;
    TextView tv_toolbarhear;
    ImageView iv_back;
    ArrayList<MojiModel> mojiModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imgelist);
        toolbar = findViewById(R.id.toolbar);
        androidGridView = findViewById(R.id.gridview_android_example);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        androidGridView.setLayoutManager(layoutManager);
        loadingDialog = new LoadingDialog(this);
        tv_toolbarhear = toolbar.findViewById(R.id.tv_toolbarhear);
        iv_back = findViewById(R.id.iv_back);
        loadingDialog.show();
        Intent intent = getIntent();
        categoryName = intent.getStringExtra("categoryName");
        tv_toolbarhear.setText(categoryName);
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
                            if (c.name.equalsIgnoreCase(categoryName)) {
                                c.models = wallData.body().get(c.name);
                                mojiModels.addAll(c.models);
                            }
                        }
                        androidGridView.setAdapter(new GIFAdaptor(ImageListActivity.this, mojiModels, "1"));
                        Log.e("astronomyModels", String.valueOf(mojiModels.size()));
                        loadingDialog.dismiss();

                    }
                });
            }
        });


        iv_back.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

}
