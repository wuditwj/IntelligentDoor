package com.njwyt.intelligentdoor;

import android.app.Dialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.njwyt.AppContext;
import com.njwyt.adapter.UniversalAdapter;
import com.njwyt.content.Type;
import com.njwyt.db.MessageHistoryDaoHelp;
import com.njwyt.entity.MessageHistory;
import com.njwyt.entity.User;
import com.njwyt.intelligentdoor.databinding.ActivityMessageHistoryBinding;
import com.njwyt.intelligentdoor.databinding.ItemMessageHistoryBinding;
import com.njwyt.utils.DateUtils;
import com.njwyt.utils.ImageUtils;
import com.njwyt.view.ItemDecoration;

import java.util.ArrayList;

/**
 * Created by jason_samuel on 2017/8/20.
 * 查看目标的历史记录
 */

public class HistoryActivity extends BaseActivity {

    private ActivityMessageHistoryBinding binding;

    private UniversalAdapter<MessageHistory> mTodayListAdapter;      // 当日历史适配器
    private MessageHistoryDaoHelp mMessageHistoryDaoHelp;
    private ArrayList<MessageHistory> mMessageHistoryList;
    private ArrayList<String> mDateList;        // 底部日期列表
    private ArrayList<Bitmap> mImageList;       // 存储第一帧的列表

    private User currentUser;                  // 当前用户
    private User targetUser;                   // 目标用户
    private Handler touchHandler;              // 触摸计时
    private Handler noTouchHandler;            // 未触摸计时
    private Handler backHomeHandler;           // 记录按home键的handler
    private int touchTime = 0;                 // 触摸按下时间

