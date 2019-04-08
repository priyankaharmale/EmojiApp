package com.makemoji.sbaar.alpha;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import in.myinnos.gifimages.task.ShareGif;

import static com.makemoji.sbaar.alpha.InputActivity.TAG;


public class ImageActivity extends AppCompatActivity {
    String image;
    ImageView imageView, iv_back, iv_share;
    String pic;
    Bitmap bitmap1;
    ProgressBar progress_item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullimage);
        imageView = findViewById(R.id.iv_main);
        iv_back = findViewById(R.id.iv_back);
        iv_share = findViewById(R.id.iv_share);
        progress_item = findViewById(R.id.progress_item);
        Bundle bundle = this.getIntent().getExtras();
        pic = bundle.getString("image");
        Log.e("Pictiyr", pic);


        Drawable drawable = ContextCompat.getDrawable(ImageActivity.this, R.drawable.appicob);

        try {
            Glide.with(this)
                    .load(pic)
                    .error(drawable)
                    .centerCrop()
                    .placeholder(drawable)
                    .crossFade()
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            progress_item.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            progress_item.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(imageView);
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }

        //bitmap1 = (( BitmapDrawable ) imageView.getDrawable()).getBitmap();

        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    URL url = null;
                    try {
                        url = new URL(pic);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    HttpURLConnection connection = null;
                    try {
                        connection = ( HttpURLConnection ) url.openConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    connection.setDoInput(true);
                    try {
                        connection.connect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    InputStream input = null;
                    try {
                        input = connection.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    bitmap1 = BitmapFactory.decodeStream(input);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();


        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        iv_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(ImageActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(ImageActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            0);
                } else {
                    new ShareImage(iv_share.getContext(), pic).execute();
                }

            }
        });

    }

    private Uri saveImageExternal(Bitmap image) {
        //TODO - Should be processed in another thread
        Uri uri = null;
        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "to-share.png");
            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.close();
            //uri = Uri.fromFile(file);
            uri = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file);

        } catch (IOException e) {
            Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }


    private void shareGif(String resourceName) {

        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "sharingGif.gif";

        File sharingGifFile = new File(baseDir, fileName);

        try {
            byte[] readData = new byte[1024 * 500];
            InputStream fis = getResources().openRawResource(getResources().getIdentifier("https://d1tvcfe0bfyi6u.cloudfront.net/emoji/1022721.gif", "drawable", getPackageName()));

            FileOutputStream fos = new FileOutputStream(sharingGifFile);
            int i = fis.read(readData);

            while (i != -1) {
                fos.write(readData, 0, i);
                i = fis.read(readData);
            }

            fos.close();
        } catch (IOException io) {
        }
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("image/gif");
        Uri uri = Uri.fromFile(sharingGifFile);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(shareIntent, "Share Emoji"));
    }


}
