package com.makemoji.sbaar.alpha;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.makemoji.mojilib.HyperMojiListener;
import com.makemoji.mojilib.IMojiSelected;
import com.makemoji.mojilib.KBCategory;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiEditText;
import com.makemoji.mojilib.MojiInputLayout;
import com.makemoji.mojilib.MojiUnlock;
import com.makemoji.mojilib.model.MojiModel;
import com.makemoji.mojilib.wall.MojiWallActivity;
import com.makemoji.sbaar.alpha.fragment.CategoryEmojiFragment;
import com.makemoji.sbaar.alpha.fragment.FreeEmojiFragment;
import com.makemoji.sbaar.alpha.fragment.GIFEmojiFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;

public class InputActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    MojiEditText outsideMojiEdit;
    MojiInputLayout mojiInputLayout;
    boolean plainTextConversion = false;
    RecyclerView rv;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    ViewPagerAdapter adapter;
    int position;
    Toolbar toolbar;
    Button button_enable;
    public static final String TAG = "InputActivity";
    SharedPreferences prefUser;
    SharedPreferences.Editor editorUser;
    Dialog dialog;
    public static final String URL = "https://docs.google.com/forms/d/e/1FAIpQLSc18GHbaP-_G_H0U_WZ001XwcuBESkW-9iPwZWdn7rkyJDkmA/formResponse";
    String flag;
    Button button_astrology, button_Wellness, button_wholeapppurchase;
    //input element ids found from the live form page
    public static final String EMAIL_KEY = "entry.104466926";
    public static final MediaType FORM_DATA_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    ImageView iv_back, iv_setting;
    String deivceId;
    Button button_setting;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_input);
        boolean isInputDeviceEnabled = false;
        dialog = new Dialog(InputActivity.this);
        String packageLocal = getPackageName();
        InputMethodManager inputMethodManager = ( InputMethodManager ) getSystemService(INPUT_METHOD_SERVICE);
        List<InputMethodInfo> list = inputMethodManager.getEnabledInputMethodList();
        String packageName = null;
        // check if our keyboard is enabled as input method
        for (InputMethodInfo inputMethod : list) {
            packageName = inputMethod.getPackageName();
            if (packageName.equals(packageLocal)) {
                isInputDeviceEnabled = true;
            }
        }
        if (isInputDeviceEnabled == true) {
            Toast.makeText(getApplicationContext(), "Your Keyboard Enable", Toast.LENGTH_SHORT).show();
        } else {
            dialogsetting();

        }
        deivceId = Moji.getUserId();
        Toolbar toolbar = ( Toolbar ) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        iv_back = toolbar.findViewById(R.id.iv_back);
        iv_setting = toolbar.findViewById(R.id.iv_setting);
        prefUser = getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
        editorUser = prefUser.edit();
        button_astrology = findViewById(R.id.button_free);
        button_Wellness = findViewById(R.id.button_Wellness);
        button_wholeapppurchase = findViewById(R.id.button_wholeapppurchase);
        button_setting = findViewById(R.id.button_setting);
        KBCategory.categoryDrawables.put("Sports", R.drawable.custom_kb_tab);
        mojiInputLayout = ( MojiInputLayout ) findViewById(R.id.mojiInput);
        viewPager = findViewById(R.id.viewpager);
        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        setupViewPager(viewPager);
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        tabLayout.setOnTabSelectedListener(this);

        button_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
        iv_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent enableIntent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(enableIntent);
            }
        });
        rv = ( RecyclerView ) findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        final RvAdapter rvAdapter = new RvAdapter();
        rv.setAdapter(rvAdapter);
        outsideMojiEdit = ( MojiEditText ) findViewById(R.id.outside_met);
        mojiInputLayout.setSendLayoutClickListener(new MojiInputLayout.SendClickListener() {
            @Override
            public boolean onClick(final String html, Spanned spanned) {
                MojiMessage mojiMessage = new MojiMessage(html);
                rvAdapter.addItem(mojiMessage);

                if (plainTextConversion) {//not needed usually, only to facilitate sharing to 3rd party places legibly
                    String plainText = Moji.htmlToPlainText(html);
                    Log.d(TAG, "plain text " + plainText);//must convert to html to show new lines
                    MojiMessage message2 = new MojiMessage(null);
                    message2.plainText = plainText;
                    rvAdapter.addItem(message2);
                }

                return true;
            }
        });
        mojiInputLayout.setCameraButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(InputActivity.this, "camera clicked", Toast.LENGTH_SHORT).show();
            }
        });
        mojiInputLayout.setHyperMojiClickListener(new HyperMojiListener() {
            @Override
            public void onClick(String url) {
                Toast.makeText(InputActivity.this, "hypermoji clicked from input activity", Toast.LENGTH_SHORT).show();
            }
        });
        mojiInputLayout.setLockedCategoryClicked(new MojiUnlock.ILockedCategoryClicked() {
            @Override
            public void lockedCategoryClick(String name) {
                InputActivity.this.lockedCategoryClick(name);
            }
        });
        mojiInputLayout.setInputConnectionCreator(new MojiEditText.MakemojiAwareConnectionCreator(mojiInputLayout));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_attach) {
            mojiInputLayout.attatchMojiEditText(outsideMojiEdit);
            outsideMojiEdit.setVisibility(View.VISIBLE);
            outsideMojiEdit.requestFocus();
            return true;
        } else if (id == R.id.action_detach) {
            mojiInputLayout.manualSaveInputToRecentsAndBackend();//send text for analytics, add emojis to recent.
            mojiInputLayout.detachMojiEditText();
            outsideMojiEdit.setVisibility(View.GONE);
            return true;
        } else if (id == R.id.action_plain_conversion) {
            plainTextConversion = !plainTextConversion;
            //mojiInputLayout.openCategoriesPage();
        } else if (id == R.id.action_kb_activate) {
            startActivity(new Intent(this, ActivateActivity.class));
        } else if (id == R.id.action_emoji_wall_activity) {
            Intent intent = new Intent(this, MojiWallActivity.class);
            //intent.putExtra(MojiWallActivity.EXTRA_THEME,R.style.MojiWallDefaultStyle_Light); //to theme it
            intent.putExtra(MojiWallActivity.EXTRA_SHOWRECENT, true);
            intent.putExtra(MojiWallActivity.EXTRA_SHOWUNICODE, true);
            startActivityForResult(intent, IMojiSelected.REQUEST_MOJI_MODEL);
        } else if (id == R.id.action_clear_unlocks) {
            MojiUnlock.clearGroups();
            mojiInputLayout.refreshCategories();
        } else if (id == R.id.action_reactions_activity) {
            startActivity(new Intent(this, ReactionsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (mojiInputLayout.handleIntent(i))
            return;
        if (Moji.ACTION_LOCKED_CATEGORY_CLICK.equals(i.getAction())) {
            lockedCategoryClick(i.getStringExtra(Moji.EXTRA_CATEGORY_NAME));
        }
    }

    public void lockedCategoryClick(String name) {
        MojiUnlock.unlockCategory(name);
        mojiInputLayout.refreshCategories();

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //mojiInputLayout.handleIntent(data);
        //OR
        if (requestCode == IMojiSelected.REQUEST_MOJI_MODEL && resultCode == RESULT_OK) {
            try {
                String json = data.getStringExtra(Moji.EXTRA_JSON);
                MojiModel model = MojiModel.fromJson(new JSONObject(json));
                mojiInputLayout.mojiSelected(model, null);
                Log.e("jsonString", json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {

        System.runFinalizersOnExit(true);
        //  Toast.makeText(this, "Test", Toast.LENGTH_SHORT).show();

        if (mojiInputLayout.canHandleBack()) {
            mojiInputLayout.onBackPressed();
            return;
        }
        super.onBackPressed();


    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        mojiInputLayout.onMultiWindowModeChanged(isInMultiWindowMode);
    }


    private void checkPayPre() {
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
                                prefUser = getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
                                editorUser = prefUser.edit();
                                editorUser.putString("flag", "1");
                                editorUser.apply();

                            } else {

                                prefUser = getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
                                editorUser = prefUser.edit();
                                editorUser.putString("flag", "0");
                                editorUser.apply();


                            }
                        } catch (JSONException e) {
                            System.out.println("jsonexeption" + e.toString());
                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
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
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);

    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition());
        position = tab.getPosition();
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();
        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }
        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }
        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }

    private void setupViewPager(ViewPager viewPager) {

        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FreeEmojiFragment(), "FREE");
        adapter.addFragment(new GIFEmojiFragment(), "GIF");
        adapter.addFragment(new CategoryEmojiFragment(), "CATEGORY");

        viewPager.setAdapter(adapter);

    }


    public void dialogsetting() {

        dialog.setContentView(R.layout.dialog_setting);
        Button btn_enable = dialog.findViewById(R.id.btn_enable);
        Button btn_close = dialog.findViewById(R.id.btn_close);
        btn_enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent enableIntent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(enableIntent);
            }
        });
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(true);
        dialog.show();
    }

    @Override
    protected void onResume() {

        super.onResume();

        boolean isInputDeviceEnabled = false;
        String packageLocal = getPackageName();
        InputMethodManager inputMethodManager = ( InputMethodManager ) getSystemService(INPUT_METHOD_SERVICE);
        List<InputMethodInfo> list = inputMethodManager.getEnabledInputMethodList();
        String packageName = null;
        // check if our keyboard is enabled as input method
        for (InputMethodInfo inputMethod : list) {

            packageName = inputMethod.getPackageName();
            if (packageName.equals(packageLocal)) {
                isInputDeviceEnabled = true;
            }

        }
        if (isInputDeviceEnabled == true) {

            dialog.dismiss();
            Toast.makeText(getApplicationContext(), "Your Keyboard Enable", Toast.LENGTH_SHORT).show();
        } else {
            dialogsetting();

        }
    }
}
