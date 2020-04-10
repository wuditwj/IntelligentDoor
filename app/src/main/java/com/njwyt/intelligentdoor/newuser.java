package com.njwyt.intelligentdoor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.njwyt.utils.ToastUtil;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import seu.smartdoor.face_eigenface;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Thread.sleep;

//import static java.security.AccessController.getContext;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class newuser extends AppCompatActivity implements Camera.PreviewCallback {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = false;//true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private String cur_name = null;

    private ImageView image;

    SurfaceView sView;
    SurfaceHolder surfaceHolder;
    SurfaceView sView2;
    SurfaceHolder surfaceHolder2;
    int screenWidth, screenHeight, previewWidth, previewHeight;
    float X_SCALE, Y_SCALE;
    // 定义系统所用的照相机
    Camera camera;
    // 是否在预览中
    boolean touch_once = true;
    boolean isPreview = false;
    public boolean isRun = false;
    private Mat yuvImg;
    // private CascadeClassifier cascadeClassifier;
    private CascadeClassifier cascadeClassifier_face;
    private CascadeClassifier cascadeClassifier_eyes;
    private Mat grayscaleImage;
    private int absoluteFaceSize;
    private Bitmap facebitmap, face;
    private int FACE_WIDTH = 92;
    private int FACE_HEIGHT = 112;
    private int CAMERA_WIDTH = 480;
    private int CAMERA_HEIGHT = 640;
    private Mat faceImage;//=new Mat(FACE_WIDTH,FACE_HEIGHT,CvType.CV_8UC1);
    private Mat recfaceImage;//=new Mat(FACE_WIDTH,FACE_HEIGHT,CvType.CV_8UC1);
    private Mat thumbnailImage;
    private Mat notrecfaceImage;
    private int rec_people = -1;
    private Bitmap recfacebitmap = null;
    private Bitmap imageview_bitmap = null;
    private boolean start_detect = false;
    private face_eigenface mfrs;
    private Rect[] facesArray;
    private Thread drawThread;
    public Date lastTime = new Date(System.currentTimeMillis());
    public String FPS;
    //    private Tab1_Guard.OnFragmentInteractionListener mListener;
    private Handler handler = null;
    private boolean lock = false;
    private int counter = 0;
    private int delay_count = 0;
    public boolean face_recognized = false;
    public boolean Identified = false;
    public String name_recognized = null;
    public String name_previous_recognized = null;
    public Rect newfaceArray = new Rect();
    public ImageButton FaceRecognizedImageButton = null;
    public Mat Face_detected_image = null;
    public Bitmap Face_detected_bitmap = null;
    public boolean start_capture = false;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            //return false;
            return true;
        }
    };

    private void showInputDialog() {
        if (isPreview) camera.stopPreview();
        final EditText et = new EditText(this);
        et.setText("");
        final AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("请输入姓名");
        inputDialog.setView(et);
        inputDialog.setNegativeButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cur_name = et.getText().toString();
                Toast toast = Toast.makeText(getApplicationContext(), "请看摄像头 3s", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                start_capture = true;
                counter = 0;
                camera.startPreview();
                onResume();
            }
        });

        inputDialog.show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_newuser);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        /*mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });*/

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.add_button).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);
        if (!OpenCVLoader.initDebug()) {

            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        }
        initializeOpenCVDependencies();
        mfrs = new face_eigenface(80, 3000);
        //mfrs.read_dbase_from_image();
        //mfrs.dataBase.write_dbase();
        mfrs.read_dbase();
        View view = mControlsView;
        // 获得SurfaceView的SurfaceHolder用于camera preview
        sView = (SurfaceView) findViewById(R.id.surfaceView4);
        surfaceHolder = sView.getHolder();
        // 获得SurfaceView2的SurfaceHolder2用于绘制人脸矩阵
        sView2 = (SurfaceView) findViewById(R.id.surfaceView5);
        sView2.setZOrderOnTop(true);
        //sView2.setBackgroundColor(Color.TRANSPARENT);
        //sView2.setAlpha((float)0.1);
        surfaceHolder2 = sView2.getHolder();
        surfaceHolder2.setFormat(PixelFormat.TRANSLUCENT);//非常重要，否则不会有透明效果

        // 为surfaceHolder添加一个回调监听器
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // 打开摄像头
                initCamera();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // 如果camera不为null ,释放摄像头
                if (camera != null) {
                    if (isPreview) camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            }
        });
        // 为surfaceHolder2添加一个回调监听器
        surfaceHolder2.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                drawThread = new Thread(DrawThread);
                if (isPreview && !isRun) {
                    isRun = true;
                    drawThread.start();
                }

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // 如果camera不为null ,释放摄像头
                isRun = false;
            }
        });
    }

    Runnable DrawThread = new Runnable() //class DrawThread extends Thread
    {
        //public SurfaceHolder holder=surfaceHolder2;

        /*public DrawThread(SurfaceHolder holder)
        {
            this.holder =holder;
            isRun = false;
        }*/
        @Override
        public void run() {
            SurfaceHolder holder = surfaceHolder2;
            int face_rec_alpha = 255;
            while (isRun & isPreview) {
                Canvas c = null;
                try {
                    synchronized (holder) {
                        c = holder.lockCanvas();//锁定画布，一般在锁定后就可以通过其返回的画布对象Canvas，在其上面画图等操作了。
                        //c.save();
                        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//设置画布背景颜色
                        Paint p = new Paint(); //创建画笔
                        p.setAntiAlias(true);
                        p.setColor(Color.GREEN);
                        p.setTextSize(30);
                        p.setStrokeWidth(5);
                        c.drawText("FPS:" + FPS, 300, 20, p);
                        if (face_recognized)
                            face_rec_alpha = 255;
                        else {
                            if (face_rec_alpha >= 1)
                                face_rec_alpha -= 1;
                        }
                        p.setAlpha(face_rec_alpha);

                        //for(int i=0;i<facesArray.length;i++)
                        {
                            /*c.drawLine((previewWidth-newfaceArray.x)*X_SCALE, newfaceArray.y*Y_SCALE,
                                    (previewWidth-(newfaceArray.x+newfaceArray.width))*X_SCALE, newfaceArray.y*Y_SCALE, p);
                            c.drawLine((previewWidth-(newfaceArray.x+newfaceArray.width))*X_SCALE, newfaceArray.y*Y_SCALE,
                                    (previewWidth-(newfaceArray.x+newfaceArray.width))*X_SCALE, (newfaceArray.y+newfaceArray.height)*Y_SCALE,p);
                            c.drawLine((previewWidth-(newfaceArray.x+newfaceArray.width))*X_SCALE, (newfaceArray.y+newfaceArray.height)*Y_SCALE,
                                    (previewWidth-newfaceArray.x)*X_SCALE, (newfaceArray.y+newfaceArray.height)*Y_SCALE,p);
                            c.drawLine((previewWidth-newfaceArray.x)*X_SCALE, (newfaceArray.y+newfaceArray.height)*Y_SCALE,
                                    (previewWidth-newfaceArray.x)*X_SCALE, newfaceArray.y*Y_SCALE,p);*/
                            c.drawLine((newfaceArray.x) * X_SCALE, newfaceArray.y * Y_SCALE,
                                    ((newfaceArray.x + newfaceArray.width)) * X_SCALE, newfaceArray.y * Y_SCALE, p);
                            c.drawLine(((newfaceArray.x + newfaceArray.width)) * X_SCALE, newfaceArray.y * Y_SCALE,
                                    ((newfaceArray.x + newfaceArray.width)) * X_SCALE, (newfaceArray.y + newfaceArray.height) * Y_SCALE, p);
                            c.drawLine(((newfaceArray.x + newfaceArray.width)) * X_SCALE, (newfaceArray.y + newfaceArray.height) * Y_SCALE,
                                    (newfaceArray.x) * X_SCALE, (newfaceArray.y + newfaceArray.height) * Y_SCALE, p);
                            c.drawLine((newfaceArray.x) * X_SCALE, (newfaceArray.y + newfaceArray.height) * Y_SCALE,
                                    (newfaceArray.x) * X_SCALE, newfaceArray.y * Y_SCALE, p);

                        }

                        match_draw(c, p, face_rec_alpha);
                        sleep(10);//睡眠时间为0.01秒
                        //c.restore();
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        holder.unlockCanvasAndPost(c);//结束锁定画图，并提交改变。
                    }
                }
            }
        }
    };

    public void match_draw(Canvas canvas, Paint p, int face_rec_alpha) {
        //更新界面
        /*Paint p = new Paint();
        Typeface font = Typeface.create("宋体", Typeface.BOLD);
        p.setTypeface(font);
        p.setAntiAlias(true);//去除锯齿
        p.setFilterBitmap(true);//对位图进行滤波处理
        p.setTextSize(50);*/
        Bitmap imageview_bitmap = Bitmap.createBitmap(sView2.getWidth(), sView2.getWidth() / 2, Bitmap.Config.ARGB_8888);
        //Canvas canvas = surfaceHolder2.lockCanvas();//new Canvas(imageview_bitmap);
            /*Matrix matrix=new Matrix();
            matrix.setScale(0.15f, 0.15f);
            matrix.postTranslate(100, 0);
            canvas.drawBitmap(bmp, matrix, paint);*/

        if (facebitmap != null) {
            if (!facebitmap.isRecycled()) facebitmap.recycle();
        }
        if (recfacebitmap != null) {
            if (!recfacebitmap.isRecycled()) recfacebitmap.recycle();
        }
        /*if(!faceImage.empty()) {
            facebitmap = Bitmap.createBitmap(FACE_WIDTH, FACE_HEIGHT, Bitmap.Config.ARGB_8888);
            if (faceImage.type() == CvType.CV_8UC1)
                Imgproc.cvtColor(faceImage, faceImage, Imgproc.COLOR_GRAY2RGB);
            Utils.matToBitmap(faceImage, facebitmap);
            facebitmap = Bitmap.createScaledBitmap(facebitmap, sView2.getWidth() / 2, sView2.getWidth()/2, false);
            canvas.drawBitmap(facebitmap, 0, 0, null);
        }*/
        try {
            synchronized (recfaceImage) {
                if (recfaceImage.type() == CvType.CV_8UC1)
                    Imgproc.cvtColor(recfaceImage, recfaceImage, Imgproc.COLOR_GRAY2RGB);
                recfacebitmap = Bitmap.createBitmap(FACE_WIDTH, FACE_HEIGHT, Bitmap.Config.ARGB_8888);
                //Point match_text_pos=new Point(10,10);
                //Imgproc.putText(recfaceImage,"Match:" +  String.valueOf((int)(mfrs._match_ratio))+"%",match_text_pos,1,0.5,new Scalar(255,255,255,0));
                if (rec_people > -1 && rec_people < mfrs._name.size())
                    mfrs._src.get(rec_people).copyTo(recfaceImage);
                Utils.matToBitmap(recfaceImage, recfacebitmap);
                recfacebitmap = Bitmap.createScaledBitmap(recfacebitmap, sView2.getWidth() / 4, sView2.getWidth() / 4, false);
                canvas.drawBitmap(recfacebitmap, 0, sView2.getHeight() * 3 / 4 - 400, p);
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        if (mfrs._match_ratio < 50) {
            p.setColor(Color.RED);
        } else if (mfrs._match_ratio < 70) {
            p.setColor(Color.YELLOW);
        } else {
            p.setColor(Color.GREEN);

        }
        p.setTextSize(50);
        p.setAlpha(face_rec_alpha);
        canvas.drawText("Match:" + String.valueOf(mfrs._match_ratio) + "%", sView2.getWidth() / 2 - 100, sView2.getWidth() / 4 + 25, p);

    }

    private void initCamera() {
        if (!isPreview) {
            // 此处默认打开后置摄像头。
            // 通过传入参数可以打开前置摄像头
            camera = Camera.open(0);  //①
            camera.setDisplayOrientation(90);
        }
        if (camera != null && !isPreview) {

            try {
                Camera.Parameters parameters = camera.getParameters();
                parameters.getSupportedPreviewSizes();
                for (int i = 0; i < parameters.getSupportedPreviewSizes().size(); i++) {
                    Log.i("<><><><>Width", parameters.getSupportedPreviewSizes().get(i).width + "");
                    Log.i("<><><><>Height", parameters.getSupportedPreviewSizes().get(i).height + "");
                }

                // 设置预览照片的大小
                parameters.setPreviewSize(screenWidth, screenHeight);
                screenWidth = sView.getWidth();
                screenHeight = sView.getHeight();
                Camera.Size test;
                test = parameters.getPreviewSize();
                previewWidth = 720;//1080;//test.height;
                previewHeight = 1280;//1920;//test.width;
                X_SCALE = screenWidth / (float) previewWidth;
                Y_SCALE = screenHeight / (float) previewHeight;
                parameters.setPreviewSize(previewHeight, previewWidth);

                //yuvImg = new Mat(previewWidth*3/2 ,previewHeight, CvType.CV_8UC1);//screenHeight*3/2
                yuvImg = new Mat(previewWidth * 3 / 2, previewHeight, CvType.CV_8UC1);//screenHeight*3/2
                //yuvImg = new Mat(0,0 , CvType.CV_8UC1);//screenHeight*3/2
                grayscaleImage = new Mat(previewHeight, previewWidth, CvType.CV_8UC3);//此处初始化为旋转后的尺寸
                Face_detected_image = new Mat();
                absoluteFaceSize = (int) (previewHeight * 0.2);
                // 设置预览照片时每秒显示多少帧的最小值和最大值
                int[] fpsrange = new int[2];
                parameters.getPreviewFpsRange(fpsrange);
                List<int[]> surportedFpsrange = parameters.getSupportedPreviewFpsRange();
                parameters.setPreviewFpsRange(4, 10);
                // 设置图片格式
                parameters.setPictureFormat(ImageFormat.JPEG);//NV21);//FLEX_RGB_888);//JPEG);
                // 设置JPG照片的质量
                parameters.set("jpeg-quality", 85);
                // 设置照片的大小
                parameters.setPictureSize(screenWidth, screenHeight);
                // 通过SurfaceView显示取景画面
                camera.setPreviewDisplay(surfaceHolder);  //②
                camera.setPreviewCallback(newuser.this);
                //camera.setParameters(parameters);
                // parameters.setPreviewFormat(ImageFormat.JPEG);
                List<Integer> formatsList = parameters.getSupportedPreviewFormats();
                List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
                parameters = camera.getParameters();
                test = parameters.getPreviewSize();
                int is = parameters.getPreviewFormat();
                if (is > 0)
                    // 开始预览
                    camera.startPreview();  //③
            } catch (Exception e) {
                e.printStackTrace();
            }
            isPreview = true;
        }
        // TODO: 2017/11/20
        //自动开始识别
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
        //Toast.makeText(getApplicationContext(), "已添加．", Toast.LENGTH_LONG).show();
//        if (!touch_once) {
//                showInputDialog();
        start_capture = true;
        counter = 0;
        camera.startPreview();
        onResume();
//        }
//        touch_once = !touch_once;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void initializeOpenCVDependencies() {
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            //String savingDirectory= "/sdcard/myImage";
            //File cascadeDir=new File(savingDirectory);
            File cascadeDir = this.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            // Load the cascade classifier
            cascadeClassifier_face = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading face cascade", e);
        }
        try {
            // Copy the resource into a temp file so OpenCV can load it

            InputStream is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
            File cascadeDir = this.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_eye_tree_eyeglasses.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();


            // Load the cascade classifier
            cascadeClassifier_eyes = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading eye cascade", e);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (isPreview) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
            isPreview = false;
        }
        sView.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        }
        delay_count = 0;
        initializeOpenCVDependencies();
        faceImage = new Mat(FACE_WIDTH, FACE_HEIGHT, CvType.CV_8UC1);
        recfaceImage = new Mat(FACE_WIDTH, FACE_HEIGHT, CvType.CV_8UC1);


        if (mfrs.read_dbase()) {
            mfrs.train();
//            mfrs._src.get(0).copyTo(faceImage);
        }
        /*if(mfrs.read_dbase()) {
            mfrs.train_native();
            mfrs._src.get(0).copyTo(faceImage);
        }*/

        sView.setVisibility(View.VISIBLE);
        show();

    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Date curTime = new Date(System.currentTimeMillis());//获取当前时间
        double fps = 1000 / ((long) curTime.getTime() - (long) lastTime.getTime());
        FPS = String.valueOf(fps);
        lastTime = curTime;
        yuvImg.put(0, 0, bytes);
        sView2.setBackgroundColor(Color.TRANSPARENT);
        sView2.setAlpha((float) 0.5);
        Mat rotateImg = new Mat(previewHeight, previewWidth, CvType.CV_8UC3);//grayscaleImage.height(),grayscaleImage.width(),CvType.CV_8UC3);
        //Imgproc.cvtColor(yuvImg, rotateImg, Imgproc.COLOR_YUV2RGB_NV21);//COLOR_YUV2GRAY_NV21);//COLOR_YUV2RGB_I420);//
        Imgproc.cvtColor(yuvImg, rotateImg, Imgproc.COLOR_YUV2RGB_YV12);//FriendArm
        Core.copyMakeBorder(rotateImg, rotateImg, (previewHeight - previewWidth) / 2, (previewHeight - previewWidth) / 2, 0, 0, Core.BORDER_CONSTANT);//放大画布1920*1920
        Point center = new Point(previewHeight / 2, previewHeight / 2);  // 旋转中心
        double angle = -90;  // 旋转角度
        double scale = 1; // 缩放尺度
        Mat rotateMat;
        rotateMat = Imgproc.getRotationMatrix2D(center, angle, scale);//计算旋转矩阵
        Imgproc.warpAffine(rotateImg, rotateImg, rotateMat, rotateImg.size());//旋转
        Rect Roi = new Rect((previewHeight - previewWidth) / 2, 0, previewWidth, previewHeight);//去黑边
        Mat detectImage = new Mat();
        rotateImg.submat(Roi).copyTo(detectImage);

        detectImage.copyTo(grayscaleImage);
        Imgproc.cvtColor(grayscaleImage, grayscaleImage, Imgproc.COLOR_RGB2GRAY);
        face_recognition(detectImage);
    }

    public void face_recognition(Mat aInputFrame) {
        Log.i("<><><>", "开始识别");
        MatOfRect faces = new MatOfRect();
        MatOfRect eyes = new MatOfRect();
        face_recognized = false;

        Bitmap bmp = Bitmap.createBitmap(aInputFrame.width(), aInputFrame.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(aInputFrame, bmp);
//        ((ImageView)findViewById(R.id.iv_temp_bmp)).setImageBitmap(bmp);

        counter++;
        if (counter > 20) start_detect = true;
        // Use the classifier to detect faces
        if (cascadeClassifier_face != null) {
            cascadeClassifier_face.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }
        //imageview.setVisibility(View.INVISIBLE);
        //notrecfaceImage.copyTo(recfaceImage);
        facesArray = faces.toArray();
        Mat eye_img, mouth_img;
        Mat tmp_img = new Mat();

        if (facesArray.length > 0) {
            for (int i = 0; i < 1/*facesArray.length*/; i++) {
                // If there are any faces found, draw a rectangle around it
                //Imgproc.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);//绘制脸框
                int face_left = (int) (facesArray[i].x - facesArray[i].width * 0.1);
                face_left = face_left > 0 ? face_left : 0;
                int face_right = face_left + (int) (facesArray[i].width * 1.2);
                int face_width = face_right < grayscaleImage.width() ? face_right - face_left : grayscaleImage.width() - face_left;
                int face_top = (int) (facesArray[i].y - facesArray[i].height * 0.2);
                face_top = face_top > 0 ? face_top : 0;
                int face_bot = face_top + (int) (facesArray[i].height * 1.4);
                int face_height = face_bot < grayscaleImage.height() ? face_bot - face_top : grayscaleImage.height() - face_top;
                if (face_left < 50 || face_top < 50 || previewHeight - face_bot < 50 || previewWidth - face_right < 50)
                    break;
                Rect faceArea = new Rect(face_left, face_top, face_width, face_height);

                Rect eyeArea = new Rect(faceArea.x, faceArea.y, faceArea.width, faceArea.height / 2);
                eye_img = new Mat();
                grayscaleImage.submat(eyeArea).copyTo(eye_img);//脸上半部分
                Imgproc.equalizeHist(eye_img, eye_img);

                //在脸上半部图片中寻眼睛
                if (cascadeClassifier_eyes != null) {
                    cascadeClassifier_eyes.detectMultiScale(eye_img, eyes, 1.1, 2, 2,
                            new Size(face_width / 15, face_height / 15), new Size(face_width / 3, face_height / 3));
                }
                Rect[] eyesArray = eyes.toArray();
                if (eyesArray.length == 2)//找到两只眼睛后找嘴巴
                {
                    Face_detected_image = aInputFrame.submat(faceArea);
                    Point eye_left_center = new Point(eyesArray[0].x + eyesArray[0].width / 2 + faceArea.x, eyesArray[0].y + eyesArray[0].height / 2 + faceArea.y);
                    Point eye_right_center = new Point(eyesArray[1].x + eyesArray[1].width / 2 + faceArea.x, eyesArray[1].y + eyesArray[1].height / 2 + faceArea.y);
                    if (abs(eye_left_center.x - eye_right_center.x) < face_width / 4) break;
                    double tan_rotate_angle = 1000000;
                    if (abs(eye_left_center.y - eye_right_center.y) < abs(eye_left_center.x - eye_right_center.x) / 5) {//小角度倾斜
                        tan_rotate_angle = (eye_left_center.y - eye_right_center.y) / (eye_left_center.x - eye_right_center.x);
                        double rotate_angle = atan(tan_rotate_angle);

                        //newfaceArray.height = (int) (abs((eye_left_center.y + eye_right_center.y) / 2 - mouth_center.y) * 1.7);
                        newfaceArray.width = (int) (abs((eye_left_center.x - eye_right_center.x) * cos(rotate_angle)) * 1.7);
                        newfaceArray.height = (int) (newfaceArray.width * FACE_HEIGHT / FACE_WIDTH);
                        if (newfaceArray.height < face_height / 3) break;
                        if (newfaceArray.height < faceArea.height && newfaceArray.width < faceArea.width) {// && start_detect) {
                            Point newfaceCenter = new Point();
                            newfaceCenter.y = (int) (abs((eye_left_center.y + eye_right_center.y) / 2 + newfaceArray.height / 5.0));//面部采集抬高20pixel
                            newfaceCenter.x = (int) (abs(eye_left_center.x + eye_right_center.x) / 2);

                            Mat mat_rot = Imgproc.getRotationMatrix2D(newfaceCenter, rotate_angle * 180 / PI, 1.0);
                            Mat mat_res = new Mat(); // result
                            Imgproc.warpAffine(grayscaleImage, mat_res, mat_rot, grayscaleImage.size(), Imgproc.INTER_CUBIC);
                            // 从旋转后的外框获取新图像
                            int offsetX = (int) newfaceCenter.x - newfaceArray.width / 2;
                            int offsetY = (int) newfaceCenter.y - newfaceArray.height / 2;
                            Mat mat_res_submat = mat_res.submat(offsetY, offsetY + newfaceArray.height, offsetX, offsetX + newfaceArray.width);
                            newfaceArray.x = offsetX;
                            newfaceArray.y = offsetY;
                            facesArray[i].width = newfaceArray.width;
                            facesArray[i].height = newfaceArray.height;
                            //开始人脸识别
                            Imgproc.resize(mat_res_submat, faceImage, new Size(FACE_WIDTH, FACE_HEIGHT));
                            //Photo.fastNlMeansDenoising(mat_res_submat, mat_res_submat,3,7,21);
                            //void fastNlMeansDenoising(Mat src, Mat& dst, float h = 3, int templateWindowSize = 7, int searchWindowSize = 21)
                            Imgproc.equalizeHist(faceImage, faceImage);
                            //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
                            if (start_capture && start_detect) {

                                start_capture = false;
                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Do something after 3s = 3000ms

                                        mfrs.dataBase.update_database(cur_name, 0, 0, faceImage);
                                        mfrs.dataBase.write_dbase_to_image();
                                        if (mfrs.read_dbase()) {
                                            mfrs.train();
                                        }
                                        if (recfacebitmap != null) {
                                            if (!recfacebitmap.isRecycled())
                                                recfacebitmap.recycle();
                                        }
                                        recfacebitmap = Bitmap.createBitmap(Face_detected_image.width(), Face_detected_image.height(), Bitmap.Config.ARGB_8888);
                                        Utils.matToBitmap(Face_detected_image, recfacebitmap);
                                        //保存下来的人脸识别头像
                                        File f = new File("/sdcard/intelligentDoor/myImage/", cur_name + ".jpg");
//                                        String fPath=f.toString();
//                                        Log.i("<><><><>",fPath);
//                                        Bitmap bm = BitmapFactory.decodeFile(fPath);
//                                        Log.i("<><><><>","宽:"+bm.getWidth()+"高:"+bm.getHeight());
                                        if (f.exists()) {

                                            f.delete();
                                        }
                                        try {
                                            FileOutputStream out = new FileOutputStream(f);
                                            recfacebitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                                            out.flush();
                                            out.close();
                                            Toast toast = Toast.makeText(getApplicationContext(), cur_name + "已添加", Toast.LENGTH_LONG);
                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                            toast.show();
                                        } catch (FileNotFoundException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }

                                        start_detect = false;
                                        //camera.stopPreview();
                                        onResume();

                                    }
                                }, 3000);
                            }

                            if (mfrs._database_status) {
                                int minClass = 0;
                                double minDist = 0;
                                // TODO: 2017/11/1 人脸识别存库
                                rec_people = mfrs.predict(faceImage,minClass,minDist);
                                //rec_people = mfrs.native_Predict(faceImage);
                                if (rec_people > -1) {
                                    face_recognized = true;
                                    name_recognized = mfrs._name.get(rec_people);
                                    if (mfrs._match_ratio > 70 && name_recognized != name_previous_recognized) {
                                        Identified = true;
                                        counter = 0;
                                            /*if(mfrs._match_ratio>70) {
                                                Thread open = new Thread(opendoorThread.OpenDoorThread);
                                                open.start();
                                                Snackbar.make(getView(), "                           门已开！", Snackbar.LENGTH_LONG)
                                                        .setAction("Action", null).show();
                                            }*/
                                    } else
                                        Identified = false;
                                    name_previous_recognized = name_recognized;

                                } else {
                                    face_recognized = false;
                                    if (counter > 10) {
                                        counter = 0;
                                        name_previous_recognized = null; //连续10帧识别失败,则清除之前的识别记录
                                    }
                                }

                            }
                        }
                    }
                }
//              imageview.setVisibility(View.VISIBLE);

            }

        } else if (counter > 30) {
            counter = 0;
            name_previous_recognized = null; //连续30帧识别失败,则清除之前的识别记录
        }
    }


    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            final Intent intent=new Intent();
            final String newPath = "/sdcard/intelligentDoor/myImage/";
            File dir = new File(newPath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            final String picturePath = dir
                    + File.separator
                    + new DateFormat().format("yyyyMMddHHmmss", new Date())
                    .toString() + ".jpg";
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
                        intent.putExtra("path",picturePath);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            camera.startPreview();
            newuser.this.setResult(0,intent);

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

    private final View.OnClickListener mOnClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            camera.takePicture(null, null, mPictureCallback);
        }
    };
}