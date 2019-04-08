package com.makemoji.mojilib;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by Scott Baar on 1/9/2016.
 */
public interface MojiApi {
    String BASE_URL = "https://api.makemoji.com/sdk/";

    @GET("emoji/categories")
    Call<List<Category>> getCategories();

    @FormUrlEncoded
    @POST("messages/create")
    Call<JsonObject> sendPressed(@Field("message") String htmlMessage);

    @POST("emoji/viewTrack")
    Call<Void> trackViews( @Body RequestBody array);

    @FormUrlEncoded
    @POST("emoji/clickTrackBatch")
    Call<Void> trackClicks(@Field("emoji") String emoji);

    @POST("emoji/share/{device-id}/{emoji-id}")
    Call<Void> trackShare(@Path("device-id")String deviceId, @Path("emoji-id")String emojiId);

    @GET("emoji/emojiwall")
    Call<Map<String,List<MojiModel>>> getEmojiWallData();

    @GET("emoji/emojiwall/3pk")
    Call<Map<String,List<MojiModel>>> getEmojiWallData3pk();

    @FormUrlEncoded
    @POST("emoji/unlockGroup")
    Call<JsonObject> unlockGroup(@Field("category_name")String categoryName);

    @GET("reactions/get/{sha1_content_id}")
    Call<JsonObject> getReactionData(@Path("sha1_content_id") String sha1ContentId);

    @FormUrlEncoded
    @POST("reactions/create/{sha1_content_id}")
    Call<JsonObject> createReaction(@Path("sha1_content_id") String sha1ContentId, @Field("emoji_id") int emojiId,@Field("emoji_type") String emojiType);


}
