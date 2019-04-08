package com.makemoji.mojilib;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * supply an implementation to MojiEditText to create your own input connection, for supporting things like commitContent from a keyboard
 * Created by Scott Baar on 11/19/2016.
 */

public interface IInputConnectionCreator {
    InputConnection onCreateInputConnection(EditorInfo atttrs, InputConnection superConnection);

}
