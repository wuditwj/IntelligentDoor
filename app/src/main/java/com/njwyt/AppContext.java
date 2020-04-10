package com.njwyt;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.anupcowkur.reservoir.Reservoir;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.njwyt.FaceUtil.SerialAPI;
import com.njwyt.content.Type;
import com.njwyt.db.ReservoirHelper;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.User;
import com.njwyt.intelligentdoor.OutDoorCameraActivity;
import com.njwyt.intelligentdoor.SettingActivity;
import com.njwyt.utils.MemoryManage;
import com.njwyt.utils.ScreenUtils;
import com.njwyt.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import skin.support.SkinCompatManager;

/**
 * Created by jason_samuel on 2017/8/19.
 */

public class AppContext extends Application {

    private final String TAG = "AppContext";

    private static AppContext instance;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private int fontSize = -1;
    private int currentInsideLuminance;     // 门内补光灯当前亮度
    private int currentOutsideLuminance;     // 门内补光灯当前亮度
    private boolean isLogin;        // 是否正已登录
    private boolean isDenyOpenOutDoor;  // 是否拒绝打开门外摄像头
    private Bitmap blurBitmap;      // 高斯模糊所用的背景图片
    private Typeface typeFaceYaHei;
    private User currentUser;
    private List<User> userList;            // 用户列表
    private List<View> headViewList;        // 家庭成员View表
    private SerialAPI mSerialAPI;               // 开门工具类

    private Uri uri;                // 临时存放url

    private AudioManager audioManager;//音频管理器

    public static AppContext getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();


        instance = this;
        initValue();
        // 导入图标库
        Fresco.initialize(this);

        // 初始化皮肤管理器
        SkinCompatManager.init(this).loadSkin();

        // 改变成skin_2
        //SkinCompatManager.getInstance().loadSkin("skin_2", SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
        // 恢复应用默认皮肤
        SkinCompatManager.getInstance().restoreDefaultTheme();

        // 切换字体
        changeFont();

        // 初始化Reservoir
        try {
            Reservoir.init(this, 1024 * 1024); //in bytes 1M
        } catch (IOException e) {
            e.printStackTrace();
        }

        getScreen();

        initFile();

        initConfig();

        // initSerial();

