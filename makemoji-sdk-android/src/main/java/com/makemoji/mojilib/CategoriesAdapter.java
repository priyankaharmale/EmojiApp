package com.makemoji.mojilib;

import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.makemoji.mojilib.model.Category;
import com.squareup.picasso252.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott Baar on 1/10/2016.
 */
public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.Holder> {
    List<Category> categories = new ArrayList<>();
    ICatListener iCatListener;
    String flag;
    @ColorInt
    int textColor;

    public interface ICatListener {
        void onClick(Category category);
    }

    public CategoriesAdapter(ICatListener iCatListener, @ColorInt int textColor, String flag) {
        this.iCatListener = iCatListener;
        this.textColor = textColor;
        this.flag = flag;

    }

    public void setCategories(List<Category> newCategories) {
        categories = newCategories;
        notifyDataSetChanged();

    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mm_item_category, parent, false);
        //v.getLayoutParams().height = parent.getHeight()/2;
        return new Holder(v, parent);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        Category category = categories.get(position);
        if (holder.position != position) {
            //Moji.loadImage(holder.image,category.image_url);
            int width = (int) (holder.view.getResources().getDimension(R.dimen.mm_cat_width) * Moji.density * 1);
            if (!category.image_url.equals(holder.image.getTag())) {
                Picasso.with(Moji.context).load(Moji.uriImage(category.image_url)).
                        resize(width, width).placeholder(R.drawable.mm_placeholder).into(holder.image);
                holder.image.setTag(category.image_url);
            }
            holder.title.setText(category.name);
            holder.view.setTag(category);
            if (category.isLocked() && !MojiUnlock.getUnlockedGroups().contains(category.name))
                holder.foreground.setVisibility(View.VISIBLE);
            else
                holder.foreground.setVisibility(View.GONE);
        }
        if (flag.equalsIgnoreCase("1")) {

            holder.foreground.setVisibility(View.GONE);
            holder.image.setVisibility(View.VISIBLE);
        } else {
            if(category.name.equalsIgnoreCase("ALL"))
            {
                holder.foreground.setVisibility(View.GONE);
                holder.image.setVisibility(View.VISIBLE);
            }else
            if(category.name.equalsIgnoreCase("GIF"))
            {
                holder.foreground.setVisibility(View.GONE);
                holder.image.setVisibility(View.VISIBLE);
            }
            else
            {
                holder.foreground.setVisibility(View.VISIBLE);
                holder.image.setVisibility(View.GONE);
            }

        }

    }

    View.OnClickListener catClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (iCatListener != null) iCatListener.onClick((Category) v.getTag());
        }
    };

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public class Holder extends RecyclerView.ViewHolder {
        public ImageView image;
        public ImageView foreground;
        public TextView title;
        public View view;
        public int position = -1;
        public View parent;

        public Holder(View itemView, ViewGroup parent) {
            super(itemView);
            this.parent = parent;
            view = itemView;
            view.setOnClickListener(catClick);
            image = (ImageView) itemView.findViewById(R.id._mm_item_category_iv);
            foreground = (ImageView) itemView.findViewById(R.id._mm_item_category_foreground);
            title = (TextView) itemView.findViewById(R.id._mm_item_category_tv);
            title.setTextColor(textColor);
        }
    }
}
