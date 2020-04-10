package com.njwyt.intelligentdoor;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;

import freemarker.template.utility.StringUtil;
import seu.smartdoor.CameraPreview;
import seu.smartdoor.face_eigenface;

import com.njwyt.adapter.UserListAdapter;
import com.njwyt.content.Address;
import com.njwyt.db.MessageHistoryDaoHelp;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.User;
import com.njwyt.utils.FileUtils;
import com.njwyt.view.CustomActionBar;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class UserManagerActivity extends BaseActivity {

    @BindView(R.id.user_manage_actionbar)
    CustomActionBar userManageActionbar;
    @BindView(R.id.user_list)
    ListView userList;
    @BindView(R.id.add_user)
    Button addUser;
    private Unbinder bind;
    private String fileName;//照片名
    private String filePath;//照片路径

    private UserListAdapter adapter;//适配器
    //    private ProgressBar deProgressBar;//进度条
    private NumberProgressBar numberProgressBar;//数字进度条
    private UserDaoHelp userDaoHelp;
    private MessageHistoryDaoHelp messageHistoryDaoHelp;
    private face_eigenface mfrs = null;

    public static boolean cameraExists = false;//USB相机是否连接

//    private CameraPreview cameraPreview;
    //    private face_eigenface mfrs=null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usermanager);
        bind = ButterKnife.bind(this);
//        cameraPreview = new CameraPreview(UserManagerActivity.this);
        if (!OpenCVLoader.initDebug()) {
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        }
        try {
            // 新开发板上会出现异常
            mfrs = new face_eigenface(80, 3000);
            if (mfrs.read_dbase()) {
                mfrs.train();   //训练更新识别
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        init();
    }

    private void init() {
        //标题栏
        View.OnClickListener leftListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        };
        userManageActionbar.initTitleBar(R.string.ic_back, leftListener, R.string.user_manage);
        userDaoHelp = new UserDaoHelp();
        messageHistoryDaoHelp = new MessageHistoryDaoHelp(this);
        //绑定适配器
        adapter = new UserListAdapter(this);
        userList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        //读取用户数据
        initUser();
        //设置iteman按钮监听
        adapter.setOnButtonClickListener(new UserListAdapter.OnButtonClickListener() {
            //设置按钮
            @Override
            public void OnSetClickListeners(User user, int position) {
                Intent intent = new Intent(UserManagerActivity.this, UserSettingActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
            }

            //删除按钮
            @Override
            public void OnDelClickListeners(User user, int position, View view) {
                showAlertDialog(position, view, user);
            }
        });

    }

    //读取用户数据
    private void initUser() {
//        for (int i = 0; i < 6; i++) {
//            final User user = new User();
//            user.setHeadUrl(null);
//            adapter.add(user);
//            adapter.notifyDataSetChanged();
//        }
        for (User user : userDaoHelp.selectAllUser(this)) {
            adapter.add(user);
            adapter.notifyDataSetChanged();
        }
        List<User> list = userDaoHelp.selectAllUser(this);
        //控制用户数量
        if (list.size() == 8 | list.size() > 8) {
            addUser.setEnabled(false);
        }
        System.out.println("用户数量>>>>>>>>>>>>" + list.size());
    }

    //弹出Dialog
    private void showAlertDialog(final int position, final View view, final User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.del_del);
        builder.setTitle(R.string.del_hint);
        //确定按钮
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //弹出进度条
                showProgressBar(position, view, user);
                dialog.dismiss();
            }
        });
        //取消按钮
        builder.setNegativeButton(R.string.off, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    //展示进度条
    private void showProgressBar(final int position, final View v, final User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_usermessage, null);
        builder.setView(view);
        final AlertDialog dialog = builder.show();
//        deProgressBar = view.findViewById(R.id.del_progressbar);
        numberProgressBar = view.findViewById(R.id.nub);
//        deProgressBar.setMax(100);
//        deProgressBar.setProgress(0);
        numberProgressBar.incrementProgressBy(1);

        mfrs.dataBase.delete_item(user.getId() + "");
        File f = new File("/sdcard/intelligentDoor/myImage/", user.getId() + ".jpg");
        if (f.exists()) {
            f.delete();
        }
        deleteFiles(Address.DATABASE, user.getId() + "");

        //删除数据库的用户个人信息
        userDaoHelp.deleteUser(this, user.getId());
        //删除数据库的用户所有留言信息
        messageHistoryDaoHelp.deleteHistoryByUserId(user.getId());
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 100; i++) {
                    try {
                        Thread.sleep(20);
                        final int finalI = i;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                numberProgressBar.setProgress(finalI);
                                if (finalI == 100) {
                                    dialog.dismiss();
                                    //删除用户
                                    deletePattern(v, position);
//                                adapter.del(position);
//                                adapter.notifyDataSetChanged();
                                }
                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void deletePattern(final View view, final int position) {
        Animation.AnimationListener al = new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Toast.makeText(UserManagerActivity.this, getString(R.string.del_ok), Toast.LENGTH_SHORT).show();
                List<User> list = userDaoHelp.selectAllUser(UserManagerActivity.this);
                //控制用户数量
                if (list.size() == 8 | list.size() > 8) {
                    addUser.setEnabled(false);
                } else {
                    addUser.setEnabled(true);
                }
                System.out.println("用户数量>>>>>>>>>>>>" + list.size());
//                mDBHelper.deleteCustomPattern(mPatternList.get(position));
//                adapter.list.remove(position);
//                adapter.notifyDataSetChanged();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        collapse(view, al);

    }

    /**
     * 批量删除文件
     *
     * @param path    文件夹地址
     * @param keyword 文件名(模糊查询)
     */
    private void deleteFiles(String path, String keyword) {

        File dir = new File(path);
        if (!dir.exists()) {
            return;
        }

        for (File file : dir.listFiles()) {
            String fileName = file.getName();
            if (fileName.indexOf('_') == -1) {
                continue;
            }
            if (fileName.substring(0, fileName.indexOf('_')).equals(keyword) && !file.isDirectory()) {
                file.delete();
            }
        }
    }

    private void collapse(final View view, Animation.AnimationListener al) {
        final int originHeight = view.getMeasuredHeight();
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1.0f) {
                    view.setVisibility(View.GONE);
                } else {
                    view.getLayoutParams().height = originHeight - (int) (originHeight * interpolatedTime);
                    view.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        if (al != null) {
            animation.setAnimationListener(al);
        }
        animation.setDuration(300);
        view.startAnimation(animation);
    }

    // TODO: 2017/12/26
    @OnClick(R.id.add_user)
    public void onViewClicked() {

//        new CameraUSB().execute();
//        if (!cameraExists) {
////            Toast.makeText(this, "没有检查到相机", Toast.LENGTH_SHORT).show();
//            Log.i( "--==>>","没有检查到相机!!!");
//        }else {
            File file = new File("/sdcard/intelligentDoor/myImage/");
            if (!file.exists()) {
                file.mkdir();//创建文件夹
            }
            //mfrs.dataBase.modify_item(prev_name,cur_name);
            startActivityForResult(new Intent(this, RegisterActivity.class), 0);

//        }
    }


    public static Uri getUriForFile(Context context, File file) {
        if (context == null || file == null) {
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(context, "intelligentdoor.fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    /**
     * 质量压缩方法
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {
        //在板子上图片要旋转270度
        //在手机上就不需要
        Matrix matrix = new Matrix();
        matrix.setRotate(270);
        image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 90;

        while (baos.toByteArray().length / 1024 > 100) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset(); // 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;// 每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        return bitmap;
    }


    private String getBitmap(String srcPath) {
        String nPath = "";
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bm = BitmapFactory.decodeFile(srcPath, newOpts);// 此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;// 这里设置高度为800f
        float ww = 480f;// 这里设置宽度为480f
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;// 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bm = BitmapFactory.decodeFile(srcPath, newOpts);
        Bitmap bitmap = compressImage(bm);// 压缩好比例大小后再进行质量压缩


        String newPath = "/sdcard/intelligentDoor/myImage/compress/";
        File dir = new File(newPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(newPath, fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        /* options表示 如果不压缩是100，表示压缩率为0。如果是70，就表示压缩率是70，表示压缩30%; */
        int options = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        while (baos.toByteArray().length / 1024 > 500) {
// 循环判断如果压缩后图片是否大于500kb继续压缩

            baos.reset();
            options -= 10;
            if (options < 11) {//为了防止图片大小一直达不到500kb，options一直在递减，当options<0时，下面的方法会报错
                // 也就是说即使达不到500kb，也就压缩到10了
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
                break;
            }
// 这里压缩options%，把压缩后的数据存放到baos中
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(baos.toByteArray());
            out.flush();
            out.close();
            nPath = file.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return nPath;
    }

    /**
     * 弹出密码提示框
     *
     * @param
     */
    private void showPas(String newPas, final User user) {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_user, null);
        deleteDialog.setView(dialogView);
        deleteDialog.setCancelable(false);
        final Dialog dialog = deleteDialog.show();
        TextView nPas = dialogView.findViewById(R.id.tv_pas);
        nPas.setText(newPas);
        // 是
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserManagerActivity.this, UserSettingActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
                dialog.dismiss();
            }
        });

        // 否
        dialogView.findViewById(R.id.btn_no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == 4) {
                User user = (User) data.getSerializableExtra("mUser");
                String s = data.getStringExtra("pas");
                showPas(s, user);
////                String s=data.getStringExtra("path");
//                if (s==null){
//                    Log.i("<><><><>","null");
//                }
//                Log.i("<><><><>",s);
//                //将照片路径转换为照片并存入数据库
//                File file = new File(s);
//                if (file.exists()) {
//                    User user = new User();
//                    String bitPath = getBitmap(filePath);
////                    Bitmap bm = BitmapFactory.decodeFile(filePath);
//                    user.setHeadUrl(bitPath);
////                    user.setBitmap(bm);
//                    //添加用户
//                    Long l = userDaoHelp.insertUser(this, user);
//                    showPas(l, user);
//
//                    adapter.add(user);
//                    adapter.notifyDataSetChanged();
//                    List<User> list=userDaoHelp.selectAllUser(this);
//                    //控制用户数量
//                    if (list.size()==8|list.size()>8){
//                        addUser.setEnabled(false);
//                    }
//                    System.out.println("用户数量>>>>>>>>>>>>"+list.size());
//                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bind.unbind();
    }

//    class CameraUSB extends AsyncTask<Void,Void,Void>{
//        @Override
//        protected Void doInBackground(Void... voids) {
//            //判断USB摄像头是否正常
//            for (int i = 0; i < 20; i++) {
//
//                int ret = cameraPreview.prepareCameraWithBase(0,i);
//                if (ret != -1) {
//                    cameraExists = true;
//                    break;
//                }
//            }
//            cameraPreview.stopCamera();
//            return null;
//        }
//    }

}
