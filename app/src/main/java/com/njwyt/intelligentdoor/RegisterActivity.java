package com.njwyt.intelligentdoor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.njwyt.AppContext;
import com.njwyt.content.Address;
import com.njwyt.content.Type;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.FaceLocation;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.User;
import com.njwyt.utils.FileUtils;
import com.njwyt.view.RoundProgressBar;

import org.greenrobot.eventbus.Subscribe;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import seu.smartdoor.CameraPreview;
import seu.smartdoor.face_eigenface;

public class RegisterActivity extends BaseActivity {

    @BindView(R.id.camera_preview)
    CameraPreview usbCameraSurfaceView;
    @BindView(R.id.tv_back)
    TextView mTvBack;
    @BindView(R.id.view_scan_line)
    View mViewScanLine;
    @BindView(R.id.iv_background)
    ImageView mIvBackground;
    @BindView(R.id.round_progress_bar)
    RoundProgressBar mRoundProgressBar;

    private face_eigenface mfrs;

    private Bitmap recfacebitmap = null;
    private Handler handler = new Handler();
    private Handler mStopHandler;

    private UserDaoHelp mUserDaoHelp;
    private User mUser;
    private boolean firstRecognition = true;
    private String newPas;
    private String pwd;
    private boolean isStopRecognition = false;  // 是否进行人脸识别
    private int registerCount;                  // 录入照片次数
    private int progressCount;                  // 进度条记录

