package com.makemoji.sbaar.alpha.adaptor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.makemoji.mojilib.model.MojiModel;
import com.makemoji.sbaar.alpha.BuildConfig;
import com.makemoji.sbaar.alpha.GIFActivity;
import com.makemoji.sbaar.alpha.ImageActivity;
import com.makemoji.sbaar.alpha.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.makemoji.sbaar.alpha.InputActivity.TAG;

public class GIFAdaptor extends RecyclerView.Adapter<GIFAdaptor.ViewHolder> {
    private Context context;
    ArrayList<MojiModel> mojiModels;
    private LayoutInflater inflater;
    Drawable drawable;
    String callfrom;

    public GIFAdaptor(Context context, ArrayList<MojiModel> mojiModels, String s) {
        this.context = context;
        this.mojiModels = mojiModels;
        this.inflater = LayoutInflater.from(context);
        this.callfrom = s;
    }


    @Override
    public GIFAdaptor.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View rowView = inflater.inflate(R.layout.adaptor_images, parent, false);
        GIFAdaptor.ViewHolder vh = new GIFAdaptor.ViewHolder(rowView);
        return vh;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final GIFAdaptor.ViewHolder holder, final int i) {
        final MojiModel details = mojiModels.get(i);


        Log.e("GIF", details.image_url);

        drawable = ContextCompat.getDrawable(context, R.drawable.appicob);

        //  Glide.with(context).load(details.image_url).asGif().diskCacheStrategy(DiskCacheStrategy.SOURCE).crossFade().into(holder.iv_Stringimage);

        try {
            Glide.with(context)
                    .load(details.image_url)
                    .error(drawable)
                    .placeholder(drawable)
                    .centerCrop()
                    .crossFade()
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            holder.progress_item.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            holder.progress_item.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.iv_Stringimage);
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (callfrom.equalsIgnoreCase("1")) {
                    Intent intent = new Intent(context, ImageActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("image", details.image_url);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, GIFActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("image", details.image_url);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                }


            }
        });


    }

    private Uri saveImageExternal(Bitmap image) {
        //TODO - Should be processed in another thread
        Uri uri = null;
        try {
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "to-share.png");
            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.close();
            //uri = Uri.fromFile(file);
            uri = FileProvider.getUriForFile(context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file);

        } catch (IOException e) {
            Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }

    @Override
    public int getItemCount() {
        return mojiModels.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView textViewStringTitle, textViewDate, textViewTime, tv_StringendDt, tv_StringAddress, tv_StringPrice;
        ImageView iv_Stringimage;
        ProgressBar progress_item;

        public ViewHolder(View itemView) {
            super(itemView);

            iv_Stringimage = itemView.findViewById(R.id.image);
            progress_item = itemView.findViewById(R.id.progress_item);
        }
    }


}
