package com.njwyt.intelligentdoor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.njwyt.AppContext;
import com.njwyt.db.MessageHistoryDaoHelp;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.MessageHistory;
import com.njwyt.entity.User;
import com.njwyt.view.IconTextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.Unbinder;


public class CustomVideoActivity extends BaseActivity {
    @BindView(R.id.record_surfaceView)
    SurfaceView surfaceView;
    @BindView(R.id.record_time)
    Chronometer mRecordTime;
    @BindView(R.id.record_control)
    IconTextView mRecordControl;
    @BindView(R.id.out_circular)
    ImageView outCircular;
    @BindView(R.id.in_circular)
    ImageView inCircular;
    @BindView(R.id.sdv_head)
    SimpleDraweeView sdvHead;
    //    @BindView(R.id.video_progressbar)
//    ProgressBar videoProgressBar;
    @BindView(R.id.time_progressbar)
    View timeProgressBar;
    private SurfaceHolder mSurfaceHolder;
    private List<User> userList;// 用户列表
    private List<View> headViewList;// 家庭成员View表

    //DATA
    private boolean isPause;//暂停标识
    private boolean isRecording;// 标记，判断当前是否正在录制
    private long mRecordCurrentTime = 0;//录制时间间隔

    // 存储文件
    private File mVecordFile;
    private Camera mCamera;
    private MediaRecorder mediaRecorder;
    private int cameraId;// 区分前后摄像头
    private MessageHistoryDaoHelp messageHistoryDaoHelp;

