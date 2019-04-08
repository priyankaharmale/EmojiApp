/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.makemoji.sbaar.alpha;
/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonObject;
import com.makemoji.sbaar.alpha.util.IabBroadcastReceiver;
import com.makemoji.sbaar.alpha.util.IabHelper;
import com.makemoji.sbaar.alpha.util.IabResult;
import com.makemoji.sbaar.alpha.util.Inventory;
import com.makemoji.sbaar.alpha.util.Purchase;
import com.makemoji.sbaar.alpha.util.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class inappNew extends Activity implements IabBroadcastReceiver.IabBroadcastListener,
        DialogInterface.OnClickListener {

    boolean mIsPremium = false;
    String mInfiniteGasSku = "";
    String mFirstChoiceSku = "";
    String mSecondChoiceSku = "";
    String mSelectedSubscriptionPeriod = "";
    static final String SKU_PLAN1 = "allcategory";
    static final int RC_REQUEST = 10001;
    static final int TANK_MAX = 4;
    int mTank;
    String orderId;
    // The helper object
    IabHelper mHelper;
    SharedPreferences prefUser;
    SharedPreferences.Editor editorUser;
    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;

    private String TAG = inappNew.class.getSimpleName();

    String device_id;
    String categoryName, isPayment;

    //////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(com.makemoji.keyboard.R.layout.inapp);
        prefUser = getApplicationContext().getSharedPreferences("AOP_PREFS", MODE_PRIVATE);
        isPayment = prefUser.getString("isPayment", "");
        // load game data
        loadData();
        Intent intent = getIntent();
        categoryName = intent.getStringExtra("categoryName");
        device_id = intent.getStringExtra("device_id");

        // String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjUKZ/1aRatTS118VTT+TKQjZ1w1vlUzdUMB0PcNcYIR40SJBZPQT7379z23830u7svD5ajWSe6VdW5sFFvr/mPFNX9QUoNIublxdW+wvnG6HO06gqRX2UQnk2f9yr4BaDgfZ+NkyPk3MZOHZ6V619qfDCZbU31xWMQgphUbP49VlH5DpiFCfrgUJf4/0Ym02r+DNNDLfKNpJHyjSFp86jCbDGGtH+062LNeIA+T4aw3T+dp73XPs9F7TO1Kwo5Dl8WRt28B3Ojygiqmxv35NrRkQ1ytplzOeblizwPbo70uMqR+Cfz74vEU7kyLUYo/9ahWEPqsgokgeBw6F39rM0wIDAQAB";

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhHseWyO7AN6T5oZT/Wp2Y0hGNO+lDnDfOSW0JnnpG8tozFstNyaNb5JrFko+yMsJQ27UO0CcTGSpsJ61L8/8RnHa2DHAW1UbBAHbYtMJSy28LYpPKu+sXE0oUR28zL8Ad5SZXbfjqe+5Dq3fKl7zlGuvRwm90DN2AwEioDWXVIDkjjiXcdkm/jNdKCunzje93dB2itVhhAmaU0b8wAUhQkoTU2DzMbD6t75MTeF3tEcTeNBPult05wjMGQsXLG2+omRuOzePWHnrvVUoxSfB+yf7kXrO1ppNR9Pv31d40BsSvPGSArFU5K6j72JJd+0cjwDr09xnOUwHXyIMCrz/vQIDAQAB";

        if (base64EncodedPublicKey.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please put your app's public key in MainActivity.java. See README.");
        }
        if (getPackageName().startsWith("com.example")) {
            throw new RuntimeException("Please change the sample's package name! See README.");
        }

//        mHelper.enableDebugLogging(true, "TAG");

        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.enableDebugLogging(true);

        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                try {


                    try {
                        String payload = "";
                        mHelper.launchPurchaseFlow(inappNew.this, SKU_PLAN1, RC_REQUEST,
                                mPurchaseFinishedListener, payload);
                      /*  if (categoryName.equals("Astrology")) {

                        } else if (categoryName.equals("Mythology")) {
                            mHelper.launchPurchaseFlow(inappNew.this, SKU_PLAN2, RC_REQUEST,
                                    mPurchaseFinishedListener, payload);
                        } else if (categoryName.equals("Wellness")) {
                            mHelper.launchPurchaseFlow(inappNew.this, SKU_PLAN3, RC_REQUEST,
                                    mPurchaseFinishedListener, payload);
                        } else if (categoryName.equals("GIF")) {
                            mHelper.launchPurchaseFlow(inappNew.this, SKU_PLAN4, RC_REQUEST,
                                    mPurchaseFinishedListener, payload);
                        } else if (categoryName.equals("Astronomy")) {
                            mHelper.launchPurchaseFlow(inappNew.this, SKU_PLAN5, RC_REQUEST,
                                    mPurchaseFinishedListener, payload);
                        } else if (categoryName.equals("Happy Birthday")) {
                            mHelper.launchPurchaseFlow(inappNew.this, SKU_PLAN6, RC_REQUEST,
                                    mPurchaseFinishedListener, payload);
                        }*/
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        complain("Service Not Available , Please Try Again Later..");
                        //setWaitScreen(false);
                    }

                } catch (Exception ex) {
                    Log.e("", "Error(btn100_onClick):" + ex.toString());
                }

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    //  complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;
                mBroadcastReceiver = new IabBroadcastReceiver(inappNew.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    // complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
        consumeProducts();
    }

    private void consumeProducts() {


    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");


/*
            if (inventory.hasPurchase(SKU_PLAN1)) {

                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_PLAN1), null);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }*/


            if (inventory == null) {

                updateUi();
            } else {
                if (mHelper == null) return;

                // Is it a failure?
                if (result.isFailure()) {
                    if (result.equals("IabResult: Error refreshing inventory (querying owned items). (response: -1003:Purchase signature verification failed)"))
                        // complain("Failed to query inventory: " + result);

                        return;
                }

                Log.d(TAG, "Query inventory was successful.");


                // Do we have the premium upgrade?
                Purchase premiumPurchase = null;
                premiumPurchase = inventory.getPurchase(SKU_PLAN1);


                mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
                Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));

                Purchase gasPurchase = null;
                gasPurchase = inventory.getPurchase(SKU_PLAN1);

                if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {
                    Log.d(TAG, "We have gas. Consuming it.");
                    try {
                        mHelper.consumeAsync(inventory.getPurchase(SKU_PLAN1), new IabHelper.OnConsumeFinishedListener() {
                            @Override
                            public void onConsumeFinished(Purchase purchase, IabResult result) {
                                Log.d("InApp Purchase", "onConsumeFinished= " + result.getMessage());
                            }
                        });
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        //  complain("Error consuming gas. Another async operation in progress.");
                        Log.d("InApp Purchase", "IabAsyncInProgressException");
                    }
                    return;
                }

                //   updateUi();
                //   setWaitScreen(false);
                Log.d(TAG, "Initial inventory query finished; enabling main UI.");
            }


            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            //complain("Error querying inventory. Another async operation in progress.");
        }
    }

  /*  public void btnBack_onClick(View view) {
        try {
            inapp.this.onBackPressed();
        } catch (Exception ex) {
            Log.e(AppConstant.TAG, "Error(btnBack_onClick):" + ex.toString());
            Toast.makeText(this, "Error:" + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }*/

    public void btnFree_onClick(View view) {
        try {

            // inapp.this.onBackPressed();
            finish();

            if (mBroadcastReceiver != null) {
                unregisterReceiver(mBroadcastReceiver);
            }

            // very important:
            Log.d(TAG, "Destroying helper.");
            if (mHelper != null) {
                mHelper.disposeWhenFinished();
                mHelper = null;
            }
        } catch (Exception ex) {
            Log.e("", "Error(btnFree_onClick):" + ex.toString());
        }
    }

    /* // User clicked the "Buy Gas" button
     public void onBuyGasButtonClicked(View arg0) {
         Log.d(TAG, "Buy gas button clicked.");

         try {

             String payload = "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ";

             try {
                 // mHelper.launchPurchaseFlow(this, SKU_PLAN2, RC_REQUEST,
                 //          mPurchaseFinishedListener, payload);

                 mHelper.launchPurchaseFlow(this, SKU_PLAN2, RC_REQUEST,
                         mPurchaseFinishedListener, payload);

             } catch (IabHelper.IabAsyncInProgressException e) {
                 complain("Service Not Available , Please Try Again Later..");
                 //  setWaitScreen(false);
             }
             // Do_Transaction(new BigDecimal("0.99"), "40 Puzzles", REQUEST_CODE_PAYMENT_40);

         } catch (Exception ex) {
             Log.e(AppConstant.TAG, "Error(btn40_onClick):" + ex.toString());
         }

        *//* if (mSubscribedToInfiniteGas) {
            complain("No need! You're subscribed to infinite gas. Isn't that awesome?");
            return;
        }

        if (mTank >= TANK_MAX) {
            complain("Your tank is full. Drive around a bit!");
            return;
        }*//*

        // launch the gas purchase UI flow.
        // We will be notified of completion via mPurchaseFinishedListener
        // setWaitScreen(true);
        Log.d(TAG, "Launching purchase flow for gas.");

        *//* TODO: for security, generate your payload here for verification. See the comments on
     *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
     *        an empty string, but on a production app you should carefully generate this. *//*

    }
*/
    // User clicked the "Upgrade to Premium" button.
    public void onUpgradeAppButtonClicked(View arg0) {
        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
        //  setWaitScreen(true);
        try {


            try {
                String payload = "";
                mHelper.launchPurchaseFlow(this, SKU_PLAN1, RC_REQUEST,
                        mPurchaseFinishedListener, payload);
               /* if (categoryName.equals("Astrology")) {
                    mHelper.launchPurchaseFlow(this, SKU_PLAN1, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } else if (categoryName.equals("Mythology")) {
                    mHelper.launchPurchaseFlow(this, SKU_PLAN2, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } else if (categoryName.equals("Wellness")) {
                    mHelper.launchPurchaseFlow(this, SKU_PLAN3, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } else if (categoryName.equals("GIF")) {
                    mHelper.launchPurchaseFlow(this, SKU_PLAN4, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } else if (categoryName.equals("Astronomy")) {
                    mHelper.launchPurchaseFlow(this, SKU_PLAN5, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                } else if (categoryName.equals("Happy Birthday")) {
                    mHelper.launchPurchaseFlow(this, SKU_PLAN6, RC_REQUEST,
                            mPurchaseFinishedListener, payload);
                }
*/
            } catch (IabHelper.IabAsyncInProgressException e) {
                complain("Service Not Available , Please Try Again Later..");
                //setWaitScreen(false);
            }

        } catch (Exception ex) {
            Log.e("", "Error(btn100_onClick):" + ex.toString());
        }
        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */

    }
