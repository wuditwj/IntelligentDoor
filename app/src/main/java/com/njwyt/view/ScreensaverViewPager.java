package com.njwyt.view;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jason_samuel on 2018/1/10.
 * 屏幕保护翻页工具
 */

public class ScreensaverViewPager extends ViewPager {

    private final String TAG = "ScreensaverViewPager";
    private Context mContext;
    private int currentItem;                            // 当前图片
    private List<Integer> mImageResList;                // 图片在Rse中的Int地址
    private ScreensaverAdapter mScreensaverAdapter;

    public ScreensaverViewPager(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * 这里是通过XML文件实例化来
     *
     * @param context
     * @param attrs
     */
    public ScreensaverViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initListener();
        startTimer();
    }

    public void initData(List<Integer> imageResList) {
        mImageResList = imageResList;
        mScreensaverAdapter = new ScreensaverAdapter();
        setAdapter(mScreensaverAdapter);
    }

    private void initListener() {
        addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentItem = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    /**
     * 打开计时器翻页
     */
    private void startTimer() {

        postDelayed(new Runnable() {
            @Override
            public void run() {

                if (currentItem >= mImageResList.size()) {
                    currentItem = 0;
                }
                setCurrentItem(currentItem);
                currentItem++;
                postDelayed(this, 5000);
            }
        }, 5000);
    }

    /**
     * 获取当前锁屏图片地址
     */
    public int getCurrentItemRes() {
        return mImageResList.get(currentItem == mImageResList.size() ? currentItem - 1 : currentItem);
    }

    /**
     * 适配器内部类
     */
    private class ScreensaverAdapter extends PagerAdapter {

        private List<ImageView> mImageViewList;     // 读取出来的图片

        private ScreensaverAdapter() {
            mImageViewList = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return mImageResList != null ? mImageResList.size() : 0;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ImageView iv = (ImageView) object;
            container.removeView(iv);
            mImageViewList.add(iv);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            ImageView image;
            if (mImageViewList.isEmpty()) {
                image = new ImageView(mContext);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                image = mImageViewList.remove(0);
            }

            image.setImageResource(mImageResList.get(position));
            container.addView(image);
            return image;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }
    }
}
