package com.makemoji.sbaar.alpha;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;


/**
 * Created by Priyanka H on 13/06/2018.
 */
public class LoadingDialog {

    private final Context context;
    private final AlertDialog dialog;

    public LoadingDialog(Context context) {
        this.context = context;
        // 1. Instantiate an AlertDialog.Builder with its constructor

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View loadingView = LayoutInflater.from(context).inflate(R.layout.loading_dialog, null, false);

// 2. Chain together various setter methods to set the dialog characteristics
        builder.setView(loadingView);


// 3. Get the AlertDialog from create()
        dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


    }

    public void show() {
        if (dialog != null && (!dialog.isShowing())) {
            dialog.show();
        }
    }

    public boolean isShowing() {
        if (dialog != null) {
            return dialog.isShowing();
        }
        return false;
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
