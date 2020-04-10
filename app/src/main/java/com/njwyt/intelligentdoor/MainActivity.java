package com.njwyt.intelligentdoor;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.njwyt.AppContext;
import com.njwyt.content.Type;
import com.njwyt.db.MessageHistoryDaoHelp;
import com.njwyt.db.ReservoirHelper;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.FaceLocation;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.MessageHistory;
import com.njwyt.entity.User;
import com.njwyt.intelligentdoor.databinding.ActivityMainBinding;
import com.njwyt.intelligentdoor.databinding.ViewHeadItemBinding;
import com.njwyt.utils.DateUtils;
import com.njwyt.utils.DisplayUtils;
import com.njwyt.utils.ImageUtils;
import com.njwyt.utils.Lunar;
import com.njwyt.utils.ScreenUtils;
import com.njwyt.view.CameraSurfaceView;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.njwyt.content.Type.FONTSIZE_BIG;
import static com.njwyt.content.Type.FONTSIZE_SMALL;

/**
 * 孙嘉鹏 8.19
 */
public class MainActivity extends BaseActivity {

    private final String TAG = "MainActivity";
    private final int LOCK_TIME = 60;       // 锁屏时间
    private final int APPLY_PERMISSIONS_CODE = 2;     // 申请权限request code

    private AudioManager audioManager;//音频管理器
    private List<User> userList;            // 用户列表
    private List<View> headViewList;        // 家庭成员View表
    private SurfaceHolder drawSurfaceHolder;
    private SoundPool mSoundPool;           // 声音播放器
    private ActivityMainBinding binding;
    private ObjectAnimator targetHandRemovAanim;    // 删除头像的动画
    private UserDaoHelp mUserDaoHelp;        // 用户操作读取
    private MessageHistoryDaoHelp mMessageHistoryDaoHelp;
    private User currentUser;               // 当前登录的用户
    private Handler loginHandler;           // 登录操作的handler
    private Handler distanceHandler;        // 距离操作的handler
    private Handler dateTimeHandler;        // 日期handler
    private Handler lockTimeHandler;        // 锁屏计时器
    private Handler matchRatioHandler;      // 识别率Handler

    private int music;                      // 定义一个整型用load（）；来设置suondID
    private int timmer;                     // 用来记录进度条
    private int countDistance = -1;         // 当前距离
    private int lockTime = LOCK_TIME;       // 锁屏时间
    private int ratio = 0;                  // 识别率
    private int clearTimer = 0;             // 记录人脸识别框清除倒计时
    private int gcTimer = 0;                // 系统回收时间计时器
    private long count;                     // 未读消息
    private boolean isLockTime;             // 是否开启计时
    private boolean isShowMyMenu;           // 我的菜单是否显示

    private final int FAR = 0;  // 远
    private final int MIDDLE = 1;   // 中
    private final int CLOSE = 2; // 近


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        applyPermissions();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.usbCameraSurfaceView.setMode(Type.RECOGNITION_DEFAULT);
        loginHandler = new Handler();
        distanceHandler = new Handler();
        dateTimeHandler = new Handler();
        lockTimeHandler = new Handler();
        matchRatioHandler = new Handler();
        startClock();
        initListener();
        initDrawSurfaceView();
        AppContext.getInstance().initSerial();
        //startScanLineAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initTheme();
        if (AppContext.getInstance().isLogin()) {
            currentUser = AppContext.getInstance().getCurrentUser();
            if (currentUser != null && headViewList == null) {
                login(currentUser);
                startMyHeadAnimation();
            } else {
                for (int i = 0; i < headViewList.size(); i++) {
                    binding.llHead.removeView(headViewList.get(i));
                }
                headViewList = new ArrayList<>();
                userList = mUserDaoHelp.selectAllUser(this);
                for (int i = 0; i < userList.size(); i++) {
                    addUser(userList.get(i), i);
                }

                startSmallSurfaceAnimation();
                startGuestAnimation();
                startMyHeadAnimation();
                initUnreadMessage();
            }
        }

        if (targetHandRemovAanim != null) {
            targetHandRemovAanim.resume();
        }

        //binding.blurView.setVisibility(View.GONE);
        binding.ivBackground.clearAnimation();
        binding.ivBackground.setVisibility(View.GONE);
        binding.tvCountDown.setVisibility(View.GONE);
        //binding.blurView.setBlurEnabled(false);

