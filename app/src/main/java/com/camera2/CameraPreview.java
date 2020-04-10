package com.camera2;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.njwyt.AppContext;
import com.njwyt.content.Type;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.FaceLocation;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.User;
import com.njwyt.intelligentdoor.R;
import com.njwyt.utils.ImageUtils;

import org.greenrobot.eventbus.EventBus;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;

import seu.smartdoor.face_eigenface;

import static com.njwyt.content.Type.FACE_SCALE;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;

/**
 * USB摄像头
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    static final String TAG = "CameraPreview";
    private static final boolean DEBUG = true;
    private final int MSG_WHAT = 8;
    protected Context context;
    private SurfaceHolder holder;
    private Thread mainLoop = null;
    private static Bitmap bmp = null;       // 原始图片与拍照的图片
    private Bitmap dstbmp;                  // 用于旋转后供页面显示的图片
    private boolean isPause = false;
    public static boolean cameraExists = false;
    private boolean shouldStop = false;
    private Paint mAntiAliasPaint;  // 抗锯齿画笔
    private PaintFlagsDrawFilter mPaintFlagsDrawFilter;
    private int mode = 0;       // 使用模式
    public Date lastTime = new Date(System.currentTimeMillis());
    public String FPS;
    private byte[] mImageData;

    private HandlerThread mFaceHandlerThread;
    private FaceHandler mFaceHandler;
    private Handler mRecordingExitHandler;      // 记录录音何时取消的Handler

    // / /dev/videox (x=cameraId+cameraBase) is used.
    // In some omap devices, system uses /dev/video[0-3],
    // so users must use /dev/video[4-].
    // In such a case, try cameraId=0 and cameraBase=4
    private int cameraId = 0;
    private int cameraBase = 0;
    private int stopRecordingTime;              // 记录录音何时取消的秒数
    private int matchRatioSum = 0;              // 人脸识别平均值
    private int currentLigth = 5;                  // 记录补光灯亮度
    private boolean isStopRecording;            // 记录这个Handler是否在运行
    private LinkedList<Integer> ratioCountList;
    //

    // This definition also exists in ImageProc.h.
    // Webcam must support the resolution 640x480 with YUYV format.
    static final int IMG_WIDTH = 640;
    static final int IMG_HEIGHT = 480;

    // The following variables are used to draw camera images.
    private int winWidth = 0;
    private int winHeight = 0;
    private android.graphics.Rect rect;
    private int dw, dh;
    private float rate;

    private face_eigenface mfrs = null;
    private CascadeClassifier cascadeClassifier_face;
    private CascadeClassifier cascadeClassifier_eyes;
    public Mat faceImage = null;
    Mat grayscaleImage = null;
    private int absoluteFaceSize;
    private int width;
    private int height;
    private int FACE_WIDTH = 92;
    private int FACE_HEIGHT = 112;
    private int counter = 0;
    private int errorCount = 10;             // 识别失败次数
    public boolean face_recognized = false;
    public boolean Identified = false;
    public String name_recognized = null;
    public String name_previous_recognized = null;
    public Rect newfaceArray = new Rect();
    private Mat detect_img = null;
    public Mat Face_detected_image = null;
    private Mat yuvImg;

    // JNI functions
    public native int prepareCamera(int videoid);

    public native int prepareCameraWithBase(int videoid, int camerabase);

    public native void processCamera();

    public native void stopCamera();

    public native void pixeltobmp(Bitmap bitmap);

    public native byte[] ctojava();

//    public native void captureCamera(byte[] buf);

    public CameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //System.loadLibrary("CameraPreview");
        System.loadLibrary("native-lib");
        this.context = context;
        if (DEBUG)
            Log.d("WebCam", "CameraPreview constructed");
        setFocusable(true);
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
        if (!OpenCVLoader.initDebug()) {

            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        }
        initializeOpenCVDependencies();
        initValue();
        initThread();
        mfrs = new face_eigenface(80, 3000);
        if (mfrs.read_dbase()) {
            mfrs.train();
            //mfrs._src.get(0).copyTo(faceImage);
        }

        //this.setZOrderOnTop(true);
        this.setZOrderMediaOverlay(true);

        mAntiAliasPaint = new Paint();
        mPaintFlagsDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    }

    /**
     * 这段是通过代码实例化时运行的
     *
     * @param context
     */
    public CameraPreview(Context context) {
        super(context);
        this.context = context;
        if (DEBUG)
            Log.d("WebCam", "CameraPreview constructed");
        setFocusable(true);
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
        if (!OpenCVLoader.initDebug()) {

            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        }
        initializeOpenCVDependencies();
        mfrs = new face_eigenface(80, 3000);
/*		if(mfrs.read_dbase())
        {
			mfrs.train();
			//mfrs._src.get(0).copyTo(faceImage);
		}*/
    }

    private void initializeOpenCVDependencies() {
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            //String savingDirectory= "/sdcard/myImage";
            //File cascadeDir=new File(savingDirectory);
            File cascadeDir = getContext().getDir("cascade", Context.MODE_PRIVATE);
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
            File cascadeDir = getContext().getDir("cascade", Context.MODE_PRIVATE);
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

    private void initValue() {
        ratioCountList = new LinkedList<>();
    }

    private void initThread() {

        mFaceHandlerThread = new HandlerThread("faceThread");
        mFaceHandlerThread.start();
    }

    private double getLightAvg(Mat img) {
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGB2GRAY);
        Scalar scalar = Core.mean(gray);
        gray.release();
        return scalar.val[0];
    }

    private void setLightAvg(Mat scr, Mat dst, double avg) {
        double fpreAvg = getLightAvg(scr);
        scr.convertTo(dst, scr.type(), avg / fpreAvg);
    }

    private class FaceHandler extends Handler {

        /* 这个构造方法必须有 */
        public FaceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_WHAT) {
                /*if (dstbmp == null) {
                    return;
                }
                Utils.bitmapToMat(dstbmp, detect_img);
                if (detect_img == null) {
                    return;
                }*/
                yuvImg.put(0, 0, mImageData);
                Mat rotateImg = new Mat(IMG_WIDTH, IMG_HEIGHT, CvType.CV_8UC3);//grayscaleImage.height(),grayscaleImage.width(),CvType.CV_8UC3);
                Imgproc.resize(yuvImg, rotateImg, new Size(yuvImg.width() * FACE_SCALE, yuvImg.height() * FACE_SCALE));


                Bitmap bmp = Bitmap.createBitmap(yuvImg.width(), yuvImg.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(yuvImg, bmp);
                FaceLocation flx = new FaceLocation();
                flx.setFaceBitmap(bmp);
                EventBus.getDefault().post(new MessageEvent<FaceLocation>(Type.FACE_RESULT, flx));

                if (true) {
                    return;
                }

                Imgproc.cvtColor(rotateImg, rotateImg, Imgproc.COLOR_YUV2RGB_NV12);//COLOR_YUV2GRAY_NV21);//COLOR_YUV2RGB_I420);//
                int blackLenght = (rotateImg.width() - rotateImg.height()) / 2;  // 需要扩充黑边的长度
                Core.copyMakeBorder(rotateImg, rotateImg, blackLenght, blackLenght, 0, 0, Core.BORDER_CONSTANT);    // 扩充画布

                // 旋转图片
                double angle = 90;
                Point center = new Point(rotateImg.width() / 2.0, rotateImg.height() / 2.0);
                Mat affineTrans = Imgproc.getRotationMatrix2D(center, angle, 1);
                Imgproc.warpAffine(rotateImg, rotateImg, affineTrans, rotateImg.size(), Imgproc.INTER_NEAREST);

                //截取中间的图片（去黑边）
                Rect Roi = new Rect(blackLenght, 0, (int) (IMG_HEIGHT * FACE_SCALE), (int) (IMG_WIDTH * FACE_SCALE));   // 因为旋转90度，所以高宽是反过来的
                rotateImg.submat(Roi).copyTo(grayscaleImage);

                Imgproc.cvtColor(grayscaleImage, grayscaleImage, Imgproc.COLOR_RGB2GRAY);   // 变黑白

                // 假定室内目前最高峰值亮度是160，所以按百分比计算就是除以十六
                int lastLight = currentLigth;
                currentLigth = 9 - (int) (getLightAvg(detect_img) / 16);
                AppContext.getInstance().lightOutsideLuminance(lastLight, currentLigth);
                faceRecognition(grayscaleImage);
            }
        }

        public void faceRecognition(Mat aInputFrame) {
            counter++;
            MatOfRect faces = new MatOfRect();
            MatOfRect eyes = new MatOfRect();
            //MatOfRect mouth = new MatOfRect();
            //Face_detected_image = new Mat();

            //aInputFrame.copyTo(grayscaleImage);
            //Imgproc.cvtColor(grayscaleImage, grayscaleImage, Imgproc.COLOR_RGB2GRAY);
            boolean face_recognized = false;

            if (cascadeClassifier_face != null) {
                cascadeClassifier_face.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                        new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
            //imageview.setVisibility(View.INVISIBLE);
            //notrecfaceImage.copyTo(recfaceImage);
            Rect[] facesArray;
            facesArray = faces.toArray();
            Mat eye_img = null;
            if (facesArray.length > 0) {
                for (int i = 0; i < 1/*facesArray.length*/; i++) {
                    // If there are any faces found, draw a rectangle around it
                    //Imgproc.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);//绘制脸框
                    /*int face_left = (int) (facesArray[i].x - facesArray[i].width * 0.1);
                    face_left = face_left > 0 ? face_left : 0;
                    int face_right = face_left + (int) (facesArray[i].width * 1.2);
                    int face_width = face_right < grayscaleImage.width() ? face_right - face_left : grayscaleImage.width() - face_left;
                    int face_top = (int) (facesArray[i].y - facesArray[i].height * 0.2);
                    face_top = face_top > 0 ? face_top : 0;
                    int face_bot = face_top + (int) (facesArray[i].height * 1.4);
                    int face_height = face_bot < grayscaleImage.height() ? face_bot - face_top : grayscaleImage.height() - face_top;
                    if (face_left < 50 || face_top < 50 || height - face_bot < 50 || width - face_right < 50)
                        break;*/
                    int face_left = facesArray[i].x, face_top = facesArray[i].y, face_width = facesArray[i].width, face_height = facesArray[i].height;
                    Rect faceArea = new Rect(face_left, face_top, face_width, face_height);
                    //Face_detected_image = aInputFrame.submat(faceArea);
                    //Imgproc.resize(Face_detected_image, Face_detected_image, new Size(FACE_WIDTH*4,FACE_HEIGHT*4));


                /*tmp_img = grayscaleImage.submat(faceArea);
                Imgproc.resize(tmp_img, tmp_img, new Size(FACE_WIDTH,FACE_HEIGHT));
                Photo.fastNlMeansDenoising(tmp_img, tmp_img,3,5,5);
                Imgproc.adaptiveThreshold(tmp_img, faceImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5,3);//3,5
                faceImage.copyTo(recfaceImage);    */
                    //Imgproc.resize(tmp_img, faceImage, faceImage.size());//脸部图片
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
                        //Imgproc.circle(aInputFrame,eye_left_center,10,new Scalar(0, 255, 0,0),1);
                        //Imgproc.circle(aInputFrame,eye_right_center,10,new Scalar(0, 255, 0,0),1);
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


                                switch (mode) {
                                    case Type.RECOGNITION_LOGIN:

                                        // 发送人脸坐标
                                        FaceLocation fl0 = new FaceLocation();
                                        fl0.setX(newfaceArray.x);
                                        fl0.setY(newfaceArray.y);
                                        fl0.setWidth(newfaceArray.width);
                                        fl0.setHeight(newfaceArray.height);
                                        EventBus.getDefault().post(new MessageEvent<>(Type.FACE_RESULT, fl0));

                                        // 人脸识别
                                        if (mfrs._database_status) {
                                            int minClass = 0;
                                            double minDist = 0;
                                            int rec_people = mfrs.predict(faceImage, minClass, minDist);
                                            //rec_people = mfrs.native_Predict(faceImage);
                                            if (rec_people > -1) {
                                                face_recognized = true;
                                                name_recognized = mfrs._name.get(rec_people);
                                                //Looper.prepare();//线程中调用UI必须添加此句
                                                //getMessage(mfrs._name.get(rec_people));
                                                //Looper.loop();
                                                //float alpha = (float) 0.5;
                                                //Btn[mfrs._people.indexOf(name_recognized)].setAlpha(alpha);//设置识别到的人显示出来
                                                // if (mfrs._match_ratio > Type.MATCH_RATIO && name_recognized != name_previous_recognized) {
                                                Log.d("CameraPreview", "-->> 识别率 = " + mfrs._match_ratio);

                                                // 平均识别率达到80开门
                                                if (mfrs._match_ratio >= Type.MATCH_RATIO) {
                                                    Identified = true;
                                                    name_previous_recognized = name_recognized;
                                                    counter = 0;
                                                    UserDaoHelp userDaoHelp = new UserDaoHelp();
                                                    User currentUser = userDaoHelp.selectUser(CameraPreview.this.getContext(), Long.parseLong(name_previous_recognized));

                                                    AppContext.getInstance().openDoor();

                                                    if (!AppContext.getInstance().isLogin() && errorCount > 0) {

                                                        // 移除停止留言计时器
                                                        if (mRecordingExitHandler != null) {
                                                            mRecordingExitHandler.removeCallbacks(mRecordingExitRunnable);
                                                        }
                                                        EventBus.getDefault().post(new MessageEvent<User>(Type.RECOGNITION_LOGIN, currentUser));
                                                        AppContext.getInstance().setCurrentUser(currentUser);
                                                    }

                                                    if (mfrs._match_ratio < 85) {
                                                        mfrs.dataBase.update_database(mfrs._name.get(rec_people), rec_people, -1, faceImage);  //ratio为-1表示添加新数据
                                                    } else {
                                                        mfrs.dataBase.update_database(mfrs._name.get(rec_people), rec_people, mfrs._rec_ratio.get(rec_people) + 1, faceImage);
                                                    }

                                                } else {
                                                    Log.d(TAG, "-->> 人脸识别失败--");
                                                    Identified = false;
                                                    //name_previous_recognized = null;

                                                    // 识别率小于70时，为陌生人脸
                                                    if (mfrs._match_ratio <= 70) {
                                                        errorCount--;
                                                    }

                                                    if (errorCount <= 0) {
                                                        // 失败10次后开启陌生人留言
                                                        // 禁止登录，等留言结束后再开启识别登录
                                                        EventBus.getDefault().post(new MessageEvent<Bitmap>(Type.START_GUEST_RECORDING, bmp));

                                                        // 开启计时器
                                                        if (!isStopRecording) {
                                                            isStopRecording = true;
                                                            mRecordingExitHandler = new Handler();
                                                            mRecordingExitHandler.postDelayed(mRecordingExitRunnable, 1000);    // 这行只能执行一次
                                                        }
                                                        errorCount = -100;  // 负100代表正在留言
                                                    }
                                                }

                                                stopRecordingTime = 0;  // 如果还能识别到人脸，那么就重新计时

                                            } else {
                                                face_recognized = false;
                                                mfrs._match_ratio = 0;

                                                if (counter > 10) {
                                                    counter = 0;
                                                    name_previous_recognized = null; //连续10帧识别失败,则清除之前的识别记录
                                                }
                                            }

                                        }
                                        break;

                                    case Type.RECOGNITION_REGISTER:
                                        // 人脸注册
                                        FaceLocation fl = new FaceLocation();
                                        fl.setX(newfaceArray.x);
                                        fl.setY(newfaceArray.y);
                                        fl.setWidth(newfaceArray.width);
                                        fl.setHeight(newfaceArray.height);
                                        fl.setFaceImage(faceImage);
                                        fl.setFaceDetectedImage(Face_detected_image);
                                        fl.setFaceBitmap(dstbmp);
                                        EventBus.getDefault().post(new MessageEvent<FaceLocation>(Type.FACE_RESULT, fl));
                                        break;
                                    default:
                                        if (DEBUG) {
                                            Log.e("CameraPreview", "-->> 请设置识别模式");
                                        }
                                }
                            /*if (mfrs._database_status) {
                                int minClass = 0;
                                double minDist = 0;
                                int rec_people = mfrs.predict(faceImage, minClass, minDist);
                                //rec_people = mfrs.native_Predict(faceImage);
                                if (rec_people > -1) {
                                    face_recognized = true;
                                    name_recognized = mfrs._name.get(rec_people);
                                    if (mfrs._match_ratio > 40 && name_recognized != name_previous_recognized) {
                                        Identified = true;
                                        counter = 0;
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

                            }*/
                                mat_rot.release();
                                mat_res.release();
                                mat_res_submat.release();
                            }
                        }
                    }
                }

            } else if (counter > 10) {
                counter = 0;
                name_previous_recognized = null; //连续10帧识别失败,则清除之前的识别记录
            }
            faces.release();
            eyes.release();
            if (eye_img != null) {
                eye_img.release();
            }
        }
    }

    // 停止留言，连续5秒钟检测不到人脸
    private Runnable mRecordingExitRunnable = new Runnable() {
        @Override
        public void run() {
            stopRecordingTime++;
            if (stopRecordingTime >= 5) {

                EventBus.getDefault().post(new MessageEvent<>(Type.STOP_GUEST_RECORDING, new Object()));
                isStopRecording = false;
                errorCount = 10;
                mRecordingExitHandler.removeCallbacks(this);
                return;
            }
            mRecordingExitHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void run() {
        while (cameraExists) {
            Date curTime = new Date(System.currentTimeMillis());//获取当前时间
            final double fps = 1000 / ((long) curTime.getTime() - (long) lastTime.getTime());
            FPS = String.valueOf(fps);
            lastTime = curTime;

            if (!isPause) {
                // obtaining display area to draw a large image
                if (winWidth == 0) {
                    winWidth = this.getWidth();
                    winHeight = this.getHeight();

                    /*if (winWidth * 3 / 4 <= winHeight) {
                        dw = 0;
                        dh = (winHeight - winWidth * 3 / 4) / 2;
                        rate = ((float) winWidth) / IMG_WIDTH;
                        rect = new android.graphics.Rect(dw, dh, dw + winWidth - 1, dh
                                + winWidth * 3 / 4 - 1);
                    } else {
                        dw = (winWidth - winHeight * 4 / 3) / 2;
                        dh = 0;
                        rate = ((float) winHeight) / IMG_HEIGHT;
                        rect = new android.graphics.Rect(dw, dh, dw + winHeight * 4 / 3 - 1, dh
                                + winHeight - 1);
                    }*/
                    rect = new android.graphics.Rect(0, 0, winHeight * 3 / 4, winHeight);
                }
                // obtaining a camera image (pixel data are stored in an array
                // in JNI).
                processCamera();
                // camera image to bmp


                mImageData = ctojava();
                bmp = changeYUV(mImageData);
                //ctojava();
                //pixeltobmp(bmp);        // 12
                // todo bmp图片最好在硬件层就旋转90度
                /*Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap dstbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);*/
                dstbmp = ImageUtils.rotateBitmap(bmp, 90, false);
                /*Mat dstMat = new Mat();
                Utils.bitmapToMat(dstbmp, dstMat);
                dstMat.convertTo(dstMat, dstMat.type(), 100 / getAvg(dstMat));
                Log.d(TAG, "-->> avg = " + getAvg(dstMat));
                Utils.matToBitmap(dstMat, dstbmp);*/

                /*if (detect_img == null) {
                    return;
                }
                Utils.bitmapToMat(dstbmp, detect_img);*/
                //if(!Identified)
                //faceRecognition(detect_img);
                // 人脸识别放在异步里

                if (Type.RECOGNITION_DEFAULT != mode) {
                    Message msg = Message.obtain();
                    msg.what = MSG_WHAT;
                    msg.setTarget(mFaceHandler);
                    msg.sendToTarget();
                }

                Canvas canvas = getHolder().lockCanvas();

                if (canvas != null) {
                    // draw camera bmp on canvas
                    mAntiAliasPaint.setAntiAlias(true);
                    canvas.setDrawFilter(mPaintFlagsDrawFilter); //设置图形、图片的抗锯齿。可用于线条等。按位或.
                    canvas.drawBitmap(dstbmp, null, rect, mAntiAliasPaint);
                    //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//设置画布背景颜色
                    Paint p = new Paint(); //创建画笔
                    p.setAntiAlias(true);
                    p.setColor(Color.GREEN);
                    p.setTextSize(30);
                    p.setStrokeWidth(5);
                    canvas.drawText("FPS:" + FPS, 30, 20, p);
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
            if (shouldStop) {
                shouldStop = false;
                stopCamera();
                Log.e(TAG, "break");
                Log.e(TAG, "线程退出");
                break;
            }
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (DEBUG)
            Log.d("WebCam", "surfaceCreated");
        if (bmp == null) {
            bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT,
                    Bitmap.Config.ARGB_8888);
        }
        // /dev/videox (x=cameraId + cameraBase) is used
        for (int i = 0; i < 20; i++) {
            File file = new File("dev/video" + i);
            if (file.exists()) {
                int ret = prepareCameraWithBase(cameraId, i);
                if (ret != -1) {
                    cameraExists = true;
                    processCamera();
                    // camera image to bmp
                    pixeltobmp(bmp);
                    width = bmp.getWidth();
                    height = bmp.getHeight();
                    absoluteFaceSize = (int) (height * 0.2);
                    yuvImg = new Mat(IMG_WIDTH * 3 / 2, IMG_HEIGHT, CvType.CV_8UC1);//screenHeight*3/2
                    detect_img = new Mat(width, height, CvType.CV_8UC3);
                    grayscaleImage = new Mat(width, height, CvType.CV_8UC1);
                    faceImage = new Mat(FACE_WIDTH, FACE_HEIGHT, CvType.CV_8UC1);
                    break;
                }
            }
        }
        if (cameraExists) {
            mainLoop = new Thread(this);
            mainLoop.start();
            mFaceHandler = new FaceHandler(mFaceHandlerThread.getLooper());// 开启人脸扫描计算线程
        } else {
            Toast.makeText(context, "没有检查到相机", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        if (DEBUG)
            Log.d("WebCam", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (DEBUG)
            Log.d("WebCam", "surfaceDestroyed");
        if (cameraExists) {
            shouldStop = true;
            cameraExists = false;
            /*while (shouldStop) {
                try {
                    Thread.sleep(100); // wait for thread stopping
                } catch (Exception e) {
                }
            }*/
        }
    }

    public Bitmap takePhoto() {
        //isPause = true;
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap dstbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        return dstbmp;
    }

    private ByteArrayOutputStream baos;
    private BitmapFactory.Options options = new BitmapFactory.Options();
    public byte[] rawImage;

    public Bitmap changeYUV(byte[] data) {
        /*BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;*/

        options.inPreferredConfig = Bitmap.Config.RGB_565;
        YuvImage yuvimage = new YuvImage(
                data,
                ImageFormat.YUY2,
                640,
                480,
                null);
        baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new android.graphics.Rect(0, 0, 640, 480), 50, baos);// 80--JPG图片的质量[0-100],100最高
        rawImage = baos.toByteArray();
        //将rawImage转换成bitmap
        return BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
    }

    /**
     * 设置使用模式
     *
     * @param mode 见Type类中"识别模式"
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    public void reStart() {
        isPause = false;
    }
}
