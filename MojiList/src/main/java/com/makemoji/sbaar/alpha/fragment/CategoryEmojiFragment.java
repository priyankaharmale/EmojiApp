package com.makemoji.sbaar.alpha.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.SmallCB;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;
import com.makemoji.sbaar.alpha.ImageListActivity;
import com.makemoji.sbaar.alpha.InputActivity;
import com.makemoji.sbaar.alpha.LoadingDialog;
import com.makemoji.sbaar.alpha.R;
import com.makemoji.sbaar.alpha.adaptor.GIFAdaptor;
import com.makemoji.sbaar.alpha.adaptor.ImageAdaptor;
import com.makemoji.sbaar.alpha.inappNew;
import com.makemoji.sbaar.alpha.util.IabBroadcastReceiver;
import com.makemoji.sbaar.alpha.util.IabHelper;
import com.makemoji.sbaar.alpha.util.IabResult;
import com.makemoji.sbaar.alpha.util.Inventory;
import com.makemoji.sbaar.alpha.util.Purchase;
import com.makemoji.sbaar.alpha.util.Utilities;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

public class CategoryEmojiFragment extends Fragment {

    ImageView iv_purchase;
    String deivceId;
    boolean isPay = false;
    LinearLayout ll_lockcategory, ll_unlockcategory;
    ImageView iv_astrology, iv_astronomy, iv_mythology, iv_wellness, iv_birthday;
    LoadingDialog loadingDialog;
    SharedPreferences prefUser;
    String isPayment = "";
    String device_id;
    String categoryName;
    RecyclerView androidGridView;
    ArrayList<Category> mojimodels ;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_category, container, false);
        iv_purchase = view.findViewById(R.id.iv_purchase);
        ll_unlockcategory = view.findViewById(R.id.ll_unlockcategory);
        ll_lockcategory = view.findViewById(R.id.ll_lockcategory);
        iv_astrology = view.findViewById(R.id.iv_astrology);
        iv_astronomy = view.findViewById(R.id.iv_astronomy);
        iv_mythology = view.findViewById(R.id.iv_mythology);
        iv_wellness = view.findViewById(R.id.iv_wellness);
        iv_birthday = view.findViewById(R.id.iv_birthday);
        deivceId = Moji.getUserId();
        loadingDialog = new LoadingDialog(getActivity());
        loadingDialog.show();

        androidGridView = view.findViewById(R.id.gridview_android_example);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        androidGridView.setLayoutManager(layoutManager);

        loadingDialog.show();
        prefUser = getActivity().getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
        isPayment = prefUser.getString("isPayment", "");
        checkPaytest(deivceId);

        iv_purchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), inappNew.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("device_id", deivceId);
                startActivity(intent);
                //loaddata();
            }
        });
        iv_astrology.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ImageListActivity.class);
                intent.putExtra("categoryName", "Astrology");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        iv_astronomy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ImageListActivity.class);
                intent.putExtra("categoryName", "Astronomy");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        iv_mythology.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ImageListActivity.class);
                intent.putExtra("categoryName", "Mythology");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        iv_wellness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ImageListActivity.class);
                intent.putExtra("categoryName", "Wellness");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        iv_birthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ImageListActivity.class);
                intent.putExtra("categoryName", "Happy Birthday");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        //loadData();
        Intent intent = getActivity().getIntent();
        categoryName = intent.getStringExtra("categoryName");
        device_id = intent.getStringExtra("device_id");
        if (isPayment.equalsIgnoreCase("") || isPayment == null) {
            ll_lockcategory.setVisibility(View.VISIBLE);
            ll_unlockcategory.setVisibility(View.GONE);
        } else {
            ll_lockcategory.setVisibility(View.GONE);
            ll_unlockcategory.setVisibility(View.VISIBLE);
        }


        Moji.mojiApi.getEmojiWallData3pk().enqueue(new SmallCB<Map<String, List<MojiModel>>>() {
            @Override
            public void done(final retrofit2.Response<Map<String, List<MojiModel>>> wallData, @Nullable Throwable t) {
                mojimodels  = new ArrayList<Category>();
                if (t != null) {
                    t.printStackTrace();
                    return;
                }

                Moji.mojiApi.getCategories().enqueue(new SmallCB<List<Category>>() {
                    @Override
                    public void done(final retrofit2.Response<List<Category>> categories, @Nullable Throwable t) {
                        if (t != null) {
                            t.printStackTrace();
                            return;
                        }

                        for (Category c : categories.body()) {

                            if (c.name.equalsIgnoreCase("Free") || c.name.equalsIgnoreCase("GIF")) {
                            } else {
                                c.models = wallData.body().get(c.name);
                                mojimodels.add(c);


                            }
                        }

                        loadingDialog.dismiss();

                        androidGridView.setAdapter(new ImageAdaptor(getActivity(), mojimodels, "1"));

                    }
                });
            }
        });
        return view;
    }


    private void checkPaytest(final String deivceId) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://tech599.com/tech599.com/johnaks/EmojiApp/check_purchased.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        loadingDialog.dismiss();
                        Log.i("ResponsePayment", "Login= " + response);

                        try {
                            JSONObject jobj = new JSONObject(response);
                            int message_code = jobj.getInt("message_code");

                            String msg = jobj.getString("message");
                            Log.e("FLag", message_code + " :: " + msg);

                            if (message_code == 1) {
                                isPay = true;

                                ll_lockcategory.setVisibility(View.GONE);
                                ll_unlockcategory.setVisibility(View.VISIBLE);


                            } else {
                                isPay = false;
                                ll_lockcategory.setVisibility(View.VISIBLE);
                                ll_unlockcategory.setVisibility(View.GONE);
                                if (isPayment.equalsIgnoreCase("") || isPayment == null) {
                                    ll_lockcategory.setVisibility(View.VISIBLE);
                                    ll_unlockcategory.setVisibility(View.GONE);
                                } else {
                                    ll_lockcategory.setVisibility(View.GONE);
                                    ll_unlockcategory.setVisibility(View.VISIBLE);
                                }


                            }
                        } catch (JSONException e) {
                            System.out.println("jsonexeption" + e.toString());
                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                       /* String reason = AppUtils.getVolleyError(PaymentActivity.this, error);
                        AlertUtility.showAlert(PaymentActivity.this, reason);*/
                        System.out.println("jsonexeption" + error.toString());
                    }
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                try {

                    params.put("device_id", android.os.Build.ID);
                    //    params.put("cat_name", categoryName);

                } catch (Exception e) {
                    System.out.println("error" + e.toString());
                }
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        stringRequest.setShouldCache(false);
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        requestQueue.add(stringRequest);


    }


}
