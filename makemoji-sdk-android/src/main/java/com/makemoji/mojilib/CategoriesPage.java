package com.makemoji.mojilib;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.makemoji.mojilib.model.Category;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;


/**
 * Created by Scott Baar on 1/10/2016.
 */
public class CategoriesPage extends MakeMojiPage implements CategoriesAdapter.ICatListener {
    RecyclerView rv;
    GridLayoutManager glm;
    MojiApi api;
    CategoriesAdapter adapter;
    Context context;
    String deivceId;
    String flag;
    MojiInputLayout mojiInputLayout;
    SharedPreferences prefUser;

    public CategoriesPage(ViewStub stub, MojiApi mojiApi, final MojiInputLayout mojiInputLayout) {
        super(stub, mojiInputLayout);
        this.mojiInputLayout = mojiInputLayout;
        api = mojiApi;
        deivceId = Moji.getUserId();
        prefUser = mojiInputLayout.getContext().getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
        // checkPayPre(deivceId);
        flag = prefUser.getString("flag", null);

        adapter = new CategoriesAdapter(this, mojiInputLayout.getHeaderTextColor(), flag);

        List<Category> categories = Category.getCategories();
        adapter.setCategories(categories);
        mojiInputLayout.requestRnUpdate();
        checkPayPre(deivceId);


        if (Moji.enableUpdates)
            api.getCategories().enqueue(new SmallCB<List<Category>>() {
                @Override
                public void done(retrofit2.Response<List<Category>> response, @Nullable Throwable t) {
                    if (t != null) {
                        t.printStackTrace();
                        return;
                    }
                    Category.saveCategories(response.body());
                    adapter.setCategories(response.body());
                    mojiInputLayout.requestRnUpdate();
                }
            });
    }

    @Override
    protected void setup() {
        super.setup();
        rv = ( RecyclerView ) mView.findViewById(R.id._mm_cat_rv);
        glm = new GridLayoutManager(mView.getContext(), 5, LinearLayoutManager.VERTICAL, false);
        rv.setLayoutManager(glm);
        rv.setAdapter(adapter);
        rv.setItemAnimator(null);
        (( TextView ) mView.findViewById(R.id._mm_page_heading)).setTextColor(mMojiInput.getHeaderTextColor());

    }

    public void show() {
        super.show();
        mojiInputLayout.requestRnUpdate();

    }

    public void hide() {
        super.hide();
    }

    public void refresh() {
        if (adapter != null) adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        mojiInputLayout.requestRnUpdate();
    }

    @Override
    public void onClick(Category category) {

            /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Purchase the Astrology package")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {


                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();*/

        if (category.isLocked() && !MojiUnlock.getUnlockedGroups().contains(category.name)) {
            mMojiInput.onLockedCategoryClicked(category.name);
            return;
        }

        MakeMojiPage mmp = new VPPage(category.name, mMojiInput, new CategoryPopulator(category));
        mMojiInput.addPage(mmp);

      /*  if (category.name.equals("Free")) {

            if (category.isLocked() && !MojiUnlock.getUnlockedGroups().contains(category.name)) {
                mMojiInput.onLockedCategoryClicked(category.name);
                return;
            }

            MakeMojiPage mmp = new VPPage(category.name, mMojiInput, new CategoryPopulator(category));
            mMojiInput.addPage(mmp);


        } else if (category.name.equals("GIF")) {

            if (category.isLocked() && !MojiUnlock.getUnlockedGroups().contains(category.name)) {
                mMojiInput.onLockedCategoryClicked(category.name);
                return;
            }

            MakeMojiPage mmp = new VPPage(category.name, mMojiInput, new CategoryPopulator(category));
            mMojiInput.addPage(mmp);

        } else if (category.name.equals("Astrology")) {
            checkPay(deivceId, category);
        } else if (category.name.equals("Mythology")) {
            checkPay(deivceId, category);
        } else if (category.name.equals("Wellness")) {
            checkPay(deivceId, category);
        } else if (category.name.equals("GIF")) {
            checkPay(deivceId, category);
        } else if (category.name.equals("Astronomy")) {
            checkPay(deivceId, category);
        } else if (category.name.equals("Happy Birthday")) {
            checkPay(deivceId, category);
        }
*/
    }


    private void checkPay(final String deivceId, final Category category) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://tech599.com/tech599.com/johnaks/EmojiApp/check_purchased.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.i("ResponsePayment", "Login= " + response);

                        try {
                            JSONObject jobj = new JSONObject(response);
                            int message_code = jobj.getInt("message_code");

                            String msg = jobj.getString("message");
                            Log.e("FLag", message_code + " :: " + msg);

                            if (message_code == 1) {
                                if (category.isLocked() && !MojiUnlock.getUnlockedGroups().contains(category.name)) {
                                    mMojiInput.onLockedCategoryClicked(category.name);
                                    return;
                                }

                                MakeMojiPage mmp = new VPPage(category.name, mMojiInput, new CategoryPopulator(category));
                                mMojiInput.addPage(mmp);

                            } else {
                                Toast.makeText(mojiInputLayout.getContext(), msg, Toast.LENGTH_LONG).show();

                               /* Intent intent = new Intent(mojiInputLayout.getContext(), inappNew.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("categoryName", category.name);
                                intent.putExtra("device_id", deivceId);
                                mojiInputLayout.getContext().startActivity(intent);*/

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
        RequestQueue requestQueue = Volley.newRequestQueue(mojiInputLayout.getContext());
        requestQueue.add(stringRequest);


    }

    private void checkPayPre(final String deivceId) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://tech599.com/tech599.com/johnaks/EmojiApp/check_purchased.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.i("ResponsePayment", "Login= " + response);

                        try {
                            JSONObject jobj = new JSONObject(response);
                            int message_code = jobj.getInt("message_code");

                            String msg = jobj.getString("message");
                            Log.e("FLag", message_code + " :: " + msg);

                            if (message_code == 1) {
                                flag = "1";


                            } else {
                                Toast.makeText(mojiInputLayout.getContext(), msg, Toast.LENGTH_LONG).show();

                                flag = "";


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
        RequestQueue requestQueue = Volley.newRequestQueue(mojiInputLayout.getContext());
        requestQueue.add(stringRequest);


    }

}
