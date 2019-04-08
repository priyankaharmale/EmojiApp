package com.makemoji.sbaar.alpha;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.view.View;

import com.makemoji.mojilib.MojiInputLayout;

import org.hamcrest.Matcher;


/**
 * Created by s_baa on 4/1/2017.
 */

public class CustomActionMojiInput implements ViewAction{

    @Override
    public Matcher<View> getConstraints(){
        return ViewMatchers.isAssignableFrom(MojiInputLayout.class);
    }


    @Override
    public String getDescription(){
        return "Moji Input Layout action";
    }

    @Override
    public void perform(UiController uiController, View view){
        MojiInputLayout mojiInputLayout = (MojiInputLayout) view;
    }
}
