package com.njwyt.intelligentdoor;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import com.njwyt.AppContext;
import com.njwyt.content.Address;
import com.njwyt.content.Type;
import com.njwyt.entity.FaceLocation;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.User;
import com.njwyt.fragment.OutSideMessageFragment;
import com.njwyt.intelligentdoor.databinding.ActivityOutdoorCameraBinding;
import com.njwyt.utils.ImageUtils;
import com.njwyt.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 门外摄像头专属Activity
 * Created by jason_samuel on 2017/11/27.
 */

public class OutDoorCameraActivity extends BaseActivity {

    private ActivityOutdoorCameraBinding binding;

    private final String TAG = "OutDoorCameraActivity";
    private final int FINISH_TIME = 30;

    private OutSideMessageFragment mOutSideMessageFragment;     // 访客录音Fragment

    private Handler handler;
    private Handler clearHandler;               // 用来消除人脸框的计时
    private Handler finishHandler;              // 记录一段时间没有人脸后关闭窗体
    private SurfaceHolder drawSurfaceHolder;    // 画人脸框的holder
    private int clearTimer = 0;                     // 记录清除倒计时
    private int finishTimer = FINISH_TIME;                // 记录关闭窗体倒计时
    private boolean isRecording = false;    // 是否正在录音

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_outdoor_camera);
        binding.usbCameraSurfaceView.setVisibility(View.VISIBLE);
        binding.usbCameraSurfaceView.setMode(getIntent().getIntExtra("recognition", Type.RECOGNITION_LOGIN));
        handler = new Handler();
        clearHandler = new Handler();
        finishHandler = new Handler();

        initListener();
        initFragment();
        initDrawSurfaceView();
        startClearHandler();
    }

    /**
     * 从CameraPreview获得通知
     *
     * @param event
     */
    @Subscribe
    public void getResult(final MessageEvent<User> event) {
        if (event.getMessage() == Type.RECOGNITION_LOGIN) {
            AppContext.getInstance().openDoor();
            // 看一看MainActivity中有没有登录成功
            if (!AppContext.getInstance().isLogin()) {
                finish();
                EventBus.getDefault().post(new MessageEvent<User>(Type.LOGIN_SUCCESS, event.getBody()));
                AppContext.getInstance().offLigth();
            } else {
                ToastUtil.showToast(OutDoorCameraActivity.this, "解锁");
            }
        }
    }

    /**
     * 从AppContext获得通知，门外传感器被触发
     */
    @Subscribe
    public void getResponse(MessageEvent event) {
        if (event.getMessage() == Type.OUTDOOR_RESPONSE) {
            finishTimer = FINISH_TIME;
        }
    }

    /**
     * 从CameraPreview获得通知
     * 用来开启或停止访客录音
     */
    @Subscribe
    public void startRecording(MessageEvent<Bitmap> event) {

        if (event.getMessage() == Type.START_GUEST_RECORDING && !isRecording) {
            isRecording = true;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    binding.tvStop.setVisibility(View.VISIBLE);
                }
            });
            mOutSideMessageFragment.startRecorder(saveGuestBitmap(event.getBody()));
            ToastUtil.showToast(this, "+++开始留言+++");
        } else if (event.getMessage() == Type.STOP_GUEST_RECORDING && isRecording) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    binding.tvStop.setVisibility(View.GONE);
                }
            });
            mOutSideMessageFragment.stopRecorder();
            ToastUtil.showToast(this, "---停止留言---");
            isRecording = false;
        }
    }

    /**
     * 开启清除线程
     */
    private void startClearHandler() {
        clearHandler.post(new Runnable() {
            @Override
            public void run() {

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
                if (clearTimer <= 2) {
                    clearTimer++;
                }
                clearHandler.postDelayed(this, 1000);
            }
        });
    }

    private void initListener() {

        binding.usbCameraSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.ivBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.tvStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOutSideMessageFragment.stopRecorder();
                binding.tvStop.setVisibility(View.GONE);
            }
        });

        // 计算页面何时结束
        finishHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // 发现窗体未正常关闭时，强制关闭
                if (binding.usbCameraSurfaceView.getVisibility() == View.GONE) {
                    finish();
                    finishHandler.removeCallbacks(this);
                    return;
                }

                if (finishTimer <= 0) {
                    finish();
                    finishHandler.removeCallbacks(this);
                    return;
                }
                finishTimer--;
                finishHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void initFragment() {
        mOutSideMessageFragment = new OutSideMessageFragment();
        getFragmentManager().beginTransaction().replace(R.id.fl_guest_recording, mOutSideMessageFragment).commit();
        getFragmentManager().beginTransaction().hide(mOutSideMessageFragment).commit();

    }

    /**
     * 加载扫描头像的方框
     */
    private void initDrawSurfaceView() {

        //binding.svDraw.setZOrderOnTop(true);
        binding.svDraw.setZOrderMediaOverlay(true);
        drawSurfaceHolder = binding.svDraw.getHolder();
        drawSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);//非常重要，否则不会有透明效果
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
            finishTimer = FINISH_TIME;
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

                    /*// 640 * 480的校准框
                    float x = fl.getX() * 1.08f;
                    float y = fl.getY() * 1.15f;
                    float width = fl.getWidth() * 1.5f;
                    float height = fl.getHeight() * 1.4f;*/

                    float x = fl.getX() * 2f;
                    float y = fl.getY() * 2f;
                    float width = fl.getWidth() * 2f;
                    float height = fl.getHeight() * 2f;

                    canvas.drawLine(x, y, x + width, y, paint);
                    canvas.drawLine(x, y, x, y + height, paint);
                    canvas.drawLine(x + width, y, x + width, y + height, paint);
                    canvas.drawLine(x, y + height, x + width, y + height, paint);

                    //binding.ivTempBmp.setImageBitmap(event.getBody().getBmp());
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
        }
    }

    /**
     * 开启人脸识别
     * todo 需要从门外人体感应监听类发送指令调用该方法
     */
    public void startRecognition() {

        // 设置USB摄像头为人脸识别模式
        binding.usbCameraSurfaceView.setMode(Type.RECOGNITION_LOGIN);
    }

    /**
     * 保存访客的照片
     */
    private String saveGuestBitmap(Bitmap bitmap) {

        //创建一个文件夹
        String guestName = System.currentTimeMillis() + ".png";
        File dir = new File(Address.GUEST);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File imageFile = new File(Address.GUEST, guestName);

        try {
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap = ImageUtils.rotateBitmap(bitmap, 90, false);
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "-->> path = " + imageFile.getPath());
        return imageFile.getPath();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOutSideMessageFragment.stopRecorder();
        binding.usbCameraSurfaceView.setVisibility(View.GONE);
        //Bitmap bmp = imageCrop(binding.usbCameraSurfaceView.takePhoto());
        binding.ivBackground.setBackground(new BitmapDrawable(null, binding.usbCameraSurfaceView.takePhoto()));
    }

    private Bitmap imageCrop(Bitmap bitmap) {
        // 算出屏幕当前比例
        int screenMod = mod(AppContext.getInstance().getScreenHeight(), AppContext.getInstance().getScreenWidth()); // 最大公约数
        int scaleHeight = AppContext.getInstance().getScreenHeight() / screenMod; // 屏幕高度比例
        int scaleWidth = AppContext.getInstance().getScreenWidth() / screenMod;   // 屏幕宽度比例
        // 根据屏幕比例截取bitmap的长宽
        int bmpWidth = bitmap.getHeight() / scaleHeight * scaleWidth;


        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bmpWidth, bitmap.getHeight(), null,
                false);
        bitmap.recycle();
        return bmp;
    }

    /**
     * 取最大公约数
     *
     * @param a
     * @param b
     * @return 最大公约数
     */
    private int mod(int a, int b) {
        if (a < b) {
            int temp;
            temp = a;
            a = b;
            b = temp;
        }
        if (0 == b) {
            return a;
        }
        return mod(a - b, b);
    }

    @Override
    public void finish() {
        super.finish();
        AppContext.getInstance().offLigth();
        AppContext.getInstance().setDenyOpenOutDoor(false);
    }
}