    private SoundPool sp;//声明一个SoundPool
    private int music;//定义一个整型用load（）；来设置suondID


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContext.getInstance().setDenyOpenOutDoor(true);

        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        //初始化提示音
        sp = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);//第一个参数为同时播放数据流的最大个数，第二数据流类型，第三为声音质量
        music = sp.load(this, R.raw.registe_head, 1); //把你的声音素材放到res/raw里，第2个参数即为资源文件，第3个为音乐的优先级

        mUserDaoHelp = new UserDaoHelp();
        mStopHandler = new Handler();
        mfrs = new face_eigenface(80, 3000);

        long dataFileSize = -1;
        String filename = Address.DATABASE + "data.bin";
        File fis = new File(filename);
        try {
            dataFileSize = FileUtils.getFileSize(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 判断data.bin文件大小不等于0
        if (dataFileSize != 0) {
            if (mfrs.read_dbase()) {
                mfrs.train();
            }
        }

        usbCameraSurfaceView.setMode(Type.RECOGNITION_REGISTER);
        initView();
        initListener();
        //startScanLineAnimation();
    }

    private void initView() {
        mRoundProgressBar.setMax(100);
    }

    private void initListener() {

        // 返回事件
        mTvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * 在数据库中初始化一个用户
     */
    private Long initUser() {

        List<User> userList = mUserDaoHelp.selectAllUser(this);
        mUser = new User();
        // 新的id是旧的最后一个id+1
        if (userList.size() == 0) {
            mUser.setId((long) 1);
        } else {
            mUser.setId(userList.get(userList.size() - 1).getId() + 1);
        }
        mUser.setFontSize(Type.FONTSIZE_MEDIUM);
        mUser.setLanguage(Type.LANGUAGE_CHINESE);
        mUser.setLevel(Type.DIFFICULTY_ADVANCED);
        mUser.setTheme(Type.THEME_SIMPLE);
        pwd = mUser.getId() + "";
        int pwdLenght = pwd.length();
        for (int i = 0; i < 4 - pwdLenght; i++) {
            pwd = "0" + pwd;
        }
        mUser.setPassword(pwd);
        //mUser.setHeadUrl("/sdcard/intelligentDoor/myImage/first.jpg");
        return mUserDaoHelp.insertUser(this, mUser);
    }

    /**
     * 从CamreaPreView获得通知
     *
     * @param event
     */
    @Subscribe
    public void getFaceLocation(final MessageEvent<FaceLocation> event) {
        if (event.getMessage() == Type.FACE_RESULT) {

            //((ImageView)findViewById(R.id.iv_temp_bmp)).setImageBitmap(event.getBody().getFaceBitmap());

            // 如果这是第一次识别成功，那么就初始化一个人
            if (firstRecognition) {
                initUser();
                firstRecognition = false;
            }

            if (!isStopRecognition) {
                FaceLocation fl = event.getBody();
                saveUser(fl.getFaceImage(), fl.getFaceDetectedImage(), fl.getFaceBitmap(), mUser.getId() + "");
                isStopRecognition = true;

                // 暂停两秒再录下一张
                mStopHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isStopRecognition = false;
                    }
                }, 200);
            }
        }
    }

    /**
     * 插入人脸识别数据库
     *
     * @param faceImage
     * @param cur_name
     */
    private void saveUser(final Mat faceImage, final Mat faceDetectedImage, final Bitmap faceBitmap, final String cur_name) {

        handler.post(new Runnable() {
            @Override
            public void run() {

                mfrs.dataBase.update_database(cur_name, 0, 0, faceImage);
                if (mfrs.read_dbase()) {
                    mfrs.train();
                }
                registerCount++;

                // todo 让滚动条转起来
                final Handler progressHandler = new Handler();
                progressHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (progressCount > registerCount * 5) {
                            progressHandler.removeCallbacks(this);
                            return;
                        }
                        progressCount++;
                        mRoundProgressBar.setProgress(progressCount);
                        progressHandler.postDelayed(this, 20);
                    }
                });

                if (registerCount == 20) {

                /*if (recfacebitmap != null) {
                    if (!recfacebitmap.isRecycled())
                        recfacebitmap.recycle();
                }
                recfacebitmap = Bitmap.createBitmap(faceDetectedImage.width(), faceDetectedImage.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(faceDetectedImage, recfacebitmap);*/
                    //保存下来的人脸识别头像
                    File f = new File(Address.MY_IMAGE, cur_name + ".jpg");
                    if (f.exists()) {
                        f.delete();
                    }
                    try {
                        FileOutputStream out = new FileOutputStream(f);
                        faceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.flush();
                        out.close();
                        // 这里把彩色照片的url存到数据库中
                        mUser.setHeadUrl(Address.MY_IMAGE + cur_name + ".jpg");
                        mUserDaoHelp.upDataUser(RegisterActivity.this, mUser);

                        Intent intent = new Intent();
                        intent.putExtra("pas", pwd);
                        intent.putExtra("mUser", mUser);
                        setResult(4, intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        //camera.stopPreview();
                        //onResume();
                        //showImageToast();
                        isStopRecognition = true;
                        handler.removeCallbacks(this);

                        //提示音        左声道(门外)   右声道(门内)
                        sp.play(music, 1.0f, 1.0f, 1, 0, 1);
                        Bitmap bmp = imageCrop(usbCameraSurfaceView.takePhoto());
                        //Bitmap bmp = usbCameraSurfaceView.takePhoto();
                        usbCameraSurfaceView.setVisibility(View.GONE);
                        mIvBackground.setBackground(new BitmapDrawable(null, bmp));

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 2000);
                    }
                    faceBitmap.recycle();
                    faceImage.release();
                    faceDetectedImage.release();
                }
            }
        });
    }

    @Override
    public void finish() {

        // 如果照片不到3张，那么就删除之前已保存的所有信息
        if (mUser != null && registerCount < 20) {
            // 删除data.bin中的数据
            mfrs.dataBase.delete_item(mUser.getId() + "");

            // 删除SQLite中的数据
            mUserDaoHelp.deleteUser(this, mUser.getId());

            // 禁止返回数据
            setResult(-1);
        }
        super.finish();
    }

    /**
     * 获得的图片保存本地
     *
     * @param bmp
     * @return
     */
    public static File saveImage(Bitmap bmp) {
        File appDir = new File("/sdcard/intelligentDoor/myImage/face/");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private void showImageToast() {
        //LayoutInflater的作用：对于一个没有被载入或者想要动态载入的界面，都需要LayoutInflater.inflate()来载入，LayoutInflater是用来找res/layout/下的xml布局文件，并且实例化
        LayoutInflater inflater = getLayoutInflater();//调用Activity的getLayoutInflater()
        View view = inflater.inflate(R.layout.toast_success, null); //加載layout下的布局
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, 0);//setGravity用来设置Toast显示的位置，相当于xml中的android:gravity或android:layout_gravity
        toast.setDuration(Toast.LENGTH_SHORT);//setDuration方法：设置持续时间，以毫秒为单位。该方法是设置补间动画时间长度的主要方法
        toast.setView(view); //添加视图文件
        toast.show();
    }

    private void startScanLineAnimation() {

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.ABSOLUTE, AppContext.getInstance().getScreenHeight());
        animation.setDuration(3000);
        animation.setRepeatCount(Animation.INFINITE);
        mViewScanLine.clearAnimation();
        mViewScanLine.startAnimation(animation);
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
}