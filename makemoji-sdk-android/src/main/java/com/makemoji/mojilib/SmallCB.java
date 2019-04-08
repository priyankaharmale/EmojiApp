package com.makemoji.mojilib;

import android.support.annotation.Nullable;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shortcut to condense retrofit callbacks. This syntax is the one and only thing parse did well.
 * Created by Scott Baar on 1/9/2016.
 */
public abstract class SmallCB<T> implements Callback<T>{
    @Override
    public void onResponse(Call<T> call,Response<T> response) {
        if (response.isSuccessful()){
            done(response,null);
        }
        else
        {
            Log.e("retrofit cb error ",""+ response.message()+ response.code()+" "+call.request().url());
            done(null, new Throwable("moji retrofit error "+response.message()));
        }
    }

    @Override
    public void onFailure(Call<T> call,Throwable t) {
        done(null,t);

    }
    public abstract void done(Response<T> response, @Nullable Throwable t);
}
