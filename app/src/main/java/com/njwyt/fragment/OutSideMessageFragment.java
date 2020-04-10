package com.njwyt.fragment;

import android.app.Fragment;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.njwyt.db.GuestRecordingDaoHelp;
import com.njwyt.entity.GuestRecording;
import com.njwyt.intelligentdoor.R;
import com.njwyt.utils.DateUtils;
import com.njwyt.view.FloatingVolume;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by Administrator on 2017/12/29.
 */

public class OutSideMessageFragment extends Fragment {

    @BindView(R.id.mic_view)
    FloatingVolume micView;
    @BindView(R.id.volume)
    LinearLayout volume;
    Unbinder unbinder;
    private String headPath;

    private View view;//布局

    public FractionListener mFracListener;//接口

    // 系统的音频文件
    private File soundFile;

    private GuestRecording guestRecording;
    private GuestRecordingDaoHelp guestRecordingHelp;

    private MediaRecorder mMediaRecorder;

    private long startTime;//开始时间(毫秒)
    private long endTime;
    public String format1;//开始时间
    public int allTime;//总时长
    private int BASE = 1;
    private int SPACE = 200;// 间隔取样时间
    private int closeTime;//关闭倒计时
    private boolean isRun = false;//是否在倒计时
    private boolean isRunning = false;//是否正在录音
    private Handler mHandler;
    private Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            updateMicStatus();
        }
    };

    private SoundPool sp;//声明一个SoundPool
    private int music;//定义一个整型用load（）；来设置suondID




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_outside_message, container, false);
        unbinder = ButterKnife.bind(this, view);
        mHandler = new Handler();
        //初始化提示音
        sp= new SoundPool(10, AudioManager.STREAM_MUSIC, 5);//第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
        music = sp.load(getActivity(), R.raw.out_side_messige, 1); //把你的声音素材放到res/raw里，第2个参数即为资源文件，第3个为音乐的优先级

        return view;
    }

    //创建视频文件
    private void initFile() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getActivity().getApplication(), getString(R.string.SDcardisxists), Toast.LENGTH_SHORT).show();
            return;
        }
        File sampleDir = new File("/sdcard/intelligentDoor/mySound/");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        String soundName = "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp3";
        soundFile = new File(sampleDir, soundName);
//        mRecorderUtils = new MediaRecorderUtils(soundFile);
        this.setFractionListener(micView);
//        //开始录音
//        mRecorderUtils.startRecord();
    }

    //初始化MediaRecorder
    public void startRecorder(String headPath) {
        if (!isRunning) {
            this.headPath = headPath;
            getFragmentManager().beginTransaction().show(this).commit();
//        getFragmentManager().beginTransaction().replace(layout,this).commit();
            initFile();
        /* ①Initial：实例化MediaRecorder对象 */
            if (mMediaRecorder == null)
                mMediaRecorder = new MediaRecorder();
            /* ②setAudioSource/setVedioSource */
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                        /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            /* ③准备 */
            mMediaRecorder.setOutputFile(soundFile.getAbsolutePath());
            try {
                mMediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            /* ④开始 */
            mMediaRecorder.start();
            isRunning = true;
            // AudioRecord audioRecord.
            /* 获取开始时间并转换成年月日* */
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            startTime = System.currentTimeMillis();
            Date curDate = new Date(startTime);
            //录音时间
            //format1 = format.format(curDate);
            format1 = DateUtils.getCurrentTime();
            //监听话筒状态
            updateMicStatus();
            //提示音        左声道(门外)   右声道(门内)
            sp.play(music, 1.0f, 0f, 1, 0, 1);
        }else {

        }
    }


    /**
     * 更新话筒状态
     */
    private void updateMicStatus() {
        if (mMediaRecorder != null) {
            double ratio = (double) mMediaRecorder.getMaxAmplitude() / BASE;
            double db = 0;// 分贝
            if (ratio > 1)
                db = 20 * Math.log10(ratio);
            Log.d("--==>>>", "分贝值：" + db);
            //============================================
            //监听分贝并关闭
//            if (db<=70){
//                if (!isRun) {
//                    closeTime=10;//关闭倒计时
//                    Log.i("--==>>", "开始计时:");
//                    mHandler.post(closeRunnable);
//                }
//            }else {
//                isRun=false;
//                mHandler.removeCallbacks(closeRunnable);
//            }
            if (mFracListener != null) {
                mFracListener.updateFraction((db - 60) / (90 - 60));
            }
            mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
        }
    }


    /**
     * 停止录音
     */
    public long stopRecorder() {
        if (isRunning) {
//        mHandler.removeCallbacks(closeRunnable);
            mHandler.removeCallbacks(mUpdateMicStatusTimer);
            if (mFracListener != null) {//停止录音
                mFracListener.updateFraction(0);
            }
            if (mMediaRecorder == null)
                return 0L;
            //结束时间
            endTime = System.currentTimeMillis();
            Log.i(">>>>>>", "开始时间" + format1);
            Log.i(">>>>>>", "结束时间" + endTime);
            Log.i(">>>>>>", "地址" + soundFile);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRunning = false;
            //总时长
            allTime = (int) ((endTime - startTime) / 1000);
            Log.i(">>>>>>", "时长" + allTime + "秒");
            //存数据库
            writeInDB();
            getFragmentManager().beginTransaction().hide(this).commit();
        }
        return endTime - startTime;

    }

    //存数据库
    private void writeInDB() {
        guestRecording = new GuestRecording();
        guestRecordingHelp = new GuestRecordingDaoHelp(getActivity().getApplication());
        guestRecording.setDatatime(format1);//录音开始时间
        guestRecording.setDuration(allTime);//录音时长
        guestRecording.setRecordingUrl(soundFile.getAbsolutePath());//录音地址
        guestRecording.setGuestPicUrl(headPath);
        guestRecordingHelp.insertGuestRecording(guestRecording);//添加进数据库
        Log.i("门外录音信息--==>> ", "录音地址: " + guestRecording.getRecordingUrl()
                + "\n录音时长: " + guestRecording.getDuration()
                + "\n录音开始时间: " + guestRecording.getDatatime()
                + "\n录音者头像地址: " + guestRecording.getGuestPicUrl()
                + "\n录音是否已读: " + guestRecording.getIsRead()
                + "\n录音id: " + guestRecording.getUuid());

    }

//    private Runnable closeRunnable=new Runnable() {
//
//        @Override
//        public void run() {
//            isRun=true;
//            Log.i("--==>>", "  "+closeTime);
//            if (closeTime==0){
//                mHandler.removeCallbacks(closeRunnable);
//                isRun=false;
//                // TODO: 2017/12/29 关闭Activity
//                return;
//            }
//            closeTime--;
//            mHandler.postDelayed(closeRunnable,1000);
//        }
//    };


    @Override
    public void onDestroyView() {
        stopRecorder();
        unbinder.unbind();
        super.onDestroyView();
    }

    //接口
    public void setFractionListener(FractionListener fractionListener) {
        mFracListener = fractionListener;
    }

    /**
     * 分贝百分比监听，默认初始值为60,最大值90
     */
    public interface FractionListener {
        /**
         * 传入当前增长分贝的百分比进度
         *
         * @param curFraction
         */
        void updateFraction(double curFraction);
    }
}