        firstLoad();

    }

    /**
     * 首次加载
     */
    private void firstLoad() {

        if (ReservoirHelper.getFirstLoad()) {
            ReservoirHelper.setSystemPassword("000000");
            ReservoirHelper.setFirstLoad(false);
        }
    }

    private void initValue() {
        userList = new ArrayList<>();
        headViewList = new ArrayList<>();
    }

    private void initFile() {
        File file = new File("/sdcard/intelligentDoor/");
        if (!file.exists()) {
            file.mkdir();//创建文件夹
        }

    }

    /**
     * 切换字体
     */
    private void changeFont() {
        typeFaceYaHei = Typeface.createFromAsset(getAssets(), "dongqing_font.otf");
        try {
            Field field = Typeface.class.getDeclaredField("SERIF");
            field.setAccessible(true);
            field.set(null, typeFaceYaHei);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载系统配置
     */
    private void initConfig() {

        switchLanguage(ReservoirHelper.getLanguage().equals(Type.LANGUAGE_ENGLISH) ? Locale.US : Locale.CHINESE);

        setFontSize(ReservoirHelper.getFontSize());
    }

    /**
     * 载入串口通信类
     */
    public void initSerial() {
        mSerialAPI = new SerialAPI();
        if (mSerialAPI.OpenSerialPort() < 0) {
            ToastUtil.showToast(this, "串口打开异常");
            Log.d(TAG, "-->> 串口打开异常");
        } else {
            ToastUtil.showToast(this, "串口打开成功");
            Log.d(TAG, "-->> 串口打开成功");
        }
    }

    /**
     * 开门
     */
    public void openDoor() {
        mSerialAPI.WriteSerialPort(Type.DOOR_OPEN);
    }

    /**
     * 关灯
     */
    public void offLigth() {
        if (mSerialAPI != null) {
            mSerialAPI.WriteSerialPort(Type.LIGHT_OFF);
        }
    }

    /**
     * 开灯
     */
    public void onLigth() {
        mSerialAPI.WriteSerialPort(Type.LIGHT_ON);
    }

    /**
     * 门内灯亮度设置
     *
     * @param startLuminance 起始亮度 0~9
     * @param endLuminance   结束亮度 0~9
     */
    public void lightInsideLuminance(final int startLuminance, final int endLuminance) {

        final boolean asc = startLuminance < endLuminance;
        currentInsideLuminance = startLuminance;
        final Handler luminanceHandler = new Handler();
        luminanceHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentInsideLuminance != (asc ? endLuminance + 1 : endLuminance - 1)) {
                    mSerialAPI.WriteSerialPort(Type.LIGHT_INSIDE + currentInsideLuminance);
                }
                if (currentInsideLuminance == endLuminance) {
                    luminanceHandler.removeCallbacks(this);
                    return;
                }
                if (asc) {
                    currentInsideLuminance++;
                } else {
                    currentInsideLuminance--;
                }
                luminanceHandler.postDelayed(this, 20);
            }
        }, 20);
    }

    /**
     * 门外灯亮度设置
     *
     * @param startLuminance 起始亮度 0~9
     * @param endLuminance   结束亮度 0~9
     */
    public void lightOutsideLuminance(final int startLuminance, final int endLuminance) {

        /*final boolean asc = startLuminance < endLuminance;
        currentOutsideLuminance = startLuminance;
        final Handler luminanceHandler = new Handler();
        luminanceHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentOutsideLuminance != (asc ? endLuminance + 1 : endLuminance - 1)) {
                    mSerialAPI.WriteSerialPort(Type.LIGHT_OUTSIDE + currentOutsideLuminance);
                }
                if (currentOutsideLuminance == endLuminance) {
                    luminanceHandler.removeCallbacks(this);
                    return;
                }
                if (asc) {
                    currentOutsideLuminance++;
                } else {
                    currentOutsideLuminance--;
                }
                luminanceHandler.postDelayed(this, 5);
            }
        }, 5);*/
        mSerialAPI.WriteSerialPort(Type.LIGHT_OUTSIDE + endLuminance);
    }

    /**
     * 关闭串口通信，在MainActivity销毁时调用
     */
    public void releaseSerialAPI() {
        //mSerialAPI.WriteSerialPort("NORMAL");
        mSerialAPI.CloseSerialPort();
    }

    /**
     * 打开门外摄像头
     */
    public void openOutdoorActivity() {

        EventBus.getDefault().post(new MessageEvent<>(Type.OUTDOOR_RESPONSE, new Object()));

        if (isDenyOpenOutDoor) {
            return;
        }

        isDenyOpenOutDoor = true;
        Intent intent = new Intent(getApplicationContext(), OutDoorCameraActivity.class);
        intent.putExtra("recognition", Type.RECOGNITION_LOGIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * 清理内存
     */
    public void clearMemory() {
        MemoryManage.clearMemory(this);
    }

    /**
     * 清理应用缓存
     */
    public void clearAppCache() {
        MemoryManage.clearAppCache(this);
    }

    /**
     * 切换语言
     *
     * @param locale
     */
    public void switchLanguage(Locale locale) {
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        DisplayMetrics dm = res.getDisplayMetrics();
        config.locale = locale;
        res.updateConfiguration(config, dm);
        EventBus.getDefault().post(new MessageEvent<>(Type.CHANGE_LANGUAGE, new Object()));
    }

    private void getScreen() {
        screenHeight = ScreenUtils.getScreenHeight(this);
        screenWidth = ScreenUtils.getScreenWidth(this);
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public boolean isDenyOpenOutDoor() {
        return isDenyOpenOutDoor;
    }

    public void setDenyOpenOutDoor(boolean denyOpenOutDoor) {
        isDenyOpenOutDoor = denyOpenOutDoor;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }

    public Bitmap getBlurBitmap() {
        return blurBitmap;
    }

    public void setBlurBitmap(Bitmap blurBitmap) {
        this.blurBitmap = blurBitmap;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public int getFontSize() {
        return fontSize;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    public List<View> getHeadViewList() {
        return headViewList;
    }

    public void setHeadViewList(List<View> headViewList) {
        this.headViewList = headViewList;
    }

    /**
     * 设置Type类中定义好的字体大小
     *
     * @param fontSize
     */
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        // 通知所有页面更换字体大小
        EventBus.getDefault().post(new MessageEvent<>(Type.CHANGE_FONT_SIZE, new Object()));
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    private void goToActivity(int i) {
        Intent intent = new Intent(this, SettingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
