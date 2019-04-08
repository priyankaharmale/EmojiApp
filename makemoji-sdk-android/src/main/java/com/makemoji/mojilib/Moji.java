package com.makemoji.mojilib;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.app.Application;
import android.widget.Toast;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.makemoji.mojilib.gif.GifSpan;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;
import com.squareup.picasso252.LruCache;
import com.squareup.picasso252.Picasso;

import org.ccil.cowan.tagsoup2.HTMLSchema;
import org.ccil.cowan.tagsoup2.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Retrofit;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

/**
 * Created by Scott Baar on 12/14/2015.
 */
public class Moji {
    public static Context context;
    public static Resources resources;

    //We use our own static import of Picasso to set our own caches, logging behavior, etc
    // and to not conflict with app-side picasso implementations
    public static Picasso picasso;

    //our own html parser to create custom spans. keep one for each thread.
    private static Map<Long,SoftReference<Parser>> parsers =new HashMap<>();
    //screen density
    public static float density;

    public static MojiApi mojiApi;
    public static OkHttpClient okHttpClient;
    public static Handler handler;
    public static final String EXTRA_JSON = "com.makemoji.mojilib.EXTRA_JSON";
    public static final String EXTRA_MM = "com.makemoji.mojilib.EXTRA_MM";
    public static final String EXTRA_PACKAGE_ORIGIN = "com.makemoji.mojilib.PACKAGE_ORIGIN";
    public static final String EXTRA_CATEGORY_NAME = "com.makemoji.mojilib.EXTRA_CATEGORY_NAME";
    public static final String ACTION_LOCKED_CATEGORY_CLICK = "com.makemoji.mojilib.action.LOCKED_CATEGORY_CLICKED";
    static String userId;
    static String channel;
    public static Gson gson;
    public static boolean enableUpdates=true;
    /**
     * Initialize the library. Required to set in {@link Application#onCreate()}  so that the library can load resources.
     * and activity lifecycle callbacks.
     * @param app The application object. Needed for resources and to register activity callbacks.
     * @param key The sdk key.
     * @param cacheSizeBytes the in-memory cache size in bytes
     */
    public static void initialize(@NonNull Application app,@NonNull final String key, int cacheSizeBytes){
        context = app.getApplicationContext();
        resources = context.getResources();
        density = resources.getDisplayMetrics().density;
        OneGridPage.DEFAULT_ROWS = resources.getInteger(R.integer._mm_emoji_rows);
        OneGridPage.DEFAULT_COLS = resources.getInteger(R.integer._mm_emoji_cols);
        OneGridPage.GIFROWS = resources.getInteger(R.integer._mm_gif_rows);
        OneGridPage.VIDEOROWS = resources.getInteger(R.integer._mm_video_rows);
        OneGridPage.useSpanSizes = resources.getBoolean(R.bool.mmUseSpanSizeForSdkImages);
        OneGridPage.vSpacing = resources.getDimensionPixelSize(R.dimen.mm_grid_page_vert_spacing);
        OneGridPage.hSpacing = resources.getDimensionPixelSize(R.dimen.mm_grid_page_horiz_spacing);
        MojiSpan.BASE_TEXT_PX_SCALED = MojiSpan.BASE_TEXT_PT*density;

        handler = new Handler(Looper.getMainLooper());
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override
            public void onActivityStarted(Activity activity) {}
            @Override
            public void onActivityResumed(Activity activity) {
                Spanimator.onResume(activity.hashCode());
            }
            @Override
            public void onActivityPaused(Activity activity) {
                Spanimator.onPause(activity.hashCode());
            }
            @Override
            public void onActivityStopped(Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override
            public void onActivityDestroyed(Activity activity) {}});

        Picasso.Builder builder = new Picasso.Builder(context);

        builder.memoryCache(new LruCache(cacheSizeBytes));
        //builder.loggingEnabled(true);
        picasso = builder.build();
        SharedPreferences sp = app.getSharedPreferences("_mm_id",0);
        String id = sp.getString("id",null);
        if  (id ==null) {
            id = UUID.randomUUID().toString();
            sp.edit().putString("id",id).apply();
        }
        userId= id;
       // HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
      //  interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        okHttpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {

