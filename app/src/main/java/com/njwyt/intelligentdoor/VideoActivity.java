package com.njwyt.intelligentdoor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.njwyt.AppContext;
import com.njwyt.db.MessageHistoryDaoHelp;
import com.njwyt.entity.MessageHistory;
import com.njwyt.entity.User;
import com.njwyt.utils.ToastUtil;
import com.njwyt.view.IconTextView;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

@SuppressLint("NewApi")
public class VideoActivity extends BaseActivity implements TextureView.SurfaceTextureListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
    @BindView(R.id.surface)
    TextureView mTextureView;
    //播放进度条
    @BindView(R.id.seek_bar)
    SeekBar seekBar;
    //播放时间
    @BindView(R.id.time_left)
    TextView timeLeft;
    //总时间
    @BindView(R.id.time_right)
    TextView timeRight;
    //时间加进度条的布局
    @BindView(R.id.seek_and_time)
    LinearLayout seekAndTime;
    //上一个按钮
    @BindView(R.id.video_last)
    IconTextView videoLast;
    //暂停播放按钮
    @BindView(R.id.video_play)
    IconTextView videoPlay;
    //下一个按钮
    @BindView(R.id.video_next)
    IconTextView videoNext;
    //控制台布局
    @BindView(R.id.control_layout)
    RelativeLayout controlLayout;
    //重播按钮
    @BindView(R.id.video_again)
    IconTextView videoAgain;
    //半透明黑色背景
    @BindView(R.id.back_view)
    View backView;
    //关闭按钮
    @BindView(R.id.video_close)
    IconTextView videoClose;
    //音量亮度图标
    @BindView(R.id.icon_view)
    IconTextView iconView;
    //音量亮度百分比
    @BindView(R.id.icon_text)
    TextView iconText;
    //音量亮度布局
    @BindView(R.id.digital_layout)
    LinearLayout digitalLayout;

    private Unbinder bind;
    private MediaPlayer mMediaPlayer;
    private String path;
    private Surface surf;
    private boolean mIsVideoReadyToBePlayed = false;
    private Uri uri;
    //是否正在播放
    private boolean isPlay = true;
    //控制台是否显示
    private boolean isControl = false;
    //视频是否播放完成
    private boolean isPlayFinish = false;
    //是否从my进入
    private boolean isMyHistory;

    //点击纵坐标
    private float dY = 0;
    //点击横坐标
    private float dX = 0;
    //抬起纵坐标
    private float uY = 0;
    //抬起横坐标
    private float uX = 0;
    //媒体音量管理
    private AudioManager audioManager;
    //屏幕当前亮度百分比
    private float f = 0;
    //手机当前亮度模式 0 1
    private int countLight;
    //系统当前亮度 1-255
    private int currLight;
    //UI界面改变,进度条,时间跳动
    private Handler mHandler;
    //    Handler mHandler = new Handler() {
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case -1:
//                    refreshControlLayout();
//                    break;
//                case 0:
//                    int currentPosition = mMediaPlayer.getCurrentPosition();
//                    seekBar.setProgress(currentPosition);
//                    String time = formatTime(currentPosition);
//                    timeLeft.setText(time);
//                    mHandler.sendEmptyMessageDelayed(0, 10);
//                    break;
//            }
//
//        }
//    };
    //当前登录用户的id
    private Long currentId;
    //视频接受者id
    private long targetId;
    private MessageHistory messageHistoryById;
    private int dateSelectedId;
    private int historySelectedId;
    private MessageHistoryDaoHelp messageHistoryDaoHelp;
    private double MaxSound;
    private float uX1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        bind = ButterKnife.bind(this);
        mHandler = new Handler();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //系统最大音量
        MaxSound = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        init();
        initPath();
    }


    private void initPath() {
        //当前登录用户
        User currentUser = AppContext.getInstance().getCurrentUser();
        //获得当前登录用户的id
        currentId = currentUser.getId();
        targetId = getIntent().getLongExtra("targetId", -1);
        dateSelectedId = getIntent().getIntExtra("dateSelectedId", 0);
        historySelectedId = getIntent().getIntExtra("position", 0);
        Log.i("==>>:targetId:", targetId + "");
        Log.i("==>>:dateSelectedId:", dateSelectedId + "");
        Log.i("==>>:historySelectedId:", historySelectedId + "");
        if (targetId == -1) {
            //从左侧进入
            isMyHistory = true;
        } else {
            //从右侧进入
            isMyHistory = false;
        }
    }

    private void init() {
        refreshControlLayout();


        videoAgain.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN://按住事件发生后执行代码的区域
                        videoAgain.setTextSize(45f);
                        break;
                    case MotionEvent.ACTION_UP://松开事件发生后执行代码的区域
                        videoAgain.setTextSize(50f);
                        break;
                }
                return false;
            }
        });
        mTextureView.setSurfaceTextureListener(this);
        //旋转
        mTextureView.setRotation(180);
        //拉伸