/*
    // "Subscribe to infinite gas" button clicked. Explain to user, then start purchase
    // flow for subscription.
    public void onInfiniteGasButtonClicked(View arg0) {
        if (!mHelper.subscriptionsSupported()) {
            complain("Subscriptions not supported on your device yet. Sorry!");
            return;
        }

        CharSequence[] options;
        if (!mSubscribedToInfiniteGas || !mAutoRenewEnabled) {
            // Both subscription options should be available
            options = new CharSequence[2];
            options[0] = getString(R.string.subscription_period_monthly);
            options[1] = getString(R.string.subscription_period_yearly);
            mFirstChoiceSku = SKU_INFINITE_GAS_MONTHLY;
            mSecondChoiceSku = SKU_INFINITE_GAS_YEARLY;
        } else {
            // This is the subscription upgrade/downgrade path, so only one option is valid
            options = new CharSequence[1];
            if (mInfiniteGasSku.equals(SKU_INFINITE_GAS_MONTHLY)) {
                // Give the option to upgrade to yearly
                options[0] = getString(R.string.subscription_period_yearly);
                mFirstChoiceSku = SKU_INFINITE_GAS_YEARLY;
            } else {
                // Give the option to downgrade to monthly
                options[0] = getString(R.string.subscription_period_monthly);
                mFirstChoiceSku = SKU_INFINITE_GAS_MONTHLY;
            }
            mSecondChoiceSku = "";
        }

        int titleResId;
        if (!mSubscribedToInfiniteGas) {
            titleResId = R.string.subscription_period_prompt;
        } else if (!mAutoRenewEnabled) {
            titleResId = R.string.subscription_resignup_prompt;
        } else {
            titleResId = R.string.subscription_update_prompt;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleResId)
                .setSingleChoiceItems(options, 0 *//* checkedItem *//*, this)
                .setPositiveButton(R.string.subscription_prompt_continue, this)
                .setNegativeButton(R.string.subscription_prompt_cancel, this);
        AlertDialog dialog = builder.create();
        dialog.show();
    }*/

    @Override
    public void onClick(DialogInterface dialog, int id) {
        if (id == 0 /* First choice item */) {
            mSelectedSubscriptionPeriod = mFirstChoiceSku;
        } else if (id == 1 /* Second choice item */) {
            mSelectedSubscriptionPeriod = mSecondChoiceSku;
        } else if (id == DialogInterface.BUTTON_POSITIVE /* continue button */) {
            /* TODO: for security, generate your payload here for verification. See the comments on
             *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
             *        an empty string, but on a production app you should carefully generate
             *        this. */
            String payload = "";

            if (TextUtils.isEmpty(mSelectedSubscriptionPeriod)) {
                // The user has not changed from the default selection
                mSelectedSubscriptionPeriod = mFirstChoiceSku;
            }

            List<String> oldSkus = null;
            if (!TextUtils.isEmpty(mInfiniteGasSku)
                    && !mInfiniteGasSku.equals(mSelectedSubscriptionPeriod)) {
                // The user currently has a valid subscription, any purchase action is going to
                // replace that subscription
                oldSkus = new ArrayList<String>();
                oldSkus.add(mInfiniteGasSku);
            }

            //  setWaitScreen(true);
            Log.d(TAG, "Launching purchase flow for gas subscription.");
            try {
                mHelper.launchPurchaseFlow(this, mSelectedSubscriptionPeriod, IabHelper.ITEM_TYPE_SUBS,
                        oldSkus, RC_REQUEST, mPurchaseFinishedListener, payload);
            } catch (IabHelper.IabAsyncInProgressException e) {
                complain("Service Not Available , Please Try Again Later..");
                //    setWaitScreen(false);
            }
            // Reset the dialog options
            mSelectedSubscriptionPeriod = "";
            mFirstChoiceSku = "";
            mSecondChoiceSku = "";
        } else if (id != DialogInterface.BUTTON_NEGATIVE) {
            // There are only four buttons, this should not happen
            Log.e(TAG, "Unknown button clicked in subscription dialog: " + id);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_REQUEST) {


            if (resultCode == RESULT_OK) {
                try {
                    int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
                    String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
                    String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
                    Log.d("InAPPPrchase", "dataSignature= " + dataSignature);
                    Log.d("InAPPPrchase", "purchaseData" + purchaseData);
                    Log.d("InAPPPrchase", "responseCode" + String.valueOf(responseCode));

                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    System.out.println("sku" + sku);
                    String Order_id = jo.getString("orderId");
                    //    AppConstant.payment_txn_id=Order_id;
                    //      Toast.makeText(getApplicationContext(),"sku"+sku,Toast.LENGTH_LONG).show();
                    System.out.println("inaapprepose" + sku);

                    addPaymentInfo(Order_id);

                } catch (JSONException e) {
                    alert("Failed to parse purchase data.");
                    e.printStackTrace();
                }
            }
        }
    }


    /* @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
         if (mHelper == null) return;

         // Pass on the activity result to the helper for handling
         if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
             // not handled, so handle it ourselves (here's where you'd
             // perform any handling of activity results not related to in-app
             // billing...
             super.onActivityResult(requestCode, resultCode, data);
         }
         else {
             Log.d(TAG, "onActivityResult handled by IABUtil.");
         }
     }

     /**
      * Verifies the developer payload of a purchase.
      */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);


            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                //  complain("" + result);

                if (result.toString().contains("Item Already Owned")) {
                    AlertDialog.Builder bld = new AlertDialog.Builder(inappNew.this);
                    bld.setMessage("Item Already Purchased using this email address");
                    bld.setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String resultsd = IabHelper.purchaseData;
                                    Log.d("resultsd", resultsd);

                                    try {
                                        JSONObject j = new JSONObject(resultsd);
                                        orderId = j.getString("orderId");
                                        Log.d("priya", orderId);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Log.d("JSONException", e.toString());

                                    }
                                    updateDeviceId(orderId);

                                }
                            });
                    Log.d(TAG, "Showing alert dialog: " + result);
                    bld.create().show();
                } else {
                    complain("" + result);
                }

                //   setWaitScreen(false);
                return;
            }
            if (!

                    verifyDeveloperPayload(purchase))

            {
                // complain(" Authenticity verification failed.");
                //  setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");
            if (purchase.getSku().

                    equals(SKU_PLAN1))

            {

                Utilities.showAlertDailog(inappNew.this, "Emojiapp", "Payment Successfully Done.", "Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        }, false);


            }

        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
              /*  Log.d(TAG, "Consumption successful. Provisioning.");
                mTank = mTank == TANK_MAX ? TANK_MAX : mTank + 1;
                saveData();
                alert("You filled 1/4 tank. Your tank is now " + String.valueOf(mTank) + "/4 full!");*/

                Utilities.showAlertDailog(inappNew.this, "EmojiApp", "Payment Successfully Done.", "Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        }, false);
            } else {
                //complain("Error while consuming: " + result);
            }
            // updateUi();
            // setWaitScreen(false);
            Log.d(TAG, "End consumption flow.");
        }
    };
