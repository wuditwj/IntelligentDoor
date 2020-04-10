package com.njwyt.intelligentdoor;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.anupcowkur.reservoir.Reservoir;
import com.njwyt.AppContext;
import com.njwyt.content.Type;
import com.njwyt.db.ReservoirHelper;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.User;
import com.njwyt.intelligentdoor.databinding.ActivityOutdoorCameraBinding;
import com.njwyt.intelligentdoor.databinding.ActivityThemePreviewBinding;
import com.njwyt.utils.DateUtils;
import com.njwyt.utils.Lunar;
import com.njwyt.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * 主题样式预览Activity
 * Created by jason_samuel on 2017/11/27.
 */

public class ThemePreviewActivity extends BaseActivity {

    private ActivityThemePreviewBinding binding;
    private Handler dateTimeHandler;                // 日期handler
    //private int theme;
    private ArrayList<Integer> imageList;    // 所有图片

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_theme_preview);
        dateTimeHandler = new Handler();
        initView();
        initListener();
    }

    private void initView() {
        imageList = getIntent().getIntegerArrayListExtra("themeList");
        if (imageList == null) {
            ToastUtil.showToast(this, "资源文件异常");
            finish();
        }

        // 设置壁纸
        // binding.rlScreensaver.setBackgroundResource(theme);
        binding.vpScreensaver.initData(imageList);
        startClock();
    }

    private void initListener() {

        // 返回按钮
        binding.tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 设置主题按钮
        binding.btnUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // todo 这里要输入多张图片
                ReservoirHelper.setTheme(imageList.get(0));
                ReservoirHelper.setThemeList(imageList);
                finish();
            }
        });
    }

    /**
     * 打开时钟
     */
    private void startClock() {
        dateTimeHandler.post(dateTimeRunnable);
    }

    // 日期记录器
    private Runnable dateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            String dateTime = DateUtils.getCurrentTime();
            String time = DateUtils.getHourMinute(dateTime);
            String date = DateUtils.dateToMonthDay(dateTime, ThemePreviewActivity.this);
            binding.tvTime.setText(time);
            binding.tvDate.setText(date);

            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getDefault());
            Lunar lunar = new Lunar(cal);
            binding.tvDateCh.setText(lunar.toString());
            dateTimeHandler.postDelayed(this, 1000);
        }
    };
}
