package com.makemoji.keyboard;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v13.view.inputmethod.EditorInfoCompat;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.makemoji.mojilib.BackSpaceDelegate;
import com.makemoji.mojilib.CategoryPopulator;
import com.makemoji.mojilib.HorizRVAdapter;
import com.makemoji.mojilib.IMojiSelected;
import com.makemoji.mojilib.KBCategory;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiGridAdapter;
import com.makemoji.mojilib.MojiInputLayout;
import com.makemoji.mojilib.MojiUnlock;
import com.makemoji.mojilib.PagerPopulator;
import com.makemoji.mojilib.RecentPopulator;
import com.makemoji.mojilib.SearchPopulator;
import com.makemoji.mojilib.SmallCB;
import com.makemoji.mojilib.SpacesItemDecoration;
import com.makemoji.mojilib.Spanimator;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;
import com.makemoji.mojilib.model.SpaceMojiModel;
import com.squareup.picasso252.Picasso;
import com.squareup.picasso252.RequestCreator;
import com.squareup.picasso252.Target;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import okhttp3.Call;
import okhttp3.Callback;


public class MMKB extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener, TabLayout.OnTabSelectedListener, IMojiSelected,
        PagerPopulator.PopulatorObserver, KBCategory.KBTAbListener, MojiUnlock.ICategoryUnlock {


    public interface ICategorySelected {
        void categorySelected(String category, boolean locked, FrameLayout parent);
    }

    public interface IKBCustomizer {
        int getRows(String category);

        int getCols(String category);
    }

    static ICategorySelected categorySelected;
    static IKBCustomizer kbCustomizer;
    //the apps and input connections in this set that support the mime type "makemoji/*" will use the private action api, not send an intent.
    //by default, it only contains the package name of the app containing this keyboard
    public static Set<String> makemojiInputConnectionWhitelist = new HashSet<>();
    //apps here will not use the app compat image keyboard support, even when they advertise their support. Use this to circumvent buggy apps.
    public static Set<String> imageKBBlacklist = new HashSet<>();


    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;
    private InputMethodManager mInputMethodManager;
    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;

    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;
    String categoryName;

    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    RecyclerView horizRv;

    private LatinKeyboard mCurKeyboard;

    private String mWordSeparators;
    FrameLayout inputView;
    String packageName;
    TabLayout tabLayout;
    RecyclerView rv;
    boolean isPay = false;
    GridLayoutManager glm;
    RecyclerView.ItemDecoration itemDecoration;
    PagerPopulator<MojiModel> populator;
    List<MojiModel> allModels = new ArrayList<>();
    List<MojiModel> allModelsNew = new ArrayList<>();
    List<MojiModel> allModelsNew2 = new ArrayList<>();

    SmoothProgressBar spb;
    int mojisPerPage;
    MojiGridAdapter adapter;
    TextSwitcher heading, kb_page_headingsubcategory;
    TextView shareText;
    View pageFrame;
    static CharSequence shareMessage;
    int rows, cols, gifRows, videoRows;
    CategoryPopulator trendingPopulator;
    SearchPopulator searchPopulator;
    boolean useTrending = true;
    View kbBottomNav;
    String category;
    Set<String> lockedCategories = new HashSet<>();

    static int forceDimen;
    public static WeakReference<MMKB> instance;
    static boolean showLockedEmojis = true;


    private int previousTotal = 0;
    private boolean loading = true;
    private int visibleThreshold = 5;
    int firstVisibleItem, visibleItemCount, totalItemCount;

    //show emojis
    public static void showLockedEmojis(boolean show) {
        showLockedEmojis = show;
    }

    public static void forceSizeDp(@Dimension(unit = Dimension.DP) int dimen) {
        forceDimen = dimen;
    }

    private Context contextForDialog = null;

    String deivceId;

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mInputMethodManager = ( InputMethodManager ) getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
        rows = Moji.context.getResources().getInteger(R.integer.mm_3pk_rows);
        gifRows = Moji.context.getResources().getInteger(R.integer.mm_3pk_gif_rows);
        videoRows = Moji.context.getResources().getInteger(R.integer._mm_video_rows);
        cols = Moji.context.getResources().getInteger(R.integer.mm_3pk_cols);
        instance = new WeakReference<>(this);
        contextForDialog = this;
        makemojiInputConnectionWhitelist.add(getPackageName());
        deivceId = Moji.getUserId();
        checkPaytest(deivceId);
        if (kbCustomizer == null)
            kbCustomizer = new IKBCustomizer() {
                @Override
                public int getRows(String category) {
                    return Moji.context.getResources().getInteger(R.integer.mm_3pk_rows);
                }

                @Override
                public int getCols(String category) {
                    return Moji.context.getResources().getInteger(R.integer.mm_3pk_cols);
                }
            };

    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
    }


    //if this is not the sample app but is using the sample authority, don't allow it, or else it will stop other apps from installing.
    public void assertAuthorityChanged() {

        if (!"com.makemoji.sbaar.alpha".equals(getContext().getApplicationInfo().packageName)
                && "com.makemoji.keyboard.fileprovider".equals(getContext().getResources().getString(R.string._mm_provider_authority))) {
            throw new IllegalStateException("You must override _mm_provider_authority in strings.xml with a unique package name!! com.your.name.kbfileprovider ");
        }
    }

    public static void setCategoryListener(@NonNull ICategorySelected listener) {
        categorySelected = listener;
    }

    public static void setKbCustomizer(@NonNull IKBCustomizer customizer) {
        kbCustomizer = customizer;
    }

    public static void setShareMessage(CharSequence message) {
        shareMessage = message;
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {
        assertAuthorityChanged();
        inputView = ( FrameLayout ) getLayoutInflater().
                cloneInContext(new ContextThemeWrapper(getContext(), R.style.KBAppTheme)).
                inflate(R.layout.kb_layout, null);
        tabLayout = ( TabLayout ) inputView.findViewById(R.id.tabs);
        rv = ( RecyclerView ) inputView.findViewById(R.id.kb_page_grid);
        rv.setHorizontalScrollBarEnabled(false);
        glm = new GridLayoutManager(inputView.getContext(), rows, LinearLayoutManager.HORIZONTAL, false);


        rv.setLayoutManager(glm);
        heading = ( TextSwitcher ) inputView.findViewById(R.id.kb_page_heading);
        kb_page_headingsubcategory = inputView.findViewById(R.id.kb_page_headingsubcategory);
        heading.setInAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.fade_in));
        heading.setOutAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.fade_out));
        kb_page_headingsubcategory.setInAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.fade_in));
        kb_page_headingsubcategory.setOutAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.fade_out));
        shareText = ( TextView ) inputView.findViewById(R.id.share_kb_tv);
        mInputView = ( LatinKeyboardView ) inputView.findViewById(R.id._mm_kb_latin);
        pageFrame = inputView.findViewById(R.id._mm_kb_pageframe);
        spb = ( SmoothProgressBar ) inputView.findViewById(R.id._mm_spb);
        horizRv = ( RecyclerView ) inputView.findViewById(R.id._mm_recylcer_view);
        spb.progressiveStop();
        kbBottomNav = inputView.findViewById(R.id.kb_bottom_nav);

        if (shareMessage == null) shareMessage = getString(R.string._mm_kb_share_message);
        if (shareMessage != null && shareMessage.length() > 0) {
            shareText.setVisibility(View.GONE);
        }

        if (categorySelected == null) {
            categorySelected = new ICategorySelected() {
                @Override
                public void categorySelected(String category, boolean locked, FrameLayout parent) {
                    if (!locked) return;
                    Intent i = new Intent(Moji.ACTION_LOCKED_CATEGORY_CLICK);
                    i.putExtra(Moji.EXTRA_CATEGORY_NAME, category);
                    i.putExtra(Moji.EXTRA_PACKAGE_ORIGIN, Moji.context.getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.setPackage(Moji.context.getPackageName());
                    try {
                        Moji.context.startActivity(i);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }
        MojiUnlock.addListener(this);
        List<TabLayout.Tab> tabs = KBCategory.getTabs(tabLayout, this, R.layout.kb_tab);

        onNewTabs(tabs);


        trendingPopulator = new CategoryPopulator(new Category("Trending", null));
        trendingPopulator.use3pk = true;
        searchPopulator = new SearchPopulator(false);
        searchPopulator.use3pk = true;


        Runnable backSpaceRunnable = new Runnable() {
            @Override
            public void run() {
                CharSequence selected = getCurrentInputConnection().getSelectedText(InputConnection.GET_TEXT_WITH_STYLES);
                if (selected != null) {
                    getCurrentInputConnection().commitText("", 1);
                    return;
                }
                CharSequence text = getCurrentInputConnection().getTextBeforeCursor(2, InputConnection.GET_TEXT_WITH_STYLES);
                if (text == null) text = "";
                int deleteLength = 1;
                if (text.length() > 1 && (Character.isSurrogatePair(text.charAt(0), text.charAt(1)) || MojiInputLayout.isVariation(text.charAt(1))))
                    deleteLength = 2;
                getCurrentInputConnection().finishComposingText();
                getCurrentInputConnection().deleteSurroundingText(deleteLength, 0);

            }
        };
        ImageView backspace = ( ImageView ) inputView.findViewById(R.id.kb_backspace_button);
        int iconColor = getResources().getColor(R.color.mmKBIconColor);
        iconColor = Color.argb(255, Color.red(iconColor), Color.green(iconColor), Color.blue(iconColor));
        backspace.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);
        new BackSpaceDelegate(backspace, backSpaceRunnable);

        ImageView abc = ( ImageView ) inputView.findViewById(R.id.kb_abc);
        abc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = ( InputMethodManager )
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                IBinder token = getToken();
                if (Build.VERSION.SDK_INT >= 19 && token != null) {
                    if (imm.shouldOfferSwitchingToNextInputMethod(token))
                        imm.switchToNextInputMethod(token, false);
                    return;
                }
                imm.showInputMethodPicker();
            }
        });
        abc.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);
        inputView.findViewById(R.id.share_kb_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shareMessage != null) {
                    getCurrentInputConnection().finishComposingText();
                    getCurrentInputConnection().setComposingText(shareMessage, 1);
                    getCurrentInputConnection().finishComposingText();
                }
            }
        });

        mInputView.setOnKeyboardActionListener(this);
        setLatinKeyboard(mQwertyKeyboard);

        return inputView;
    }

    private void setLatinKeyboard(LatinKeyboard nextKeyboard) {
        if (Build.VERSION.SDK_INT > 18) {
            final boolean shouldSupportLanguageSwitchKey =
                    mInputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken());

            nextKeyboard.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey);
        }
        mInputView.setKeyboard(nextKeyboard);
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override
    public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */

    boolean gifSupported = false;
    boolean pngSupported = false;
    boolean mp4Supported = false;
    boolean makemojiSupported = false;

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        packageName = attribute.packageName;

        String[] mimeTypes = EditorInfoCompat.getContentMimeTypes(attribute);
        gifSupported = false;
        pngSupported = false;
        mp4Supported = false;
        makemojiSupported = false;
        if (!imageKBBlacklist.contains(attribute.packageName)) {//facebook is doing something bad. Same error happens when inserting gifs from gboard.
            for (String mimeType : mimeTypes) {
                if (ClipDescription.compareMimeTypes(mimeType, "image/gif")) {
                    gifSupported = true;
                }
                if (ClipDescription.compareMimeTypes(mimeType, "image/png")) {
                    pngSupported = true;
                }
                if (ClipDescription.compareMimeTypes(mimeType, "video/mp4")) {
                    mp4Supported = true;
                }
                if (ClipDescription.compareMimeTypes(mimeType, "makemoji/*") && makemojiInputConnectionWhitelist.contains(attribute.packageName)) {
                    makemojiSupported = true;
                }
            }
        }
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                mPredictionOn = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override
    public void onFinishInput() {
        super.onFinishInput();

        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
        Spanimator.onKbStop();
    }

    boolean firstStart = true;
    int currentTab = 0;

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        if (firstStart)
            inputView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    TabLayout.Tab tab = tabLayout.getTabAt(currentTab);
                    if (tab != null) tab.select();
                }
            }, 20);
        firstStart = false;

        setLatinKeyboard(mCurKeyboard);
        mInputView.closing();
        //      final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