                Request original = chain.request();

                // Customize the request
                 Request.Builder builder = original.newBuilder()
                        .header("makemoji-sdkkey", key)
                        .header("makemoji-deviceId", userId)
                        .method(original.method(), original.body());
                if (channel!=null) builder.addHeader("makemoji-channel",channel);
                Request request = builder.build();

                Response response = chain.proceed(request);
                return response;
            }
        })
               // .addInterceptor(interceptor)
                .build();
        gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return false;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                if (clazz.equals(WeakReference.class)) return true;
                return false;
            }
        }).addDeserializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return false;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                if (clazz.equals(WeakReference.class)) return true;
                return false;
            }
        }).create();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(MojiApi.BASE_URL).client(okHttpClient).
                addConverterFactory(GsonConverterFactory.create(gson)).build();
        mojiApi = retrofit.create(MojiApi.class);
    }
    //calls initialize with the default cache size, 5%
    public static void initialize(@NonNull Application app, @NonNull String key){
        initialize(app,key,calculateMemoryCacheSize(app));
    }

    public static void setUserId(String id){
        SharedPreferences sp = context.getSharedPreferences("_mm_id",0);
        sp.edit().putString("id",id).apply();
        userId=id;
    }
    public static String getUserId(){
        return userId;
    }
    public static void setChannel(String channel){
        Moji.channel= channel;
    }

    /**
     * Parses an html message and converts it into a spanned, using the TextView to size the emoji spans properly.
     * Optionally applies other style attributes from html such as color, margin, and text size.
     * @param html the html message to parse
     * @param tv the TextView to set the text on. Used for sizing the emoji spans.
     * @param simple If true, will not apply any styling information beyond setting the parsed message with emojis.
     * @param paddingForEditText bookend emojis with spaces, needed for edittexts
     * @return Returns the parsed attributes from the html so you can cherry pick which styles to apply.
     */
    @UiThread
    public static ParsedAttributes setText(@NonNull String html, @NonNull TextView tv, boolean simple,boolean paddingForEditText){
        ParsedAttributes parsedAttributes =parseHtml(html,tv,simple,paddingForEditText);
        setText(parsedAttributes.spanned,tv);
        if (!simple){
            tv.setPadding((int)(parsedAttributes.marginLeft *density),(int)(parsedAttributes.marginTop *density),
                    (int) (parsedAttributes.marginRight * density),(int)(parsedAttributes.marginBottom *density));
            if (parsedAttributes.color!=-1)tv.setTextColor(parsedAttributes.color);
            if (parsedAttributes.fontSizePt!=-1) tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,parsedAttributes.fontSizePt);
        }
        return parsedAttributes;
    }
    @UiThread
    public static ParsedAttributes setText(@NonNull String html, @NonNull TextView tv, boolean simple){
        return setText(html,tv,simple,false);
    }

    /**
     * Set the spanned into the textview, subscribing and unsubscribing from animation as appropriate. Use this if you cache the spanned
     * after calling #parseHtml
     * @param spanned The spanned produced from #parseHtml
     * @param tv The textview to change.
     */
    @UiThread
    public static void setText(Spanned spanned, TextView tv){
        CharSequence cs = tv.getText();
        if (cs instanceof Spanned)
            unsubSpanimatable((Spanned)cs);

        if (tv.getTag(R.id._makemoji_text_watcher)!=null && tv.getTag(R.id._makemoji_text_watcher) instanceof IMojiTextWatcher)
            spanned = ((IMojiTextWatcher)tv.getTag(R.id._makemoji_text_watcher)).textAboutToChange(spanned);
        tv.setText(spanned);
        subSpanimatable(spanned,tv);

    }

    public static IMojiTextWatcher getDefaultTextWatcher(){
        return IMojiTextWatcher.NoChangeWatcher;
    }

    /**
     * Call this to unsubscribe the spanned created from #setText from animation. Only need to call this yourself if you
     * are going to be calling TextView.setText manually.
     * @param spanned All moji spans in spanned will be unsubscribed
     */
    public static void unsubSpanimatable(CharSequence spanned){
        if (spanned == null || !(spanned instanceof Spanned))return;
        MojiSpan[] mojiSpans = ((Spanned)spanned).getSpans(0, spanned.length(), MojiSpan.class);
        for (MojiSpan mojiSpan : mojiSpans) {
                Spanimator.unsubscribe(Spanimator.HYPER_PULSE, mojiSpan);
        }
    }
    /**
     * Call this to subscribe the spanned created from #setText to animate. Only need to call this yourself if you
     * are going to be calling TextView.setText  manually.
     * @param spanned All moji spans in spanned will be subscribed to @Spanimator
     */
    public static void subSpanimatable(CharSequence spanned, TextView tv){
        if (spanned == null || !(spanned instanceof Spanned))return;
        MojiSpan[] mojiSpans = ((Spanned) spanned).getSpans(0, spanned.length(), MojiSpan.class);
        for (MojiSpan mojiSpan : mojiSpans) {
                Spanimator.subscribe(Spanimator.HYPER_PULSE, mojiSpan);
                mojiSpan.setTextView(tv);
        }
    }

    /**
     * Parse the html message without side effect. Returns the spanned and attributes.
     * @param html the html message to parse
     * @param tv An optional textview to size the emoji spans.
     * @param paddingForEditText addSpaces between emojis to help keyboards in edittexts
     * @return An @ParsedAttributes object containing the spanned and style attributes.
     */
    @CheckResult
    public static ParsedAttributes parseHtml(@NonNull String html, @Nullable TextView tv, boolean simple,boolean paddingForEditText){
        return new SpanBuilder(html,null,null,getParser(),simple,tv,paddingForEditText).convert();
    }
    @CheckResult
    public static ParsedAttributes parseHtml(@NonNull String html, @Nullable TextView tv, boolean simple){
        return parseHtml(html,tv,simple,false);
    }

    //gets the parser for the current thread.
    private static Parser getParser(){
       SoftReference<Parser> softReference = parsers.get(Thread.currentThread().getId());

        Parser parser;
        if (softReference!=null && softReference.get()!=null)return softReference.get();

        parser= new Parser();
        try {
            parser.setProperty(Parser.schemaProperty, new HTMLSchema());
        }catch (Exception e){}
        parsers.put(Thread.currentThread().getId(),new SoftReference<>(parser));
        return parser;
    }
    private static  HyperMojiListener defaultHyperMojiListener = new HyperMojiListener() {
        @Override
        public void onClick(String url) {
            Toast.makeText(context,"default hypermoji click "+ url,Toast.LENGTH_SHORT).show();
        }
    };
    private static HyperMojiListener customDefaultHyperMojiListener;
    static HyperMojiListener getDefaultHyperMojiClickBehavior(){
        return customDefaultHyperMojiListener==null?defaultHyperMojiListener:customDefaultHyperMojiListener;
    }

    /**
     * When a hypermoji is clicked but no OnClickListener has been set with
     * setTag(R.id._makemoji_hypermoji_listener_tag_id,HyperMojiListener), the default HyperMojiListener will be called.
     * @param hyperMojiListener
     */
    public static void setDefaultHyperMojiListener(HyperMojiListener hyperMojiListener ){
        customDefaultHyperMojiListener = hyperMojiListener;
    }

    private static int calculateMemoryCacheSize(Context context) {
        ActivityManager am =(ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && SDK_INT >= HONEYCOMB) {
            memoryClass = ActivityManagerHoneycomb.getLargeMemoryClass(am);
        }
        // Target ~x% of the available heap.
        return 1024 * 1024 * memoryClass / 12;
    }

    @TargetApi(HONEYCOMB)
    private static class ActivityManagerHoneycomb {
        static int getLargeMemoryClass(ActivityManager activityManager) {
            return activityManager.getLargeMemoryClass();
        }
    }
     static AppCompatActivity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (AppCompatActivity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }
    public static String toHtml(CharSequence cs){
     StringBuilder sb = new StringBuilder();
        Spanned spanned = new SpannableStringBuilder(cs);
        sb.append("<p dir=\"auto\" style=\"margin-bottom:16px;font-family:'.Helvetica Neue Interface';font-size:16px;\"><span style=\"color:#000000;\">");
        int next;
        int end = spanned.length();
        for (int i = 0; i < end; i = next) {
            next = spanned.nextSpanTransition(i, end, MojiSpan.class);
            MojiSpan[] style = spanned.getSpans(i, next,
                    MojiSpan.class);
            if (style.length>0)//if the mojispan has length >1, ignore the rest
                for (int j = 0; j < style.length; j++) {
                    sb.append(style[j].toHtml());
                }
            else
                withinStyle(sb,spanned,i,next);
        }
        sb.append("</p>");
        return sb.toString();
    }

    /**
     * <br /> converted to \n
     * emoji images <img> converted to [flashtagname.base62emojiid hypermojiurl]
     * @param spanned
     * @return
     */
    public static String spannedToPlainText(Spanned spanned){
        StringBuilder sb = new StringBuilder();
        int next;
        int end = spanned.length();
        for (int i = 0; i < end;) {
            next = spanned.nextSpanTransition(i, i+1, MojiSpan.class);
            MojiSpan[] style = spanned.getSpans(i, next,
                    MojiSpan.class);
            if (style.length>0){//if the mojispan has length >1, ignore the rest
                    sb.append(style[0].toPlainText());
                    i += spanned.getSpanEnd(style[0])-spanned.getSpanStart(style[0]);
                }
            else {
                sb.append(spanned.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }
    @WorkerThread
    public static String htmlToPlainText(String html){
        return spannedToPlainText(parseHtml(html,null,true,false).spanned);
    }
    static Pattern plainMojiRegex = Pattern.compile("\\[([^\\.]+)\\.([^\\[\\]\\s]+)( [^\\]\\s]+)?\\]");
    @WorkerThread
    public static String plainTextToHtml(String plainText){
        return toHtml(plainTextToSpanned(plainText));
    }
    public static Base62 base62 = new Base62();
    @WorkerThread
    public static Spanned plainTextToSpanned(String plainText) {
        String modifiedText = plainText;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        List<MojiSpan> spans = new ArrayList<>();
        Matcher m = plainMojiRegex.matcher(modifiedText);
        if (!m.find())
            return new SpannableStringBuilder(plainText);
        m.reset();
        while (m.find()) {
            String name = m.group(1);
            String idString = m.group(2);
            String url = m.group(3);
            int id = (int) base62.decodeBase62(idString);
            MojiModel model = new MojiModel();
            model.id = id;
            model.image_url = "https://d1tvcfe0bfyi6u.cloudfront.net/emoji/" + id + "-large@2x.png";
            model.name = name;
            if ("gif".equals(name)){
                if (GifSpan.USE_SMALL_GIFS) model.image_url = "http://d1tvcfe0bfyi6u.cloudfront.net/emoji/" + id + "-40x40@2x.gif";
                else
                    model.image_url = "https://d1tvcfe0bfyi6u.cloudfront.net/emoji/" + id + ".gif";
            }
            model.link_url = url;
            MojiSpan mojiSpan = MojiSpan.fromModel(model, null, null);
            spans.add(mojiSpan);
            modifiedText = m.replaceFirst(" " + MojiEditText.replacementChar+ " ");
            m = plainMojiRegex.matcher(modifiedText);
        }
        int spanCount = 0;
        for (int i = 0; i < modifiedText.length(); i++) {
            char c = modifiedText.charAt(i);
            ssb.append(modifiedText.charAt(i));
            if (MojiEditText.replacementChar.equals(c) && spanCount < spans.size()) {
                MojiSpan mojiSpan = spans.get(spanCount);
                ++i;
                ssb.append(modifiedText.charAt(i));
                ssb.setSpan(mojiSpan, i-2, i+1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanCount++;
                final String link = mojiSpan.getLink();
                if (mojiSpan.getLink() != null && !mojiSpan.getLink().isEmpty()) {
                    ClickableSpan clickableSpan = new MojiClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            HyperMojiListener hyperMojiListener = (HyperMojiListener) widget.getTag(R.id._makemoji_hypermoji_listener_tag_id);
                            if (hyperMojiListener == null)
                                hyperMojiListener = Moji.getDefaultHyperMojiClickBehavior();
                            hyperMojiListener.onClick(link);
                        }
                    };
                    ssb.setSpan(clickableSpan, i-2, i+1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        return ssb;
    }

    private static void withinStyle(StringBuilder out, CharSequence text,
                                    int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            }
            else if(c==0xFFFC){
                //do nothing
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                if (c < 0xDC00 && i + 1 < end) {
                    char d = text.charAt(i + 1);
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        i++;
                        int codepoint = 0x010000 | (int) c - 0xD800 << 10 | (int) d - 0xDC00;
                        out.append("&#").append(codepoint).append(";");
                    }
                }
            }
            else if (c =='\n'){
                out.append("<br \\>");
            } else if (c > 0x7E || c < ' ') {
                out.append("&#").append((int) c).append(";");
            }
              else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }

    /**
     * Invalidate a view to trigger a redraw for a spanimator animation. EditText requires more work.
     * Only invalidate once a frame.
     * request layout when a new emoji is loaded or always depending on tag
     * @param tv
     */
    @UiThread
    public static void invalidateTextView(TextView tv){
        if (tv ==null)return;

        long lastInvalidated = tv.getTag(R.id._makemoji_last_invalidated_id)==null?0:
                (long)tv.getTag(R.id._makemoji_last_invalidated_id);
        Long now = System.currentTimeMillis();

        if (lastInvalidated+15>now) return;

        tv.invalidate();
        if (tv.getTag(R.id._makemoji_tv_has_new_load)!=null){
            tv.requestLayout();
            tv.setTag(R.id._makemoji_tv_has_new_load,null);
        }
        else if (tv.getTag(R.id._makemoji_request_layout_id)!=null)
            tv.requestLayout();
        else if (Boolean.TRUE.equals(tv.getTag(R.id._makemoji_gif_invalidated_id)))
        {
            tv.requestLayout();
            tv.setTag(R.id._makemoji_gif_invalidated_id,false);
        }
        else if (tv instanceof EditText)
            tv.requestLayout();


        if (tv instanceof ISpecialInvalidate){
            ((ISpecialInvalidate) tv).specialInvalidate();
        }
        tv.setTag(R.id._makemoji_last_invalidated_id,now);

    }
    public static void toast(final CharSequence cs, final int duration){
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,cs,duration).show();
            }
        });
    }
    static boolean isMain(){
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }



    public static void setEnableUpdates(boolean enableUpdates) {
        Moji.enableUpdates = enableUpdates;
    }
    public static void loadOfflineFromAssets(){

        MojiSQLHelper mojiSQLHelper = MojiSQLHelper.getInstance(context,false);
        MojiSQLHelper mojiSQLHelper3pk = MojiSQLHelper.getInstance(context,true);
        try {

            String wallString = loadJSONFromAsset("emojiwall.json");
            context.getSharedPreferences("emojiWall",0).edit().putString("data", wallString).apply();
            context.getSharedPreferences("emojiWall3pk",0).edit().putString("data", wallString).apply();
            String categoriesString = loadJSONFromAsset("categories.json");
            context.getSharedPreferences("_mm_categories",0).edit().putString("categories",categoriesString).apply();
            Map<String, List<MojiModel>> data =
                    Moji.gson.fromJson(wallString, new TypeToken<Map<String, List<MojiModel>>>() {
                    }.getType());

            List<MojiModel> accumulated = new ArrayList<MojiModel>();
            for (Map.Entry<String,List<MojiModel>> entry:data.entrySet()) {
                accumulated.addAll(entry.getValue());
                MojiModel.saveList(entry.getValue(),entry.getKey());
            }
            mojiSQLHelper.insert(accumulated);
            mojiSQLHelper3pk.insert(accumulated);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static Uri uriImage(String s){
        if (enableUpdates){
            return Uri.parse(s);
        }
        else{
            return Uri.parse("file:///android_asset/makemoji/sdkimages/"+s);
        }
    }
    static String loadJSONFromAsset(String name) {
        String s = null;
        //JSONObject jo = null;
        try {
            InputStream is = context.getAssets().open("makemoji/"+name);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            s = new String(buffer, "UTF-8");
            //jo = new JSONObject(s);
        } catch (Exception ex ) {
            ex.printStackTrace();
            return null;
        }
        return s;
    }


}
