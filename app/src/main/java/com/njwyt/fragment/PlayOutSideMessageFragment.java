package com.njwyt.fragment;

import android.app.Fragment;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.njwyt.entity.GuestRecording;
import com.njwyt.intelligentdoor.R;
import com.njwyt.utils.DateUtils;
import com.njwyt.view.IconTextView;

import java.io.IOException;
import java.text.SimpleDateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by Administrator on 2018/1/2.
 */

public class PlayOutSideMessageFragment extends Fragment implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener{

    //头像
    @BindView(R.id.recording_head)
    ImageView recordingHead;
    //标题
    @BindView(R.id.play_recording_title)
    TextView playRecordingTitle;
    //进度条
    @BindView(R.id.play_recording_seek_bar)
    SeekBar seekBar;
    //播放时间
    @BindView(R.id.play_recording_time_left)
    TextView timeLeft;
    //总时间
    @BindView(R.id.play_recording_time_right)
    TextView timeRight;
    //进度条布局
    @BindView(R.id.seek_and_time)
    LinearLayout seekAndTime;
    //    //音量键
//    @BindView(R.id.ic_volum)
//    IconTextView icVolum;
//    //上一个
//    @BindView(R.id.video_last)
//    IconTextView videoLast;
    //暂停
    @BindView(R.id.video_play)
    IconTextView videoPlay;
    //    //下一个
//    @BindView(R.id.video_next)
//    IconTextView videoNext;
    //总布局
    @BindView(R.id.root_layout)
    FrameLayout rootLayout;
    //结束
    @BindView(R.id.icon_finish)
    IconTextView iconFinish;
    //音量加
    @BindView(R.id.volum_plus)
    IconTextView volumPlus;
    //音量进度条
    @BindView(R.id.volum_progress)
    ProgressBar volumProgress;
    //音量减
    @BindView(R.id.volum_reduce)
    IconTextView volumReduce;
    //fragment布局
    @BindView(R.id.fragment_layout)
    RelativeLayout fragmentLayout;
    Unbinder unbinder;
    private View view;//布局
    private MediaPlayer mMediaPlayer;
    private String path;
    //是否正在播放
    private boolean isPlay = true;
    //是否播放完成
    private boolean isPlayFinish = false;
    private Handler mHandler;
    //系统最大音量
    private int MaxSound;
    //媒体音量管理
    private AudioManager audioManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_play_outside_message, container, false);
        unbinder = ButterKnife.bind(this, view);
        mHandler = new Handler();
        fragmentLayout.setOnClickListener(null);
        initVolum();
        initMedia();
        return view;
    }

    private void initVolum() {
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        //系统最大音量
        MaxSound = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //音量控制Bar的最大值设置为系统音量最大值
        volumProgress.setMax(MaxSound);
        changeVolum();
    }

    private void changeVolum() {
        //获取当前音量
        int currentSount = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //音量控制Bar的当前值设置为系统音量当前值
        volumProgress.setProgress(currentSount);
    }

    private void initMedia() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        // 左右声道控制         左声道(门外)   右声道(门内)
        mMediaPlayer.setVolume(0, 1);
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

    }

    //开始播放
    public void startPlay(GuestRecording guestRecording) {
        //隐藏Fragment
        getFragmentManager().beginTransaction().show(this).commit();

        System.out.println("访客照片地址>>>>>>>>>>>>" + guestRecording.getGuestPicUrl()
                + "\n来访时间>>>>>>>>>>>>" + guestRecording.getDatatime()
                + "\n录音地址>>>>>>>>>>>>" + guestRecording.getRecordingUrl()
                + "\nuuid>>>>>>>>>>>>" + guestRecording.getUuid()
                + "\n是否已读>>>>>>>>>>>>" + guestRecording.getIsRead()
                + "\n录音时长>>>>>>>>>>>>" + guestRecording.getDuration()
                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        path = guestRecording.getRecordingUrl();
        playRecordingTitle.setText(DateUtils.getHourMinute(guestRecording.getDatatime()));
        recordingHead.setImageBitmap(BitmapFactory.decodeFile(guestRecording.getGuestPicUrl()));
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.start();
        isPlay = true;
        videoPlay.setText(R.string.ic_video_suspend);

        //计算视频的长度
        int position = mMediaPlayer.getDuration();
        timeRight.setText(formatTime(position));
        seekBar.setMax(position);
        mHandler.post(seekAndTimeRunnable);
        View.OnTouchListener seekBarTouchListener=new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    //按下
                    case MotionEvent.ACTION_DOWN:
                        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                        break;
                    //抬起
                    case MotionEvent.ACTION_UP:
                        seekBar.setOnSeekBarChangeListener(null);
                        break;
                    //移动
                    case MotionEvent.ACTION_MOVE:
                        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                        break;
                }
                return false;
            }
        };
        seekBar.setOnTouchListener(seekBarTouchListener);

    }

    private Runnable seekAndTimeRunnable = new Runnable() {

        @Override
        public void run() {
            int currentPosition = mMediaPlayer.getCurrentPosition();
            seekBar.setProgress(currentPosition);
            String time = formatTime(currentPosition);
            timeLeft.setText(time);
            mHandler.postDelayed(seekAndTimeRunnable, 10);
        }
    };


    //时间格式
    private String formatTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        return format.format(time);
    }

    //停止播放
    public void stopPlay() {
        mMediaPlayer.stop();
        isPlay = false;
        getFragmentManager().beginTransaction().hide(this).commit();
        mHandler.removeCallbacks(seekAndTimeRunnable);
    }

    //准备完成
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

    }

    //发生错误
    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Toast.makeText(getActivity(), "发生错误,停止播放", Toast.LENGTH_SHORT).show();
        return true;
    }

    //播放完成
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        isPlay = false;
        isPlayFinish = true;
        videoPlay.setText(R.string.ic_video_play);
        mHandler.removeCallbacks(seekAndTimeRunnable);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onDestroyView() {
        releaseMediaPlayer();
        unbinder.unbind();
        super.onDestroyView();
    }

    //释放MediaPlayer资源
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mHandler.removeCallbacks(seekAndTimeRunnable);
    }

    @OnClick({R.id.video_play, R.id.icon_finish, R.id.volum_plus, R.id.volum_reduce, R.id.root_layout})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            //音量键
//            case R.id.ic_volum:
//                break;
//            //上一个
//            case R.id.video_last:
//                break;
            //暂停
            case R.id.video_play:
                if (isPlayFinish) {
                    isPlayFinish = false;
                    isPlay=true;
                    mMediaPlayer.start();
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
//            //下一个
//            case R.id.video_next:
//                break;
            //结束
            case R.id.icon_finish:
                stopPlay();
                break;
            //音量加
            case R.id.volum_plus:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                        0);
                changeVolum();
                break;
            //音量减
            case R.id.volum_reduce:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                        0);
                changeVolum();
                break;
            //透明背景
            case R.id.root_layout:
                //停止播放并隐藏
                stopPlay();
                break;
        }
    }

    SeekBar.OnSeekBarChangeListener seekBarChangeListener=new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            mMediaPlayer.seekTo(seekBar.getProgress());
            timeLeft.setText(formatTime(seekBar.getProgress()));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            timeLeft.setText(formatTime(seekBar.getProgress()));
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            timeLeft.setText(formatTime(seekBar.getProgress()));
        }
    };

}
