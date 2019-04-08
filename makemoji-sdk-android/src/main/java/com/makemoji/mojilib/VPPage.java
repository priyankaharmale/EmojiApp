package com.makemoji.mojilib;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.makemoji.mojilib.model.MojiModel;
import com.rd.PageIndicatorView;

import java.util.Collection;
import java.util.List;

/**
 * page that contains multiple pages of swipeable emojis
 */
public class VPPage extends MakeMojiPage implements PagerPopulator.PopulatorObserver {

    PagerPopulator<MojiModel> mPopulator;
    TextView heading;
    int count;
    public int ROWS;
    public int COLS;
    ViewPager viewPager;
    boolean gifs;
    MojiInputLayout mojiInputLayout;
    View footer;
    PageIndicatorView pageIndicatorView;
    public static int RNDELAY = 100;
    public static boolean useSpanSizes;

    int oldH;
    int height;
    public VPPage(String title, MojiInputLayout mil, PagerPopulator<MojiModel> p) {
        super("gifs".equalsIgnoreCase(title)?R.layout.mm_vp_page:R.layout.mm_vp_page, mil);
        this.mojiInputLayout = mil;
        ROWS = OneGridPage.DEFAULT_ROWS;
        COLS = OneGridPage.DEFAULT_COLS;
        if ("gifs".equalsIgnoreCase(title)) {
            gifs=true;
            ROWS = OneGridPage.GIFROWS;
        }
        mPopulator = p;
        heading = (TextView) mView.findViewById(R.id._mm_page_heading);
        if (!gifs)heading.setTextColor(mMojiInput.getHeaderTextColor());
        heading.setText(title);
        footer = mView.findViewById(R.id._mm_one_grid_footer);
        viewPager = (ViewPager) mView.findViewById(R.id._mm_view_pager);
        pageIndicatorView = (PageIndicatorView) mView.findViewById(R.id.pageIndicatorView);
        mPopulator.setup(this);

        mView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (oldH== mView.getHeight())return;
                oldH = mView.getHeight();
                onNewDataAvailable();

            }
        });

        height = (mojiInputLayout.getPageFrame().getHeight() - heading.getHeight() - footer.getHeight());
        if (mojiInputLayout.hasRnListener())
            mojiInputLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewPager.invalidate();
                    viewPager.requestLayout();
                    mojiInputLayout.requestRnUpdate();
                    mojiInputLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onNewDataAvailable();
                        }
                    },RNDELAY);
                }
            }, RNDELAY);


    }

    //called by the populater once a query is complete.
    @Override
    public void onNewDataAvailable() {
        height = viewPager.getHeight();
        mPopulator.reload();
        if (height==0 || mPopulator.getTotalCount()==0)return;
        count = mPopulator.getTotalCount();
        List<MojiModel> mojiModelList = mPopulator.populatePage(count,0);
        int pageSize = ROWS * COLS;
        PageAdapter adapter = new PageAdapter(mojiInputLayout,Moji.getActivity(mView.getContext()).getSupportFragmentManager(),mojiModelList,pageSize);

        viewPager.setAdapter(adapter);
        pageIndicatorView.setViewPager(viewPager);
    }

    public static boolean hasVideo(Collection<MojiModel> list){
        for (MojiModel m : list)
            if (m.isVideo())return true;
        return false;

    }
    @Override
    public void hide(){
        super.hide();
    }

    class PageAdapter extends FragmentStatePagerAdapter {
        List<MojiModel> mojiModels;
        int pageSize;
        IMojiSelected iMojiSelected;
        public PageAdapter(IMojiSelected iMojiSelected,FragmentManager manager,List<MojiModel> mojiModels, int pageSize){
            super(manager);
            this.mojiModels = mojiModels;
            this.pageSize = pageSize;
            this.iMojiSelected = iMojiSelected;

        }

        @Override
        public Fragment getItem(int position) {
            return GridFragment.newInstance(iMojiSelected,mojiModels,Math.max(0,(position*pageSize)-1), pageSize);
        }

        @Override
        public int getCount() {
            return mojiModels.size()/pageSize + (mojiModels.size()%pageSize>0 ? 1:0);
        }
    }

    public static class GridFragment extends Fragment{
        int offset;
        int pageSize;
        List<MojiModel> mojiModels;
        IMojiSelected iMojiSelected;
        RecyclerView.ItemDecoration itemDecoration;
        public static   GridFragment newInstance(IMojiSelected iMojiSelected,List<MojiModel> mojiModels,int offset, int size)
        {
            GridFragment fragment = new GridFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("offset",offset);
            bundle.putInt("size",size);
            fragment.mojiModels = mojiModels;
            fragment.iMojiSelected = iMojiSelected;
            fragment.setArguments(bundle);
            return fragment;
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            offset = getArguments().getInt("offset");
            pageSize = getArguments().getInt("size");
            View view = inflater.inflate(R.layout.mm_grid_frag, container,false);
            RecyclerView rv = (RecyclerView) view.findViewById(R.id._mm_page_grid);
            rv.setTag("page"+offset);
            int rows = OneGridPage.DEFAULT_ROWS;
            int cols = OneGridPage.DEFAULT_COLS;

            int height = container.getHeight();
            if (height==0 || mojiModels.size()==0)return view;
            int length = Math.min(pageSize,mojiModels.size()-offset);
            List<MojiModel> mojiModelList = mojiModels.subList(offset,length+offset);
            if (hasVideo(mojiModelList)){
                rows = OneGridPage.VIDEOROWS;
                rv.setLayoutManager(new GridLayoutManager(rv.getContext(), rows, LinearLayoutManager.HORIZONTAL, false));
            }
            int h = container.getHeight();
            int w = container.getWidth();
            int size = Math.min(h / rows, w/cols);
            int vSpace = 0;

            int hSpace = (container.getWidth() - (size * cols)) / ((cols+1)*2);

            MojiGridAdapter adapter = new MojiGridAdapter(mojiModelList, iMojiSelected, false, size);
            adapter.setImagesSizedtoSpan(useSpanSizes);
            if (itemDecoration!=null) rv.removeItemDecoration(itemDecoration);
            // if (!gifs){
            itemDecoration = new SpacesItemDecoration(vSpace, hSpace);
            rv.addItemDecoration(itemDecoration);
            //}

            rv.setLayoutManager(new GridLayoutManager(view.getContext(), rows, LinearLayoutManager.HORIZONTAL, false));
            rv.setAdapter(adapter);


            return view;
        }
    }


}