    private MediaRecorder.OnErrorListener onErrorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mediaRecorder, int what, int extra) {
            try {
                if (mediaRecorder != null) {
                    mediaRecorder.reset();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private int time;
    private Unbinder bind;
    private MessageHistory messageHistory;
    private String videoName;
    private File videoImageFile;
    //    private Thread thread;

    private SoundPool sp;//声明一个SoundPool
    private int music;//定义一个整型用load（）；来设置suondID
    private AudioManager audioManager;//音频管理器

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_custom_video);
        bind = ButterKnife.bind(this);
        initView();
        initHeadAnim();

    }

    /**
     * 加载动画
     */
    private void initHeadAnim() {
        UserDaoHelp userDaoHelp = new UserDaoHelp();
        User selectUser = userDaoHelp.selectUser(this, messageHistory.getTargetId());

        Bitmap bm = BitmapFactory.decodeFile(selectUser.getHeadUrl());
        sdvHead.getHierarchy().setPlaceholderImage(new BitmapDrawable(null, bm));
//        sdvHead.setImageBitmap(bm);

        //先加载头像放大动画
        Animation headAnim = AnimationUtils.loadAnimation(CustomVideoActivity.this, R.anim.head_portrait);
        sdvHead.startAnimation(headAnim);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //再加载外面两个圈旋转加缩小动画
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //停止按钮可点击
                        mRecordControl.setEnabled(true);
                        outCircular.setVisibility(View.VISIBLE);
                        inCircular.setVisibility(View.VISIBLE);
                        Animation outCircularAnim = AnimationUtils.loadAnimation(CustomVideoActivity.this, R.anim.out_picture_frame);
                        LinearInterpolator lin = new LinearInterpolator();
                        outCircularAnim.setInterpolator(lin);
                        if (outCircularAnim != null) {
                            outCircular.startAnimation(outCircularAnim);
                        }
                        Animation inCircularAnim = AnimationUtils.loadAnimation(CustomVideoActivity.this, R.anim.in_picture_frame);
                        LinearInterpolator lin1 = new LinearInterpolator();
                        inCircularAnim.setInterpolator(lin1);
                        if (inCircularAnim != null) {
                            inCircular.startAnimation(inCircularAnim);
                        }
                    }
                });
            }
        }).start();
    }

    private void initView() {
        //初始化提示音
        sp = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);//第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
        music = sp.load(this, R.raw.custom_video, 1); //把你的声音素材放到res/raw里，第2个参数即为资源文件，第3个为音乐的优先级

        mRecordControl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN://按住事件发生后执行代码的区域
                        mRecordControl.setTextSize(75f);
                        break;
                    case MotionEvent.ACTION_UP://松开事件发生后执行代码的区域
                        mRecordControl.setTextSize(80f);
                        break;
                }
                return false;
            }
        });
        TypedArray mTypedArray = this.obtainStyledAttributes(R.styleable.CameraSurfaceView);
        cameraId = mTypedArray.getColor(R.styleable.CameraSurfaceView_cameraId, 0);

        mSurfaceHolder = surfaceView.getHolder();
        // 设置Surface不需要维护自己的缓冲区
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(mCallBack);
//        // 设置分辨率
//        mSurfaceHolder.setFixedSize(600, 800);
//        // 设置该组件不会让屏幕自动关闭
//        mSurfaceHolder.setKeepScreenOn(true);

        messageHistory = (MessageHistory) getIntent().getSerializableExtra("messageHistory");
        messageHistoryDaoHelp = new MessageHistoryDaoHelp(this);
        System.out.println("刚进界面时发送者id>>>>>>>>>>>>" + messageHistory.getSenderId()
                + "\n接收者id>>>>>>>>>>>>" + messageHistory.getTargetId()
                + "\n时间>>>>>>>>>>>>" + messageHistory.getDatatime()
                + "\n视频地址>>>>>>>>>>>>" + messageHistory.getVideoUrl()
                + "\n是否已读>>>>>>>>>>>>" + messageHistory.getIsRead()
                + "\n视频时长>>>>>>>>>>>>" + messageHistory.getDuration()
                + "\nuuid>>>>>>>>>>>>" + messageHistory.getUuid()
                + "\n第一帧地址>>>>>>>>>>>>" + messageHistory.getFirstFrameUrl()
                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Log.i("--==++", "高:" + AppContext.getInstance().getScreenHeight() + "" + "宽:" + AppContext.getInstance().getScreenWidth());
        Log.i("--==++", AppContext.getInstance().getScreenHeight() * (3 / 4) + "");
    }


    private SurfaceHolder.Callback mCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            initCamera();
            //提示音        左声道(门外)   右声道(门内)
            sp.play(music, 0f, 1.0f, 1, 0, 1);

            try {
                startRecord();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            if (mSurfaceHolder.getSurface() == null) {
                return;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            stopCamera();
        }
    };

    private void initCamera() {
        mCamera = Camera.open(0);  //①
        mCamera.setDisplayOrientation(90);
//        //屏蔽系统拍照声
//        mCamera.enableShutterSound(false);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);

            mCamera.cancelAutoFocus();//此句加上 可自动聚焦 必须加
            Camera.Parameters parameters = mCamera.getParameters();
            //查询摄像头支持的分辨率
            parameters.getSupportedPreviewSizes();
            for (int i = 0; i < parameters.getSupportedPreviewSizes().size(); i++) {
//                Log.i("<><><><>Width", parameters.getSupportedPreviewSizes().get(i).width + "");
//                Log.i("<><><><>Height", parameters.getSupportedPreviewSizes().get(i).height + "");
            }
//            I/<><><><>Width: 2592
//            I/<><><><>Height: 1944
//            I/<><><><>Width: 1600
//            I/<><><><>Height: 1200
//            I/<><><><>Width: 1280
//            I/<><><><>Height: 720
//            I/<><><><>Width: 800
//            I/<><><><>Height: 600
//            I/<><><><>Width: 640
//            I/<><><><>Height: 480
            //设置分辨率
            parameters.setPreviewSize(1280, 720);

//            //预览变形
//            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();  //获取系统的size集合
//            Camera.Size optimalSize = getOptimalPreviewSize(sizes, surfaceView.getHeight(), surfaceView.getWidth()); //根据surfaceview控件的比例选择size
//            parameters.setPreviewSize(optimalSize.width, optimalSize.height); //进行size设置
            //设置聚焦模式
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            //缩短Recording启动时间
            parameters.setRecordingHint(true);
//            是否支持影像稳定能力，支持则开启
            if (parameters.isVideoStabilizationSupported())
                parameters.setVideoStabilization(true);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.i("=====", "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * 关闭摄像头
     */
    private void stopCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 退出并保存视频
     */
    @OnClick(R.id.record_control)
    public void onViewClicked() {
        //提示音        左声道(门外)   右声道(门内)
        sp.play(music, 0f, 1.0f, 1, 0, 1);

        mRecordControl.setText(R.string.ic_video_finish);
        mRecordControl.setTextColor(getBaseContext().getResources().getColorStateList(R.color.colorRed));
        stopRecord();

        finish();
    }

    /**
     * 长按退出不保存视频
     *
     * @return
     */
    @SuppressLint("ResourceAsColor")
    @OnLongClick(R.id.record_control)
    public boolean onLongViewClicked() {
        mRecordControl.setText(R.string.ic_video_finish);
        mRecordControl.setTextColor(getBaseContext().getResources().getColorStateList(R.color.colorRed));

//        stopCamera();
//        stopRecord();
        if (isRecording && mediaRecorder != null) {


            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setPreviewDisplay(null);

            mediaRecorder.stop();
            mediaRecorder.reset();

            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
        }
        mVecordFile.delete();
        finish();
        return true;
    }

    /**
     * 开始录制
     *
     * @throws IOException
     */
    private void startRecord() throws IOException {
        boolean creakOk = createRecordDir();
        if (!creakOk) {
            return;
        }
//        initCamera();
        mCamera.unlock();
        setConfigRecord();
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRecording = true;
        if (mRecordCurrentTime != 0) {
            mRecordTime.setBase(SystemClock.elapsedRealtime() - (mRecordCurrentTime - mRecordTime.getBase()));
        } else {
            mRecordTime.setBase(SystemClock.elapsedRealtime());
        }
        mRecordTime.start();

        //监听留言时间,让其60秒自动停止
        mRecordTime.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(final Chronometer chronometer) {
                final String clickTime = chronometer.getText().toString();

                if ("01:00".equals(clickTime)) {
                    stopRecord();
                    Toast.makeText(CustomVideoActivity.this, getString(R.string.messageok), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.time_progressbar);
        timeProgressBar.startAnimation(anim);
    }

    /**
     * 停止录制
     */
    private void stopRecord() {
        if (isRecording && mediaRecorder != null) {

            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setPreviewDisplay(null);
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
//                bb();
            System.out.println("地址>>>>>>>>>>>>" + mVecordFile.toString());
            new imageTask().execute(mVecordFile);
        }
    }

//    private void bb() {
//        EpVideo epVideo = new EpVideo(mVecordFile.toString());
//        epVideo.rotation(180,false);
//        final String outPath = "/sdcard/intelligentDoor/myVideo/out.mp4";
//        new EpEditor(this).exec(epVideo, new EpEditor.OutputOption(outPath), new OnEditorListener() {
//            @Override
//            public void onSuccess() {
//                Toast.makeText(CustomVideoActivity.this, "编辑完成:"+outPath, Toast.LENGTH_SHORT).show();
////                Intent v = new Intent(Intent.ACTION_VIEW);
////                v.setDataAndType(Uri.parse(outPath), "video/mp4");
////                startActivity(v);
//                pp(outPath);
//            }
//
//            @Override
//            public void onFailure() {
//                Toast.makeText(CustomVideoActivity.this, "编辑失败", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onProgress(float v) {
//            }
//        });
//    }
//
//    private void pp(String path) {
//        EpVideo epVideo = new EpVideo(path);
//        epVideo.rotation(180,false);
//        final String outPath1 = "/sdcard/intelligentDoor/myVideo/out1.mp4";
//        new EpEditor(this).exec(epVideo, new EpEditor.OutputOption(outPath1), new OnEditorListener() {
//            @Override
//            public void onSuccess() {
//                Toast.makeText(CustomVideoActivity.this, "编辑完成:"+outPath1, Toast.LENGTH_SHORT).show();
////                Intent v = new Intent(Intent.ACTION_VIEW);
////                v.setDataAndType(Uri.parse(outPath), "video/mp4");
////                startActivity(v);
//            }
//
//            @Override
//            public void onFailure() {
//                Toast.makeText(CustomVideoActivity.this, "编辑失败", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onProgress(float v) {
//            }
//        });
//    }

    /**
     * 创建视频文件
     *
     * @return
     */
    private boolean createRecordDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(this, getString(R.string.SDcardisxists), Toast.LENGTH_SHORT).show();
            return false;
        }

        File sampleDir = new File("/sdcard/intelligentDoor/myVideo/");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        videoName = "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
        mVecordFile = new File(sampleDir, videoName);
        return true;
    }

    /**
     * 配置MediaRecorder()
     */
    private void setConfigRecord() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.reset();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setOnErrorListener(onErrorListener);
        //录像角度
        mediaRecorder.setOrientationHint(270);
        //使用SurfaceView预览
        mediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        //1.设置采集声音
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置采集图像
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //2.设置视频，音频的输出格式 mp4
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        //3.设置音频的编码格式
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //设置图像的编码格式
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //设置立体声
//        mediaRecorder.setAudioChannels(2);
//        mediaRecorder.setMaxDuration(60 * 1000);
//        mediaRecorder.setMaxFileSize(1024 * 1024);
        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);

//        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mediaRecorder.setAudioEncodingBitRate(44100);
        if (mProfile.videoBitRate > 2 * 1024 * 1024) {
            mediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
        } else {
            mediaRecorder.setVideoEncodingBitRate(1024 * 1024);
        }
        mediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
//        Log.i( "<><><><><>",mProfile.videoFrameWidth+"\n"+mProfile.videoFrameHeight+"");
//        mediaRecorder.setVideoSize(mProfile.videoFrameWidth,mProfile.videoFrameHeight);
        mediaRecorder.setVideoSize(1280, 720);
//        mediaRecorder.setVideoSize(AppContext.getInstance().getScreenHeight(),
//                AppContext.getInstance().getScreenWidth());

        mediaRecorder.setOutputFile(mVecordFile.getAbsolutePath());

    }


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
        bind.unbind();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        stopCamera();
        stopRecord();
        finish();
        super.onPause();
    }

    /**
     * 压缩图片
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 90;

        while (baos.toByteArray().length / 1024 > 100) {
            baos.reset();
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }


    /**
     * 获取到视频第一帧
     */
    class imageTask extends AsyncTask<File, Void, String> {

        private int videoTime;

        @Override
        protected String doInBackground(File... files) {
            //创建一个文件夹
            String VideoImagePath = "/sdcard/intelligentDoor/myVideo/compress/";
            String videoImageName = "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".png";
            File dir = new File(VideoImagePath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            videoImageFile = new File(VideoImagePath, videoImageName);
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(mVecordFile.toString());
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION); // 播放时长单位为毫秒
            videoTime = (int) (Integer.parseInt(duration) * 0.001);
            Log.i("<<<<<<<<<<<<<<<", duration);
//        Bitmap bitmap = ImageUtils.compressImage(mmr.getFrameAtTime(), 50, 50);
            Bitmap bitmap = compressImage(mmr.getFrameAtTime());
            try {
                FileOutputStream out = new FileOutputStream(videoImageFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return videoImageFile.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                messageHistory.setFirstFrameUrl(s);
                messageHistory.setVideoUrl(mVecordFile.toString());
                messageHistory.setDuration(videoTime);
                messageHistoryDaoHelp.insertHistory(messageHistory);
                System.out.println("录完之后发送者id>>>>>>>>>>>>" + messageHistory.getSenderId()
                        + "\n接收者id>>>>>>>>>>>>" + messageHistory.getTargetId()
                        + "\n时间>>>>>>>>>>>>" + messageHistory.getDatatime()
                        + "\n视频地址>>>>>>>>>>>>" + messageHistory.getVideoUrl()
                        + "\n是否已读>>>>>>>>>>>>" + messageHistory.getIsRead()
                        + "\n视频时长>>>>>>>>>>>>" + messageHistory.getDuration()
                        + "\nuuid>>>>>>>>>>>>" + messageHistory.getUuid()
                        + "\n第一帧地址>>>>>>>>>>>>" + messageHistory.getFirstFrameUrl()
                        + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//                messageHistoryDaoHelp.upDataMessageHistory(messageHistory);
//                System.out.println("录完之后第一帧图片>>>>>>>>>>>>" + messageHistory.getFirstFrameUrl());
            }
        }
    }

}
