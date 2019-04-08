package com.makemoji.sbaar.alpha;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;
import android.widget.EditText;

import org.hamcrest.Matcher;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.allOf;

/**
 * Created by s_baa on 4/2/2017.
 */

public class SetSelectionEndAction implements ViewAction {

        public SetSelectionEndAction() {
        }

        @SuppressWarnings("unchecked")
        @Override
        public Matcher<View> getConstraints() {
            return allOf(isDisplayed(), isAssignableFrom(EditText.class));
        }

        @Override
        public void perform(UiController uiController, View view) {
            ((EditText) view).setSelection(((EditText) view).getText().length());
        }

        @Override
        public String getDescription() {
            return "set selection end text";
        }
    }


