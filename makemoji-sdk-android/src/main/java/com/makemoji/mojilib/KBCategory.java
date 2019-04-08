package com.makemoji.mojilib;

import android.content.SharedPreferences;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;

import com.google.gson.reflect.TypeToken;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiApi;
import com.makemoji.mojilib.SmallCB;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import retrofit2.Response;

/**
 * Created by Scott Baar on 3/29/2016.
 */
public class KBCategory {
    public interface KBTAbListener{
        void onNewTabs(List<TabLayout.Tab> tabs);
    }
    public static Map<String,Integer> defaults = new HashMap<>();
    public static Map<String,Integer> categoryDrawables = new HashMap<>();
    private static String [] defaultCategories = {"trending","animals","clothing","expression","food",
            "hands","objects","politics","pop culture","sports","keyboard"};
    private static int [] icons = {R.drawable.mm_trending,R.drawable.mm_animals,R.drawable.mm_clothing,R.drawable.mm_expression,R.drawable.mm_food,
            R.drawable.mm_hands,R.drawable.mm_objects,R.drawable.mm_politics,R.drawable.mm_popculture,R.drawable.mm_sports,R.drawable.mm_abc};
    static{
        for (int i = 0; i < defaultCategories.length; i++) {
            defaults.put(defaultCategories[i],icons[i]);
        }


    }

