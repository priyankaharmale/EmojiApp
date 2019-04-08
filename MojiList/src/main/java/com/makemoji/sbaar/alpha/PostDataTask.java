package com.makemoji.sbaar.alpha;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


import static com.makemoji.sbaar.alpha.InputActivity.EMAIL_KEY;
import static com.makemoji.sbaar.alpha.InputActivity.FORM_DATA_TYPE;

/**
 * Created by PC-21 on 16-Apr-18.
 */

public class PostDataTask extends AsyncTask<String, Void, Boolean>

{

    @Override
    protected Boolean doInBackground(String... contactData) {
        Boolean result = true;
        String url = contactData[0];
        String email = contactData[1];
/* String subject = contactData[2];
String message = contactData[3];
String device = contactData[4];*/
        String postBody = "";

        try {
//all values must be URL encoded to make sure that special characters like & | ",etc.
//do not cause problems
            postBody = EMAIL_KEY + "=" + URLEncoder.encode(email, "UTF-8");

        } catch (UnsupportedEncodingException ex) {
            result = false;
        }

        try {
//Create OkHttpClient for sending request
            OkHttpClient client = new OkHttpClient();
//Create the request body with the help of Media Type
            RequestBody body = RequestBody.create(FORM_DATA_TYPE, postBody);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
//Send the request
            Response response = client.newCall(request).execute();
        } catch (IOException exception) {
            result = false;
        }
        return result;
    }


    @Override
    protected void onPostExecute(Boolean result) {
//Print Success or failure message accordingly
// Toast.makeText(context, result ? "Model_Message successfully sent!" : "There was some error in sending message. Please try again after some time.", Toast.LENGTH_LONG).show();
    }


}
