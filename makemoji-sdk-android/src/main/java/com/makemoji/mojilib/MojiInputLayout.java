package com.makemoji.mojilib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonObject;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import retrofit2.Response;

/**
 * To switch between emoji pages that paginate or that are one cotinuous list, change new OneGridPage(...) to ViewPagerPage
 * in toggleTrending, toggleRecent, and in CategoriesPage onclick
 * <p>
 * Customize colors and properties by applying a style in xml inheriting from MojiInputLayoutDefaultStyle. To customize the editText's appearance
 * set a default "android:editTextStyle" in a theme then apply that theme to this view in xml.
 * <p>
 * Created by Scott Baar on 1/4/2016.
 */
public class MojiInputLayout extends LinearLayout implements
        ViewTreeObserver.OnGlobalLayoutListener, IMojiSelected {
    ImageButton cameraImageButton;
    EditText editText;//active
    EditText myEditText;
    View sendLayout;
    RecyclerView rv;
    String flag;
    FrameLayout pageContainer;
    CategoriesPage categoriesPage;
    MakeMojiPage trendingPage;
    MakeMojiPage recentPage;
    String deivceId;
    ResizeableLL topScroller;
    LinearLayout horizontalLayout;

    ImageView trendingButton, toggleButton, categoriesButton, recentButton, backButton;
    Stack<MakeMojiPage> pages = new Stack<>();
    CategoryPopulator trendingPopulator;
    SearchPopulator searchPopulator;
    HorizRVAdapter adapter;
    Drawable bottomPageBg, trendingBarBg, topScrollerBg, buttonBg;
    View leftButtons;

    String currentSearchQuery;

    boolean replaceSuggestions = true;
    boolean searchAsYouType = true;

    @ColorInt
    int headerTextColor;
    @ColorInt
    int phraseBgColor;
    @DrawableRes
    int backSpaceDrawableRes;
    final static String TAG = "MojiInputLayout";
    boolean alwaysShowBar = false;
    boolean tryAlwaysShowBar = false;
    int minimumSendLength;
    boolean largeEmojiSizing;


    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (!isInMultiWindowMode)
            alwaysShowBar = tryAlwaysShowBar;
        else
            alwaysShowBar = true;

        setTopScrollerVisiblity(alwaysShowBar ? View.VISIBLE : View.GONE);
    }

    public void setLargeEmojiSizing(boolean largeEmojiSizing) {
        this.largeEmojiSizing = largeEmojiSizing;
        editText.setTag(R.id._makemoji_load_exact_size, !largeEmojiSizing);
        editText.setTag(R.id._makemoji_text_watcher, largeEmojiSizing ? IMojiTextWatcher.BigThreeTextWatcher : null);
    }

    public interface SendClickListener {
        /**
         * The send layout has been clicked. Returns the raw message and the transformed html
         *
         * @param html     the parsed html of the edit text
         * @param spanned, the raw spans in the edit text
         * @return true to clear the input, false to keep it.
         */
        boolean onClick(String html, Spanned spanned);
    }

    public interface RNUpdateListener {
        void needsUpdate();
    }

    RNUpdateListener rnUpdateListener;

    @Deprecated
    protected void setRnUpdateListener(RNUpdateListener listener) {
        rnUpdateListener = listener;
        topScroller.setRnUpdateListener(listener);
    }

    void requestRnUpdate() {
        if (rnUpdateListener != null) rnUpdateListener.needsUpdate();
    }

    boolean hasRnListener() {
        return rnUpdateListener != null;
    }


    public MojiInputLayout(Context context) {
        super(context);
        init(null, 0);
        deivceId = Moji.getUserId();

        //checkPayPre(deivceId);
    }

    public MojiInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public MojiInputLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void init(AttributeSet attributeSet, int defStyle) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attributeSet, R.styleable.MojiInputLayout, 0, R.style.MojiInputLayoutDefaultStyle_White);
        int cameraDrawableRes = a.getResourceId(R.styleable.MojiInputLayout__mm_cameraButtonDrawable, R.drawable.mm_camera_icon);
        backSpaceDrawableRes = a.getResourceId(R.styleable.MojiInputLayout__mm_backSpaceButtonDrawable, R.drawable.mm_backspace_grey600_24dp);
        int sendLayoutRes = a.getResourceId(R.styleable.MojiInputLayout__mm_sendButtonLayout, R.layout.mm_default_send_layout);
        boolean cameraVisibility = a.getBoolean(R.styleable.MojiInputLayout__mm_cameraButtonVisible, true);
        int buttonColor = a.getColor(R.styleable.MojiInputLayout__mm_leftButtonColor, ContextCompat.getColor(getContext(), R.color._mm_left_button_cf));
        buttonBg = a.getDrawable(R.styleable.MojiInputLayout__mm_leftButtonBg);
        headerTextColor = a.getColor(R.styleable.MojiInputLayout__mm_headerTextColor, ContextCompat.getColor(getContext(), R.color._mm_header_text_color));
        phraseBgColor = a.getColor(R.styleable.MojiInputLayout__mm_phraseBgColor, ContextCompat.getColor(getContext(), R.color._mm_default_phrase_bg_color));
        minimumSendLength = a.getInteger(R.styleable.MojiInputLayout__mm_minimumSendLength, 1);
        Drawable leftContainerDrawable = a.getDrawable(R.styleable.MojiInputLayout__mm_leftContainerDrawable);

        topScrollerBg = a.getDrawable(R.styleable.MojiInputLayout__mm_topBarBg);
        bottomPageBg = a.getDrawable(R.styleable.MojiInputLayout__mm_bottomPageBg);
        trendingBarBg = a.getDrawable(R.styleable.MojiInputLayout__mm_trendingBarBg);

        boolean showKbOnInflate = a.getBoolean(R.styleable.MojiInputLayout__mm_showKbOnInflate, false);
        tryAlwaysShowBar = a.getBoolean(R.styleable.MojiInputLayout__mm_alwaysShowEmojiBar, false);
        alwaysShowBar = tryAlwaysShowBar;
        if (Build.VERSION.SDK_INT >= 24) {
            Activity activity = Moji.getActivity(getContext());
            if (activity != null && activity.isInMultiWindowMode()) alwaysShowBar = true;
        }

        inflate(getContext(), R.layout.mm_moji_input_layout, this);
        findViewById(R.id._mm_horizontal_ll).setBackgroundDrawable(topScrollerBg);
        horizontalLayout = (LinearLayout) findViewById(R.id._mm_horizontal_ll);
        topScroller = (ResizeableLL) findViewById(R.id._mm_horizontal_top_scroller);
        topScroller.setBackgroundDrawable(topScrollerBg);
        if (alwaysShowBar) setTopScrollerVisiblity(View.VISIBLE);

        sendLayout = ((LinearLayout) inflate(getContext(), sendLayoutRes, horizontalLayout)).getChildAt(2);

        leftButtons = findViewById(R.id._mm_left_buttons);
        leftButtons.setBackgroundDrawable(leftContainerDrawable);
        setBackgroundDrawable(topScrollerBg);

        cameraImageButton = (ImageButton) findViewById(R.id._mm_camera_ib);
        cameraImageButton.setImageResource(cameraDrawableRes);
        if (!cameraVisibility) cameraImageButton.setVisibility(View.GONE);

        editText = (EditText) findViewById(R.id._mm_edit_text);
        myEditText = editText;
        sendLayout.setEnabled(editText.getText().length() >= minimumSendLength);

        setLargeEmojiSizing(a.getBoolean(R.styleable.MojiInputLayout__mm_largeEmojiSizing, true));

        rv = (RecyclerView) findViewById(R.id._mm_recylcer_view);
        LinearLayoutManager llm = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        rv.setLayoutManager(llm);
        rv.setItemViewCacheSize(20);
        rv.setBackgroundDrawable(trendingBarBg);
        adapter = new HorizRVAdapter(this);
        rv.setAdapter(adapter);
        pageContainer = (FrameLayout) findViewById(R.id._mm_page_container);
        pageContainer.setBackgroundDrawable(bottomPageBg);
        categoriesPage = new CategoriesPage((ViewStub) findViewById(R.id._mm_stub_cat_page), Moji.mojiApi, this);
        getRootView().getViewTreeObserver().addOnGlobalLayoutListener(this);

        categoriesButton = (ImageView) findViewById(R.id._mm_categories_button);
        toggleButton = (ImageView) findViewById(R.id._mm_toggle_button);
        recentButton = (ImageView) findViewById(R.id._mm_recent_button);
        trendingButton = (ImageView) findViewById(R.id._mm_trending_button);
        categoriesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCategoryPage();
            }
        });
        trendingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTrendingPage();
            }
        });
        recentButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecentPage();
            }
        });
        backButton = (ImageView) findViewById(R.id._mm_back_button);
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popPage();
            }
        });

        //don't show kb if pages are showing.
        editText.setOnTouchListener(new OnTouchListener() {
            boolean within(int t, int min, int max) {
                if (t > max) return false;
                return t >= min;
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && drawableClick != null)

                    if (editText.getCompoundDrawables()[MojiEditText.DRAWABLE_RIGHT] != null &&
                            event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[MojiEditText.DRAWABLE_RIGHT].getBounds().width())) {
                        drawableClick.onClick(MojiEditText.DRAWABLE_RIGHT);
                        return true;
                    }
                if (editText.getCompoundDrawables()[MojiEditText.DRAWABLE_LEFT] != null && event.getRawX() <=
                        (editText.getCompoundDrawables()[MojiEditText.DRAWABLE_LEFT].getBounds().width() + editText.getLeft())) {
                    {
                        drawableClick.onClick(MojiEditText.DRAWABLE_LEFT);
                        return true;
                    }
                }


                if (pages.empty()) return editText.onTouchEvent(event);
                else {
                    int pos = editText.getOffsetForPosition(event.getX(), event.getY());
                    editText.setSelection(pos);
                    /*if (event.getAction()== MotionEvent.ACTION_UP){
                        ClickableSpan spans[] =editText.getText().getSpans(pos,pos,ClickableSpan.class);
                        if (spans.length>0)spans[0].onClick(editText);
                    }*/
                    return true;
                }
            }
        });
        if (showKbOnInflate)
            grabFocusShowKb();

        trendingPopulator = new CategoryPopulator(new Category("Trending", null));
        trendingPopulator.setup(trendingObserver);
        searchPopulator = new SearchPopulator(true);
        searchPopulator.setup(searchObserver);
        ((IMakemojiDelegate) editText).setMojiInputLayout(this);
        toggleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deactiveButtons();
                topScroller.snapOpenOrClose();
                layoutRunnable = new Runnable() {
                    @Override
                    public void run() {
                        clearStack();
                    }
                };
            }
        });

        setButtonBackground(buttonBg);
        setButtonColor(buttonColor);

        a.recycle();
    }

    void setButtonBackground(Drawable d) {
        recentButton.setBackgroundDrawable(d.getConstantState().newDrawable());
        trendingButton.setBackgroundDrawable(d.getConstantState().newDrawable());
        categoriesButton.setBackgroundDrawable(d.getConstantState().newDrawable());
        toggleButton.setBackgroundDrawable(d.getConstantState().newDrawable());
    }

    void setButtonColor(int color) {
        recentButton.setColorFilter(color);
        trendingButton.setColorFilter(color);
        categoriesButton.setColorFilter(color);
    }

    @ColorInt
    int getHeaderTextColor() {
        return headerTextColor;
    }

    private StringBuilder alphaBuilder = new StringBuilder(255);

    private int lastAlphaBeforePosition(Editable editable, int limit) {
        alphaBuilder.setLength(0);
        alphaBuilder.append(editable);
        MojiSpan[] spans = editable.getSpans(0, editable.length(), MojiSpan.class);
        //first transform models inserted from search into a new format to exclude from next step
        for (int i = 0; i < spans.length; i++) {
            int start = editable.getSpanStart(spans[i]);
            int end = editable.getSpanEnd(spans[i]);
            if (spans[i].model.fromSearch && (end - start == 3)) {
                alphaBuilder.replace(start, end, "~~~");
            }
        }
        //if two spaces in a row, stop searching back, or find last alpha.
        boolean previousSpace = false;
        for (int i = limit; i > 0; i--) {
            int type = Character.getType(alphaBuilder.charAt(i));

            if (previousSpace && alphaBuilder.charAt(i) == '~')
                return i;

            if (type == Character.SPACE_SEPARATOR) {
                if (!previousSpace) previousSpace = true;
                else return -1;//double space
            } else
                previousSpace = false;


            if (type == Character.LOWERCASE_LETTER || type == Character.UPPERCASE_LETTER || type == Character.OTHER_PUNCTUATION)
                return i;
        }
        return -1;
    }

    void onSelectionChanged() {
        final String t = editText.getText().toString();
        sendLayout.setEnabled(t.length() >= minimumSendLength);

        if (!searchAsYouType) return;
        if (!showLeft) return;

        int selectionEnd = editText.getSelectionEnd();
        if (selectionEnd == -1 || editText.length() < 1) {
            useTrendingAdapter(true);
            return;
        }
        int lastAlpha = lastAlphaBeforePosition(editText.getText(), Math.min(editText.getText().length() - 1, selectionEnd - 1));
        if (lastAlpha == -1) {
            useTrendingAdapter(true);
            return;
        }
        selectionEnd = lastAlpha;
        String text = t.substring(0, Math.min(selectionEnd + 1, t.length()));//only look at what's before selection
        int lastSpace = text.lastIndexOf(' ', selectionEnd);
        if (lastSpace == -1) lastSpace = 0;

        final String query = text.substring(lastSpace, text.length()).replace("\'", "").trim();
        if (query.length() <= 1) {
            useTrendingAdapter(true);
            return;
        }
        currentSearchQuery = query;
        useTrendingAdapter(false);
        searchPopulator.search(query);

    }

    boolean usingTrendingAdapter = true;

    void useTrendingAdapter(boolean trending) {
        boolean wasUsingTrending = usingTrendingAdapter;
        usingTrendingAdapter = trending;
        if (usingTrendingAdapter && !wasUsingTrending) {
            adapter.showNames(false);
            adapter.setMojiModels(trendingPopulator.populatePage(200, 0));
            if (rnUpdateListener != null) rnUpdateListener.needsUpdate();
        }

    }

    PagerPopulator.PopulatorObserver trendingObserver = new PagerPopulator.PopulatorObserver() {
        @Override
        public void onNewDataAvailable() {
            if (usingTrendingAdapter) adapter.setMojiModels(trendingPopulator.populatePage(200, 0));
        }
    };
    int searchUpdated = 0;
    PagerPopulator.PopulatorObserver searchObserver = new PagerPopulator.PopulatorObserver() {
        @Override
        public void onNewDataAvailable() {
            MakeMojiPage page = pages.empty() ? null : pages.peek();
            if (searchUpdated < 2) {
                if (page != null) page.onNewDataAvailable();
                trendingPopulator.onNewDataAvailable();
                searchUpdated++;
            }
            if (!usingTrendingAdapter) {
                //adapter.showNames(true);
                List<MojiModel> filteredList = new ArrayList<>();
                LinkedHashSet<MojiModel> set = new LinkedHashSet<>(searchPopulator.populatePage(50, 0));
                for (MojiModel m : set) {
                    m.fromSearch = true;
                    if (m.categoryName != null && MojiUnlock.getLockedGroups().contains(m.categoryName) && !MojiUnlock.getUnlockedGroups().contains(m.categoryName))
                        continue;
                    else
                        filteredList.add(m);
                }
                filteredList.addAll(trendingPopulator.populatePage(100, 0));
                adapter.setMojiModels(new ArrayList<>(filteredList));
                if (rnUpdateListener != null) rnUpdateListener.needsUpdate();
            }
        }
    };

    OnClickListener abcClick = new OnClickListener() {
        @Override
        public void onClick(View v) {

            clearStack();
            topScroller.snapOpen();
            showKeyboard();
            deactiveButtons();
        }
    };

    Runnable backspaceRunnable = new Runnable() {
        @Override
        public void run() {
            int selection = editText.getSelectionStart();
            CharSequence text = editText.getText();
            if (selection > 1) {
                if (isVariation(text.charAt(1))) {
                    editText.getText().delete(selection - 2, selection);
                    return;
                }
            }
            editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        }
    };

    void deactiveButtons() {
        categoriesButton.setActivated(true);
        recentButton.setActivated(false);
        trendingButton.setActivated(false);
    }

    void toggleCategoryPage() {
        measureHeight = true;
        hideKeyboard();
        deactiveButtons();
        if (categoriesPage.isVisible()) {
            onLeftClosed();
            if (outsideEditText) {
                layoutRunnable.run();
                requestRnUpdate();
                layoutRunnable = null;
            }
        } else
            layoutRunnable = new Runnable() {
                @Override
                public void run() {
                    categoriesButton.setActivated(true);
                    clearStack();
                    addPage(categoriesPage);
                }
            };

        if (!keyboardVisible && layoutRunnable != null) {
            layoutRunnable.run();
            requestRnUpdate();
            layoutRunnable = null;
        }
        requestRnUpdate();
    }

    void toggleTrendingPage() {
        measureHeight = true;
        hideKeyboard();
        deactiveButtons();
        if (trendingPage == null)
            trendingPage = new VPPage(getContext().getString(R.string._mm_trending), this, new CategoryPopulator(new Category("Trending", null)));

        if (trendingPage.isVisible()) {
            onLeftClosed();
            if (outsideEditText) {
                layoutRunnable.run();
                requestRnUpdate();
                layoutRunnable = null;
            }
        } else
            layoutRunnable = new Runnable() {
                @Override
                public void run() {
                    trendingButton.setActivated(true);
                    clearStack();
                    addPage(trendingPage);
                }
            };

        if (!keyboardVisible && layoutRunnable != null) {
            layoutRunnable.run();
            requestRnUpdate();
            layoutRunnable = null;
        }
        requestRnUpdate();
    }

    void toggleRecentPage() {
        measureHeight = true;
        hideKeyboard();
        deactiveButtons();
        if (recentPage == null)
            recentPage = new VPPage(getContext().getString(R.string._mm_recent), this, new RecentPopulator());

        if (recentPage.isVisible()) {
            onLeftClosed();
            if (outsideEditText) {
                layoutRunnable.run();
                requestRnUpdate();
                layoutRunnable = null;
            }
        } else
            layoutRunnable = new Runnable() {
                @Override
                public void run() {
                    clearStack();
                    recentButton.setActivated(true);
                    addPage(recentPage);
                    recentPage.onNewDataAvailable();
                }
            };

        if (!keyboardVisible && layoutRunnable != null) {
            layoutRunnable.run();
            requestRnUpdate();
            layoutRunnable = null;
        }
        requestRnUpdate();
    }

    void addPage(MakeMojiPage page) {
        if (!pages.isEmpty() && pages.peek() != null) pages.peek().hide();
        pages.push(page);
        page.show();
        setHeight();
        backButton.setVisibility(pages.size() > 1 ? View.VISIBLE : View.GONE);
        requestRnUpdate();
    }

    void clearStack() {
        while (!pages.empty()) {
            MakeMojiPage page = pages.pop();
            page.hide();
            page.detatch();
        }
        backButton.setVisibility(View.GONE);
        getPageFrame().setVisibility(View.GONE);
        requestRnUpdate();
    }

    void popPage() {
        if (pages.size() == 0) return;
        MakeMojiPage page = pages.pop();
        MakeMojiPage oldPage = pages.size() > 0 ? pages.peek() : null;
        page.hide();
        page.detatch();
        if (oldPage != null) oldPage.show();
        backButton.setVisibility(pages.size() > 1 ? View.VISIBLE : View.GONE);
        if (pages.empty()) getPageFrame().setVisibility(View.GONE);
    }

    protected ViewGroup getPageFrame() {
        return pageContainer;
    }

    void hideKeyboard() {
        View view = editText;
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        requestRnUpdate();
    }

    void showKeyboard() {
        InputMethodManager keyboard = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(editText, 0);
        requestRnUpdate();
    }

    boolean kbVisible = false;
    boolean measureHeight;
    Runnable layoutRunnable;
    boolean keyboardVisible;
    int oldh, oldtop, oldDiff;
    int maxTopScrollherH;

    @Override
    public void onGlobalLayout() {

        Rect r = new Rect();
        getRootView().getWindowVisibleDisplayFrame(r);
        topScroller.setMaxSize(getWidth());
        int screenHeight = getRootView().getHeight();
        int heightDifference = screenHeight - (r.bottom - r.top);
        //Log.d("kb","kb h "+ heightDifference + " " + getHeight());
        if (heightDifference > screenHeight / 3) {
            measureHeight = false;
            //newHeight = heightDifference - topScroller.getHeight();// -topScroller.getHeight() - horizontalLayout.getHeight();
            // Log.d("newh","new h "+ newHeight);
            keyboardVisible = true;
            oldtop = getTop() - r.bottom;
            maxTopScrollherH = Math.max(topScroller.getHeight(), maxTopScrollherH);
            int parentHeight = ((ViewGroup) getParent()).getHeight()
                    - editText.getPaddingBottom() - editText.getPaddingTop() - (int) (20 * Moji.density);
            //if (!outsideEditText) editText.setMaxHeight(parentHeight -maxTopScrollherH);
            deactiveButtons();
            clearStack();
            setTopScrollerVisiblity(View.VISIBLE);
            requestRnUpdate();
        } else {
            keyboardVisible = false;
            // Log.d("newh", "kb not visible " + heightDifference );
        }

        if (layoutRunnable != null && oldDiff != heightDifference) {
            layoutRunnable.run();
            requestRnUpdate();
            layoutRunnable = null;

        }

        if (!keyboardVisible && pages.isEmpty() && !alwaysShowBar) {
            setTopScrollerVisiblity(View.GONE);
            requestRnUpdate();
        }

        oldDiff = heightDifference;
    }

    void setHeight() {
        getPageFrame().setVisibility(View.VISIBLE);
       /* if (!pages.empty())pages.peek().setHeight(newHeight);
        else
        {
           // topScroller.getLayoutParams().height=newHeight;
        }
        */
    }

    //remove last search term
    void removeSuggestion() {
        if (!replaceSuggestions) return;
        if (usingTrendingAdapter || editText.getSelectionStart() == -1) return;
        int selectionStart = editText.getSelectionStart();
        int lastBang = editText.getText().toString().substring(0, editText.getSelectionStart()).lastIndexOf(" ") + 1;
        if (lastBang == -1) return;
        SpannableStringBuilder ssb = new SpannableStringBuilder(editText.getText());
        ssb.delete(lastBang, selectionStart);
        editText.setText(ssb);
        editText.setSelection(Math.min(lastBang, ssb.length()));
    }

    //the range of text to replace when inserting a moji
    @Nullable
    Pair<Integer, Integer> getReplaceRange() {
        if (!replaceSuggestions) return null;
        if (editText.getSelectionStart() == -1) return null;
        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();
        if (selectionEnd == -1) return null;

        return new Pair<>(selectionStart, selectionEnd);
    }

    @Override
    public void mojiSelected(MojiModel model, @Nullable BitmapDrawable bitmapDrawable) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(editText.getText());
        Mojilytics.trackClick(model);
        int selectionStart = editText.getSelectionStart();
        if (selectionStart == -1) selectionStart = editText.length();
        Pair<Integer, Integer> range = getReplaceRange();

        if (model.character != null && !model.character.isEmpty()) {
            if (range != null)
                ssb.replace(range.first, range.second, model.character);
            else
                ssb.insert(selectionStart, model.character);
            editText.setText(ssb);
            editText.setSelection(Math.min(selectionStart + model.character.length(), ssb.length()));
            return;
        }
        final MojiSpan mojiSpan = MojiSpan.fromModel(model, editText, bitmapDrawable);

        if (range != null)
            ssb.replace(range.first, range.second, " \uFFFC ");
        else
            ssb.insert(selectionStart, " \uFFFC ");

        if (range != null) selectionStart = range.first;
        ssb.setSpan(mojiSpan, selectionStart, selectionStart + 3,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (mojiSpan.getLink() != null && !mojiSpan.getLink().isEmpty()) {
            ClickableSpan clickableSpan = new MojiClickableSpan() {
                @Override
                public void onClick(View widget) {
                    HyperMojiListener hyperMojiListener = (HyperMojiListener) widget.getTag(R.id._makemoji_hypermoji_listener_tag_id);
                    if (hyperMojiListener == null)
                        hyperMojiListener = Moji.getDefaultHyperMojiClickBehavior();
                    hyperMojiListener.onClick(mojiSpan.getLink());
                }
            };
            ssb.setSpan(clickableSpan, selectionStart, selectionStart + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            editText.setMovementMethod(LinkMovementMethod.getInstance());
        }
        Moji.setText(ssb, editText);
        editText.setSelection(Math.min(selectionStart + 3, editText.length()));
    }

    void onLeftClosed() {
        showKeyboard();
        deactiveButtons();
        layoutRunnable = new Runnable() {
            @Override
            public void run() {

                clearStack();
            }
        };

    }

    void onLeftOpened() {

    }

    //from 0-1
    void onLeftAnimationProgress(float fractionOpen) {
        toggleButton.setRotation(180f * (1f - fractionOpen));
        toggleButton.setBackgroundDrawable(fractionOpen < .20f ? trendingBarBg : buttonBg);
        horizontalLayout.setBackgroundDrawable(topScrollerBg);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Mojilytics.forceSend();
    }

    public String getInputAsHtml() {
        return Moji.toHtml(new SpannableStringBuilder(editText.getText()));
    }

    public Spanned getInputAsSpanned() {
        return new SpannableStringBuilder(editText.getText());
    }

    public void setInputText(CharSequence cs) {
        editText.setText(cs);
    }

    public ImageButton getCameraImageButton() {
        return cameraImageButton;
    }

    boolean outsideEditText = false;

    public void attatchMojiEditText(@NonNull MojiEditText met) {
        attatchEditText(met);
        setLargeEmojiSizing(largeEmojiSizing);
    }

    public void detachMojiEditText() {
        if (editText instanceof IMakemojiDelegate)
            ((IMakemojiDelegate) editText).setMojiInputLayout(null);
        editText = myEditText;
        horizontalLayout.setVisibility(View.VISIBLE);
        editText.requestFocus();
        outsideEditText = false;
        if (editText instanceof IMakemojiDelegate)
            ((IMakemojiDelegate) editText).setMojiInputLayout(this);
        editText.setSelection(editText.getText().length());//set selection to end
        setLargeEmojiSizing(largeEmojiSizing);
    }

    public EditText getEditText() {
        return editText;
    }

    void attatchEditText(EditText met) {
        editText = met;
        if (hyperMojiListener != null) setHyperMojiClickListener(hyperMojiListener);
        horizontalLayout.setVisibility(View.GONE);
        outsideEditText = true;
        if (met instanceof IMakemojiDelegate) ((IMakemojiDelegate) met).setMojiInputLayout(this);
        if (myEditText instanceof IMakemojiDelegate)
            ((IMakemojiDelegate) myEditText).setMojiInputLayout(null);
        editText.setSelection(editText.getText().length());
        onSelectionChanged();
    }

    /**
     * the action when a hypermoji is clicked in the edit text.
     *
     * @param hml
     */
    HyperMojiListener hyperMojiListener;

    public void setHyperMojiClickListener(HyperMojiListener hml) {
        hyperMojiListener = hml;
        editText.setTag(R.id._makemoji_hypermoji_listener_tag_id, hml);
        myEditText.setTag(R.id._makemoji_hypermoji_listener_tag_id, hml);
    }

    public void setCameraButtonClickListener(View.OnClickListener onClickListener) {
        cameraImageButton.setOnClickListener(onClickListener);
    }

    public void setCameraVisibility(boolean visible) {
        cameraImageButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setSendLayoutClickListener(final SendClickListener sendClickListener) {
        sendLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendClickListener != null) {
                    SpannableStringBuilder ssb = new SpannableStringBuilder(editText.getText());
                    String html = Moji.toHtml(ssb);
                    onSaveInputToRecentAndsBackend(ssb);
                    if (sendClickListener.onClick(html, ssb))
                        editText.setText("");
                }
            }
        });
    }

    public static void onSaveInputToRecentAndsBackend(final CharSequence cs) {
        final SpannableStringBuilder ssb = new SpannableStringBuilder(cs);

        MojiSpan[] spans = ssb.getSpans(0, ssb.length(), MojiSpan.class);
        for (int i = 0; i < spans.length; i++) {
            MojiModel model = spans[i].model;
            RecentPopulator.addRecent(model);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Integer> removals = new ArrayList<>();
                for (int i = ssb.length() - 1; i >= 0; i--) {
                    if (!keepCharForAnalytics(ssb.charAt(i))) {
                        //Log.d(TAG,"removed "+ssb.charAt(i) );
                        removals.add(i);
                    }
                }
                for (Integer i : removals) {
                    ssb.replace(i, i + 1, "");
                }
                String html = Moji.toHtml(ssb);
                if (Moji.enableUpdates)
                    Moji.mojiApi.sendPressed(html).enqueue(new SmallCB<JsonObject>() {
                        @Override
                        public void done(Response<JsonObject> response, @Nullable Throwable t) {
                            if (t != null) {
                                t.printStackTrace();
                            }
                        }
                    });
            }
        }).start();


    }

    public static boolean keepCharForAnalytics(char c) {
        int type = Character.getType(c);
        return c == MojiEditText.replacementChar ||
                Character.isHighSurrogate(c) || Character.isLowSurrogate(c) ||
                type == 28 || type == 25 || type == 27 || type == 6;//symbol, math,modifier,mark nonspacing

    }

    public void manualSaveInputToRecentsAndBackend() {
        onSaveInputToRecentAndsBackend(editText.getText());
    }

    public boolean handleIntent(Intent i) {
        try {
            if (i.hasExtra(Moji.EXTRA_JSON)) {
                String s = i.getStringExtra(Moji.EXTRA_JSON);
                if (!getContext().getPackageName().equals(i.getStringExtra(Moji.EXTRA_PACKAGE_ORIGIN))) {
                    Log.e(TAG, "origin package != dest package");
                    return false;
                }
                JSONObject jo = new JSONObject(s);
                MojiModel model = MojiModel.fromJson(jo);
                if (model == null) return false;
                mojiSelected(model, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isVariation(char c) {
        return (c > 65023 && c < 65040);
    }

    protected MojiUnlock.ILockedCategoryClicked iLockedCategoryClicked;

    public void setLockedCategoryClicked(MojiUnlock.ILockedCategoryClicked clickListener) {
        iLockedCategoryClicked = clickListener;

    }

    protected void onLockedCategoryClicked(String name) {
        if (iLockedCategoryClicked == null) {
            Toast.makeText(getContext(), "use setLockedCategoryClicked to listen for this event", Toast.LENGTH_LONG).show();
            return;
        }
        iLockedCategoryClicked.lockedCategoryClick(name);

    }

    public void refreshCategories() {
        if (categoriesPage != null) {
            categoriesPage.refresh();
        }
    }

    public boolean canHandleBack() {
        return !pages.isEmpty();
    }

    public boolean onBackPressed() {
        if (!canHandleBack()) return false;
        popPage();
        if (pages.isEmpty()) {
            deactiveButtons();
            topScroller.snapOpen();
            grabFocusShowKb();
        }
        return true;
    }

    public void grabFocusShowKb() {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    boolean hasJiggled = false;

    protected void setTopScrollerVisiblity(int visiblity) {
        //if (visiblity==View.GONE && outsideEditText)return;
        topScroller.setVisibility(visiblity);
        if (!hasJiggled && visiblity == VISIBLE) {
            topScroller.jiggle();
            hasJiggled = true;
        }
    }

    Drawable getPageBackground() {
        return bottomPageBg;
    }

    boolean showLeft = true;

    public void showLeftNavigation(boolean visible) {
        topScroller.setEnableScroll(visible);
        topScroller.setShowLeft(visible);
        showLeft = visible;
    }

    //replace the word after clicking a suggestion
    public void setReplaceSuggestions(boolean enabled) {
        replaceSuggestions = enabled;
    }

    public void setSearchAsYouType(boolean enabled) {
        searchAsYouType = enabled;
        if (!searchAsYouType) useTrendingAdapter(true);
    }

    protected MojiEditText.IDrawableClick drawableClick;

    public void setDrawableClickListener(MojiEditText.IDrawableClick drawableClick) {
        this.drawableClick = drawableClick;
    }

    public void setInputConnectionCreator(IInputConnectionCreator creator) {
        if (editText instanceof MojiEditText) ((MojiEditText) editText).connectionCreator = creator;
    }


    public void openCategoriesPage() {
        if (pages.contains(categoriesPage)) return;
        hasJiggled = true;
        setTopScrollerVisiblity(View.VISIBLE);
        topScroller.snapClose();
        toggleCategoryPage();
    }

    private void checkPayPre(final String deivceId) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://tech599.com/tech599.com/johnaks/EmojiApp/check_purchased.php",
                new com.android.volley.Response.Listener<String>() {
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
                                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

                                flag = "0";


                            }
                        } catch (JSONException e) {
                            System.out.println("jsonexeption" + e.toString());
                        }
                    }
                },

                new com.android.volley.Response.ErrorListener() {
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
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);


    }

}