/*
    // Drive button clicked. Burn gas!
    public void onDriveButtonClicked(View arg0) {
        Log.d(TAG, "Drive button clicked.");
        if (!mSubscribedToInfiniteGas && mTank <= 0) alert("Oh, no! You are out of gas! Try buying some!");
        else {
            if (!mSubscribedToInfiniteGas) --mTank;
            saveData();
            alert("Vroooom, you drove a few miles.");
           // updateUi();
            Log.d(TAG, "Vrooom. Tank is now " + mTank);
        }
    }*/

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        if (mBroadcastReceiver != null) {
            try {
                unregisterReceiver(mBroadcastReceiver);
            } catch (Exception e) {

            }
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    // updates UI to reflect model
    public void updateUi() {
        // update the car color to reflect premium status or lack thereof
        editorUser = prefUser.edit();
        editorUser.putString("isPayment", "done");
        editorUser.commit();

    }

// Enables or disables the "please wait" screen.
   /* void setWaitScreen(boolean set) {
        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }*/

/////////////////////////////////apter in app purchase success////////////////


    private void addPaymentInfo(final String order_id) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://tech599.com/tech599.com/johnaks/EmojiApp/insert_payment.php",
                new Response.Listener<String>() {

                    public void onResponse(String response) {
                        System.out.println("respnsePaymnet= " + response);
                        try {
                            JSONObject j = new JSONObject(response);

                            System.out.println("resArsh" + response.toString() + response.toString());

                            String flag = j.getString("flag");
                            if (flag.equals("1")) {
                                String res = j.getString("response");
                                AlertDialog.Builder builder = new AlertDialog.Builder(inappNew.this);
                                builder.setMessage(res)
                                        .setCancelable(false)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                             /*   Intent intent = new Intent(inapp.this, GetCreditsActivity.class);
                                                startActivity(intent);
                                                finish();*/
                                                onBackPressed();
                                                dialog.dismiss();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            } else {
                                String res = j.getString("response");
                                AlertDialog.Builder builder = new AlertDialog.Builder(inappNew.this);
                                builder.setMessage(res)
                                        .setCancelable(false)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            }

                        } catch (JSONException e) {
                            System.out.println("jsonexeption1111" + e.toString());
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                        System.out.println("jsonexeption" + error.toString());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                try {
                    //selected_views_count
                    params.put("device_id", android.os.Build.ID);
                    params.put("amount", "$ 2.99");
                    params.put("device_type", "Android");
                    params.put("txn_id", order_id);
                    params.put("payment_dt", "");
                    //  params.put("cat_name", categoryName);

                   /* if (categoryName.equals("Astrology")) {
                        params.put("amount", "$ 1.99");
                    } else if (categoryName.equals("Mythology")) {
                        params.put("amount", "$ 1.99");
                    } else if (categoryName.equals("Wellness")) {
                        params.put("amount", "$ 1.99");
                    } else if (categoryName.equals("GIF")) {
                        params.put("amount", "$ 1.99");
                    }*/


                } catch (Exception e) {
                    System.out.println("arsherror" + e.toString());
                }


                System.out.println("myparams" + params);
                return params;
            }

        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }


    private void updateDeviceId(final String order_id) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://tech599.com/tech599.com/johnaks/EmojiApp/update.php",
                new Response.Listener<String>() {

                    public void onResponse(String response) {
                        System.out.println("respnsePaymnet= " + response);
                        try {
                            JSONObject j = new JSONObject(response);

                            System.out.println("priya" + response.toString() + response.toString());

                            int message_code = j.getInt("message_code");
                            if (message_code == 1) {
                                String res = j.getString("message");
                                AlertDialog.Builder builder = new AlertDialog.Builder(inappNew.this);
                                builder.setMessage(res)
                                        .setCancelable(false)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                Intent intent = new Intent(inappNew.this, InputActivity.class);
                                                startActivity(intent);
                                                finish();
                                                dialog.dismiss();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            } else {
                                String res = j.getString("message");
                                AlertDialog.Builder builder = new AlertDialog.Builder(inappNew.this);
                                builder.setMessage(res)
                                        .setCancelable(false)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            }

                        } catch (JSONException e) {
                            System.out.println("jsonexeption1111" + e.toString());
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                        System.out.println("jsonexeption" + error.toString());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                try {
                    //selected_views_count
                    params.put("device_id", android.os.Build.ID);
                    params.put("txn_id", order_id);


                } catch (Exception e) {
                    System.out.println("arsherror" + e.toString());
                }


                System.out.println("myparams" + params);
                return params;
            }

        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }


    void complain(String message) {
        Log.e(TAG, "" + message);
        alert("" + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                        finish();
                    }
                });
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    /* @Override
     public void onClick(DialogInterface dialog, int which) {
         // TODO Auto-generated method stub
         switch(which){
             case DialogInterface.BUTTON_POSITIVE: // yes

                 break;
             case DialogInterface.BUTTON_NEGATIVE: // no

                 break;
             case DialogInterface.BUTTON_NEUTRAL: // neutral

                 break;
             default:
                 // nothing
                 break;
         }
     }*/
    void saveData() {

        /*
         * WARNING: on a real application, we recommend you save data in a secure way to
         * prevent tampering. For simplicity in this sample, we simply store the data using a
         * SharedPreferences.
         */

        SharedPreferences.Editor spe = getPreferences(MODE_PRIVATE).edit();
        spe.putInt("tank", mTank);
        spe.apply();
        Log.d(TAG, "Saved data: tank = " + String.valueOf(mTank));
    }

    void loadData() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        mTank = sp.getInt("tank", 2);
        Log.d(TAG, "Loaded data: tank = " + String.valueOf(mTank));
    }


}