    public static List<TabLayout.Tab> getTabs(final TabLayout tabLayout, final KBTAbListener kbtAbListener, @LayoutRes final int layoutRes){
        List<TabLayout.Tab> tabs = new ArrayList<>();
        List<Category> cachedCategories = Category.getCategories();
        cachedCategories.add(0,new Category("Trending",null));
        final SharedPreferences sp = Moji.context.getSharedPreferences("emojiWall3pk",0);
        if (Moji.enableUpdates)
            Moji.mojiApi.getEmojiWallData3pk().enqueue(new SmallCB<Map<String, List<MojiModel>>>() {
                @Override
                public void done(final Response<Map<String, List<MojiModel>>> wallData, @Nullable Throwable t) {
                    if (t!=null){
                        t.printStackTrace();
                        return;
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final List<MojiModel> accumulated = new ArrayList<MojiModel>();
                            for (Map.Entry<String,List<MojiModel>> entry:wallData.body().entrySet()) {
                                if (!"osemoji".equalsIgnoreCase(entry.getKey()))accumulated.addAll(wallData.body().get(entry.getKey()));
                                MojiModel.saveList(entry.getValue(), entry.getKey()+"3pk");
                            }
                            sp.edit().putString("data",Moji.gson.toJson(wallData.body())).apply();
                            MojiSQLHelper.getInstance(Moji.context,true).insert(accumulated);
                        }
                    }).start();
                    Moji.mojiApi.getCategories().enqueue(new SmallCB<List<Category>>() {
                        @Override
                        public void done(final Response<List<Category>> categories, @Nullable Throwable t) {
                            if (t!=null){
                                t.printStackTrace();
                                return;
                            }
                            Category.saveCategories(categories.body());
                            categories.body().add(0,new Category("Trending",null));
                            for (Category c : categories.body())
                                if (wallData.body().containsKey(c.name))
                                    c.models = wallData.body().get(c.name);
                            kbtAbListener.onNewTabs(returnTabs(tabLayout,categories.body(),layoutRes));

                        }
                    });
                }
            });

        if (cachedCategories.isEmpty()) {
            for (int i = 0; i < defaultCategories.length; i++) {
                tabs.add(tabLayout.newTab().setCustomView(layoutRes).
                        setContentDescription(defaultCategories[i]).setIcon(icons[i]));
            }
            return tabs;
        }
        else {
            try {
                String s = sp.getString("data", null);
                Map<String, List<MojiModel>> data =
                        Moji.gson.fromJson(s, new TypeToken<Map<String, List<MojiModel>>>() {
                        }.getType());

                if (data != null) {

                    for (Category c : cachedCategories)
                        if (data.containsKey(c.name))
                            c.models = data.get(c.name);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

            return returnTabs(tabLayout,cachedCategories,layoutRes);
        }
    }
    static List<TabLayout.Tab> returnTabs(TabLayout tabLayout, List<Category> categories, @LayoutRes int layoutRes){
        return addTrendingAndKB(createTabs(tabLayout,mergeCategoriesDrawable(categories,true,true),layoutRes),tabLayout,layoutRes);

    }

    @SuppressWarnings("ConstantConditions")
    public static List<TabLayout.Tab> createTabs(TabLayout tabLayout, List<Category> categories, @LayoutRes int layoutRes){
        List<TabLayout.Tab> tabs = new ArrayList<>();
        for (Category c : categories) {
            if ("phrases".equalsIgnoreCase(c.name))//currently unicode is good, multiple makemojis does not work.
                continue;
            TabLayout.Tab tab = null;
            if (c.drawableRes!=0){
                tab = tabLayout.newTab().setCustomView(layoutRes).
                        setContentDescription(c.name).setIcon(c.drawableRes);
                tabs.add(tab);
                tab.getCustomView().setSelected(false);
                if ("recent".equals(c.name)){
                    View v = tabs.get(tabs.size()-1).getCustomView().findViewWithTag("iv");
                    if ((v!=null) && v instanceof ImageView)
                        ((ImageView) v).setColorFilter(Moji.resources.getColor(R.color._mm_left_button_cf));
                }
            }
            else if (c.image_url!=null){
                tab = tabLayout.newTab().setCustomView(layoutRes).
                        setContentDescription(c.name).setIcon(R.drawable.mm_placeholder);
                tab.getCustomView().setSelected(false);
                ImageView iv =(ImageView) tab.getCustomView().findViewWithTag("iv");
                if (c.image_url!= null && !c.image_url.isEmpty()) Moji.picasso.load(Moji.uriImage(c.image_url)).into(iv);
                tabs.add(tab);

            }
            tab.getCustomView().setTag(R.id._makemoji_category_tag_id,c);
            //if ("Animals".equals(c.name))c.locked=1;
            if (tab!=null && c.isLocked() && !MojiUnlock.getUnlockedGroups().contains(c.name)){
                tab.getCustomView().setTag(R.id._makemoji_locked_tag_id,true);
                if (tab.getCustomView().findViewWithTag("iv") instanceof MMForegroundImageView)
                    ((MMForegroundImageView) tab.getCustomView().findViewWithTag("iv")).setForegroundResource(R.drawable.mm_locked_foreground);
            }
        }
        return tabs;

    }
    public static List<Category> mergeCategoriesDrawable(List<Category> oldCategories,boolean keepOs,boolean keepRecent){
        ListIterator<Category> iterator = oldCategories.listIterator();
        while (iterator.hasNext()){
            Category c = iterator.next();
            if (defaults.containsKey(c.name.toLowerCase())){
                c.drawableRes = defaults.get(c.name.toLowerCase());
            }
            if ("osemoji".equalsIgnoreCase(c.name)){
                c.drawableRes=R.drawable.mm_globe_drawable;
                if (!keepOs) iterator.remove();
            }
            if ("recent".equalsIgnoreCase(c.name)){
                c.drawableRes=R.drawable.mm_recent_drawable;
                if (!keepRecent)iterator.remove();
            }
            if (categoryDrawables.containsKey(c.name)){
                c.drawableRes = categoryDrawables.get(c.name);
            }
        }
        return oldCategories;
    }
    @SuppressWarnings("ConstantConditions")
    private static List<TabLayout.Tab> addTrendingAndKB(List<TabLayout.Tab> tabs, TabLayout tabLayout, @LayoutRes int layoutRes){

        tabs.add(tabs.size(),tabLayout.newTab().setCustomView(layoutRes).
                setContentDescription(defaultCategories[defaultCategories.length-1]).setIcon(icons[defaultCategories.length-1]));

         View v = tabs.get(tabs.size()-1).getCustomView().findViewWithTag("iv");
        if ((v!=null) && v instanceof ImageView)
            ((ImageView) v).setColorFilter(tabLayout.getContext().getResources().getColor(R.color.mmKBIconColor));

        return tabs;
    }

}