//        mTextureView.setScaleX(1280f / 805);
//        mTextureView.setScaleY(805f / 1280);
        //VIDEO_SCALING_MODE_SCALE_TO_FIT：适应屏幕显示
        //VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING：充满屏幕显示，保持比例，如果屏幕比例不对，则进行裁剪
//        mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

        //获得视频的uri
        //一条视频的数据
        MessageHistory messageHistory = (MessageHistory) getIntent().getSerializableExtra("messageHistory");
        messageHistoryDaoHelp = new MessageHistoryDaoHelp(this);
        messageHistoryById = messageHistoryDaoHelp.getMessageHistoryById(messageHistory.getUuid());
        System.out.println("发送者id>>>>>>>>>>>>" + messageHistoryById.getSenderId()
                + "\n接收者id>>>>>>>>>>>>" + messageHistoryById.getTargetId()
                + "\n时间>>>>>>>>>>>>" + messageHistoryById.getDatatime()
                + "\n视频地址>>>>>>>>>>>>" + messageHistoryById.getVideoUrl()
                + "\n是否已读>>>>>>>>>>>>" + messageHistoryById.getIsRead()
                + "\n视频时长>>>>>>>>>>>>" + messageHistoryById.getDuration()
                + "\nuuid>>>>>>>>>>>>" + messageHistoryById.getUuid()
                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        messageHistoryById.setIsRead(true);
        messageHistoryDaoHelp.upDataMessageHistory(messageHistoryById);

        File file = new File(messageHistoryById.getVideoUrl());
        uri = Uri.fromFile(file);
        initScreenLight();

        //手势控制
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    //按下
                    case MotionEvent.ACTION_DOWN:
                        dX = motionEvent.getX();
                        dY = motionEvent.getY();
                        uX1 = dX;
                        if (dY <= getHeight() / 2) {//声音控制
                            //获取当前音量
                            double currentSount = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            double i = currentSount / MaxSound;
                            if (i == 0) {
                                iconView.setText(R.string.ic_volume_no);
                            } else {
                                iconView.setText(R.string.ic_volume);
                            }
                            //设置百分比
                            iconText.setText(doubleToString(i) + "");
                        } else if (dY > getHeight() / 2) {//亮度控制
                            iconView.setText(R.string.ic_sun);
                            //设置百分比
                            iconText.setText(doubleToString(f));
                        }
                        break;
                    //抬起
                    case MotionEvent.ACTION_UP:
                        digitalLayout.setVisibility(View.GONE);
                        break;
                    //移动
                    case MotionEvent.ACTION_MOVE:
                        uY = motionEvent.getY();
                        uX = motionEvent.getX();
                        if (uX == uX1) {
                            Log.i("--==", "滑动停止");
                        } else {
                            Log.i("--==", "正在滑动");
                            if (dY <= getHeight() / 2) {//声音控制
                                if (Math.abs(uX1 - uX) > 3)
                                    setVolume(uX1 - uX);
                            } else if (dY > getHeight() / 2) {//亮度控制
                                if (Math.abs(uX1 - uX) > 1)
                                    setLight(uX1 - uX);
                            }
                            uX1 = uX;
                        }

                        break;
                }
                return false;
            }
        });
    }


    //初始化屏幕亮度
    private void initScreenLight() {
        try {
            //获取亮度模式 0：手动 1：自动
            countLight = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
            //设置手动设置
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            //获取屏幕亮度,获取失败则返回255
            currLight = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    255);
            f = currLight / 255f;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    //手势调节音量
    private void setVolume(float vol) {
        digitalLayout.setVisibility(View.VISIBLE);
        if (vol > 0) {//增大音量
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                    0);
        } else if (vol < 0) {//降低音量
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                    0);
        } else if (vol == 0) {

        }

        //----------------------