    private int historySelectedId = 0;         // 历史条目被选中的下标
    private int dateSelectedId = 0;            // 日期条目被选中的下标

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_message_history);

        setBlur();
        initValue();
        initListener();
        initRecyclerView();
        initDateList();
    }

    /**
     * 设置该activity页面背景高斯模糊
     */
    private void setBlur() {
/*

        final View decorView = getWindow().getDecorView();
        //Activity's root View. Can also be root View of your layout (preferably)
        final ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
        //set background, if your root layout doesn't have one
        final Drawable windowBackground = decorView.getBackground();

        binding.blurView.setupWith(rootView)
                .windowBackground(windowBackground)
                .blurAlgorithm(new RenderScriptBlur(this))
                .blurRadius(8);

        binding.sdvBackground.getHierarchy().setPlaceholderImage(new BitmapDrawable(ImageUtils.rotateBitmap(AppContext.getInstance().getBlurBitmap(), -90)));
*/

        Bitmap blurBmp = ImageUtils.blurBitmap(this, AppContext.getInstance().getBlurBitmap(), 20);
        binding.ivBackground.setBackground(new BitmapDrawable(null, blurBmp));
    }

    private void initView() {

    }

    private void initValue() {
        touchHandler = new Handler();
        noTouchHandler = new Handler();
        backHomeHandler = new Handler();
        mImageList = new ArrayList<>();
        mMessageHistoryList = new ArrayList<>();
        mMessageHistoryDaoHelp = new MessageHistoryDaoHelp(this);

        targetUser = (User) getIntent().getSerializableExtra("targetUser");
        currentUser = (User) getIntent().getSerializableExtra("currentUser");
    }

    private void initListener() {

        // 上一条
        binding.llLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 如果时间轴为显示状态，那么就执行向下日期翻页操作
                if (binding.viewPager.getVisibility() == View.VISIBLE) {

                    // 翻页
                    if (dateSelectedId > 0) {
                        dateSelectedId--;
                    }
                    binding.viewPager.setCurrentItem(dateSelectedId);
                    // 顶部显示当前日期
                    dateChange(mDateList.get(dateSelectedId));

                    // 刷新隐藏时间
                    noTouchHandler.removeCallbacks(dateHideRunnable);
                    noTouchHandler.postDelayed(dateHideRunnable, 2000);
                    return;
                }

                // 如果时间轴为隐藏状态，那么就执行上下选择item操作
                if (historySelectedId == 0) {
                    return;
                }
                historySelectedId--;
                binding.rvMessageHistory.smoothScrollToPosition(historySelectedId + 1);
                mTodayListAdapter.refresh(mMessageHistoryList);
            }
        });

        // 上一条按钮按下与抬起
        binding.llLast.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Handle开始计时1秒
                        touchHandler.postDelayed(minusDateRunnable, 500);
                        break;

                    case MotionEvent.ACTION_UP:
                        // 打断Handle
                        touchHandler.removeCallbacks(minusDateRunnable);

                        touchTime = 0;

                        // 抬起手之后延迟2秒，无任何操作则隐藏时间栏
                        noTouchHandler.postDelayed(dateHideRunnable, 2000);

                        // 如果按下的时间大于1秒，证明是已启动长按翻页，那么就终止事件分发
                        if (touchTime > 1) {
                            return true;
                        }
                        // touchTime = 0;
                        break;
                }
                return false;
            }
        });


        // 下一条
        binding.llNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 如果时间轴为显示状态，那么就执行向下日期翻页操作
                if (binding.viewPager.getVisibility() == View.VISIBLE) {
                    // 翻页
                    if (dateSelectedId < mDateList.size() - 1) {
                        dateSelectedId++;
                    }
                    binding.viewPager.setCurrentItem(dateSelectedId);
                    // 顶部显示当前日期
                    dateChange(mDateList.get(dateSelectedId));

                    // 刷新隐藏时间
                    noTouchHandler.removeCallbacks(dateHideRunnable);
                    noTouchHandler.postDelayed(dateHideRunnable, 2000);
                    return;
                }

                // 如果时间轴为隐藏状态，那么就执行上下选择item操作
                if (historySelectedId == mMessageHistoryList.size() - 1) {
                    return;
                }
                historySelectedId++;
                // 移动到指定位置
                binding.rvMessageHistory.smoothScrollToPosition(historySelectedId + 1);
                // 刷新列表显示
                mTodayListAdapter.refresh(mMessageHistoryList);
            }
        });

        // 下一条按钮按下与抬起
        binding.llNext.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Handle开始计时1秒
                        touchHandler.postDelayed(addDateRunnable, 500);
                        break;

                    case MotionEvent.ACTION_UP:
                        // 打断Handle
                        touchHandler.removeCallbacks(addDateRunnable);

                        touchTime = 0;

                        // 抬起手之后延迟2秒，无任何操作则隐藏时间栏
                        noTouchHandler.postDelayed(dateHideRunnable, 2000);

                        // 如果按下的时间大于1秒，证明是已启动长按翻页，那么就终止事件分发
                        if (touchTime > 1) {
                            return true;
                        }
                        // touchTime = 0;
                        break;
                }
                return false;
            }
        });


        // 播放或确定
        binding.llPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 如果画廊正在显示
                if (binding.viewPager.getVisibility() == View.VISIBLE) {
                    // 直接隐藏画廊
                    hideGallery();
                    noTouchHandler.removeCallbacks(dateHideRunnable);
                    return;
                }

                Intent intent = new Intent(HistoryActivity.this, VideoActivity.class);
                intent.putExtra("messageHistory", mMessageHistoryList.get(historySelectedId));
                startActivity(intent);
            }
        });

        // 删除
        binding.llPlay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                showDeleteDialog(0, null);
                return true;
            }
        });

        // 长按play两秒，回到主页面
        binding.llPlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 两秒之后回到主页面
                        backHomeHandler.postDelayed(backHomeRunnable, 2000);
                        break;

                    case MotionEvent.ACTION_UP:
                        // 打断回到主页面的计时
                        backHomeHandler.removeCallbacks(backHomeRunnable);
                        break;
                }
                return false;
            }
        });

        // 时间轴滑动事件
        binding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                dateSelectedId = position;
                dateChange(mDateList.get(position));
                getMessageHistoryList(mDateList.get(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // 返回按钮事件
        binding.tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {

        // 配置当日历史列表适配器
        mTodayListAdapter = new UniversalAdapter<>(mMessageHistoryList, R.layout.item_message_history, BR.messageHistory, new UniversalAdapter.AdapterView() {
            @Override
            public void getViewDataBinding(UniversalAdapter.ViewHolder viewHolder, final int position) {
                final ItemMessageHistoryBinding binding = (ItemMessageHistoryBinding) viewHolder.getBinding();
                final MessageHistory messageHistory = mMessageHistoryList.get(position);

                binding.tvMessageDatatime.setText(DateUtils.getHourMinute(messageHistory.getDatatime()));
                binding.sdvFirstFrame.setImageURI("file://" + messageHistory.getFirstFrameUrl());
                /*if (position < mImageList.size()) {
                    binding.sdvFirstFrame.setImageBitmap(mImageList.get(position));
                }*/
                //binding.ivFirstFrame.setImageBitmap(mImageList.get(position));

                /*if (historySelectedId == position) {
                    binding.llRoot.setBackgroundResource(R.color.colorPrimary);
                } else {
                    binding.llRoot.setBackgroundResource(R.color.transparent);
                }*/

                binding.llRoot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(HistoryActivity.this, VideoActivity.class);
                        intent.putExtra("messageHistory", messageHistory);
                        intent.putExtra("targetId", targetUser.getId());intent.putExtra("dateSelectedId",dateSelectedId);
                        intent.putExtra("position",position);

                        startActivity(intent);
                    }
                });

                binding.llRoot.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        showDeleteDialog(position, messageHistory);
                        return true;
                    }
                });

            }
        });

        // 配置列表样式
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.rvMessageHistory.setLayoutManager(linearLayoutManager);
        binding.rvMessageHistory.addItemDecoration(new ItemDecoration(20));
        binding.rvMessageHistory.setAdapter(mTodayListAdapter);
        // 禁止上拉刷新和下拉加载
        binding.rvMessageHistory.setLoadingMoreEnabled(false);
        binding.rvMessageHistory.setPullRefreshEnabled(false);
    }

    // 时间轴递增
    private Runnable addDateRunnable = new Runnable() {
        @Override
        public void run() {
            touchTime++;
            // 如果按下的时间超过1秒，则开始滚动
            if (touchTime > 1) {
                if (dateSelectedId == mDateList.size() - 1) {
                    return;
                }
                //binding.hpTime.setSelectedItem(dateSelectedId + 1);
                noTouchHandler.removeCallbacks(dateHideRunnable);
                showGallery();
                dateSelectedId++;
                binding.viewPager.setCurrentItem(dateSelectedId);
                dateChange(mDateList.get(dateSelectedId));
            }

            // 每150毫秒连续调用自己
            touchHandler.postDelayed(this, 150);
        }
    };

    // 时间轴递减
    private Runnable minusDateRunnable = new Runnable() {
        @Override
        public void run() {
            touchTime++;
            // 如果按下的时间超过1秒，则开始滚动
            if (touchTime > 1) {
                if (dateSelectedId == 0) {
                    return;
                }
                //binding.hpTime.setSelectedItem(dateSelectedId - 1);
                noTouchHandler.removeCallbacks(dateHideRunnable);
                showGallery();
                dateSelectedId--;
                binding.viewPager.setCurrentItem(dateSelectedId);
                dateChange(mDateList.get(dateSelectedId));
            }

            // 每150毫秒连续调用自己
            touchHandler.postDelayed(this, 150);
        }
    };

    // 时间轴隐藏
    private Runnable dateHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideGallery();
        }
    };

    // 长按home键
    private Runnable backHomeRunnable = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    /**
     * 日期变换
     */
    private void dateChange(String date) {

        binding.tvDate.setText(DateUtils.dateToChinese(date, this));
    }

    /**
     * 初始化第一帧图片
     */
    private void initImages() {

        final HandlerThread handlerThread = new HandlerThread("Load Image");
        handlerThread.start();

        Handler handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {

                for (int i = 0; i < mMessageHistoryList.size(); i++) {

                    // 事先把视频第一帧bitmap图片放在一个list中
                    Log.d("HistoryActivity", "-->> load Image");
                    //创建MediaMetadataRetriever对象
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    //绑定资源
                    mmr.setDataSource(mMessageHistoryList.get(i).getVideoUrl());
                    //Bitmap bitmap = ImageUtils.compressImage(mmr.getFrameAtTime(), 50, 50);
                    mImageList.add(mmr.getFrameAtTime());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTodayListAdapter.refresh(mMessageHistoryList);
                            handlerThread.quit();
                        }
                    });
                }

            }
        };
        handler.sendEmptyMessage(0);
    }

    /**
     * 初始化日期选择器
     */
    private void initDateList() {

        mDateList = (ArrayList<String>) mMessageHistoryDaoHelp.getMessageHistoryDateList(currentUser.getId(), targetUser.getId());
        if (mDateList.size() == 0) {
            return;
        }
        initViewPager();
        dateChange(mDateList.get(0));

        getMessageHistoryList(mDateList.get(0));
    }

    /**
     * 从数据可中获取历史列表
     */
    private void getMessageHistoryList(String date) {

        // 刷新适配器
        mMessageHistoryList = (ArrayList<MessageHistory>) mMessageHistoryDaoHelp.getMessageHistoryListByTargetId(currentUser.getId(), targetUser.getId(), date);
        mTodayListAdapter.refresh(mMessageHistoryList);

        //initImages();

    }

    /**
     * 提示框
     */
    private void showDeleteDialog(final int position, final MessageHistory messageHistory) {
        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_history, null);
        deleteDialog.setView(dialogView);
        deleteDialog.setCancelable(false);
        final Dialog dialog = deleteDialog.show();

        // 是
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageHistoryDaoHelp.deleteHistoryById(messageHistory.getUuid());
                mMessageHistoryList.remove(position);
                mTodayListAdapter.notifyItemRemoved(position + 1);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mTodayListAdapter.refresh(mMessageHistoryList);
                    }
                }, 400);
                dialog.dismiss();
            }
        });

        // 否
        dialogView.findViewById(R.id.btn_no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    ///////////////横向日期列表(画廊)///////////////

    /**
     * 显示画廊
     */
    private void showGallery() {
        binding.viewPager.setVisibility(View.VISIBLE);
        binding.tvPrevious.setText(R.string.previous_day);
        binding.tvNext.setText(R.string.next_day);
        binding.tvPlay.setText(R.string.ok);
    }

    /**
     * 隐藏画廊
     */
    private void hideGallery() {

        // 如果此时画廊正在显示，则证明选中了当前日期
        if (binding.viewPager.getVisibility() == View.VISIBLE) {
            getMessageHistoryList(mDateList.get(dateSelectedId));
        }

        binding.viewPager.setVisibility(View.GONE);
        binding.tvPrevious.setText(R.string.up);
        binding.tvNext.setText(R.string.down);
        binding.tvPlay.setText(R.string.play);

    }

    /**
     * 加载横向日期列表(画廊)
     */
    private void initViewPager() {

        // 设置画廊item的宽度为屏幕的三分之一，高度为屏幕的12分之一
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                AppContext.getInstance().getScreenWidth() / 3,
                AppContext.getInstance().getScreenHeight() / 12);

        binding.viewPager.setLayoutParams(params);
        binding.viewPager.setAdapter(new ViewPagerAdapter());

        binding.viewPager.setOffscreenPageLimit(2);
        binding.viewPager.setPageMargin(15);
        binding.viewPager.setClipChildren(false); // 用来定义他的子控件是否要在他应有的边界内进行绘制
        binding.viewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        binding.rlTimer.setClipChildren(false);
    }


    /**
     * 画廊翻页适配器
     */
    private class ViewPagerAdapter extends PagerAdapter {


        @Override
        public int getCount() {
            return mDateList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            TextView textView = new TextView(HistoryActivity.this);
            textView.setText(mDateList.get(position));
            textView.setTextSize(16);
            textView.setTextColor(Color.WHITE);
            textView.setGravity(Gravity.CENTER);
            container.addView(textView);

            return textView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((TextView) object);
        }
    }

    /**
     * 设置切换动画
     */
    private class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MAX_SCALE = 1.5f;
        private static final float MIN_SCALE = 1.0f;//0.85f

        @Override
        public void transformPage(View page, float position) {

            if (position <= 1) {

                float scaleFactor = MIN_SCALE + (1 - Math.abs(position)) * (MAX_SCALE - MIN_SCALE);

                page.setScaleX(scaleFactor);

                if (position > 0) {
                    page.setTranslationX(-scaleFactor * 2);
                } else if (position < 0) {
                    page.setTranslationX(scaleFactor * 2);
                }
                page.setScaleY(scaleFactor);
            } else {

                page.setScaleX(MIN_SCALE);
                page.setScaleY(MIN_SCALE);
            }
        }

    }

}
