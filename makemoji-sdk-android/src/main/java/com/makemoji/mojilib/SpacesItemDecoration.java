package com.makemoji.mojilib;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * For column spacing
 * http://stackoverflow.com/questions/28531996/android-recyclerview-gridlayoutmanager-column-spacing
 * Created by Scott Baar on 2/6/2016.
 */

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int hSpace;
    private int vSpace;

    public SpacesItemDecoration(int vSpace,int hSpace) {
        this.vSpace = vSpace;
        this.hSpace = hSpace;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        outRect.left = hSpace;
        outRect.right = hSpace;
        outRect.bottom = vSpace;
        outRect.top = vSpace;

    }
}