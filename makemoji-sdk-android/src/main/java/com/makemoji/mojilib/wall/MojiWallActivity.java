package com.makemoji.mojilib.wall;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.makemoji.mojilib.IMojiSelected;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiUnlock;
import com.makemoji.mojilib.R;
import com.makemoji.mojilib.RecentPopulator;
import com.makemoji.mojilib.SmallCB;
import com.makemoji.mojilib.model.MojiModel;

import org.json.JSONObject;

/**
 * Created by Scott Baar on 4/16/2016.
 */
public class MojiWallActivity extends AppCompatActivity implements IMojiSelected,MojiUnlock.ILockedCategoryClicked{
    boolean selected =false;
    public static final String EXTRA_THEME = "com.makemoji.mojilib.wall.MojiWallActivity.THEME";
    public static final String EXTRA_SHOWRECENT = "com.makemoji.mojilib.wall.MojiWallActivity.SHOWRECENT";
    public static final String EXTRA_SHOWUNICODE = "com.makemoji.mojilib.wall.MojiWallActivity.SHOWUNICODE";
    public static final String EXTRA_REACTION_ID = "com.makemoji.mojilib.wall.MojiWallActivity.REACTION_ID";
    public MojiWallFragment fragment;
    String reactionId;
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setTheme(getIntent().getIntExtra(EXTRA_THEME, R.style.MojiWallDefaultStyle));
        setContentView(R.layout.mm_moji_wall_activity);
        reactionId = getIntent().getStringExtra(EXTRA_REACTION_ID);
        fragment = MojiWallFragment.newInstance(getIntent().getBooleanExtra(EXTRA_SHOWRECENT,false),
                getIntent().getBooleanExtra(EXTRA_SHOWUNICODE,false));
        getSupportFragmentManager().beginTransaction().
                add(R.id._mm_page_container,fragment ,"mojiWall")
                .commitAllowingStateLoss();
    }
    @Override
    public void mojiSelected(MojiModel model, @Nullable BitmapDrawable bd) {
        Intent intent = new Intent();
        JSONObject jo = MojiModel.toJson(model);
        if (jo!=null) {
            intent.putExtra(Moji.EXTRA_JSON, jo.toString());
            intent.putExtra(EXTRA_REACTION_ID,reactionId);
            setResult(RESULT_OK, intent);
            selected = true;
            finish();
        }
        else{
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void lockedCategoryClick(String name) {
        MojiUnlock.addGroup(name);
        fragment.refresh();

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (!selected)
            setResult(RESULT_CANCELED);
    }
}
