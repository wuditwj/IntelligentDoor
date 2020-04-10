package com.njwyt.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageButton;

import com.njwyt.AppContext;
import com.njwyt.FaceUtil.SerialAPI;
import com.njwyt.content.Type;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.FaceLocation;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.User;
import com.njwyt.intelligentdoor.R;

import org.greenrobot.eventbus.EventBus;
import org.opencv.android.OpenCVLoader;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import seu.smartdoor.face_eigenface;

import static com.njwyt.content.Type.FACE_SCALE;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;

/**
 * 门内摄像头
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    //SurfaceView sView2; //用来画识别到的人脸区域
    //SurfaceHolder surfaceHolder2;
    private int previewWidth, previewHeight;
    private float X_SCALE, Y_SCALE;
    // 是否在预览中
    private boolean isPreview = false;
    public boolean isRun = false;

    private Mat yuvImg;
    // private CascadeClassifier cascadeClassifier;
    private CascadeClassifier cascadeClassifier_face;
    private CascadeClassifier cascadeClassifier_eyes;
    private CascadeClassifier cascadeClassifier_mouth;
    private Mat grayscaleImage;
    private int absoluteFaceSize;
    private Bitmap facebitmap, face;
    private final int FACE_WIDTH = 92;
    private final int FACE_HEIGHT = 112;
    // 1280x720, 960x540
    private final int CAMERA_WIDTH = 1280;
    private final int CAMERA_HEIGHT = 720;
    private final int MSG_WHAT = 8;
    private Mat faceImage;//=new Mat(FACE_WIDTH,FACE_HEIGHT,CvType.CV_8UC1);
    private Mat recfaceImage;//=new Mat(FACE_WIDTH,FACE_HEIGHT,CvType.CV_8UC1);
    private Mat notrecfaceImage;
    private int rec_people = -1;
    private Bitmap recfacebitmap = null;
    private Bitmap imageview_bitmap = null;
    private boolean start_detect = true;
    private face_eigenface mfrs;
    private org.opencv.core.Rect[] facesArray;
    private Bundle mBytesBundle;                // 存放字节数组的容器
    private android.graphics.Rect mFrameRect;

    private Thread drawThread;
    private Thread usb_camera_recThread;
    public Date lastTime = new Date(System.currentTimeMillis());
    public String FPS;
    //private Handler handler = null;
    private boolean lock = false;
    private int counter = 0;
    private int delay_count = 0;
    //private int matchRatioAvg = 0;              // 人脸识别平均值
    private int ratioCount = 0;                 // 识别次数
    private double avgRoi = 0;                     // 计算亮度
    private int recognitionTime;                // 人脸识别倒计时
    private int currentLight;
    public boolean face_recognized = false;
    public boolean Identified = false;
    public String name_recognized = null;
    public String name_previous_recognized = null;
    public Rect newfaceArray = new Rect();
    public ImageButton FaceRecognizedImageButton = null;
    public Mat Face_detected_image = null;
    public Bitmap Face_detected_bitmap = null;
    public Mat rotateImg = null;
    public Mat rotateMat = null;
    public SerialAPI serialAPI;

    private static final String TAG = "CameraSurfaceView";
    private Context mContext;
    private SurfaceHolder holder;
    private Camera mCamera;
    private int cameraId;          // 区分前后摄像头
    private OnRecognitionListener mOnRecognitionListener;

    private HandlerThread mFaceHandlerThread;
    private FaceHandler mFaceHandler;
    private Handler mRecognitionTimeHandler;    // 人脸识别倒计时
    private boolean isRecognition = false;
    private boolean isLightOn;


    public CameraSurfaceView(Context context) {
        this(context, null);

    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;

        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.CameraSurfaceView);
        cameraId = mTypedArray.getColor(R.styleable.CameraSurfaceView_cameraId, 0);

        initValue();
        initThread();
        initView();
    }

    private void initValue() {
        //mFrameRect = new android.graphics.Rect(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
    }

    private void initThread() {

        mFaceHandlerThread = new HandlerThread("faceThread");
        mFaceHandlerThread.start();
        mRecognitionTimeHandler = new Handler();
        mRecognitionTimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (recognitionTime == -10) {
                    recognitionTime = 0;
                }

                if (recognitionTime > 0) {
                    recognitionTime--;
                } else {
                    if (isLightOn) {
                        // 关闭门内补光灯
                        isLightOn = false;
                        recognitionTime = -10;
                        AppContext.getInstance().lightInsideLuminance(currentLight, 0);
                        mRecognitionTimeHandler.postDelayed(this, 2000);
                        return;
                    }
                }
                mRecognitionTimeHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    /**
     * 初始化脸框大小
     */
    private void initRotation() {
        double angle = 90;  // 旋转角度（前置摄像头）
        if (cameraId == 0) {
            angle = -90;    // （后置摄像头）
        }
        double scale = 1; // 缩放尺度
        // 旋转图片
        Point center = new Point(rotateImg.width() / 2.0, rotateImg.height() / 2.0);
        rotateMat = Imgproc.getRotationMatrix2D(center, -angle, scale);
    }

    private void initListener() {

        // mCamera.addCallbackBuffer(new byte[previewWidth * 3 / 2 * previewHeight]);
        // 摄像头每帧返回数据
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] bytes, Camera camera) {

                if (!AppContext.getInstance().isLogin()) {

                    /*if (mFaceTask != null) {
                        switch (mFaceTask.getStatus()) {
                            case RUNNING:
                                return;
                            case PENDING:
                                mFaceTask.cancel(false);
                                break;
                        }
                    }
                    mFaceTask = new FaceTask(bytes);
                    mFaceTask.execute();*/

                    if (!isRecognition) {
                        mBytesBundle.putByteArray("bytes", bytes);
                        Message msg = Message.obtain();
                        msg.what = MSG_WHAT;
                        msg.setData(mBytesBundle);
                        msg.setTarget(mFaceHandler);
                        msg.sendToTarget();
                    }
                }
                //camera.addCallbackBuffer(bytes);
            }
        });
    }

    private class FaceHandler extends Handler {

        // 这个构造方法必须有
        public FaceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_WHAT) {
                isRecognition = true;
                Bundle bundle = msg.getData();
                // 接收消息
                Date curTime = new Date(System.currentTimeMillis());//获取当前时间
                double fps = 1000 / ((long) curTime.getTime() - (long) lastTime.getTime());
                FPS = String.valueOf(fps);
                lastTime = curTime;
                Log.d(TAG, "-->> FPS = " + FPS);

                yuvImg.put(0, 0, bundle.getByteArray("bytes"));

                // 检测门前是否移动，如果有移动就开灯
                double lastAvgRoi = avgRoi;
                avgRoi = getAvgROI(yuvImg, (int) (CAMERA_HEIGHT * FACE_SCALE), (int) (CAMERA_WIDTH * FACE_SCALE));
                //Log.d(TAG, "-->> avg roi = " + avgRoi);
                //Log.d(TAG, "-->> sub abs = " + Math.abs(lastAvgRoi - avgRoi));
                if (Math.abs(lastAvgRoi - avgRoi) >= 0.4 && recognitionTime >= 0) {
                    //Log.d(TAG, "-->> 开灯");
                    if (!isLightOn) {
                        isLightOn = true;
                        currentLight = 9 - (int) avgRoi / 16;
                        AppContext.getInstance().lightInsideLuminance(0, currentLight);
                    }
                    // 开启10秒的人脸识别
                    recognitionTime = 5;
                }

                // 有物体在门内移动时才进一步图像操作和人脸识别
                if (recognitionTime > 0) {

                    Mat rotateImg = new Mat(CAMERA_WIDTH, CAMERA_HEIGHT, CvType.CV_8UC3);//grayscaleImage.height(),grayscaleImage.width(),CvType.CV_8UC3);
                    Imgproc.resize(yuvImg, rotateImg, new Size(yuvImg.width() * FACE_SCALE, yuvImg.height() * FACE_SCALE));
                    Imgproc.cvtColor(rotateImg, rotateImg, Imgproc.COLOR_YUV2RGB_NV21);//COLOR_YUV2GRAY_NV21);//COLOR_YUV2RGB_I420);//
                    int blackLenght = (rotateImg.width() - rotateImg.height()) / 2;  // 需要扩充黑边的长度
                    Core.copyMakeBorder(rotateImg, rotateImg, blackLenght, blackLenght, 0, 0, Core.BORDER_CONSTANT);    // 扩充画布

                    // 旋转图片
                    double angle = 90;  // 旋转角度（前置摄像头）
                    if (cameraId == 0) {
                        angle = -90;    // （后置摄像头）
                    }
                    Point center = new Point(rotateImg.width() / 2.0, rotateImg.height() / 2.0);
                    Mat affineTrans = Imgproc.getRotationMatrix2D(center, angle, 1);
                    Imgproc.warpAffine(rotateImg, rotateImg, affineTrans, rotateImg.size(), Imgproc.INTER_NEAREST);

                    //截取中间的图片（去黑边）
                    Rect Roi = new Rect(blackLenght, 0, (int) (CAMERA_HEIGHT * FACE_SCALE), (int) (CAMERA_WIDTH * FACE_SCALE));   // 因为旋转90度，所以高宽是反过来的
                    rotateImg.submat(Roi).copyTo(grayscaleImage);

                    Imgproc.cvtColor(grayscaleImage, grayscaleImage, Imgproc.COLOR_RGB2GRAY);   // 变黑白

                    affineTrans.release();
                    rotateImg.release();
                    faceRecognition();
                } else {
                    try {
                        System.gc();
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                /*Bitmap bmp = Bitmap.createBitmap(grayscaleImage.width(), grayscaleImage.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(grayscaleImage, bmp);
                FaceLocation flx = new FaceLocation();
                flx.setFaceBitmap(bmp);
                EventBus.getDefault().post(new MessageEvent<FaceLocation>(Type.FACE_RESULT, flx));*/



                /*Imgproc.cvtColor(rotateImg, rotateImg, Imgproc.COLOR_YUV2RGB_NV21);//COLOR_YUV2GRAY_NV21);//COLOR_YUV2RGB_I420);//
                Core.copyMakeBorder(rotateImg, rotateImg, (previewHeight - previewWidth) / 2, (previewHeight - previewWidth) / 2, 0, 0, Core.BORDER_CONSTANT);//放大画布1920*1920
                Imgproc.warpAffine(rotateImg, rotateImg, rotateMat, rotateImg.size());//旋转
                Rect Roi = new Rect((int) ((previewHeight - previewWidth) / 2), 0, (int) (previewWidth), (int) (previewHeight));//去黑边
                Mat detectImage = new Mat();
                rotateImg.submat(Roi).copyTo(detectImage);

                rotateImg.copyTo(grayscaleImage);
                Imgproc.cvtColor(grayscaleImage, grayscaleImage, Imgproc.COLOR_RGB2GRAY);*/

                /*byte[] bitmapdata = bundle.getByteArray("bytes");
                YuvImage yuvimage = new YuvImage(bitmapdata, ImageFormat.NV21, CAMERA_WIDTH, CAMERA_HEIGHT, null); //20、20分别是图的宽度与高度
                ByteArrayOutputStream mFrameBAOS = new ByteArrayOutputStream();
                android.graphics.Rect frameRect = new android.graphics.Rect(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
                yuvimage.compressToJpeg(frameRect, 50, mFrameBAOS);//50--JPG图片的质量[0-100],100最高
                byte[] jdata = mFrameBAOS.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
                bitmap = ImageUtils.rotateBitmap(bitmap, 90, false);
                Utils.bitmapToMat(bitmap, grayscaleImage);
                Imgproc.cvtColor(grayscaleImage, grayscaleImage, Imgproc.COLOR_RGB2GRAY);*/

                // 检测门前是否移动，如果有移动就开灯
                /*int lastAvgRoi = avgRoi;
                avgRoi = (int) getAvgROI(grayscaleImage, CAMERA_HEIGHT, CAMERA_WIDTH);
                //Log.d(TAG, "-->> avg roi = " + avgRoi);
                if (Math.abs(lastAvgRoi - avgRoi) > 10) {
                    //Log.d(TAG, "-->> 开灯");
                    AppContext.getInstance().onLigth();
                }

                faceRecognition();

                rotateImg.release();*/
                //rotateMat.release();
                //bitmap.recycle();
                bundle.clear();

                isRecognition = false;
            }
        }

        private void faceRecognition() {

            if (AppContext.getInstance().isLogin()) {
                return;
            }
            if (counter <= 20) {
                counter++;
            }
            MatOfRect faces = new MatOfRect();
            MatOfRect eyes = new MatOfRect();
            //MatOfRect mouth = new MatOfRect();

            face_recognized = false;

            //if(counter>20)start_detect=true;
            // Use the classifier to detect faces
            if (cascadeClassifier_face != null) {
                cascadeClassifier_face.detectMultiScale(grayscaleImage, faces, 1.1, 1, 1,
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
                    if (face_left < 50 || face_top < 50 || previewHeight - face_bot < 50 || previewWidth - face_right < 50)
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

                    FaceLocation fl = new FaceLocation();
                    fl.setX(faceArea.x);
                    fl.setY(faceArea.y + faceArea.height / 5);
                    fl.setWidth(faceArea.width);
                    fl.setHeight(faceArea.height);
                    EventBus.getDefault().post(new MessageEvent<>(Type.FACE_RESULT, fl));

                    //在脸上半部图片中寻眼睛
                    if (cascadeClassifier_eyes != null) {
                        cascadeClassifier_eyes.detectMultiScale(eye_img, eyes, 1.1, 1, 1,
                                new Size(face_width / 15, face_height / 15), new Size(face_width / 3, face_height / 3));
                    }
                    Rect[] eyesArray = eyes.toArray();
                    if (eyesArray.length == 2) {

                        recognitionTime = 5;   // 检测到人脸后，延长人脸识别时间
                        // 通过眼睛在脸上的位置+脸在画框上的位置+眼睛的宽或高除以2，算出左右眼的中心点
                        Point eye_left_center = new Point(eyesArray[0].x + eyesArray[0].width / 2 + faceArea.x, eyesArray[0].y + eyesArray[0].height / 2 + faceArea.y);
                        Point eye_right_center = new Point(eyesArray[1].x + eyesArray[1].width / 2 + faceArea.x, eyesArray[1].y + eyesArray[1].height / 2 + faceArea.y);
                        //Imgproc.circle(aInputFrame,eye_left_center,10,new Scalar(0, 255, 0,0),1);
                        //Imgproc.circle(aInputFrame,eye_right_center,10,new Scalar(0, 255, 0,0),1);

                        // 计算两眼中心点之间距离，并发送通知
                        double faceDistance = abs(eye_left_center.x - eye_right_center.x);
                        if (faceDistance < face_width / 4) break;
                        EventBus.getDefault().post(new MessageEvent<Integer>(Type.CHANGE_DISTANCE, (int) faceDistance));
                        // 如果距离未达到最近距离，那么就不进行人脸识别
                        if (faceDistance < Type.DISTANCE_CLOSE) {
                            break;
                        }

                        if (abs(eye_left_center.y - eye_right_center.y) < abs(eye_left_center.x - eye_right_center.x) / 5) {//小角度倾斜
                            double tan_rotate_angle = (eye_left_center.y - eye_right_center.y) / (eye_left_center.x - eye_right_center.x);
                            double rotate_angle = atan(tan_rotate_angle);

                            //newfaceArray.height = (int) (abs((eye_left_center.y + eye_right_center.y) / 2 - mouth_center.y) * 1.7);
                            newfaceArray.width = (int) (abs((eye_left_center.x - eye_right_center.x) * cos(rotate_angle)) * 1.7);
                            newfaceArray.height = (int) (newfaceArray.width * FACE_HEIGHT / FACE_WIDTH);
                            if (newfaceArray.height < face_height / 3) break;
                            if (newfaceArray.height < faceArea.height && newfaceArray.width < faceArea.width) {// && start_detect) {
                                Point newfaceCenter = new Point();
                                newfaceCenter.y = (int) (abs((eye_left_center.y + eye_right_center.y) / 2 + newfaceArray.height / 5.0));//面部采集抬高20pixel
                                newfaceCenter.x = (int) (abs(eye_left_center.x + eye_right_center.x) / 2);

                                // 为大图像旋转
                                Mat mat_rot = Imgproc.getRotationMatrix2D(newfaceCenter, rotate_angle * 180 / PI, 1.0);
                                Mat mat_res = new Mat(); // result
                                Imgproc.warpAffine(grayscaleImage, mat_res, mat_rot, grayscaleImage.size(), Imgproc.INTER_CUBIC);
                                // 从旋转后的外框获取新图像
                                int offsetX = (int) newfaceCenter.x - newfaceArray.width / 2;
                                int offsetY = (int) newfaceCenter.y - newfaceArray.height / 2;

                                // 从大图像中抠出人脸
                                Mat mat_res_submat = mat_res.submat(offsetY, offsetY + newfaceArray.height, offsetX, offsetX + newfaceArray.width);
                                newfaceArray.x = offsetX;
                                newfaceArray.y = offsetY;
                                facesArray[i].width = newfaceArray.width;
                                facesArray[i].height = newfaceArray.height;
                                Imgproc.resize(mat_res_submat, faceImage, new Size(FACE_WIDTH, FACE_HEIGHT));

                                //Photo.fastNlMeansDenoising(mat_res_submat, mat_res_submat,3,7,21);
                                //void fastNlMeansDenoising(Mat src, Mat& dst, float h = 3, int templateWindowSize = 7, int searchWindowSize = 21)
                                Imgproc.equalizeHist(faceImage, faceImage);
                                if (mfrs._database_status) {
                                    int minClass = 0;
                                    double minDist = 0;
                                    //开始人脸识别
                                    rec_people = mfrs.predict(faceImage, minClass, minDist);
                                    //rec_people = mfrs.native_Predict(faceImage);
                                    if (rec_people > -1) {
                                        face_recognized = true;
                                        name_recognized = mfrs._name.get(rec_people);
                                        //Looper.prepare();//线程中调用UI必须添加此句
                                        //getMessage(mfrs._name.get(rec_people));
                                        //Looper.loop();
                                        float alpha = (float) 0.5;
                                        //Btn[mfrs._people.indexOf(name_recognized)].setAlpha(alpha);//设置识别到的人显示出来
                                        Log.d(TAG, "-->> 识别率 = " + mfrs._match_ratio);
                                        int matchRatio = mfrs._match_ratio + 20;
                                        //FaceLocation fl = new FaceLocation();
                                        fl.setMatchRatio(matchRatio);
                                        EventBus.getDefault().post(new MessageEvent<FaceLocation>(Type.FACE_RESULT, fl));

                                        // if (mfrs._match_ratio >= 75 && name_recognized != name_previous_recognized) {
                                        if (matchRatio >= Type.MATCH_RATIO) {
                                            Identified = true;
                                            name_previous_recognized = name_recognized;
                                            counter = 0;
                                            if (AppContext.getInstance().getCurrentUser() == null) {
                                                UserDaoHelp userDaoHelp = new UserDaoHelp();
                                                User currentUser = userDaoHelp.selectUser(CameraSurfaceView.this.getContext(), Long.parseLong(name_previous_recognized));
                                                EventBus.getDefault().post(new MessageEvent<User>(Type.LOGIN_SUCCESS, currentUser));
                                                AppContext.getInstance().setCurrentUser(currentUser);
                                            }
                                            //mOnRecognitionListener.recognitionSuccess(currentUser);

                                            /*if (matchRatio < 85) {
                                                mfrs.dataBase.update_database(mfrs._name.get(rec_people), rec_people, -1, faceImage);  //ratio为-1表示添加新数据
                                            } else {
                                                mfrs.dataBase.update_database(mfrs._name.get(rec_people), rec_people, mfrs._rec_ratio.get(rec_people) + 1, faceImage);
                                            }*/

                                        } else {
                                            //Log.d(TAG, "-->> 人脸识别失败--");
                                            Identified = false;
                                            //name_previous_recognized = null;
                                        }


                                    } else {
                                        face_recognized = false;
                                        mfrs._match_ratio = 0;

                                        if (counter > 10) {
                                            counter = 0;
                                            name_previous_recognized = null; //连续10帧识别失败,则清除之前的识别记录
                                        }
                                    }

                                }
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
            grayscaleImage.release();
            faceImage.release();
        }

    }

    /**
     * OpenCV 实现获取平均亮度（Java方式)
     *
     * @param img    传入图片(以图片中心计算感兴趣区域)
     * @param width  感兴趣区域的宽
     * @param height 感兴趣区域的长
     * @return 感兴趣区域的返回值
     */
    private double getAvgROI(Mat img, int width, int height) {
        //Imgproc.cvtColor(img,gray,Imgproc.COLOR_RGB2GRAY);   //转换为灰度
        Mat roi = new Mat();
        Rect roiRect = new Rect();
        roiRect.x = width / 2;
        roiRect.y = 0;
        roiRect.width = width / 2;
        roiRect.height = height;
        img.submat(roiRect).copyTo(roi);
        Scalar scalar = Core.mean(roi);
        roi.release();
        return scalar.val[0];
    }

    private void initView() {
        holder = getHolder();//获得surfaceHolder引用
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//设置类型
        //add by zx 应该放在resume里
        //sView2 =  findViewById(R.id.surface_view2);
        //sView2.setZOrderOnTop(true);
        //surfaceHolder2 = sView2.getHolder();
        //surfaceHolder2 .setFormat(PixelFormat.TRANSLUCENT);//非常重要，否则不会有透明效果

        if (!OpenCVLoader.initDebug()) {
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
            delay_count = 0;
        }
        // todo 关于门锁操作需要打开
        /*if(serialAPI.OpenSerialPort()<0)  //串口开门通讯类
            Toast.makeText(this.getContext(),"Fail to open the serial port!",Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this.getContext(),"Serial port openned success!",Toast.LENGTH_SHORT).show();*/

        delay_count = 0;
        initializeOpenCVDependencies(); //人脸识别数据库初始化
        faceImage = new Mat(FACE_WIDTH, FACE_HEIGHT, CvType.CV_8UC1);
        //recfaceImage = new Mat(FACE_WIDTH, FACE_HEIGHT, CvType.CV_8UC1);
        rotateImg = new Mat(previewHeight, previewWidth, CvType.CV_8UC3);//grayscaleImage.height(),grayscaleImage.width(),CvType.CV_8UC3);
        mfrs = new face_eigenface(80, 3000);
        if (mfrs.read_dbase())  //读取已存人脸库数据
        {
            mfrs.train();   //训练更新识别
            //mfrs._src.get(0).copyTo(faceImage);
        }
        /*RelativeLayout layouts = (RelativeLayout)findViewById(R.id.usb_camera_layoutB);
        cp = new CameraPreview(layouts.getContext());
        layouts.addView(cp);
        layouts.setTranslationZ(100);*/
        //add by zx 应该放在resume里

    }

    private void setCameraParams(int width, int height) {
        //Log.i(TAG, "setCameraParams  width=" + width + "  height=" + height);
        Camera.Parameters parameters = mCamera.getParameters();
        // 获取摄像头支持的PictureSize列表
        /*List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        for (Camera.Size size : pictureSizeList) {
            Log.i(TAG, "pictureSizeList size.width=" + size.width + "  size.height=" + size.height);
        }
        *//**从列表中选取合适的分辨率*//*
        Camera.Size picSize = getProperSize(pictureSizeList, ((float) height / width));
        if (null == picSize) {
            Log.i(TAG, "null == picSize");
            picSize = parameters.getPictureSize();
        }
        Log.i(TAG, "picSize.width=" + picSize.width + "  picSize.height=" + picSize.height);
        // 根据选出的PictureSize重新设置SurfaceView大小
        float w = 800;//picSize.width;
        float h = 600;//picSize.height;
        parameters.setPictureSize(picSize.width, picSize.height);
        this.setLayoutParams(new RelativeLayout.LayoutParams((int) (height * (h / w)), height));

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : previewSizeList) {
            Log.i(TAG, "previewSizeList size.width=" + size.width + "  size.height=" + size.height);
        }
        Camera.Size preSize = getProperSize(previewSizeList, ((float) height) / width);
        if (null != preSize) {
            Log.i(TAG, "preSize.width=" + preSize.width + "  preSize.height=" + preSize.height);
            //parameters.setPreviewSize(preSize.width, preSize.height);
            parameters.setPreviewSize(800, 600);
        }*/

        // 获取摄像头支持的PictureSize列表
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        for (Camera.Size size : pictureSizeList) {
            Log.i(TAG, "pictureSizeList size.width=" + size.width + "  size.height=" + size.height);
        }

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        for (Camera.Size size : previewSizeList) {
            Log.i(TAG, "previewSizeList size.width=" + size.width + "  size.height=" + size.height);
        }

        parameters.setRecordingHint(true);//去掉这句，12fps
        //parameters.setAutoExposureLock(true);//去掉这句，30fps
        //parameters.setAutoWhiteBalanceLock(true);//去掉这句，30fps
        parameters.setPreviewFpsRange(15000, 30000);
        parameters.setPictureSize(CAMERA_WIDTH, CAMERA_HEIGHT);
        parameters.setPreviewSize(CAMERA_WIDTH, CAMERA_HEIGHT);
        parameters.setJpegQuality(80); // 设置照片质量
        if (parameters.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦模式
        }

        mCamera.cancelAutoFocus();//自动对焦。
        mCamera.setDisplayOrientation(90);// 设置PreviewDisplay的方向，效果就是将捕获的画面旋转多少度显示
        mCamera.setParameters(parameters);
    }

    /**
     * 拍照
     */
    public void takePicture(final OnTakePicture<Bitmap> onTakePicture) {
        // 当调用camera.takePiture方法后，camera关闭了预览，这时需要调用startPreview()来重新开启预览
        //setCameraParams(mCamera, AppContext.getInstance().getScreenWidth(), AppContext.getInstance().getScreenHeight());
        try {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera Camera) {
                    Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                    onTakePicture.onTakePictureFinish(bm);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        //mCamera.stopPreview();// 关闭预览
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "-->> surfaceCreated");
        if (mCamera == null) {
            mCamera = Camera.open(cameraId);//开启相机
            try {
                mCamera.setPreviewDisplay(holder);//摄像头画面显示在Surface上
                setCameraParams(AppContext.getInstance().getScreenWidth(), AppContext.getInstance().getScreenHeight());
                previewWidth = mCamera.getParameters().getPreviewSize().height;
                previewHeight = mCamera.getParameters().getPreviewSize().width;
                yuvImg = new Mat(previewWidth * 3 / 2, previewHeight, CvType.CV_8UC1);//screenHeight*3/2
                grayscaleImage = new Mat(previewHeight, previewWidth, CvType.CV_8UC3);//此处初始化为旋转后的尺寸
                absoluteFaceSize = (int) (previewHeight * 0.2);
                Face_detected_image = new Mat();
                mFaceHandler = new FaceHandler(mFaceHandlerThread.getLooper());// 开启人脸扫描计算线程
                mBytesBundle = new Bundle();
                initRotation();
                initListener();
                // 读取已存人脸库数据
                if (mfrs.read_dbase()) {
                    mfrs.train();   //训练更新识别
                    //mfrs._src.get(0).copyTo(faceImage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            Log.i(TAG, "surfaceDestroyed");
            //mFaceHandlerThread.getLooper().quit();// 退出线程
            mCamera.stopPreview();//停止预览
            mCamera.setPreviewCallback(null);
            mCamera.release();//释放相机资源
            mCamera = null;
            //holder = null;
        }
    }

    /**
     * 手动释放相机资源
     */
    public void destroyed() {
        if (mCamera != null) {
            Log.i(TAG, "surfaceDestroyed");
            mCamera.stopPreview();//停止预览
            mCamera.setPreviewCallback(null);
            mCamera.release();//释放相机资源
            mCamera = null;
        }
    }

    /**
     * 摄像头每帧返回事件
     */
    private void onCallBack() {

    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
        mCamera.stopPreview();//停掉原来摄像头的预览
        mCamera.setPreviewCallback(null);
        mCamera.release();//释放资源
        mCamera = null;//取消原来摄像头
        mCamera = Camera.open(cameraId);//打开当前选中的摄像头
        try {
            setCameraParams(AppContext.getInstance().getScreenWidth(), AppContext.getInstance().getScreenHeight());
            mCamera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();//开始预览
    }

    public int getCameraId() {
        return cameraId;
    }

    /**
     * 拍照回调接口
     *
     * @param <T>
     */
    public interface OnTakePicture<T> {
        void onTakePictureFinish(T body);
    }


    public static String getRootPath() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStorageDirectory().getAbsolutePath(); // filePath:  /sdcard/
        } else {
            return Environment.getDataDirectory().getAbsolutePath() + "/data"; // filePath:  /data/data/
        }
    }

    private void initializeOpenCVDependencies() {
        /*String savingDirectory = getRootPath() + "/myImage";
        File dir = new File(savingDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }*/
        File cascadeDir = getContext().getDir("cascade", Context.MODE_PRIVATE);

        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            /*File mCascadeFile = new File(savingDirectory + "/lbpcascade_frontalface.xml");
            if (!mCascadeFile.exists()) {
                mCascadeFile.createNewFile();
            }*/
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
            cascadeClassifier_face = new CascadeClassifier(mCascadeFile.getPath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading face cascade", e);
        }
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
            /*File mCascadeFile = new File(savingDirectory + "/haarcascade_eye_tree_eyeglasses.xml");
            if (!mCascadeFile.exists()) {
                mCascadeFile.createNewFile();
            }*/
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
            //Thread.sleep(1000);
            cascadeClassifier_eyes = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading eye cascade", e);
        }
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.haarcascade_mcs_mouth);
            /*File mCascadeFile = new File(savingDirectory + "/haarcascade_mcs_mouth.xml");
            if (!mCascadeFile.exists()) {
                mCascadeFile.createNewFile();
            }*/
            File mCascadeFile = new File(cascadeDir, "haarcascade_mcs_mouth.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();


            // Load the cascade classifier
            //Thread.sleep(1000);
            cascadeClassifier_mouth = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading mouth cascade", e);
        }
    }

    /*
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            Log.d(TAG, "-->> onPreviewFrame");
            Date curTime = new Date(System.currentTimeMillis());//获取当前时间
            double fps = 1000 / ((long) curTime.getTime() - (long) lastTime.getTime());
            FPS = String.valueOf(fps);
            lastTime = curTime;
            yuvImg.put(0, 0, bytes);
            //sView2.setBackgroundColor(Color.TRANSPARENT);
            //sView2.setAlpha((float)0.5);


            //Imgproc.cvtColor(yuvImg, rotateImg, Imgproc.COLOR_YUV2RGB_NV21);//COLOR_YUV2GRAY_NV21);//COLOR_YUV2RGB_I420);//
            Imgproc.cvtColor(yuvImg, rotateImg, Imgproc.COLOR_YUV2RGB_YV12);//FriendArm

            Core.copyMakeBorder(rotateImg, rotateImg, (previewHeight - previewWidth) / 2, (previewHeight - previewWidth) / 2, 0, 0, Core.BORDER_CONSTANT);//放大画布1920*1920
            Imgproc.warpAffine(rotateImg, rotateImg, rotateMat, rotateImg.size());//旋转
            Bitmap detected_bitmap = Bitmap.createBitmap(rotateImg.width(), rotateImg.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rotateImg, detected_bitmap);


            Rect Roi = new Rect((previewHeight - previewWidth) / 2, 0, previewWidth, previewHeight);//去黑边
            Mat detectImage = new Mat();
            rotateImg.submat(Roi).copyTo(detectImage);
            Bitmap detected_bitmap1 = Bitmap.createBitmap(detectImage.width(), detectImage.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(detectImage, detected_bitmap1);
            detectImage.copyTo(grayscaleImage);
            Imgproc.cvtColor(grayscaleImage, grayscaleImage, Imgproc.COLOR_RGB2GRAY);

            faceRecognition(detectImage);
        }*/

    public void setOnRecognitionListener(OnRecognitionListener onRecognitionListener) {
        mOnRecognitionListener = onRecognitionListener;
    }

    public interface OnRecognitionListener {
        void recognitionSuccess(User currentUser);
    }
}