//        mInputView.setSubtypeOnSpaceKey(subtype);
        Spanimator.onKbStart();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        firstStart = true;
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }

        //runnablle so getTextBeforeCursor is not null


        Moji.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                CharSequence cs = getCurrentInputConnection().getTextBeforeCursor(15, 0);
                String before = cs == null ? null : cs.toString();
                if (before == null || before.isEmpty() || before.endsWith(" ")) {
                    useTrending = true;
                    if (trendingPopulator != null) trendingPopulator.onNewDataAvailable();
                    return;
                }
                int idx = Math.max(before.lastIndexOf(' '), 0);
                String query = before.substring(idx, before.length());
                useTrending = false;
                if (searchPopulator != null) searchPopulator.search(query.trim());
            }
        }, 10);

    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;
        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() - 1);
            int composed = KeyEvent.getDeadChar(accent, c);
            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length() - 1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState() & KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf(( char ) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener
    public void onKey(int primaryCode, int[] keyCodes) {
        if (isWordSeparator(primaryCode)) {
            // Handle separator
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                setLatinKeyboard(mQwertyKeyboard);
            } else {
                setLatinKeyboard(mSymbolsKeyboard);
                mSymbolsKeyboard.setShifted(false);
            }
        } else if (primaryCode == 300) {
            kbBottomNav.setVisibility(View.VISIBLE);
            mInputView.setVisibility(View.GONE);
            pageFrame.setVisibility(View.VISIBLE);
            horizRv.setVisibility(View.GONE);
            tabLayout.getTabAt(0).select();
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {

        setCandidatesViewShown(false);
        /*
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
        */
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            CharSequence text = getCurrentInputConnection().getTextBeforeCursor(2, InputConnection.GET_TEXT_WITH_STYLES);
            if (text == null) return;
            int deleteLength = 1;
            if (text.length() > 1 && Character.isSurrogatePair(text.charAt(0), text.charAt(1)))
                deleteLength = 2;
            mComposing.delete(length - deleteLength, length);
            getCurrentInputConnection().setComposingText(mComposing, deleteLength);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            CharSequence text = getCurrentInputConnection().getTextBeforeCursor(2, InputConnection.GET_TEXT_WITH_STYLES);
            if (text != null) {
                int deleteLength = 1;
                if (text.length() > 1 && (Character.isSurrogatePair(text.charAt(0), text.charAt(1)) || MojiInputLayout.isVariation(text.charAt(1))))
                    deleteLength = 2;
                getCurrentInputConnection().deleteSurroundingText(deleteLength, 0);
            }
            //keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            setLatinKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            setLatinKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append(( char ) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            getCurrentInputConnection().finishComposingText();
            updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates();
        } else {
            commitTyped(getCurrentInputConnection());
            getCurrentInputConnection().commitText(
                    String.valueOf(( char ) primaryCode), 1);
        }
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    private void handleLanguageSwitch() {
        if (Build.VERSION.SDK_INT > 15)
            mInputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    private String getWordSeparators() {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf(( char ) code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }

    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }

    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }

    public void onPress(int primaryCode) {
    }

    public void onRelease(int primaryCode) {
    }


    boolean ignoreNextTabSelect = false;

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if (ignoreNextTabSelect) {
            ignoreNextTabSelect = false;
            return;
        }
        categoryName = tab.getContentDescription().toString();
        setHeading(tab.getContentDescription());
        getCatgoryEmoji(tab);

        if (categoryName.equals("Free")) {
            setHeading(tab.getContentDescription());

            getCatgoryEmoji(tab);
        } else if (categoryName.equals("GIF")) {
            setHeading(tab.getContentDescription());

            getCatgoryEmoji(tab);
        } else if (categoryName.equals("keyboard")) {

            getCatgoryEmoji(tab);
        } else {
            checkPay(deivceId, tab, categoryName);
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        onTabSelected(tab);
    }


    @Override
    public void onNewDataAvailable() {
        rows = kbCustomizer.getRows(category);
        cols = kbCustomizer.getCols(category);
        int h = rv.getHeight();
        int size = h / rows;
        int vSpace = (h - (size * rows)) / rows;
        int hSpace = (rv.getWidth() - (size * cols)) / (cols * 2);


        mojisPerPage = Math.max(rows * 2, cols * rows);
       /* List<MojiModel> models = allModels;
        adapter = new MojiGridAdapter(models, this, false, size);
*/

        for (MojiModel mojiModel : allModels) {
            if (mojiModel.categoryName.equalsIgnoreCase("Free") || mojiModel.categoryName.equalsIgnoreCase("GIF")) {
                allModelsNew2.add(mojiModel);
            } else {
                allModelsNew.add(mojiModel);

            }

        }


        if (isPay == false) {
            List<MojiModel> models = allModelsNew2;
            adapter = new MojiGridAdapter(models, this, false, size);

        } else {
            List<MojiModel> models = allModels;
            adapter = new MojiGridAdapter(models, this, false, size);


        }

        adapter.setImagesSizedtoSpan(getContext().getResources().getBoolean(R.bool.mmUseSpanSizeFor3pkImages));
        adapter.setEnablePulse(false);
        adapter.useKbLifecycle();
        if (itemDecoration != null) rv.removeItemDecoration(itemDecoration);
        //  if (gifs) {
        itemDecoration = new SpacesItemDecoration(vSpace, hSpace);
        rv.addItemDecoration(itemDecoration);
        //   }
        glm.setSpanCount(rows);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                MojiModel m = adapter.getMojiModels().get(position);
                if ("gifs".equalsIgnoreCase(m.categoryName)) return 2;
                switch (adapter.getItemViewType(position)) {
                    case MojiGridAdapter.ITEM_HSPACE:
                        return glm.getSpanCount();
                    case MojiGridAdapter.ITEM_VIDEO:
                        return glm.getSpanCount() / 2;
                    default:
                        return 1;
                }
            }
        });
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {


                int firstVisible = glm.findFirstCompletelyVisibleItemPosition();
                if (firstVisible != -1) {
                    MojiModel m = adapter.getMojiModels().get(firstVisible);
                    setHeading(m.categoryName);
                    rv.setNestedScrollingEnabled(false);
                    for (TabLayout.Tab tab : tabList) {

                        if (!tab.isSelected() && tab.getCustomView().getTag(R.id._makemoji_category_tag_id) != null &&
                                m.categoryName.equalsIgnoreCase((( Category ) tab.getCustomView().getTag(R.id._makemoji_category_tag_id)).name)) {
                            ignoreNextTabSelect = true;
                            tab.select();
                          /*  if (tab.getContentDescription().equals("Astrology")) {
                                rv.setNestedScrollingEnabled(false);

                            }
*/

                            break;
                        }
                    }
                }

                super.onScrolled(recyclerView, dx, dy);
            }
        });
        //if (OneGridPage.hasVideo(models)) ((GridLayoutManager) rv.getLayoutManager()).setSpanCount(videoRows);
        rv.setAdapter(adapter);


    }

    CharSequence currentHeading = null;

    void setHeading(CharSequence s) {
        if (s != null && !s.equals(currentHeading)) {
            currentHeading = s;
            heading.setText(s);
          /*  if(s.equals("Astrology"))
            {
                for(MojiModel mojiModel : allModels)
                {
                    if(mojiModel.categoryName.equalsIgnoreCase("Astrology"))
                    {
                        if(mojiModel.name.matches("Aries"))
                        {
                            kb_page_headingsubcategory.setText("Aries");
                        }else
                        {

                        }
                    }
                }

            }*/
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Spanimator.onKbStop();
        MojiUnlock.removeListener(this);
    }

    public void share(MojiModel model, File cacheFile) {
        Uri uri = MMFileProvider.getUriForFile(getContext(), getContext().getString(R.string._mm_provider_authority), cacheFile);
        String mime = getContentResolver().getType(uri);
        Moji.mojiApi.trackShare(Moji.getUserId(), String.valueOf(model.id)).enqueue(new SmallCB<Void>() {
            @Override
            public void done(retrofit2.Response<Void> response, @Nullable Throwable t) {
                if (t != null) t.printStackTrace();
            }
        });
        if ((pngSupported && cacheFile.getName().toLowerCase().endsWith(".png")) ||
                (gifSupported && cacheFile.getName().toLowerCase().endsWith(".gif"))
                || (mp4Supported && cacheFile.getName().toLowerCase().endsWith(".mp4")) || makemojiSupported) {
            InputContentInfoCompat inputContentInfo = new InputContentInfoCompat(
                    uri, new ClipDescription(model.name, new String[]{mime, "makemoji/*"}), model.link_url != null ? Uri.parse(model.link_url) : null);
            InputConnection inputConnection = getCurrentInputConnection();
            EditorInfo editorInfo = getCurrentInputEditorInfo();
            int flags = 0;
            if (android.os.Build.VERSION.SDK_INT >= 25) {
                flags |= InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
            }
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Bundle bundle = new Bundle();
            bundle.putBoolean("emoji", true);
            bundle.putBoolean("makemoji", true);
            bundle.putString(Moji.EXTRA_JSON, MojiModel.toJson(model).toString());
            InputConnectionCompat.commitContent(
                    inputConnection, editorInfo, inputContentInfo, flags, bundle);
            return;
        }

        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setPackage(packageName);
        i.putExtra(Moji.EXTRA_MM, true);
        i.putExtra(Moji.EXTRA_PACKAGE_ORIGIN, getContext().getPackageName());
        i.putExtra(Intent.EXTRA_STREAM, uri);

        // i.putExtra(Intent.EXTRA_STREAM, "Html.fromHtml(\"<p>This is the text shared.</p>\")");
        i.setData(uri);
        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.putExtra(Moji.EXTRA_JSON, MojiModel.toJson(model).toString());


        i.setType("image/*");
        if (cacheFile.getName().toLowerCase().endsWith("mp4"))
            i.setType("video/*");
        List<ResolveInfo> bcs = pm.queryBroadcastReceivers(i, 0);
        List<ResolveInfo> ris = pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
        if (ris.isEmpty()) {
            Moji.toast("App does not support sharing " + (model.isVideo() ? "videos" : "images") + ". URL copied to clip board", Toast.LENGTH_LONG);
            ClipboardManager clipboard = ( ClipboardManager ) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("MakeMoji emoji", model.isVideo() ? model.video_url : model.image_url);
            clipboard.setPrimaryClip(clip);
            return;
        }
        i.setPackage(ris.get(0).activityInfo.packageName);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);

    }

    boolean isDownloading;

    void onDownloadStart() {
        isDownloading = true;
        spb.setVisibility(View.VISIBLE);
        spb.progressiveStart();
    }

    void onDownloadFinish() {
        isDownloading = false;
        spb.progressiveStop();
    }

    public void copy(InputStream in, File dst) throws IOException {
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public void getOther(final MojiModel model) {
        if (model.isVideo()) onDownloadStart();
        final String url = (model.isVideo() ? model.video_url : model.image_url);
        if (!Moji.enableUpdates) {
            File path = new File(getCacheDir(), "images");
            path.mkdir();
            String extension = ".png";
            if (url.lastIndexOf('.') != -1)
                extension = url.substring(url.lastIndexOf('.') - 1, url.length());
            final File cacheFile = new File(path, "" + model.name + extension);
            try {
                copy(getAssets().open("makemoji/sdkimages/" + url), cacheFile);
                share(model, cacheFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            onDownloadFinish();
            return;
        }
        Moji.okHttpClient.newCall(new okhttp3.Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onDownloadFinish();
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (model.isVideo()) onDownloadFinish();
                FileOutputStream out = null;
                File path = new File(getCacheDir(), "images");
                path.mkdir();
                String extension = ".png";
                if (url.lastIndexOf('.') != -1)
                    extension = url.substring(url.lastIndexOf('.') - 1, url.length());
                final File cacheFile = new File(path, "" + model.name + extension);
                try {
                    out = new FileOutputStream(cacheFile.getPath());
                    out.write(response.body().bytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    Moji.toast("Load failed", Toast.LENGTH_SHORT);
                    return;
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Moji.toast("Load failed", Toast.LENGTH_SHORT);
                        return;
                    }
                    Moji.handler.post(new Runnable() {
                        @Override
                        public void run() {

                            share(model, cacheFile);
                        }
                    });
                }
            }
        });
    }

    public Target getTarget(final MojiModel model) {
        //onDownloadStart();
        return new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // onDownloadFinish();
                FileOutputStream out = null;
                File path = new File(getCacheDir(), "images");
                path.mkdir();
                File cacheFile = new File(path, "" + model.name + "share.png");
                try {
                    out = new FileOutputStream(cacheFile.getPath());
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) {
                    e.printStackTrace();
                    Moji.toast("Load failed", Toast.LENGTH_SHORT);
                    return;
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Moji.toast("Load failed", Toast.LENGTH_SHORT);
                        return;
                    }
                    share(model, cacheFile);
                   /* Doesn't work
                   Intent i2 = new Intent(getContext(),BlankActivity.class);
                    i2.putExtra("uri",uri);
                    i2.putExtra("package",ris.get(0).activityInfo.packageName);
                    i2.putExtra(Moji.EXTRA_JSON,MojiModel.toJson(model).toString());
                    i2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i2);*/

                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                // onDownloadFinish();
                Moji.toast("Load failed", Toast.LENGTH_SHORT);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
    }

    Target t;

    @Override
    public void mojiSelected(MojiModel model, BitmapDrawable d) {
        if (model.locked && !MojiUnlock.getUnlockedGroups().contains(model.categoryName)) {

            categorySelected.categorySelected(model.categoryName, true, inputView);
            return;
        }
        if (model.name.equals("download")) {

            Toast.makeText(this, "Please purchase the Package to See more emoji", Toast.LENGTH_LONG).show();

        } else {
            RecentPopulator.addRecent(model);
            if (model.character != null && !model.character.isEmpty()) {
                getCurrentInputConnection().finishComposingText();
                getCurrentInputConnection().setComposingText(model.character, 1);
                getCurrentInputConnection().finishComposingText();
                return;
            }
            if (isDownloading) return;
            t = getTarget(model);
            if (model.image_url != null && model.image_url.toLowerCase().endsWith(".gif") || model.isVideo())
                getOther(model);
            else if (model.image_url != null && !model.image_url.isEmpty()) {
                if (!Moji.enableUpdates) {
                    if (!Moji.enableUpdates) {
                        File path = new File(getCacheDir(), "images");
                        path.mkdir();
                        String extension = ".png";
                        final File cacheFile = new File(path, "" + model.name + extension);
                        try {
                            copy(getAssets().open("makemoji/sdkimages/" + model.image_url), cacheFile);
                            share(model, cacheFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return;
                }
                RequestCreator requestCreator = Moji.picasso.load(model.image_url);
                if (forceDimen > 0)
                    requestCreator.resize(( int ) (forceDimen * Moji.density), ( int ) (forceDimen * Moji.density));
                requestCreator.into(t);
            }
        }


    }

    public Context getContext() {
        return Moji.context;
    }


    public static boolean showTrending = false;
    public static boolean showRecent = true;
    public static int recentCount = 20;
    List<TabLayout.Tab> tabList = new ArrayList<>();

    @Override
    public void onNewTabs(List<TabLayout.Tab> tabs) {

        if (!showTrending) tabs.remove(0);

      /*  TabLayout.Tab recent = tabLayout.newTab().setCustomView(R.layout.kb_tab)
                .setContentDescription("Used").setIcon(R.drawable.mm_recent_drawable);
        View v = recent.getCustomView().findViewWithTag("iv");
        if ((v != null) && v instanceof ImageView)
            ((ImageView) v).setColorFilter(Moji.resources.getColor(R.color._mm_left_button_cf));

        Category recentCategory = new Category("Used", null);
        List<MojiModel> recents = RecentPopulator.getRecents();
        recentCategory.models = new ArrayList<>(recents.subList(0, Math.min(recentCount, recents.size())));//set truncated list of recents.
        recent.getCustomView().setTag(R.id._makemoji_category_tag_id, recentCategory);
        if (showRecent) tabs.add(tabs.size() > 0 ? 1 : 0, recent);
*/
        //to resolve timing issue: If tablayout/inputview is rapidly created->destroyed->created, old tab will try to be added to the new tablayout.
        if (!tabs.isEmpty()) {
            if (tabs.get(0).getCustomView().getParent() != null && tabs.get(0).getCustomView().getParent().getParent() != null) {
                Log.d("MMKB", "tried to insert old tabs into new tablayout. aborting.");
                return;
            }
        }
        tabLayout.removeAllTabs();
        allModels.clear();
        allModelsNew.clear();
        allModelsNew2.clear();
        tabList = tabs;
        for (TabLayout.Tab tab : tabs) {
            if (tab.getCustomView().getTag(R.id._makemoji_category_tag_id) != null) {
                Category category = ( Category ) tab.getCustomView().getTag(R.id._makemoji_category_tag_id);

                if (category.models != null && !category.models.isEmpty()) {
                    if (category.isLocked()) lockedCategories.add(category.name);
                    for (MojiModel m : category.models) {
                        m.locked = category.isLocked();
                        m.categoryName = category.name;
                    }
                    if (showLockedEmojis ||
                            (!category.isLocked() || MojiUnlock.getUnlockedGroups().contains(category.name))) {
                        allModels.addAll(category.models);
                        allModels.add(new SpaceMojiModel(category.name));
                    }

                }
            }

            tabLayout.addTab(tab);
        }
        tabLayout.setOnTabSelectedListener(this);

        onNewDataAvailable();
      /*  int selectedPosition= tabLayout.getSelectedTabPosition();
        if (selectedPosition!= 1 ) {
            tabLayout.getTabAt(selectedPosition).select();//setscrollposition doesn't work...
        }*/
    }

    @Override
    public void unlockChange() {
        Moji.handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    List<TabLayout.Tab> tabs = KBCategory.getTabs(tabLayout, MMKB.this, R.layout.kb_tab);
                    onNewTabs(tabs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void checkPay(final String deivceId, final TabLayout.Tab tab, final String categoryName) {
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
                                Log.i("TabPosition", String.valueOf(tab.getPosition()));
                                isPay = true;
                                getCatgoryEmoji(tab);
                                setHeading(tab.getContentDescription());


                            } else {

                                Toast.makeText(MMKB.this, msg, Toast.LENGTH_LONG).show();
                                isPay = false;

                               /* Intent intent = new Intent(getApplicationContext(), inappNew.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("categoryName", categoryName);
                                intent.putExtra("device_id", deivceId);
                                startActivity(intent);
*/
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
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);


    }

    public void getCatgoryEmoji(TabLayout.Tab tab) {
        if (tab.getCustomView().getTag(R.id._makemoji_category_tag_id) != null && tab.getCustomView().getTag(R.id._makemoji_category_tag_id) instanceof Category) {
            Category c = (( Category ) tab.getCustomView().getTag(R.id._makemoji_category_tag_id));
            categorySelected.categorySelected(c.name, c.isLocked() && !MojiUnlock.getUnlockedGroups().contains(c.name), inputView);
        }
        if ("keyboard".equals(tab.getContentDescription())) {
            category = "keyboard";
            categorySelected.categorySelected("keyboard", false, inputView);
            mInputView.setVisibility(View.VISIBLE);
            pageFrame.setVisibility(View.GONE);
            kbBottomNav.setVisibility(View.GONE);
            horizRv.setVisibility(View.VISIBLE);
            horizRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            final HorizRVAdapter adapter = new HorizRVAdapter(this);
            adapter.enablePulse = false;
            horizRv.setAdapter(adapter);
            trendingPopulator.reload();
            trendingPopulator.setup(new PagerPopulator.PopulatorObserver() {
                @Override
                public void onNewDataAvailable() {
                    if (useTrending) {
                        List<MojiModel> list = trendingPopulator.populatePage(trendingPopulator.getTotalCount(), 0);
                        List<MojiModel> filterdList = new ArrayList<MojiModel>();
                        for (MojiModel m : list) {
                            if (m.categoryName != null && lockedCategories.contains(m.categoryName) && !MojiUnlock.getUnlockedGroups().contains(m.categoryName))
                                continue;
                            else
                                filterdList.add(m);
                        }
                        adapter.setMojiModels(filterdList);
                    }
                }
            });
            searchPopulator.setup(new PagerPopulator.PopulatorObserver() {
                @Override
                public void onNewDataAvailable() {
                    if (searchPopulator.getTotalCount() > 0 && !useTrending) {
                        List<MojiModel> list = searchPopulator.populatePage(searchPopulator.getTotalCount(), 0);
                        List<MojiModel> filterdList = new ArrayList<MojiModel>();
                        for (MojiModel m : list) {
                            if (m.categoryName != null && lockedCategories.contains(m.categoryName) && !MojiUnlock.getUnlockedGroups().contains(m.categoryName))
                                continue;
                            else
                                filterdList.add(m);
                        }

                        adapter.setMojiModels(filterdList);
                    }
                }
            });
            return;
        } else {
            mInputView.setVisibility(View.GONE);
            pageFrame.setVisibility(View.VISIBLE);
            kbBottomNav.setVisibility(View.VISIBLE);
            horizRv.setVisibility(View.GONE);
            horizRv.setAdapter(null);
        }


        currentTab = tab.getPosition();
        MojiModel m;
        for (int i = 0; i < allModels.size(); i++) {
            m = allModels.get(i);
            if (m.categoryName.equals(categoryName)) {
                glm.scrollToPositionWithOffset(i, 0);
                break;
            }
        }
    }

    private void checkPaytest(final String deivceId) {
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
                                isPay = true;
                            } else {
                                Toast.makeText(MMKB.this, msg, Toast.LENGTH_LONG).show();
                                isPay = false;


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
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);


    }


}