        // 判断是否开启锁屏倒计时
        if (binding.rlScreensaver.getVisibility() == View.GONE && !isLockTime) {
            isLockTime = startLockTime();
        }
    }

    /**
     * 申请权限回调方法
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case APPLY_PERMISSIONS_CODE: {
                // 判断用户选择允许还是拒绝
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 允许
                    binding.cameraSurfaceView.setVisibility(View.VISIBLE);

                } else {
                    // 拒绝
                }
            }
            break;
        }
    }

    /**
     * 系统设置权限打开回调方法
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APPLY_PERMISSIONS_CODE) {
            closeSystemSound();
        }
    }

    /**
     * 申请权限
     */
    private void applyPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            // 批量申请相机打开、文件读写权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    APPLY_PERMISSIONS_CODE);

            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, APPLY_PERMISSIONS_CODE);
            }
        }
    }

    /**
     * 关闭系统触摸音
     */
    private void closeSystemSound() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //屏蔽系统提示声
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);//系统
//        audioManager.setStreamMute(AudioManager.STREAM_MUSIC,false);//媒体
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);//闹铃
//        audioManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);//音调
//        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);//系统提示
//        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);//电话铃声
        Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
        audioManager.unloadSoundEffects();
        Log.d(TAG, "-->> 成功关闭系统触摸音");
    }

    /**
     * 从PassowdEnterActivity和CameraSurfaceView获得通知
     *
     * @param event
     */
    @Subscribe
    public void loginSuccess(final MessageEvent<User> event) {
        if (event.getMessage() == Type.LOGIN_SUCCESS) {

            loginHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lockTime = LOCK_TIME;
                    binding.rlScreensaver.setVisibility(View.GONE);
                    currentUser = event.getBody();
                    login(event.getBody());
                    startMyHeadAnimation();
                    loginHandler.removeCallbacks(this);
                }
            }, 1000);
        }
    }

    /**
     * 从CameraSurfaceView获得通知
     *
     * @param event
     */
    @Subscribe
    public void changeDistance(final MessageEvent<Integer> event) {
        if (event.getMessage() == Type.CHANGE_DISTANCE) {
            distanceHandler.post(new Runnable() {
                @Override
                public void run() {

                    if (event.getBody() <= Type.DISTANCE_FAR && countDistance != FAR) {

                        // 最远的距离（屏保）
                        binding.rlScreensaver.setVisibility(View.VISIBLE);
                        countDistance = FAR;

                    } else if (event.getBody() > Type.DISTANCE_FAR && event.getBody() < Type.DISTANCE_CLOSE && countDistance != MIDDLE) {
                        // 中段距离（镜子）
                        binding.rlScreensaver.setVisibility(View.GONE);
                        hindAllView();
                        countDistance = MIDDLE;

                    } else if (event.getBody() >= Type.DISTANCE_CLOSE && countDistance != CLOSE) {
                        // 近距离（识别）
                        binding.rlScreensaver.setVisibility(View.GONE);
                        showAllView();
                        countDistance = CLOSE;
                    }

                    if (binding.rlScreensaver.getVisibility() == View.GONE && !isLockTime) {
                        isLockTime = startLockTime();
                    } else {
                        lockTime = LOCK_TIME;
                    }
                    distanceHandler.removeCallbacks(this);
                }
            });
        }
    }

    /**
     * 开始锁屏计时
     */
    private boolean startLockTime() {
        lockTime = LOCK_TIME;
        lockTimeHandler.postDelayed(lockTimeRunnable, 1000);
        return true;
    }

    /**
     * 停止锁屏计时
     *
     * @return
     */
    private boolean stopLockTime() {
        lockTimeHandler.removeCallbacks(lockTimeRunnable);
        return false;
    }

    private Runnable lockTimeRunnable = new Runnable() {
        @Override
        public void run() {

            if (lockTime == 0) {
                if (AppContext.getInstance().isLogin()) {
                    logout();
                }
                screensaver();
                lockTimeHandler.removeCallbacks(this);
                isLockTime = false;
                return;
            }
            lockTime--;
            lockTimeHandler.postDelayed(this, 1000);
        }
    };

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
            String date = DateUtils.dateToMonthDay(dateTime, MainActivity.this);
            binding.tvTime.setText(time);
            binding.tvDate.setText(date);

            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getDefault());
            Lunar lunar = new Lunar(cal);
            binding.tvDateCh.setText(lunar.toString());

            // 日期计时器也顺便做清空人脸识别框的作用
            checkClearTime();

            // 垃圾回收
            clearMemory();
            dateTimeHandler.postDelayed(dateTimeRunnable, 1000);
        }
    };

    private void initTheme() {
        //binding.rlScreensaver.setBackgroundResource(ReservoirHelper.getTheme());
        binding.vpScreensaver.initData(ReservoirHelper.getThemeList());
    }

    private void initListener() {

        binding.sdvMyHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 重置时间
                //isLockTime = stopLockTime();
                lockTime = LOCK_TIME;

                /*if (AppContext.getInstance().isLogin()) {
                    binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                        @Override
                        public void onTakePictureFinish(Bitmap body) {
                            Intent intent = new Intent(MainActivity.this, MyHistoryActivity.class);
                            intent.putExtra("currentUser", currentUser);
                            AppContext.getInstance().setBlurBitmap(body);
                            startActivity(intent);
                        }
                    });
                } else {
                    binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                        @Override
                        public void onTakePictureFinish(Bitmap body) {
                            Intent intent = new Intent(MainActivity.this, PasswordEnterActivity.class);
                            AppContext.getInstance().setBlurBitmap(body);
                            startActivity(intent);
                        }
                    });
                }*/

                /*HiddenAnimUtils myMenu = HiddenAnimUtils.newInstance(MainActivity.this, binding.llMyMenu, binding.sdvMyHead, 200);
                myMenu.toggle();*/

                if (isShowMyMenu) {
                    removeMyMenuAnimation();
                } else {
                    startMyMenuAnimation();
                    removeOtherUserMenu(-1);
                }
            }
        });

        binding.flMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 重置时间
                isLockTime = stopLockTime();
                removeMyMenuAnimation();
                binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                    @Override
                    public void onTakePictureFinish(Bitmap body) {
                        Intent intent = new Intent(MainActivity.this, MyHistoryActivity.class);
                        intent.putExtra("currentUser", currentUser);
                        AppContext.getInstance().setBlurBitmap(ImageUtils.rotateBitmap(body, 90, true));
                        startActivity(intent);
                    }
                });

            }
        });

        binding.flExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 重置时间
                removeMyMenuAnimation();
                if (AppContext.getInstance().isLogin()) {
                    logout();
                    hindAllView();
                    isLockTime = startLockTime();
                }
            }
        });

        /*binding.sdvMyHead.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                // 重置时间

                if (AppContext.getInstance().isLogin()) {
                    logout();
                    hindAllView();
                }

                return true;
            }
        });*/

        binding.viewSettingPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 已登录后不允许进入设置页面
                if (AppContext.getInstance().isLogin()) {
                    return;
                }

                // 重置时间
                isLockTime = stopLockTime();

                binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                    @Override
                    public void onTakePictureFinish(Bitmap body) {
                        AppContext.getInstance().setBlurBitmap(ImageUtils.rotateBitmap(body, 90, true));

                        binding.cameraSurfaceView.destroyed();
                        Intent intent = new Intent(MainActivity.this, PasswordEnterActivity.class);
                        intent.putExtra("hintWordRes", R.string.title_enter_admin_pwd);
                        intent.putExtra("passwordLenght", 6);
                        intent.putExtra("mode", Type.PASSWORD_ADMIN);
                        startActivity(intent);
                    }
                });
