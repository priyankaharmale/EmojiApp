package com.makemoji.sbaar.alpha.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;
import android.util.Patterns;

public class Utilities {
    public static void showAlertDailog(Context context, String title, String message, String positivebtn, OnClickListener positive_onclicklistner, Boolean cancelable) {
        try {
            Builder alertDialogBuilder = new Builder(context);
            alertDialogBuilder.setTitle((CharSequence) title);
            alertDialogBuilder.setCancelable(cancelable.booleanValue());
            alertDialogBuilder.setMessage((CharSequence) message);
            alertDialogBuilder.setPositiveButton((CharSequence) positivebtn, positive_onclicklistner);
            alertDialogBuilder.create().show();
        } catch (Exception ex) {
            Log.e(AppConstant.TAG, "Error(showAlertDailog):" + ex.toString());
        }
    }

    public static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static ProgressDialog showProgressDialog(Context context, String text) {
        ProgressDialog progress = new ProgressDialog(context);
        progress.setMessage(text);
        progress.setProgressStyle(0);
        progress.show();
        return progress;
    }

   public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public ProgressDialog showDailog(Context con) {
        ProgressDialog m_Dialog = new ProgressDialog(con);
        m_Dialog.setMessage("Please wait while logging...");
        m_Dialog.setProgressStyle(0);
        m_Dialog.setCancelable(false);
        return m_Dialog;
    }
}
