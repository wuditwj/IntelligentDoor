package com.njwyt.view;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * RecyclerView每个item之间的分割线
 */
public class ItemDecoration extends RecyclerView.ItemDecoration {

    private int decorationHeight;

    /**
     * @param decorationHeight 分割高度
     */
    public ItemDecoration(int decorationHeight) {
        this.decorationHeight = decorationHeight;
    }

    /**
     * @param outRect 边界
     * @param view    recyclerView ItemView
     * @param parent  recyclerView
     * @param state   recycler 内部数据管理
     */
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        //设定底部边距为1px
        outRect.set(0, 0, 0, decorationHeight);
    }
}