//                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
//                startActivity(intent);
            }
        });

        // 拿当前锁屏页面当壁纸
        binding.tvTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 已登录后不允许进入设置页面
                if (AppContext.getInstance().isLogin()) {
                    return;
                }

                // 重置时间
                isLockTime = stopLockTime();
                AppContext.getInstance().setBlurBitmap(BitmapFactory.decodeResource(getResources(), binding.vpScreensaver.getCurrentItemRes()));

                binding.cameraSurfaceView.destroyed();
                Intent intent = new Intent(MainActivity.this, PasswordEnterActivity.class);
                intent.putExtra("hintWordRes", R.string.title_enter_admin_pwd);
                intent.putExtra("passwordLenght", 6);
                intent.putExtra("mode", Type.PASSWORD_ADMIN);
                startActivity(intent);
            }
        });

        binding.cameraSurfaceView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                // 重置时间
                showAllView();
                return true;
            }
        });

        binding.llSmallSurface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 重置时间
                isLockTime = stopLockTime();

                binding.cameraSurfaceView.destroyed();
                binding.llSmallSurface.setVisibility(View.GONE);
                binding.usbCameraSurfaceView.setVisibility(View.GONE);
                Intent intent = new Intent(MainActivity.this, OutDoorCameraActivity.class);
                intent.putExtra("recognition", Type.RECOGNITION_LOGIN);
                startActivity(intent);
            }
        });

        binding.llLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 重置时间
                isLockTime = stopLockTime();

                binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                    @Override
                    public void onTakePictureFinish(Bitmap body) {
                        AppContext.getInstance().setBlurBitmap(ImageUtils.rotateBitmap(body, 90, true));
                        Intent intent = new Intent(MainActivity.this, PasswordEnterActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });

        binding.llGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 重置时间
                isLockTime = stopLockTime();
                removeMyMenuAnimation();
                binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                    @Override
                    public void onTakePictureFinish(Bitmap body) {
                        Intent intent = new Intent(MainActivity.this, GuestHistoryAcitvity.class);
                        AppContext.getInstance().setBlurBitmap(ImageUtils.rotateBitmap(body, 90, true));
                        startActivity(intent);
                    }
                });
            }
        });
    }

    /**
     * 加载扫描头像的方框
     */
    private void initDrawSurfaceView() {

        //binding.svDraw.setZOrderOnTop(true);
        binding.svDraw.setZOrderMediaOverlay(true);
        drawSurfaceHolder = binding.svDraw.getHolder();
        drawSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);//非常重要，否则不会有透明效果

        // 为surfaceHolder2添加一个回调监听器
        /*drawSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //drawThread=new Thread(DrawThread);
                if (isPreview && !isRun) {
                    isRun = true;
                    //drawThread.start();
                }

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // 如果camera不为null ,释放摄像头
                isRun = false;
            }
        });*/
    }

    /**
     * 获取头像坐标
     * 从CameraSurfaceView获取通知
     *
     * @param event
     */
    @Subscribe
    public void getFaceLocation(final MessageEvent<FaceLocation> event) {

        if (event.getMessage() == Type.FACE_RESULT) {

            Canvas canvas = null;
            try {
                synchronized (drawSurfaceHolder) {
                    canvas = drawSurfaceHolder.lockCanvas();//锁定画布，一般在锁定后就可以通过其返回的画布对象Canvas，在其上面画图等操作了。
                    canvas.save();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//设置画布背景颜色
                    Paint paint = new Paint(); //创建画笔
                    paint.setAntiAlias(true);
                    paint.setColor(Color.GREEN);
                    paint.setTextSize(50);
                    paint.setStrokeWidth(5);

                    FaceLocation fl = event.getBody();
                    // 校准识别框位置
                    float x = fl.getX() / Type.FACE_SCALE * 1.3f;// * 2.7f;
                    float y = fl.getY() / Type.FACE_SCALE * 1f;// * 2f;
                    float width = fl.getWidth() / Type.FACE_SCALE * 0.7f;// * 1.5f;
                    float height = fl.getHeight() / Type.FACE_SCALE * 0.7f;// * 1.5f;


                    /*float x = fl.getX() / Type.FACE_SCALE * 2.7f;
                    float y = fl.getY() / Type.FACE_SCALE * 2f;
                    float width = fl.getWidth() / Type.FACE_SCALE * 1.5f;
                    float height = fl.getHeight() / Type.FACE_SCALE * 1.5f;*/

                    // 前置摄像头需要做镜面校对
                    //x = x + (AppContext.getInstance().getScreenWidth() - x) * 2 - AppContext.getInstance().getScreenWidth() - width;
                    canvas.drawLine(x, y, x + width, y, paint);
                    canvas.drawLine(x, y, x, y + height, paint);
                    canvas.drawLine(x + width, y, x + width, y + height, paint);
                    canvas.drawLine(x, y + height, x + width, y + height, paint);

                    //binding.ivTempBmp.setImageBitmap(event.getBody().getFaceBitmap());
                    canvas.restore();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    drawSurfaceHolder.unlockCanvasAndPost(canvas);//结束锁定画图，并提交改变。
                    clearTimer = 0;
                }
            }

            matchRatioHandler.post(new Runnable() {
                @Override
                public void run() {
                    FaceLocation fl = event.getBody();
                    if (fl.getMatchRatio() > ratio) {
                        ratio = fl.getMatchRatio();
                    }
                    binding.tvMatchRatio.setText("识别率：" + ratio);
                    matchRatioHandler.removeCallbacks(this);
                }
            });
        }
    }

    /**
     * 加载未读消息数
     */
    private void initUnreadMessage() {

        if (count == 0) {
            binding.tvUnread.setVisibility(View.GONE);
        }

        final Handler unreadMessageHandler = new Handler();
        unreadMessageHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mMessageHistoryDaoHelp = new MessageHistoryDaoHelp(MainActivity.this);
                count = mMessageHistoryDaoHelp.getUnreadMessageCountByUserId(currentUser.getId());
                if (count == 0) {
                    binding.tvUnread.setVisibility(View.GONE);
                } else if (count > 99) {
                    binding.tvUnread.setVisibility(View.VISIBLE);
                    binding.tvUnread.setText("99");
                    playUnreadMessage();
                } else {
                    binding.tvUnread.setVisibility(View.VISIBLE);
                    binding.tvUnread.setText(count + "");
                    playUnreadMessage();
                }
                unreadMessageHandler.removeCallbacks(this);
            }
        }, 2000);
    }

    /**
     * 播放未读消息提示音
     */
    private void playUnreadMessage() {
        if (mSoundPool == null) {
            mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);//第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
            music = mSoundPool.load(this, R.raw.unread_message, 3);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 提示音        左声道(门外)   右声道(门内)
                    mSoundPool.play(music, 1.0f, 1.0f, 1, 0, 1);
                }
            }, 100);
        }
    }

    /**
     * 加载个人配置
     */
    private void initConfig(User currentUser) {

        switchLanguage(currentUser.getLanguage().equals(Type.LANGUAGE_ENGLISH) ? Locale.US : Locale.CHINESE);

        switchFontSize(currentUser.getFontSize());
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
    }

    /**
     * 获取字体大小
     */
    private void switchFontSize(int fontSize) {
        switch (fontSize) {
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
     * 清理内存
     */
    private void clearMemory() {

        if (gcTimer == 60) {
            File[] dir = getCacheDir().listFiles();
            if (dir != null) {
                for (File f : dir) {
                    f.delete();
                }
            }
            gcTimer = 0;
        }
        gcTimer++;
    }

    /**
     * 通过clearTimeJ检测清空人脸识别框
     */
    private void checkClearTime() {
        if (clearTimer == 2) {
            // 清空背景色
            synchronized (drawSurfaceHolder) {
                Canvas clearCanvas = drawSurfaceHolder.lockCanvas();
                if (clearCanvas != null) {
                    clearCanvas.save();
                    clearCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//设置画布背景颜色
                    clearCanvas.restore();
                    drawSurfaceHolder.unlockCanvasAndPost(clearCanvas);//结束锁定画图，并提交改变。
                }
            }
        }

        if (clearTimer >= 10 && binding.viewScanLine.getVisibility() == View.VISIBLE) {
            // 10秒后无人脸，则返回中段距离镜子模式
            hindAllView();
            countDistance = MIDDLE;
        }

        if (clearTimer <= 10) {
            clearTimer++;
        }
    }

    /**
     * 登录
     */
    private void login(User currentUser) {

        if (AppContext.getInstance().isLogin()) {
            return;
        }

        initValue();
        initView(currentUser);
        initUnreadMessage();
        initConfig(currentUser);
        AppContext.getInstance().setLogin(true);
        removeHomeKeyAnimation();
    }

    private void logout() {

        if (headViewList != null) {
            for (int i = 0; i < headViewList.size(); i++) {
                binding.llHead.removeView(headViewList.get(i));
            }
        }

        binding.llSmallSurface.setVisibility(View.GONE);
        binding.usbCameraSurfaceView.setVisibility(View.GONE);
        binding.llMyMenu.setVisibility(View.GONE);
        binding.tvUnread.setVisibility(View.GONE);

        removeGuestAnimation();
        AppContext.getInstance().setLogin(false);
        AppContext.getInstance().setCurrentUser(null);

        /*binding.viewScanLine.setVisibility(View.VISIBLE);
        binding.tvSeeScreen.setVisibility(View.VISIBLE);
        binding.ivFrame.setVisibility(View.VISIBLE);
        startScanLineAnimation();
        setFrameAnimation();

        startHomeKeyAnimation();*/

        binding.sdvMyHead.getHierarchy().setPlaceholderImage(R.drawable.ic_action_name);

        binding.roundProgressBar.setProgress(0);
        binding.sdvMyHead.setEnabled(false);
        removeMyHeadAnimation();

        // 重置人脸距离
        countDistance = -1;

        // 设置未读消息为空
        mSoundPool = null;

        ratio = 0;
        binding.tvMatchRatio.setText("识别率：" + ratio);

        // 切换为系统设置
        switchLanguage(ReservoirHelper.getLanguage().equals(Type.LANGUAGE_ENGLISH) ? Locale.US : Locale.CHINESE);
        switchFontSize(ReservoirHelper.getFontSize());

        AppContext.getInstance().clearMemory();
        AppContext.getInstance().clearAppCache();
    }


    /**
     * 导入数据
     */
    private void initValue() {

        headViewList = new ArrayList<>();
        mUserDaoHelp = new UserDaoHelp();
        userList = mUserDaoHelp.selectAllUser(this);
    }

    private void initView(User currentUser) {

        binding.sdvMyHead.setImageURI("file://" + currentUser.getHeadUrl());
        binding.sdvMyHead.setEnabled(false);    // 等下面handler加载完了之后它将会可点击
        binding.rlMyHead.setVisibility(View.VISIBLE);
        binding.ivSuccess.clearAnimation();

        binding.roundProgressBar.setVisibility(View.VISIBLE);
        binding.roundProgressBar.setMax(50);
        final Handler loadHandler = new Handler();
        loadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                timmer++;
                binding.roundProgressBar.setProgress(timmer);

                if (timmer == 50) {

                    for (int i = 0; i < userList.size(); i++) {
                        addUser(userList.get(i), i);
                    }

                    startSmallSurfaceAnimation();
                    startSuccessAnimation();
                    startGuestAnimation();
                    binding.sdvMyHead.setEnabled(true);

                    timmer = 0;
                    loadHandler.removeCallbacks(this);
                    return;
                }
                loadHandler.postDelayed(this, 5);
            }
        }, 5);


        binding.viewScanLine.clearAnimation();
        binding.viewScanLine.setVisibility(View.GONE);
        binding.tvSeeScreen.clearAnimation();
        binding.tvSeeScreen.setVisibility(View.GONE);
        binding.ivFrame.clearAnimation();
        binding.ivFrame.setVisibility(View.GONE);

        binding.sdvMyHead.getHierarchy().setPlaceholderImage(R.drawable.first);
    }

    /**
     * 动态添加一个用户
     */
    private void addUser(final User user, final int position) {

        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT;
        View view = inflater.inflate(R.layout.view_head_item, null);
        view.setLayoutParams(params);
        final ViewHeadItemBinding viewHeadItemBinding = DataBindingUtil.bind(view);
        viewHeadItemBinding.sdvHead.setImageURI("file://" + user.getHeadUrl());

        // 设置每个成员的头像点击事件
        viewHeadItemBinding.sdvHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                removeMyMenuAnimation();
                removeOtherUserMenu(position);

                if (viewHeadItemBinding.llUserMenu.getVisibility() == View.GONE) {
                    startUserMenuAnimation(viewHeadItemBinding);
                } else {
                    removeUserMenuAnimation(viewHeadItemBinding);
                }
                /*// 重置时间
                isLockTime = stopLockTime();

                for (int i = 0; i < headViewList.size(); i++) {

                    if (i != position) {
                        removeRightHeadAnimation(headViewList.get(i));
                    }
                }

                startTargetHeadAnimation(headViewList.get(position), position);

                removeMyHeadAnimation();
                removeSmallSufaceAnimation();

                binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                    @Override
                    public void onTakePictureFinish(Bitmap body) {
                        setBlur(body);
                    }
                });

                binding.tvCountDown.setVisibility(View.GONE);
                binding.tvCountDown.setText("3");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.cameraSurfaceView.destroyed();

                        MessageHistory messageHistory = new MessageHistory();
                        messageHistory.setSenderId(currentUser.getId());
                        messageHistory.setTargetId(user.getId());
                        messageHistory.setDatatime(DateUtils.getCurrentTime());

                        Intent intent = new Intent(MainActivity.this, CustomVideoActivity.class);
                        //intent.putExtra("position", position);
                        intent.putExtra("messageHistory", messageHistory);
                        startActivity(intent);
                    }
                }, 2000);*/
            }
        });

        // 设置每个成员的头像长按事件
        viewHeadItemBinding.sdvHead.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                // 重置时间
                isLockTime = stopLockTime();

                binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                    @Override
                    public void onTakePictureFinish(Bitmap body) {
                        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                        intent.putExtra("targetUser", user);
                        intent.putExtra("currentUser", currentUser);
                        AppContext.getInstance().setBlurBitmap(ImageUtils.rotateBitmap(body, 90, true));
                        startActivity(intent);
                    }
                });
                return true;
            }
        });

        viewHeadItemBinding.flMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 当能拍下照片的时候再去跳转页面
                binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                    @Override
                    public void onTakePictureFinish(Bitmap body) {
                        setBlur(body);

                        viewHeadItemBinding.llUserMenu.setVisibility(View.GONE);

                        // 重置时间
                        isLockTime = stopLockTime();

                        for (int i = 0; i < headViewList.size(); i++) {

                            if (i != position) {
                                removeRightHeadAnimation(headViewList.get(i));
                            }
                        }

                        startTargetHeadAnimation(headViewList.get(position), position);

                        removeGuestAnimation();
                        removeMyHeadAnimation();
                        removeSmallSufaceAnimation();

                        binding.tvCountDown.setVisibility(View.GONE);
                        binding.tvCountDown.setText("3");

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                binding.cameraSurfaceView.destroyed();

                                MessageHistory messageHistory = new MessageHistory();
                                messageHistory.setSenderId(currentUser.getId());
                                messageHistory.setTargetId(user.getId());
                                messageHistory.setDatatime(DateUtils.getCurrentTime());

                                Intent intent = new Intent(MainActivity.this, CustomVideoActivity.class);
                                //intent.putExtra("position", position);
                                intent.putExtra("messageHistory", messageHistory);
                                startActivity(intent);
                            }
                        }, 2000);
                    }
                });
            }
        });

        viewHeadItemBinding.flList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 重置时间
                isLockTime = stopLockTime();

                binding.cameraSurfaceView.takePicture(new CameraSurfaceView.OnTakePicture<Bitmap>() {
                    @Override
                    public void onTakePictureFinish(Bitmap body) {
                        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                        intent.putExtra("targetUser", user);
                        intent.putExtra("currentUser", currentUser);
                        AppContext.getInstance().setBlurBitmap(ImageUtils.rotateBitmap(body, 90, true));
                        startActivity(intent);
                    }
                });
            }
        });


        binding.llHead.addView(view);
        headViewList.add(view);
        setRightHeadLoginAnimation(view, position * 60);

        /*viewHeadItemBinding.sdvHead.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d("MainActivity", "-->> Key Down");
                        break;

                    case MotionEvent.ACTION_UP:

                        Log.d("MainActivity", "-->> Key Up");
                        EventBus.getDefault().post(new MessageEvent<Object>(Type.ON_MAIN_KEY_UP, new Object()));
                        break;
                }
                return false;
            }
        });*/
    }

    /**
     * 启动屏保
     */
    private void screensaver() {
        binding.rlScreensaver.setVisibility(View.VISIBLE);
        countDistance = -1;
        lockTime = LOCK_TIME;
    }

    /**
     * 锁屏
     */
    private void lockScreen() {

    }

    private void setFrameAnimation() {

        ScaleAnimation animation = new ScaleAnimation(
                1.0f, 1.07f, 1.0f, 1.07f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        );
        animation.setDuration(1000);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(Animation.INFINITE);
        binding.ivFrame.clearAnimation();
        binding.ivFrame.startAnimation(animation);
    }

    /**
     * 设置右侧头像登录时动画
     *
     * @param view 头像view
     */
    private void setRightHeadLoginAnimation(View view, int startOffset) {

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(200);
        animation.setStartOffset(startOffset);
        view.clearAnimation();
        view.startAnimation(animation);
    }

    /**
     * 成员item的菜单平移动画
     */
    private void startUserMenuAnimation(ViewHeadItemBinding viewHeadItemBinding) {

        viewHeadItemBinding.llUserMenu.setVisibility(View.VISIBLE);

        TranslateAnimation exitAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        exitAnimation.setDuration(200);

        TranslateAnimation messageAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 2.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        messageAnimation.setDuration(200);

        // 设置虚化层显示渐变动画
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        //设置动画持续时长
        alphaAnimation.setDuration(200);

        AnimationSet exitAnimationSet = new AnimationSet(true);
        exitAnimationSet.setFillAfter(true);  // 停留在动画结束的位置上
        exitAnimationSet.addAnimation(exitAnimation);
        exitAnimationSet.addAnimation(alphaAnimation);

        AnimationSet messageAnimationSet = new AnimationSet(true);
        messageAnimationSet.setFillAfter(true);  // 停留在动画结束的位置上
        messageAnimationSet.addAnimation(messageAnimation);
        messageAnimationSet.addAnimation(alphaAnimation);

        viewHeadItemBinding.flList.clearAnimation();
        viewHeadItemBinding.flList.startAnimation(exitAnimationSet);

        viewHeadItemBinding.flMessages.clearAnimation();
        viewHeadItemBinding.flMessages.startAnimation(messageAnimationSet);
    }

    /**
     * 启动访客头像平移动画
     */
    private void startGuestAnimation() {

        binding.llGuest.setVisibility(View.VISIBLE);
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(400);
        animation.setFillAfter(true);   // 停留在动画结束的位置上
        binding.llGuest.clearAnimation();
        binding.llGuest.startAnimation(animation);
    }

    /**
     * 我的菜单平移动画
     */
    private void startMyMenuAnimation() {

        isShowMyMenu = true;
        binding.llMyMenu.setVisibility(View.VISIBLE);

        TranslateAnimation messageAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -2.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        messageAnimation.setDuration(400);

        TranslateAnimation exitAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -3.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        exitAnimation.setDuration(400);

        // 设置虚化层显示渐变动画
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        //设置动画持续时长
        alphaAnimation.setDuration(200);

        AnimationSet messageAnimationSet = new AnimationSet(true);
        messageAnimationSet.setFillAfter(true);  // 停留在动画结束的位置上
        messageAnimationSet.addAnimation(messageAnimation);
        messageAnimationSet.addAnimation(alphaAnimation);

        AnimationSet exitAnimationSet = new AnimationSet(true);
        exitAnimationSet.setFillAfter(true);  // 停留在动画结束的位置上
        exitAnimationSet.addAnimation(exitAnimation);
        exitAnimationSet.addAnimation(alphaAnimation);

        binding.flMessages.clearAnimation();
        binding.flMessages.startAnimation(messageAnimationSet);

        binding.flExit.clearAnimation();
        binding.flExit.startAnimation(exitAnimationSet);
    }

    /**
     * 启动视频小窗口弹出动画
     */
    private void startSmallSurfaceAnimation() {

        binding.llSmallSurface.setVisibility(View.VISIBLE);
        binding.usbCameraSurfaceView.setVisibility(View.VISIBLE);
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(400);
        binding.llSmallSurface.clearAnimation();
        binding.llSmallSurface.startAnimation(animation);
    }

    /**
     * 启动home键动画
     */
    private void startHomeKeyAnimation() {

        binding.llLogin.setVisibility(View.VISIBLE);
        binding.llLogin.setEnabled(true);
        binding.llLogin.setClickable(true);
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(200);
        binding.llLogin.clearAnimation();
        binding.llLogin.startAnimation(animation);
    }

    private void startMyHeadAnimation() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(200);
        binding.rlMyHead.clearAnimation();
        binding.rlMyHead.startAnimation(animation);
    }

    private void startSuccessAnimation() {

        AlphaAnimation animationHide = new AlphaAnimation(1.0f, 0.0f);
        animationHide.setDuration(300);
        animationHide.setFillAfter(true);
        binding.ivSuccess.clearAnimation();
        binding.ivSuccess.startAnimation(animationHide);
    }

    private void startScanLineAnimation() {

        // 这里实时的获得一次屏幕高度进行同步匹配屏幕
        /*TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                //Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, (float) (AppContext.getInstance().getScreenHeight() / 4));
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.ABSOLUTE, ScreenUtils.getScreenHeight(this));
        animation.setDuration(3000);
        animation.setRepeatCount(Animation.INFINITE);
        binding.viewScanLine.clearAnimation();
        binding.viewScanLine.startAnimation(animation);*/
        binding.viewScanLine.setVisibility(View.VISIBLE);
        ObjectAnimator animator = ObjectAnimator.ofFloat(binding.viewScanLine, "translationY", -200, ScreenUtils.getScreenHeight(this));
        animator.setDuration(3000);
        animator.setRepeatCount(Animation.INFINITE);
        animator.start();
    }

    private void startTargetHeadAnimation(final View view, int position) {

        final float halfHeigh = AppContext.getInstance().getScreenHeight() / 2;
        final float halfWidth = AppContext.getInstance().getScreenWidth() / 2;

        final int[] locationHead = new int[2];
        view.getLocationOnScreen(locationHead);

        final AnimationSet animationSet = new AnimationSet(true);

        int headW = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int headH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.findViewById(R.id.line).measure(headW, headH);
        final float headWidth = view.findViewById(R.id.sdv_head).getMeasuredWidth();
        final float headHeight = view.findViewById(R.id.sdv_head).getMeasuredHeight();

        view.findViewById(R.id.line).setVisibility(View.INVISIBLE);
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, -(halfWidth - headWidth / 2 - DisplayUtils.dip2px(25, this)),
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, halfHeigh - locationHead[1] - headHeight / 2);
        translateAnimation.setDuration(700);

        final ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.5f, 1.0f, 1.5f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(700);    // 执行700毫秒
        //逐渐减速进入的动画  先快后慢
        scaleAnimation.setInterpolator(AnimationUtils.loadInterpolator(this,
                android.R.anim.decelerate_interpolator));

        // 向上平移
        TranslateAnimation translateAnimationTop = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, -(halfHeigh + headHeight));
        translateAnimationTop.setDuration(700);//设置动画持续时间
        translateAnimationTop.setStartOffset(1500); // 1.7秒后网上移动


        // 设置虚化层显示渐变动画
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        //设置动画持续时长
        alphaAnimation.setDuration(500);
        alphaAnimation.setStartOffset(1500);


        animationSet.setFillAfter(true);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        //animationSet.addAnimation(translateAnimationTop);
        //animationSet.addAnimation(scaleAnimationSmall);

        view.clearAnimation();
        view.startAnimation(animationSet);

    }


    /**
     * 显示文字展开动画
     */
    private void showTitleAnimation(View view) {
        view.setVisibility(View.VISIBLE);
        // 设置渐变动画（显示）
        AlphaAnimation animationHide = new AlphaAnimation(0.0f, 1.0f);
        //设置动画持续时长
        animationHide.setDuration(500);
        animationHide.setFillAfter(true);
        view.clearAnimation();
        view.startAnimation(animationHide);
    }

    /**
     * 显示所有组件，在切换到近距离时触发该效果
     */
    private void showAllView() {

        binding.ivFrame.setVisibility(View.VISIBLE);
        binding.viewScanLine.setVisibility(View.VISIBLE);
        setFrameAnimation();
        startScanLineAnimation();

        // 显示文字展开
        showTitleAnimation(binding.tvSeeScreen);
        startHomeKeyAnimation();
    }

    /**
     * 隐藏所有组件
     */
    private void hindAllView() {

        binding.ivFrame.clearAnimation();
        binding.ivFrame.setVisibility(View.GONE);
        binding.viewScanLine.clearAnimation();
        binding.viewScanLine.setVisibility(View.GONE);
        binding.tvSeeScreen.clearAnimation();
        binding.tvSeeScreen.setVisibility(View.GONE);
        binding.llLogin.setVisibility(View.GONE);
        binding.llLogin.setEnabled(false);
        binding.llLogin.setClickable(false);
    }

    /**
     * 右侧头像的移除动画
     *
     * @param view
     */
    private void removeRightHeadAnimation(View view) {

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(200);//设置动画持续时间
        animation.setFillAfter(true);   // 停留在动画结束的位置上
        view.clearAnimation();
        view.startAnimation(animation);
    }

    /**
     * 移除右侧其他弹出的菜单
     *
     * @param position 不想被移除的菜单，输入-1移除所有
     */
    private void removeOtherUserMenu(int position) {

        for (int i = 0; i < headViewList.size(); i++) {
            if (i != position) {
                ViewHeadItemBinding viewHeadItemBinding = DataBindingUtil.bind(headViewList.get(i));
                removeUserMenuAnimation(viewHeadItemBinding);
            }
        }
    }

    /**
     * 用户菜单移除动画
     */
    private void removeUserMenuAnimation(final ViewHeadItemBinding viewHeadItemBinding) {

        TranslateAnimation messageAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        messageAnimation.setDuration(200);

        TranslateAnimation exitAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 2f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        exitAnimation.setDuration(200);

        // 设置虚化层显示渐变动画
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        //设置动画持续时长
        alphaAnimation.setDuration(200);

        AnimationSet messageAnimationSet = new AnimationSet(true);
        messageAnimationSet.setFillAfter(true);  // 停留在动画结束的位置上
        messageAnimationSet.addAnimation(messageAnimation);
        messageAnimationSet.addAnimation(alphaAnimation);

        AnimationSet exitAnimationSet = new AnimationSet(true);
        exitAnimationSet.setFillAfter(true);  // 停留在动画结束的位置上
        exitAnimationSet.addAnimation(exitAnimation);
        exitAnimationSet.addAnimation(alphaAnimation);

        viewHeadItemBinding.flMessages.clearAnimation();
        viewHeadItemBinding.flMessages.startAnimation(messageAnimationSet);

        viewHeadItemBinding.flList.clearAnimation();
        viewHeadItemBinding.flList.startAnimation(exitAnimationSet);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewHeadItemBinding.llUserMenu.setVisibility(View.GONE);
            }
        }, 200);
    }

    /**
     * 访客头像移除动画
     */
    private void removeGuestAnimation() {

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(200);     //设置动画持续时间
        animation.setFillAfter(true);   // 停留在动画结束的位置上
        binding.llGuest.clearAnimation();
        binding.llGuest.startAnimation(animation);
    }

    /**
     * 我的菜单移除动画
     */
    private void removeMyMenuAnimation() {

        if (!isShowMyMenu) {
            return;
        }

        isShowMyMenu = false;
        TranslateAnimation messageAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -2f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        messageAnimation.setDuration(400);
        //messageAnimation.setFillAfter(true);   // 停留在动画结束的位置上

        TranslateAnimation exitAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -3f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        exitAnimation.setDuration(400);
        //exitAnimation.setFillAfter(true);   // 停留在动画结束的位置上

        // 设置虚化层显示渐变动画
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        //设置动画持续时长
        alphaAnimation.setDuration(200);

        AnimationSet messageAnimationSet = new AnimationSet(true);
        messageAnimationSet.setFillAfter(true);  // 停留在动画结束的位置上
        messageAnimationSet.addAnimation(messageAnimation);
        messageAnimationSet.addAnimation(alphaAnimation);

        AnimationSet exitAnimationSet = new AnimationSet(true);
        exitAnimationSet.setFillAfter(true);  // 停留在动画结束的位置上
        exitAnimationSet.addAnimation(exitAnimation);
        exitAnimationSet.addAnimation(alphaAnimation);

        binding.flMessages.clearAnimation();
        binding.flMessages.startAnimation(messageAnimationSet);

        binding.flExit.clearAnimation();
        binding.flExit.startAnimation(exitAnimationSet);
    }

    /**
     * 我的头像移除动画
     */
    private void removeMyHeadAnimation() {

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.3f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(200);//设置动画持续时间
        animation.setFillAfter(true);   // 停留在动画结束的位置上
        binding.rlMyHead.clearAnimation();
        binding.rlMyHead.startAnimation(animation);

    }

    /**
     * 小视频窗体移除动画
     */
    private void removeSmallSufaceAnimation() {

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        animation.setDuration(200);     //设置动画持续时间
        animation.setFillAfter(true);   // 停留在动画结束的位置上
        binding.llSmallSurface.clearAnimation();
        binding.llSmallSurface.startAnimation(animation);
    }

    /**
     * 隐藏home键
     */
    private void removeHomeKeyAnimation() {

        binding.llLogin.setEnabled(false);
        binding.llLogin.setClickable(false);

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        animation.setDuration(200);     //设置动画持续时间
        animation.setFillAfter(true);   // 停留在动画结束的位置上
        binding.llLogin.clearAnimation();
        binding.llLogin.startAnimation(animation);
    }

    /**
     * 设置该activity页面背景高斯模糊
     */
    private void setBlur(Bitmap bitmap) {

        /*final View decorView = getWindow().getDecorView();
        //Activity's root View. Can also be root View of your layout (preferably)
        final ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
        //set background, if your root layout doesn't have one
        final Drawable windowBackground = decorView.getBackground();

        binding.blurView.setupWith(rootView)
                .windowBackground(windowBackground)
                .blurAlgorithm(new RenderScriptBlur(this))
                .blurRadius(8);
*/
        //binding.ivBackground.getHierarchy().setPlaceholderImage(new BitmapDrawable(ImageUtils.rotateBitmap(bitmap, -90)));
        Bitmap blurBmp = ImageUtils.blurBitmap(this, bitmap, 20);
        binding.ivBackground.setBackground(new BitmapDrawable(ImageUtils.rotateBitmap(blurBmp, 90, true)));

        //binding.blurView.setVisibility(View.VISIBLE);
        binding.ivBackground.setVisibility(View.VISIBLE);

        // 设置虚化层显示渐变动画
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        //设置动画持续时长
        alphaAnimation.setDuration(200);
        //设置动画结束之后的状态是否是动画的最终状态，true，表示是保持动画结束时的最终状态
        alphaAnimation.setFillAfter(true);
        //binding.blurView.clearAnimation();
        //binding.blurView.startAnimation(alphaAnimation);
        binding.ivBackground.clearAnimation();
        binding.ivBackground.startAnimation(alphaAnimation);
    }

    //界面结束淡出动画
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 重置时间
        isLockTime = stopLockTime();

        binding.cameraSurfaceView.destroyed();
        binding.llSmallSurface.setVisibility(View.GONE);
        binding.usbCameraSurfaceView.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 设置为未登录
        //AppContext.getInstance().setLogin(false);

        //logout();
        //EventBus.getDefault().unregister(this);
        Log.d(TAG, "-->> onDestory");
        AppContext.getInstance().releaseSerialAPI();
        logout();
    }
}
