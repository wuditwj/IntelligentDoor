package com.njwyt.intelligentdoor;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.njwyt.AppContext;
import com.njwyt.content.Type;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.User;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import skin.support.app.SkinCompatActivity;

import static com.njwyt.content.Type.FONTSIZE_BIG;
import static com.njwyt.content.Type.FONTSIZE_SMALL;

/**
 * Created by jason_samuel on 2017/7/5.
 */

public class BaseActivity extends SkinCompatActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setStatusBar();
        //取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // todo 隐藏导航栏
        setFontSize();
    }

    /**
     * 获取字体大小
     */
    private void setFontSize() {
        switch (AppContext.getInstance().getFontSize()) {
            case FONTSIZE_SMALL:
                setTheme(R.style.Default_TextSize_Small);
                break;
            case FONTSIZE_BIG:
                setTheme(R.style.Default_TextSize_Big);
                break;
            default:
                setTheme(R.style.Default_TextSize_Middle);
        }
    }

    /**
     * 通过EventBus从AppContext中接收数据
     * 表示字体大小已被切换
     */
    @Subscribe
    public void changeFontSize(MessageEvent event) {
        if (event.getMessage() == Type.CHANGE_FONT_SIZE) {
            recreate();
        }
    }

    /**
     * 通过EventBus从AppContext中接收数据
     * 表示语言已被切换
     */
    @Subscribe
    public void changeLanguage(MessageEvent event) {
        if (event.getMessage() == Type.CHANGE_LANGUAGE) {
            recreate();
        }
    }

    /**
     * 设置状态栏为透明色
     */
    private void setStatusBar() {

        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
