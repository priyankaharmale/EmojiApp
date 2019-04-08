package com.makemoji.sbaar.alpha;
import static android.support.test.espresso.Espresso.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.contrib.RecyclerViewActions.*;
import android.support.test.espresso.core.deps.guava.base.CharMatcher;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.matcher.*;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiInputLayout;
import com.makemoji.mojilib.MojiSQLHelper;
import com.makemoji.mojilib.RecentPopulator;
import com.makemoji.mojilib.model.MojiModel;
import com.makemoji.mojilib.wall.MojiWallActivity;

import android.support.test.InstrumentationRegistry;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert.*;

import java.util.List;



/**
 * Created by s_baa on 3/21/2017.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class InputTest {
    final String SPAN_STRING = " \uFFFC ";
    @Rule
    public ActivityTestRule<InputActivity> mActivityRule = new ActivityTestRule<>(
            InputActivity.class,true,true);

    public void openKb(){
        onView(withId(R.id._mm_edit_text)).perform(ViewActions.click(),ViewActions.typeTextIntoFocusedView("ab"),ViewActions.clearText());
    }
    @Test//test search is substitted on search click when appropriate
    public void atestSearchClick(){
        openKb();
        onView(withId(R.id._mm_edit_text)).perform(ViewActions.click(),ViewActions.typeTextIntoFocusedView("sm"), new SetSelectionEndAction());
        onView(withId(R.id._mm_recylcer_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0,ViewActions.click()));
        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withText(SPAN_STRING)));
        onView(withId(R.id._mm_edit_text)).perform(ViewActions.click(),ViewActions.clearText(),ViewActions.typeTextIntoFocusedView("a "));
        onView(withId(R.id._mm_recylcer_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0,ViewActions.click()));

        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withText("a "+SPAN_STRING)));
    }

    @Test public void testOpenLeft(){
        openKb();
        onView(withId(R.id._mm_horizontal_top_scroller)).perform(ViewActions.swipeRight());
        onView(withId(R.id._mm_categories_button)).perform(ViewActions.click());
        onView(withId(R.id._mm_page_container)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }
    public void openLeft(){
        onView(withId(R.id._mm_horizontal_top_scroller)).perform(ViewActions.swipeRight());
    }
    //tests handling the wall result intent
    @Test public void testHandleIntent(){
        final Intent intent = new Intent();
        intent.putExtra(Moji.EXTRA_PACKAGE_ORIGIN,InstrumentationRegistry.getTargetContext().getPackageName());
        intent.putExtra(Moji.EXTRA_JSON,"{\"native\":0,\"emoji\":[],\"flashtag\":\"SWAG\",\"gif\":0,\"id\":1011327," +
                "\"image_url\":\"https:\\/\\/d1tvcfe0bfyi6u.cloudfront.net\\/emoji\\/1011327-large@2x.png\"," +
                "\"locked\":false,\"name\":\"SWAG\",\"phrase\":0,\"video\":0,\"video_url\":\"\"}");
        onView(withId(R.id.mojiInput)).perform(new CustomActionMojiInput(){
            @Override
            public void perform(UiController uiController, View view) {
                ((MojiInputLayout)view).handleIntent(intent);
            }
        });
        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withText(" \uFFFC ")));
    }
    //test visibility and focus when attatch/detaching an outside edit text
    @Test public void testAttatchDetatch(){
        onView(withId(R.id._mm_edit_text)).perform(ViewActions.click(),ViewActions.typeTextIntoFocusedView("a"));
        onView(withId(R.id._mm_recylcer_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0,ViewActions.click()));
        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withText("a"+SPAN_STRING)));
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withText("Attatch EditText")).perform(ViewActions.click());

        onView(withId(R.id.outside_met)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.outside_met)).perform(ViewActions.click(),ViewActions.typeTextIntoFocusedView("b "));
        onView(withId(R.id._mm_recylcer_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0,ViewActions.click()));
        onView(withId(R.id.outside_met)).check(ViewAssertions.matches(ViewMatchers.withText("b " + SPAN_STRING)));

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withText("Detach EditText")).perform(ViewActions.click());

        onView(withId(R.id._mm_edit_text)).perform(ViewActions.click(),ViewActions.typeTextIntoFocusedView("b "));
        onView(withId(R.id._mm_recylcer_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0,ViewActions.click()));
        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withText("a"+SPAN_STRING+"b "+ SPAN_STRING)));
        onView(withId(R.id.outside_met)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

    }

    //test that categories can open and navigate to a different page, and that the displayed and hw back button work
    @Test public void testCategoriesAndBackButton() throws Exception{
        openKb();
        openLeft();
        onView(withId(R.id._mm_categories_button)).perform(ViewActions.click());
        onView(withId(R.id._mm_cat_rv)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id._mm_cat_rv)).perform(RecyclerViewActions.actionOnItemAtPosition(0,ViewActions.click()));
        onView(withId(R.id._mm_cat_rv)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id._mm_back_button)).perform(ViewActions.click());
        onView(withId(R.id._mm_cat_rv)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id._mm_cat_rv)).perform(RecyclerViewActions.actionOnItemAtPosition(0,ViewActions.click()));
        onView(withId(R.id._mm_cat_rv)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        mActivityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
            mActivityRule.getActivity().onBackPressed();
            }
        });
        onView(withId(R.id._mm_cat_rv)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

    }

    //test the send layout enabling and disabling and the on screen backspace button
    @Test public void testBackspaceAndSendLayoutEnable(){
        onView(withText("Send")).check(ViewAssertions.matches(Matchers.not(isEnabled())));
        openKb();
        openLeft();
        onView(withId(R.id._mm_trending_button)).perform(ViewActions.click());

        onView(withId(R.id._mm_edit_text)).perform(ViewActions.click(),ViewActions.typeTextIntoFocusedView("a"));
        onView(withText("Send")).check(ViewAssertions.matches(isEnabled()));
        onView(withText("Send")).perform(ViewActions.click());
        onView(withText("Send")).check(ViewAssertions.matches(Matchers.not(isEnabled())));
        onView(withTagValue(CoreMatchers.is((Object)"page0"))).perform(RecyclerViewActions.actionOnItemAtPosition(0,ViewActions.click()));
        onView(withText("Send")).check(ViewAssertions.matches(isEnabled()));

        onView(withId(R.id._mm_edit_text)).perform(ViewActions.click(),ViewActions.typeTextIntoFocusedView("abc"));
        onView(withId(R.id._mm_backspace_button)).perform(ViewActions.click());
        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withText(SPAN_STRING+"ab")));

    }

    //test that sending an emoji adds it to the recent list
    @SuppressLint("Assert")
    @Test public void testTrendingToRecentOnSend(){
        openKb();
        openLeft();
        onView(withId(R.id._mm_trending_button)).perform(ViewActions.click());
        MojiModel trendingModel = MojiModel.getList("Trending").get(3);
        onView(withTagValue(CoreMatchers.is((Object)"page0"))).perform(RecyclerViewActions.actionOnItemAtPosition(3,ViewActions.click()));
        onView(withText("Send")).perform(ViewActions.click());
        MojiModel recent = RecentPopulator.getRecents().get(0);
        assert recent.equals(trendingModel);

    }

    @Test
    public void testCameraButtonVisiblity(){
        onView(withId(R.id._mm_camera_ib)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.mojiInput)).perform(new CustomActionMojiInput(){
            @Override
            public void perform(UiController uiController, View view) {
                ((MojiInputLayout) view).setCameraVisibility(false);
            }
        });
        onView(withId(R.id._mm_camera_ib)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.mojiInput)).perform(new CustomActionMojiInput(){
            @Override
            public void perform(UiController uiController, View view) {
                ((MojiInputLayout) view).setCameraVisibility(true);
            }
        });
        onView(withId(R.id._mm_camera_ib)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    //test the abc button to close kb and back button to close kb and hide emoji bar
    @Test
    public void testAbcClickAndViewMinimization(){
        openKb();
        openLeft();
        onView(withId(R.id._mm_trending_button)).perform(ViewActions.click());
        onView(withId(R.id._mm_abc_tv)).perform(ViewActions.click());
        onView(withId(R.id._mm_page_container)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        closeSoftKeyboard();
        onView(withId(R.id._mm_recylcer_view)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

    }
    @Test
    public void testSetInput(){
        onView(withId(R.id.mojiInput)).perform(new CustomActionMojiInput(){
            @Override
            public void perform(UiController uiController, View view) {
                ((MojiInputLayout) view).setInputText("abc");
            }
        });
        onView(withId(R.id._mm_edit_text)).check(ViewAssertions.matches(ViewMatchers.withText("abc")));
    }

    @Test
    public void testDisableLeftSwipe(){
        openKb();
        onView(withId(R.id.mojiInput)).perform(new CustomActionMojiInput(){
            @Override
            public void perform(UiController uiController, View view) {
                ((MojiInputLayout) view).showLeftNavigation(false);
            }
        });
        onView(withId(R.id._mm_recylcer_view)).perform(ViewActions.swipeRight());
        onView(withId(R.id._mm_categories_button)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    //test that top bar proclivity works and responds to multiwindow changes
    @Test
    public void testAlwaysShowBarAndMultiWindowMode(){
        onView(withId(R.id._mm_recylcer_view)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.mojiInput)).perform(new CustomActionMojiInput(){
            @Override
            public void perform(UiController uiController, View view) {
                ((MojiInputLayout) view).onMultiWindowModeChanged(true);
            }
        });
        onView(withId(R.id._mm_recylcer_view)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.mojiInput)).perform(new CustomActionMojiInput(){
            @Override
            public void perform(UiController uiController, View view) {
                ((MojiInputLayout) view).onMultiWindowModeChanged(false);
            }
        });
        onView(withId(R.id._mm_recylcer_view)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    //test that sql searches tags, that limit is respected
    @Test
    public void testSQLSearch(){
        MojiSQLHelper helper = MojiSQLHelper.getInstance(InstrumentationRegistry.getTargetContext(),false);
        List<MojiModel> results = helper.search("smi",50);
        List<MojiModel> smallResults = helper.search("smi",1);
        Assert.assertTrue(!results.isEmpty());
        for (MojiModel m : results)
            Assert.assertTrue( m.tags.toLowerCase().contains("smi"));
        Assert.assertTrue( results.size()>=smallResults.size() && smallResults.size()==1);
    }

}