//        f += vol / getWidth();
//        if (f > 1) {
//            f = 15f;
//        } else if (f <= 0) {
//            f = 0.000f;
//        }
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) f, 0);

        //获取当前音量
        double currentSount = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        double i = currentSount / MaxSound;
        if (i == 0) {
            iconView.setText(R.string.ic_volume_no);
        } else {
            iconView.setText(R.string.ic_volume);
        }
        //设置百分比
        iconText.setText(doubleToString(i) + "");

        //音量控制Bar的当前值设置为系统音量当前值
//        volumeProgressBar.setProgress(currentSount);
    }

    /**
     * double转String,保留小数点后两位
     *
     * @param num
     * @return
     */
    public static String doubleToString(double num) {
        double v = num * 100;
        //使用0.00不足位补0，#.##仅保留有效位
        return new DecimalFormat("0").format(v);
    }

    /**
     * 手势设置屏幕亮度
     * 设置当前的屏幕亮度值，及时生效 0.004-1
     * 该方法仅对当前应用屏幕亮度生效
     */
    private void setLight(float vol) {
        digitalLayout.setVisibility(View.VISIBLE);
        Window localWindow = getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        f += (vol / getWidth()) * 4;
        if (f > 1) {
            f = 1f;
        } else if (f <= 0) {
            f = 0.000f;
        }
        localLayoutParams.screenBrightness = f;
        localWindow.setAttributes(localLayoutParams);
        iconView.setText(R.string.ic_sun);
        //设置百分比
        iconText.setText(doubleToString(f));
    }

    public int getWidth() {
        WindowManager manager = getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    public int getHeight() {
        WindowManager manager = getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    @OnClick({R.id.video_last, R.id.video_play, R.id.video_next, R.id.surface, R.id.video_again, R.id.video_close})
    public void onViewClicked(View view) {
//        if (isControl){
//            isControl = true;
//            refreshControlLayout();
//        }else {
//        isControl = false;
//        refreshControlLayout();
//        }
        switch (view.getId()) {
            //播放暂停按钮
            case R.id.video_play:
                if (isPlayFinish) {
                    //如果播放完成
                    isPlayFinish = false;
//                    if (isMyHistory) {
//                        //重新获得视频地址
//                        path = nextVideoPathForMyHistory();
//                    } else {
//                        path = nextVideoPathForHistory();
//                    }
//                    mMediaPlayer.reset();
//                    try {
//                        mMediaPlayer.setDataSource(path);
//                        mMediaPlayer.prepare();
//                        mMediaPlayer.start();
//                        isPlay = true;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    isPlay=true;
                    mMediaPlayer.start();
                    visibleButton();
                    mHandler.post(seekAndTimeRunnable);
                    videoPlay.setText(R.string.ic_video_suspend);
                } else {
                    //如果正在播放
                    if (isPlay) {
                        //暂停
                        mMediaPlayer.pause();
                        videoPlay.setText(R.string.ic_video_play);
                        isPlay = false;
                    } else {
                        //播放
                        mMediaPlayer.start();
                        videoPlay.setText(R.string.ic_video_suspend);
                        isPlay = true;
                    }
                }
                break;
            //上一个按钮
            case R.id.video_last:
                //获得视频地址
                if (isMyHistory) {
                    //重新获得视频地址
                    historySelectedId--;
                    path = nextVideoPathForMyHistory();
                } else {
                    historySelectedId--;
                    path = nextVideoPathForHistory();
                }
                if (path == null) {
                    return;
                } else {
                    mMediaPlayer.reset();
                    try {
                        mMediaPlayer.setDataSource(path);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                        isPlay = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    videoPlay.setText(R.string.ic_video_suspend);
//                    Toast.makeText(this, "上一个", Toast.LENGTH_SHORT).show();
                }
                break;
            //下一个按钮
            case R.id.video_next:
                if (isMyHistory) {
                    //重新获得视频地址
                    historySelectedId++;
                    path = nextVideoPathForMyHistory();
                } else {
                    historySelectedId++;
                    path = nextVideoPathForHistory();
                }
                Log.i("==>>historySelectedId:", historySelectedId + "");
                if (path == null) {
                    return;
                } else {

                    mMediaPlayer.reset();
                    try {
                        mMediaPlayer.setDataSource(path);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                        isPlay = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    videoPlay.setText(R.string.ic_video_suspend);
//                    Toast.makeText(this, "下一个", Toast.LENGTH_SHORT).show();
                }
                break;
            //重播按钮
            case R.id.video_again:
                isPlayFinish = false;
                if (isMyHistory) {
                    //重新获得视频地址
                    path = nextVideoPathForMyHistory();
                } else {
                    path = nextVideoPathForHistory();
                }
                mMediaPlayer.reset();
                try {
                    mMediaPlayer.setDataSource(path);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                    isPlay = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                videoPlay.setText(R.string.ic_video_suspend);
                isPlay = true;
                break;
            case R.id.surface:
                mHandler.removeCallbacks(refreshControlRunnable);
//                mHandler.removeMessages(-1);
                refreshControlLayout();
                //控制台显示与隐藏
//                if (isControl){
//                    //显示就隐藏
//                    isControl=true;
//                    refreshControlLayout();
//                }else {
//                    //隐藏就显示
//                    isControl=false;
//                    refreshControlLayout();
//                }
                break;
            //关闭
            case R.id.video_close:
                mHandler.removeCallbacks(seekAndTimeRunnable);
                mHandler.removeCallbacks(refreshControlRunnable);
//                mHandler.removeMessages(0);
//                mHandler.removeMessages(-1);
                finish();
                break;
        }
    }

    @SuppressLint("NewApi")
    private void playVideo(SurfaceTexture surfaceTexture) {
        mIsVideoReadyToBePlayed = false;
        try {
            path = String.valueOf(uri);
            if (path == null) {
                return;
            }
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(path);
            if (surf == null) {
                surf = new Surface(surfaceTexture);
            }
            mMediaPlayer.setSurface(surf);
            mMediaPlayer.prepareAsync();
            //监听事件，网络流媒体的缓冲监听
            mMediaPlayer.setOnBufferingUpdateListener(this);
            //监听事件，网络流媒体播放结束监听
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            // 左右声道控制         左声道(门外)   右声道(门内)
            mMediaPlayer.setVolume(0, 1);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
        }
    }

    //开始播放
    private void startVideoPlayback() {
        mMediaPlayer.start();
        isPlay = true;
        visibleButton();
    }

    //刷新控制台 显示则隐藏 隐藏则显示 并5S之后隐藏
    private void refreshControlLayout() {
        if (isControl) {
            controlLayout.setVisibility(View.INVISIBLE);
            isControl = false;
        } else {
            controlLayout.setVisibility(View.VISIBLE);
            isControl = true;
            mHandler.removeCallbacks(refreshControlRunnable);
            mHandler.postDelayed(refreshControlRunnable, 5000);
//            mHandler.removeMessages(-1);
//            mHandler.sendEmptyMessageDelayed(-1, 5000);
        }
    }

    //隐藏重播按钮
    private void visibleButton() {
        if (isPlay) {
            backView.setVisibility(View.INVISIBLE);
            videoAgain.setVisibility(View.GONE);
        }
    }

    //释放MediaPlayer资源
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    //时间格式
    private String formatTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        return format.format(time);
    }

    //准备完成
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mIsVideoReadyToBePlayed = true;
        if (mIsVideoReadyToBePlayed) {
            startVideoPlayback();
        }
        //计算视频的长度
        int position = mMediaPlayer.getDuration();
        timeRight.setText(formatTime(position));
        seekBar.setMax(position);
//        mHandler.removeCallbacks(seekAndTimeRunnable);
        mHandler.post(seekAndTimeRunnable);
//        mHandler.removeMessages(0);
//        mHandler.sendEmptyMessageDelayed(0, 10);
        final View.OnTouchListener seekBarTouchListener=new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    //按下
                    case MotionEvent.ACTION_DOWN:
                        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                        mHandler.removeCallbacks(seekAndTimeRunnable);
                        break;
                    //抬起
                    case MotionEvent.ACTION_UP:
                        seekBar.setOnSeekBarChangeListener(null);
                        mHandler.post(seekAndTimeRunnable);
                        break;
                    //移动
                    case MotionEvent.ACTION_MOVE:
                        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                        mHandler.removeCallbacks(seekAndTimeRunnable);
                        break;
                }
                return false;
            }
        };
        seekBar.setOnTouchListener(seekBarTouchListener);
//        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
    }

    //播放完成
    @SuppressLint("ResourceAsColor")
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        isPlayFinish = true;
        videoPlay.setText(R.string.ic_video_play);
        isPlay = false;
        visibleButton();
        //屏幕变灰效果
        backView.setVisibility(View.VISIBLE);
        backView.setAlpha(0.5f);
        videoAgain.setVisibility(View.VISIBLE);
        isControl = false;
        refreshControlLayout();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        playVideo(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    //缓冲中
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        ToastUtil.showToast(this, getString(R.string.isBuffering));
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(seekAndTimeRunnable);
        mHandler.removeCallbacks(refreshControlRunnable);
//        mHandler.removeMessages(0);
//        mHandler.removeMessages(-1);
        super.onPause();
        releaseMediaPlayer();
        mIsVideoReadyToBePlayed = false;
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(seekAndTimeRunnable);
        mHandler.removeCallbacks(refreshControlRunnable);
//        mHandler.removeMessages(0);
//        mHandler.removeMessages(-1);
        super.onDestroy();
        mIsVideoReadyToBePlayed = false;
        releaseMediaPlayer();
//        bind.unbind();
    }

    /**
     * 根据发送者id查询到关于他的所有视频的日期列表
     *
     * @param dateSelected 日期
     * @return
     */
    public List<MessageHistory> getvideoPathList(int dateSelected) {
        //查询日期斌放入集合
        MessageHistoryDaoHelp messageHistoryDaoHelp = new MessageHistoryDaoHelp(this);
        List<String> messageHistoryDate = messageHistoryDaoHelp.getMessageHistoryDateListByUserId(currentId);
        //如果视频放到最后一个了
        if (dateSelected >= messageHistoryDate.size()) {
            //重置日期
            dateSelectedId = messageHistoryDate.size() - 1;
            ToastUtil.showToast(this, getString(R.string.isTheLastOne));
            return null;
        }
        //如果视频放到第一个了
        if (dateSelected < 0) {
            dateSelectedId = 0;
            ToastUtil.showToast(this, getString(R.string.isTheFirstOne));
            return null;
        }
        //当天所有的视频信息
        return messageHistoryDaoHelp.getMessageHistoryListByUserId(currentId, messageHistoryDate.get(dateSelected));
        //把当天的所有视频地址放入一个集合
//        List<String> videoPathList = new ArrayList<>();
//        for (int i = 0; i < allHistoryList.size(); i++) {
//            videoPathList.add(allHistoryList.get(i).getVideoUrl());
//        }
//
//        return videoPathList;
    }

    //查询关于我的所有视频地址(从My进入)
    public String nextVideoPathForMyHistory() {
        Log.i("==>>下一条的序列号", historySelectedId + "");
        Log.i("==>>下一条日期的序列号", historySelectedId + "");
        List<MessageHistory> videoPathList = getvideoPathList(dateSelectedId);
        if (historySelectedId >= videoPathList.size()) {
            dateSelectedId++;
        }
        if (historySelectedId < 0) {
            dateSelectedId--;
        }
        List<MessageHistory> pathList = getvideoPathList(dateSelectedId);

        if (pathList == null) {
            if (historySelectedId >= videoPathList.size()) {
                historySelectedId = videoPathList.size() - 1;
            }
            if (historySelectedId < 0) {
                historySelectedId = 0;
            }
            return null;
        } else {
            if (historySelectedId >= videoPathList.size()) {
                historySelectedId = 0;
            }
            if (historySelectedId < 0) {
                historySelectedId = pathList.size() - 1;
            }
            Log.i("==>>最后处理完的序列号", historySelectedId + "");
            Log.i("==>最后处理完的日期的序列号", dateSelectedId + "");
            MessageHistory messageHistory = pathList.get(historySelectedId);
            //把视频标位已读
            messageHistory.setIsRead(true);
            messageHistoryDaoHelp.upDataMessageHistory(messageHistory);
            return messageHistory.getVideoUrl();
        }
    }

    /**
     * 根据发送者和接受者id查询到关于所有视频的日期列表
     *
     * @param dateSelected 日期
     * @return
     */
    public List<MessageHistory> getvideoPathListForHistory(int dateSelected) {
        //查询日期斌放入集合
        MessageHistoryDaoHelp messageHistoryDaoHelp = new MessageHistoryDaoHelp(this);
        List<String> messageHistoryDate = messageHistoryDaoHelp.getMessageHistoryDateList(currentId, targetId);
        //如果视频放到最后一个了
        if (dateSelected >= messageHistoryDate.size()) {
            //重置日期
            dateSelectedId = messageHistoryDate.size() - 1;
            ToastUtil.showToast(this, getString(R.string.isTheLastOne));
            return null;
        }
        //如果视频放到第一个了
        if (dateSelected < 0) {
            dateSelectedId = 0;
            ToastUtil.showToast(this, getString(R.string.isTheFirstOne));
            return null;
        }
        //当天所有的视频信息
        return messageHistoryDaoHelp.getMessageHistoryListByTargetId(currentId, targetId, messageHistoryDate.get(dateSelected));
        //把当天的所有视频地址放入一个集合
//        List<String> videoPathList = new ArrayList<>();
//        for (int i = 0; i < allHistoryList.size(); i++) {
//            videoPathList.add(allHistoryList.get(i).getVideoUrl());
//        }
        //return videoPathList;
    }

    //查询关于我的所有视频地址()
    public String nextVideoPathForHistory() {
        Log.i("==>>得到:", historySelectedId + "");
        Log.i("==>>dateSelected:", dateSelectedId + "");
        List<MessageHistory> videoPathList = getvideoPathListForHistory(dateSelectedId);
        if (historySelectedId >= videoPathList.size()) {
            dateSelectedId++;
        }
        if (historySelectedId < 0) {
            dateSelectedId--;
        }
        List<MessageHistory> pathList = getvideoPathListForHistory(dateSelectedId);
        if (pathList == null) {
            if (historySelectedId >= videoPathList.size()) {
                historySelectedId = videoPathList.size() - 1;
            }
            if (historySelectedId < 0) {
                historySelectedId = 0;
            }
            return null;
        } else {
            if (historySelectedId >= videoPathList.size()) {
                historySelectedId = 0;
            }
            if (historySelectedId < 0) {
                historySelectedId = pathList.size() - 1;
            }
            Log.i("==>>position2:", historySelectedId + "");
            Log.i("==>>dateSelected:", dateSelectedId + "");
            MessageHistory messageHistory = pathList.get(historySelectedId);
            //把视频标位已读
            messageHistory.setIsRead(true);
            messageHistoryDaoHelp.upDataMessageHistory(messageHistory);
            return messageHistory.getVideoUrl();
        }
    }


    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            Log.i("--==>>",seekBar.getProgress()+"");
            mMediaPlayer.seekTo(seekBar.getProgress());
            timeLeft.setText(formatTime(seekBar.getProgress()));
            if (timeLeft != timeRight) {
                backView.setVisibility(View.INVISIBLE);
                videoAgain.setVisibility(View.GONE);
            } else {
                //屏幕变灰效果
                backView.setVisibility(View.VISIBLE);
                backView.setAlpha(0.5f);
                videoAgain.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            timeLeft.setText(formatTime(seekBar.getProgress()));
            visibleButton();
            isControl = false;
            refreshControlLayout();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            timeLeft.setText(formatTime(seekBar.getProgress()));
            visibleButton();
            isControl = false;
            refreshControlLayout();
        }
    };

    private Runnable seekAndTimeRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO: 2018/3/22  
            int currentPosition = mMediaPlayer.getCurrentPosition();
            seekBar.setProgress(currentPosition);
            String time = formatTime(currentPosition);
            timeLeft.setText(time);
            mHandler.postDelayed(seekAndTimeRunnable,10);
        }
    };

    private Runnable refreshControlRunnable = new Runnable() {

        @Override
        public void run() {
            refreshControlLayout();
        }
    };


//    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
//        @Override
//        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//            if (b) {
//                int SeekPosition=seekBar.getProgress();
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SeekPosition, 0);
//            }
//        }
//
//        @Override
//        public void onStartTrackingTouch(SeekBar seekBar) {
//
//        }
//
//        @Override
//        public void onStopTrackingTouch(SeekBar seekBar) {
//
//        }
//    }
}
