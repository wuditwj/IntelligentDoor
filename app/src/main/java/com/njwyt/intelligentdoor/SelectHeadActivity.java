package com.njwyt.intelligentdoor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.njwyt.view.IconTextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SelectHeadActivity extends BaseActivity implements SurfaceHolder.Callback {

    @BindView(R.id.surfaceView)
    SurfaceView surfaceView;
    @BindView(R.id.photo)
    FrameLayout photo;
    @BindView(R.id.back)
    IconTextView back;
    @BindView(R.id.circle_solid)
    IconTextView circleSolid;
    @BindView(R.id.back_view1)
    View backView1;
    @BindView(R.id.progressBar)
    LinearLayout progressBar;

    private Camera mCamera;
    private SurfaceHolder mHolder;
    private String fileName;

    private SoundPool sp;//声明一个SoundPool
    private int music;//定义一个整型用load（）；来设置suondID

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_head);
        ButterKnife.bind(this);
        initViews();
    }

    private void initViews() {

        //初始化提示音
        sp= new SoundPool(10, AudioManager.STREAM_MUSIC, 5);//第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
        music = sp.load(this, R.raw.photo, 1); //把你的声音素材放到res/raw里，第2个参数即为资源文件，第3个为音乐的优先级

        mHolder = surfaceView.getHolder();
        mHolder.addCallback(this);

        photo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN://按住事件发生后执行代码的区域
                        circleSolid.setTextSize(55f);
                        break;
                    case MotionEvent.ACTION_UP://松开事件发生后执行代码的区域
                        circleSolid.setTextSize(60f);
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
// 如果camera不为null ,释放摄像头
        if (mCamera != null) {
            stopCamera();
        }
    }

    private void initCamera() {
        mCamera = Camera.open(0);  //①
        mCamera.setDisplayOrientation(90);
//        //屏蔽系统拍照声
//        mCamera.enableShutterSound(false);
        try {
            mCamera.setPreviewDisplay(mHolder);

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
//            parameters.setPreviewSize(800, 600);
            parameters.setPictureSize(1280, 720);
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


    @OnClick({R.id.surfaceView, R.id.photo, R.id.back})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.surfaceView:
                if (null != mCamera)
                    mCamera.autoFocus(null);
                break;
            case R.id.photo:
                //提示音        左声道(门外)   右声道(门内)
                sp.play(music, 0f, 1.0f, 1, 0, 1);
//                setCameraParams(mCamera, AppContext.getInstance().getScreenWidth(), AppContext.getInstance().getScreenHeight());
                mCamera.takePicture(null, null, mPictureCallback);
                backView1.setOnClickListener(null);
//                //屏幕变灰效果
                backView1.setVisibility(View.VISIBLE);
                backView1.setAlpha(0.5f);
                progressBar.setVisibility(View.VISIBLE);
                break;
            case R.id.back:
                finish();
                break;
        }
    }

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
//            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//            AppContext.getInstance().setBlurBitmap(bitmap);

            final Intent intent = new Intent();
            final String newPath = "/sdcard/intelligentDoor/myImage/";
            File dir = new File(newPath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            fileName = new SimpleDateFormat("yyyyMMdd_hhmmss").format(System.currentTimeMillis()) + ".jpg";

            final String picturePath = "/sdcard/intelligentDoor/myImage/" + fileName;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(picturePath);
                    try {
                        // 获取当前旋转角度, 并旋转图片
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        bitmap = rotateBitmapByDegree(bitmap, 90);
                        BufferedOutputStream bos = new BufferedOutputStream(
                                new FileOutputStream(file));
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                        bos.flush();
                        bos.close();
                        bitmap.recycle();
                        intent.putExtra("picturePath", picturePath);
                        setResult(0, intent);
                        finish();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

//            camera.startPreview();
//            getBitmap(picturePath);
        }
    };

    // 旋转图片
    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }


}
