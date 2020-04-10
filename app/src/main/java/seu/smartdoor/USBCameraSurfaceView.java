package seu.smartdoor;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.njwyt.intelligentdoor.R;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;

/**
 * USB摄像头支持
 */
public class USBCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    static final String tag = "USBCameraSurfaceView";
    private static final boolean DEBUG = true;
    protected Context context;
    private SurfaceHolder holder;
    Thread mainLoop = null;
    private static Bitmap bmp = null;
    private boolean isPause = false;
    public static boolean cameraExists = false;
    private static boolean shouldStop = false;
    public Date lastTime = new Date(System.currentTimeMillis());
    public String FPS;

    // / /dev/videox (x=cameraId+cameraBase) is used.
    // In some omap devices, system uses /dev/video[0-3],
    // so users must use /dev/video[4-].
    // In such a case, try cameraId=0 and cameraBase=4
    private int cameraId = 0;
    private int cameraBase = 0;
    //

    // This definition also exists in ImageProc.h.
    // Webcam must support the resolution 640x480 with YUYV format.
    static final int IMG_WIDTH = 640;
    static final int IMG_HEIGHT = 480;

    // The following variables are used to draw camera images.
    private int winWidth = 0;
    private int winHeight = 0;
    private Rect rect;
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
    public boolean face_recognized = false;
    public boolean Identified = false;
    public String name_recognized = null;
    public String name_previous_recognized = null;
    public org.opencv.core.Rect newfaceArray = new org.opencv.core.Rect();
    Mat detect_img = null;

    // JNI functions
    public native int prepareCamera(int videoid);

    public native int prepareCameraWithBase(int videoid, int camerabase);

    public native void processCamera();

    public native void stopCamera();

    public native void pixeltobmp(Bitmap bitmap);

    static {
        System.loadLibrary("CameraPreview");
    }


    public USBCameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public USBCameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        setZOrderOnTop(true);
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
        if (mfrs.read_dbase()) {
            // todo 训练更新识别有异常
            //mfrs.train();
            //mfrs._src.get(0).copyTo(faceImage);
        }
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

    int count = 0;

    @Override
    public void run() {
        while (true && cameraExists) {
            Date curTime = new Date(System.currentTimeMillis());//获取当前时间
            double fps = 1000 / ((long) curTime.getTime() - (long) lastTime.getTime());
            FPS = String.valueOf(fps);
            lastTime = curTime;

            if (!isPause) {
                Log.i(tag, "loop");
                // obtaining display area to draw a large image
                if (winWidth == 0) {
                    winWidth = this.getWidth();
                    winHeight = this.getHeight();

                    if (winWidth * 3 / 4 <= winHeight) {
                        dw = 0;
                        dh = (winHeight - winWidth * 3 / 4) / 2;
                        rate = ((float) winWidth) / IMG_WIDTH;
                        rect = new Rect(dw, dh, dw + winWidth - 1, dh
                                + winWidth * 3 / 4 - 1);
                    } else {
                        dw = (winWidth - winHeight * 4 / 3) / 2;
                        dh = 0;
                        rate = ((float) winHeight) / IMG_HEIGHT;
                        rect = new Rect(dw, dh, dw + winHeight * 4 / 3 - 1, dh
                                + winHeight - 1);
                    }
                }
                // obtaining a camera image (pixel data are stored in an array
                // in JNI).
                processCamera();
                // camera image to bmp
                pixeltobmp(bmp);

                Utils.bitmapToMat(bmp, detect_img);
                //if(!Identified)
                face_recognition(detect_img);
                Canvas canvas = getHolder().lockCanvas();

                if (canvas != null) {
                    // draw camera bmp on canvas
                    canvas.drawBitmap(bmp, null, rect, null);
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
                Log.e(tag, "break");
                Log.e(tag, "线程退出");
                break;
            }
        }

    }

    public void face_recognition(Mat aInputFrame) {
        counter++;
        MatOfRect faces = new MatOfRect();
        MatOfRect eyes = new MatOfRect();
        MatOfRect mouth = new MatOfRect();

        aInputFrame.copyTo(grayscaleImage);
        Imgproc.cvtColor(grayscaleImage, grayscaleImage, Imgproc.COLOR_RGB2GRAY);
        boolean face_recognized = false;

        if (cascadeClassifier_face != null) {
            cascadeClassifier_face.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }
        //imageview.setVisibility(View.INVISIBLE);
        //notrecfaceImage.copyTo(recfaceImage);
        org.opencv.core.Rect[] facesArray;
        facesArray = faces.toArray();
        Mat eye_img;
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
                if (face_left < 50 || face_top < 50 || height - face_bot < 50 || width - face_right < 50)
                    break;
                org.opencv.core.Rect faceArea = new org.opencv.core.Rect(face_left, face_top, face_width, face_height);
                //Face_detected_image = aInputFrame.submat(faceArea);
                //Imgproc.resize(Face_detected_image, Face_detected_image, new Size(FACE_WIDTH*4,FACE_HEIGHT*4));


                /*tmp_img = grayscaleImage.submat(faceArea);
                Imgproc.resize(tmp_img, tmp_img, new Size(FACE_WIDTH,FACE_HEIGHT));
                Photo.fastNlMeansDenoising(tmp_img, tmp_img,3,5,5);
                Imgproc.adaptiveThreshold(tmp_img, faceImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5,3);//3,5
                faceImage.copyTo(recfaceImage);    */
                //Imgproc.resize(tmp_img, faceImage, faceImage.size());//脸部图片
                org.opencv.core.Rect eyeArea = new org.opencv.core.Rect(faceArea.x, faceArea.y, faceArea.width, faceArea.height / 2);
                eye_img = new Mat();
                grayscaleImage.submat(eyeArea).copyTo(eye_img);//脸上半部分
                Imgproc.equalizeHist(eye_img, eye_img);

                //在脸上半部图片中寻眼睛
                if (cascadeClassifier_eyes != null) {
                    cascadeClassifier_eyes.detectMultiScale(eye_img, eyes, 1.1, 2, 2,
                            new Size(face_width / 15, face_height / 15), new Size(face_width / 3, face_height / 3));
                }
                org.opencv.core.Rect[] eyesArray = eyes.toArray();
                if (eyesArray.length == 2)//找到两只眼睛后找嘴巴
                {
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
                            if (mfrs._database_status) {
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

                            }
                        }
                    }
                }
            }

        } else if (counter > 10) {
            counter = 0;
            name_previous_recognized = null; //连续10帧识别失败,则清除之前的识别记录
        }
    }

    ;


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
            while (shouldStop) {
                try {
                    Thread.sleep(100); // wait for thread stopping
                } catch (Exception e) {
                }
            }
        }
        // todo java.lang.UnsatisfiedLinkError: Native method not found: com.njwyt.FaceUtil.CameraPreview.stopCamera:()
        // stopCamera();
    }

    public Bitmap TakePhoto() {
        isPause = true;
        return bmp;
    }

    public void reStart() {
        isPause = false;
    }
}
