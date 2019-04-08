package com.makemoji.sbaar.alpha;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static com.makemoji.sbaar.alpha.InputActivity.TAG;


public class GIfImageActivity extends AppCompatActivity {
    String image;
    ImageView iv_back, iv_share;
    GifImageview gifImageview;
    Bitmap bitmap1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_giffullimage);
        gifImageview = findViewById(R.id.GifImageView);
        iv_share = findViewById(R.id.iv_share);
        Bundle bundle = this.getIntent().getExtras();
        String pic = bundle.getString("image");
        Log.e("Pictiyr", pic);

        Drawable drawable = ContextCompat.getDrawable(GIfImageActivity.this, R.drawable.lock);
        Uri myUri = Uri.parse(pic);

        gifImageview.setGifImageUri(myUri);




/*
        try {
            URL url = new URL(pic);
            HttpURLConnection connection = (HttpURLConnection ) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            bitmap1 = BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            System.out.println("Exception"+e);
        }*/

        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        iv_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, saveImageExternal(bitmap1));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/*");
                startActivity(intent);

            }
        });
        //  imageView.setImageBitmap(bitmap);

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
}
