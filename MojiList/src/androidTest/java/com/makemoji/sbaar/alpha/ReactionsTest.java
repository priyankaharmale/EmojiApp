package com.makemoji.sbaar.alpha;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.base.MainThread;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.jakewharton.espresso.OkHttp3IdlingResource;
import com.makemoji.mojilib.IMojiSelected;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.model.ReactionsData;
import com.makemoji.mojilib.wall.MojiWallActivity;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsAnything;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert.*;

import java.util.List;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by s_baa on 6/11/2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ReactionsTest {
    private final String MOJI_JSON = "{\"native\":0,\"emoji\":[],\"flashtag\":\"HoldingHands\",\"fromSearch\":false,\"gif\":0,\"id\":542," +
            "\"image_url\":\"https:\\/\\/d1tvcfe0bfyi6u.cloudfront.net\\/emoji\\/542-large@2x.png\",\"locked\":false,\"name\":\"Holding Hands\",\"" +
            "phrase\":0,\"video\":0,\"video_url\":\"\"}";
    IdlingResource resource;
    @Rule
    public ActivityTestRule<ReactionsActivity> mActivityRule = new ActivityTestRule<>(
            ReactionsActivity.class, true, true);

    @Before
    public void setup(){
    }
    @After
    public void cleanup(){
    }

    @Test
    public void onReactionsClick(){

        Assert.assertTrue( !ReactionsData.onActivityResult(0,0,null));//no data
        ReactionsData data = new ReactionsData("abc");
        ReactionsData.onNewReactionClicked(data);
        Intent i = new Intent();

        i.putExtra(MojiWallActivity.EXTRA_REACTION_ID,"abcd");
        i.putExtra(Moji.EXTRA_JSON,MOJI_JSON);
        Assert.assertTrue( !ReactionsData.onActivityResult(IMojiSelected.REQUEST_MOJI_MODEL, Activity.RESULT_OK,i)); //wrong id
        i.putExtra(MojiWallActivity.EXTRA_REACTION_ID,"abc");
        Assert.assertTrue( ReactionsData.onActivityResult(IMojiSelected.REQUEST_MOJI_MODEL, Activity.RESULT_OK,i));
    }

    //does the reaction reorder when selected?
    //total should go up or down by 1, if we're selecting or deselecting the emoji.
    @Test
    public void testReorderOnClick(){
        SystemClock.sleep(2000);
        int idAt2 = mActivityRule.getActivity().adapter.getItem(0).reactionsData.reactions.get(2).emoji_id;
        int count = mActivityRule.getActivity().adapter.getItem(0).reactionsData.reactions.get(2).total;
        onData(CoreMatchers.anything()).inAdapterView(withId(R.id.list_view)).atPosition(0).onChildView(withId(R.id._mm_recylcer_view)).
                perform(RecyclerViewActions.actionOnItemAtPosition(2, ViewActions.click()));
        int idAt0 = mActivityRule.getActivity().adapter.getItem(0).reactionsData.reactions.get(0).emoji_id;


        int newCount = mActivityRule.getActivity().adapter.getItem(0).reactionsData.reactions.get(0).total;
        Assert.assertTrue(idAt0==idAt2);
        Assert.assertTrue(count + 1 == newCount || count - 1 == newCount);

    }

    @After
    public void unregisterIdlingResource() {
        if (resource != null) {
            Espresso.unregisterIdlingResources(resource);
        }
    }
}
