package com.makemoji.mojilib;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.makemoji.mojilib.gif.GifImageView;
import com.makemoji.mojilib.model.MojiModel;
import com.makemoji.mojilib.model.SpaceMojiModel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Baar on 2/5/2016.
 */
public class MojiGridAdapter extends RecyclerView.Adapter<MojiGridAdapter.Holder>
{
    List<MojiModel> mojiModels = new ArrayList<>();
    boolean vertical;
    int spanSize;
    Drawable phraseBg;
    boolean enablePulse = true;
    boolean imaagesSizedToSpan = true;
    boolean useKbLifecycle;


    public static final int ITEM_NORMAL = 0;
    public static final int ITEM_GIF = 1;
    public static final int ITEM_PHRASE = 2;
    public static final int ITEM_VIDEO = 3;
    public static final int ITEM_HSPACE = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ITEM_NORMAL,ITEM_GIF,ITEM_PHRASE,ITEM_VIDEO})
    public @interface ItemType {}
    IMojiSelected iMojiSelected;


    public void setEnablePulse(boolean enable){
        enablePulse = enable;
    }
    //force gif image views to have the mmkb hash and NOT the open activity's.
    public void useKbLifecycle(){
        useKbLifecycle = true;

    }

    public MojiGridAdapter (List<MojiModel> models, IMojiSelected iMojiSelected,boolean vertical, int spanSize) {
        mojiModels = models;
        this.iMojiSelected = iMojiSelected;
        this.spanSize = spanSize;
        this.vertical =vertical;

        phraseBg = ContextCompat.getDrawable(Moji.context,R.drawable.mm_phrase_bg);
    }

    public void setMojiModels(List<MojiModel> models){
        mojiModels = new ArrayList<>(models);
        notifyDataSetChanged();
    }
    public List<MojiModel> getMojiModels(){
        return mojiModels;
    }
    public void setImagesSizedtoSpan(boolean enable){
        imaagesSizedToSpan = enable;
    }
    @Override
    public int getItemCount() {
        return mojiModels.size();
    }


    @Override public int getItemViewType(int position){
        if (mojiModels.get(position) instanceof SpaceMojiModel) return ITEM_HSPACE;
        if (mojiModels.get(position).gif==1)return ITEM_GIF;
        if (mojiModels.get(position).isPhrase()) return ITEM_PHRASE;
        if (mojiModels.get(position).isVideo()) return ITEM_VIDEO;
        return 0;
    }
    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType==ITEM_NORMAL)
        v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mm_rv_moji_item, parent, false);
        else if (viewType==ITEM_GIF){
            v = LayoutInflater.from(parent.getContext())
                    .inflate(vertical?R.layout.mm_gif_iv_vertical:R.layout.mm_gif_iv,parent,false);
        }
        else if (viewType==ITEM_PHRASE){
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.mm_rv_phrase_item, parent, false);
            v.setBackgroundDrawable(phraseBg);
        }
        else if (viewType == ITEM_HSPACE){
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.mm_item_hspace, parent, false);
        }
        else{
            v = LayoutInflater.from(parent.getContext())
                    .inflate(vertical?R.layout.mm_video_moji_item_vertical:R.layout.mm_video_moji_item, parent, false);
        }

        //v.getLayoutParams().height = parent.getHeight()/2;
        return new Holder(v,parent);
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {
        final MojiModel model = mojiModels.get(position);

        Mojilytics.trackView(model.id);
        Log.d("catego",model.categoryName);

            if (getItemViewType(position) == ITEM_NORMAL) {
                holder.imageView.setPulseEnabled(enablePulse);
                holder.imageView.forceDimen(holder.dimen);
                holder.imageView.sizeImagesToSpanSize(imaagesSizedToSpan);
                holder.imageView.setModel(model);
                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iMojiSelected.mojiSelected(model, null);
                    }
                });
            } else if (getItemViewType(position) == ITEM_GIF) {
                holder.gifImageView.getFromUrl(model.image_url);
                holder.gifImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iMojiSelected.mojiSelected(model, null);
                    }
                });
            } else if (getItemViewType(position) == ITEM_VIDEO) {
                holder.imageView.setModel(model);
                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iMojiSelected.mojiSelected(model, null);
                    }
                });
                holder.title.setText(model.name);
                FrameLayout.LayoutParams lp = ( FrameLayout.LayoutParams ) holder.overlay.getLayoutParams();
                lp.width = holder.dimen / 2;
                lp.height = holder.dimen / 2;
                holder.overlay.setLayoutParams(lp);
            }

       /*else if (getItemViewType(position) == ITEM_PHRASE) {
            LinearLayout ll = ( LinearLayout ) holder.itemView;
            ll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (MojiModel emoji : model.emoji)
                        iMojiSelected.mojiSelected(model, null);
                }
            });
            while (holder.mojiImageViews.size() < model.emoji.size()) {
                MojiImageView v = ( MojiImageView ) LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.mm_rv_moji_item, ll, false);
                v.setPulseEnabled(enablePulse);
                v.sizeImagesToSpanSize(false);
                //v.setPadding(0,(int)(2*Moji.density),(int)(-5*Moji.density),(int)(2*Moji.density));
                ll.addView(v);
                holder.mojiImageViews.add(( MojiImageView ) v);
            }
            for (int i = 0; i < ll.getChildCount(); i++) {
                MojiImageView mojiImageView = ( MojiImageView ) ll.getChildAt(i);

                MojiModel sequence = model.emoji.size() > i ? model.emoji.get(i) : null;
                if (sequence != null) {
                    mojiImageView.forceDimen(holder.dimen);
                    mojiImageView.setModel(sequence);
                    mojiImageView.setVisibility(View.VISIBLE);
                } else mojiImageView.setVisibility(View.GONE);
            }

        }
*/

    }



class Holder extends RecyclerView.ViewHolder {
    MojiImageView imageView;
    int dimen;
    List<MojiImageView> mojiImageViews = new ArrayList<>();
    GifImageView gifImageView;
    TextView title;
    ImageView overlay;
    ViewGroup parent;

    public Holder(View v, ViewGroup parent) {
        super(v);
        this.parent = parent;
        if (v instanceof MojiImageView)imageView = (MojiImageView) v;
        else if (v instanceof GifImageView) {
            gifImageView = (GifImageView) v;
            if (useKbLifecycle) gifImageView.useKbLifecycle = true;
        }
        else{
            imageView = (MojiImageView)v.findViewById(R.id._mm_moji_iv);
            title = (TextView) v.findViewById(R.id.mm_item_title);
            overlay = (ImageView) v.findViewById(R.id._mm_play_overlay);
        }
        dimen = spanSize;


    }
}
}