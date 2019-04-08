package com.makemoji.mojilib;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.makemoji.mojilib.model.ReactionsData;
import com.makemoji.mojilib.wall.MojiWallActivity;

import java.util.List;

/**
 * Created by Scott Baar on 7/5/2016.
 */
public class ReactionsLayout extends LinearLayout implements PagerPopulator.PopulatorObserver, ReactionsAdapter.onItemClick {
    ReactionsData data;
    List<ReactionsData.Reaction> reactions;
    ReactionsAdapter adapter;
    RecyclerView rv;
    ImageButton addReaction;

    public ReactionsLayout(Context context) {
        super(context);
        init(null,0);
    }

    public ReactionsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs,0);
    }

    public ReactionsLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs,defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReactionsLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context,attrs,defStyleAttr,defStyleRes);
    }

    public void init(AttributeSet attrs,int defStyle){
        inflate(getContext(),R.layout.mm_reactions_layout,this);
        rv = (RecyclerView)findViewById(R.id._mm_recylcer_view);
        addReaction = (ImageButton) findViewById(R.id.mm_add_reaction);
        rv.setLayoutManager(new LinearLayoutManager(getContext(),HORIZONTAL,false));
        adapter = new ReactionsAdapter(this);
        rv.setAdapter(adapter);

        addReaction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (data==null) return;
                Intent intent = new Intent(getContext(), MojiWallActivity.class);
                //intent.putExtra(MojiWallActivity.EXTRA_THEME,R.style.MojiWallDefaultStyle_Light); //to theme it
                intent.putExtra(MojiWallActivity.EXTRA_SHOWRECENT,true);
                intent.putExtra(MojiWallActivity.EXTRA_SHOWUNICODE,true);
                intent.putExtra(MojiWallActivity.EXTRA_REACTION_ID,data.id);
                if (getContext()instanceof Activity)
                    ((Activity) getContext()).startActivityForResult(intent,IMojiSelected.REQUEST_MOJI_MODEL);
                else
                    Toast.makeText(getContext(),"Reaction layout must be in an activity",Toast.LENGTH_LONG).show();

                ReactionsData.onNewReactionClicked(data);
            }
        });

    }
    public void setReactionsData(ReactionsData newData){
        if (data!=null&& newData!=data) data.removeObserver(this);//remove old observer so we don't recieve updates from it anymore.
        this.data = newData;
        data.setObserver(this);
    }

    @Override
    public void onNewDataAvailable() {
        reactions = data.getReactions();
        adapter.setReactions(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void scrollNeeded() {
        rv.scrollToPosition(0);
    }
}
