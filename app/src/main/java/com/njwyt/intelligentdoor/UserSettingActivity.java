package com.njwyt.intelligentdoor;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.njwyt.AppContext;
import com.njwyt.content.Type;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.User;
import com.njwyt.view.CustomActionBar;
import com.njwyt.view.PasswordInputView;
import com.njwyt.view.VirtualKeyboardView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class UserSettingActivity extends BaseActivity {

    @BindView(R.id.layout_head)
    RelativeLayout layoutHead;
    @BindView(R.id.user_head)
    SimpleDraweeView userHead;
    @BindView(R.id.layout_id)
    RelativeLayout layoutId;
    @BindView(R.id.user_id)
    TextView userId;
    @BindView(R.id.layout_passage)
    RelativeLayout layoutPassage;
    @BindView(R.id.user_passage)
    TextView userPassage;
//    @BindView(R.id.layout_difficulty)
//    RelativeLayout layoutDifficulty;
//    @BindView(R.id.user_difficulty)
//    TextView userDifficulty;
    @BindView(R.id.layout_language)
    RelativeLayout layoutLanguage;
    @BindView(R.id.user_language)
    TextView userLanguage;
    @BindView(R.id.layout_fontsize)
    RelativeLayout layoutFontSize;
    @BindView(R.id.user_fontsize)
    TextView userFontSize;
//    @BindView(R.id.layout_theme)
//    RelativeLayout layoutTheme;
//    @BindView(R.id.user_theme)
//    TextView userTheme;
    @BindView(R.id.usersetting_actionbar)
    CustomActionBar actionbar;
    private Unbinder bind;
    private User user;
    private UserDaoHelp userDaoHelp;
    private File mediaFile;
    private String fileName;
    private PasswordInputView newPas;
    //    private File tileFile;
    private ArrayList<Map<String, String>> valueList;
    private GridView gridView;
    private ArrayList<Map<String, String>> valueList1;
    private GridView gridView1;
    private PasswordInputView newPasToo;
    private String picturePath;
    private VirtualKeyboardView virtualKeyboardView;
    private VirtualKeyboardView virtualKeyboardView1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setting);
        bind = ButterKnife.bind(this);
        init();
    }

    private void init() {
        //标题栏设置
        View.OnClickListener leftListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        };
        actionbar.initTitleBar(R.string.ic_back, leftListener, R.string.user_manage);
        userDaoHelp = new UserDaoHelp();
        //读取用户信息
        user = (User) getIntent().getSerializableExtra("user");
        if (user.getHeadUrl() == null) {
            //默认头像
            userHead.getHierarchy().setPlaceholderImage(new BitmapDrawable(null, String.valueOf(R.drawable.first)));
        } else {
            //用户的头像
            Bitmap bm = BitmapFactory.decodeFile(user.getHeadUrl());
            Log.i("><><><", user.getHeadUrl() + "");
            Long id = user.getId();
            userId.setText(id.toString());
            userHead.getHierarchy().setPlaceholderImage(new BitmapDrawable(null, bm));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        User selectUser = userDaoHelp.selectUser(this, user.getId());
        System.out.println("用户名>>>>>>>>>>>>" + selectUser.getId()
                + "\n密码>>>>>>>>>>>>" + selectUser.getPassword()
                + "\n操作难度>>>>>>>>>>>>" + selectUser.getLevel()
                + "\n头像>>>>>>>>>>>>" + selectUser.getHeadUrl()
                + "\n语言>>>>>>>>>>>>" + selectUser.getLanguage()
                + "\n字体大小>>>>>>>>>>>>" + selectUser.getFontSize()
                + "\n主题>>>>>>>>>>>>" + selectUser.getTheme()
                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

//        //根据数据库字段来显示操作等级
//        switch (selectUser.getLevel()) {
//            case 0:
//                userDifficulty.setText(R.string.user_briefness);
//                break;
//            case 1:
//                userDifficulty.setText(R.string.user_medium);
//                break;
//            case 2:
//                userDifficulty.setText(R.string.user_normal);
//                break;
//        }
        //根据数据库字段来显示语言
        if (selectUser.getLanguage() == null) {
            userLanguage.setText(R.string.language_chinese);
        } else {
            switch (selectUser.getLanguage()) {
                case "ch":
                    userLanguage.setText(R.string.language_chinese);
                    break;
                case "en":
                    userLanguage.setText(R.string.language_english);
                    break;
            }
        }
        //根据数据库字段来显示字体大小
        switch (selectUser.getFontSize()) {
            case 2:
                userFontSize.setText(R.string.fontsize_big);
                break;
            case 1:
                userFontSize.setText(R.string.fontsize_small);
                break;
            case 0:
                userFontSize.setText(R.string.fontsize_medium);
                break;
        }

//        //根据数据库字段来显示主题
//        switch (selectUser.getTheme()) {
//            case 0:
//                userTheme.setText(R.string.theme_simple);
//                break;
//        }
    }

    @OnClick({R.id.layout_head, R.id.layout_id, R.id.layout_passage, R.id.layout_language, R.id.layout_fontsize})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            //用户头像
            case R.id.layout_head:
                showDialogChange();
                break;
            //用户名
            case R.id.layout_id:
                break;
            //用户密码
            case R.id.layout_passage:
                showDialogSelectionPassword();
                break;
//            //用户操作难度
//            case R.id.layout_difficulty:
//                showDialogSelectionDifficulty();
//                break;
            //语言
            case R.id.layout_language:
                showDialogSelectionLanguage();
                break;
            //字体大小
            case R.id.layout_fontsize:
                showDialogSelectionFontSize();
                break;
//            //主题
//            case R.id.layout_theme:
//                showDialogSelectionTheme();
//                break;
        }
    }


    //---------------------------------弹出提示框----------------------------------------------
    //点击头像弹出来两个选项
    private void showDialogChange() {
        final String[] items = {getString(R.string.theBigPicture), getString(R.string.replaceHead)};
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(UserSettingActivity.this);
        listDialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // which 下标从0开始
                switch (which) {
                    case 0:
                        showBigImage();
//                        Toast.makeText(UserSettingActivity.this, "点击了1", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        changeUserTile();
//                        Toast.makeText(UserSettingActivity.this, "点击了2", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        listDialog.show();
    }

    //查看大图
    private void showBigImage() {
        //大图所依附的dialog
        final Dialog dialog;
        dialog = new Dialog(UserSettingActivity.this);
        ImageView mImageView = new ImageView(this);
        mImageView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        //从数据库获得用户头像
        Bitmap bm = BitmapFactory.decodeFile(user.getHeadUrl());
        mImageView.setImageBitmap(bm);
        mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        mImageView.setAdjustViewBounds(true);
        dialog.setContentView(mImageView);
        //大图的点击事件（点击让他消失）
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    //修改密码
    private void showDialogSelectionPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_selection_password, null);
        builder.setView(view);
        final AlertDialog dialog = builder.show();
        //新密码
        newPas = view.findViewById(R.id.new_pas);
        //禁用系统输入法
        newPas.setInputType(InputType.TYPE_NULL);
        //再次输入
        newPasToo = view.findViewById(R.id.new_pas_too);
        //禁用系统输入法
        newPasToo.setInputType(InputType.TYPE_NULL);


        //新密码软键盘
        virtualKeyboardView = view.findViewById(R.id.virtualKeyboardView);
        virtualKeyboardView1 = view.findViewById(R.id.virtualKeyboardView1);
        valueList = virtualKeyboardView.getValueList();
        virtualKeyboardView.getLayoutBack().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                virtualKeyboardView.startAnimation(exitAnim);
                virtualKeyboardView.setVisibility(View.GONE);
            }
        });

        gridView = virtualKeyboardView.getGridView();
        gridView.setOnItemClickListener(onItemClickListener);

        newPas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                virtualKeyboardView1.setVisibility(View.GONE);
                virtualKeyboardView.setFocusable(true);
                virtualKeyboardView.setFocusableInTouchMode(true);

//                virtualKeyboardView.startAnimation(enterAnim);
                virtualKeyboardView.setVisibility(View.VISIBLE);
                virtualKeyboardView.bringToFront();
                return false;
            }
        });
//        newPas.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                virtualKeyboardView1.setVisibility(View.GONE);
//                virtualKeyboardView.setFocusable(true);
//                virtualKeyboardView.setFocusableInTouchMode(true);
//
////                virtualKeyboardView.startAnimation(enterAnim);
//                virtualKeyboardView.setVisibility(View.VISIBLE);
//                virtualKeyboardView.bringToFront();
//            }
//        });
        //再次输入软键盘
        valueList1 = virtualKeyboardView1.getValueList();
        virtualKeyboardView1.getLayoutBack().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                virtualKeyboardView.startAnimation(exitAnim);
                virtualKeyboardView1.setVisibility(View.GONE);
            }
        });
        gridView1 = virtualKeyboardView1.getGridView();
        gridView1.setOnItemClickListener(onItemClickListener1);

        newPasToo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (newPas.getSelectionEnd()!=4){
//                    Toast.makeText(UserSettingActivity.this, "密码不足四位", Toast.LENGTH_SHORT).show();
                }else {
                    virtualKeyboardView.setVisibility(View.GONE);
                    virtualKeyboardView1.setFocusable(true);
                    virtualKeyboardView1.setFocusableInTouchMode(true);

//                virtualKeyboardView1.startAnimation(enterAnim);
                    virtualKeyboardView1.setVisibility(View.VISIBLE);
                    virtualKeyboardView1.bringToFront();
                }
                return false;
            }
        });
//        newPasToo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                virtualKeyboardView.setVisibility(View.GONE);
//                virtualKeyboardView1.setFocusable(true);
//                virtualKeyboardView1.setFocusableInTouchMode(true);
//
////                virtualKeyboardView1.startAnimation(enterAnim);
//                virtualKeyboardView1.setVisibility(View.VISIBLE);
//                virtualKeyboardView1.bringToFront();
//            }
//        });

        Button ok = view.findViewById(R.id.pas_ok);
        Button off = view.findViewById(R.id.pas_off);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectionPas(newPas, newPasToo, dialog);
            }
        });
        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

//    //选择难度
//    private void showDialogSelectionDifficulty() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        View view = LayoutInflater.from(this).inflate(R.layout.dialog_selection_difficulty, null);
//        builder.setView(view);
//        final AlertDialog dialog = builder.show();
//        RelativeLayout layoutNormal = view.findViewById(R.id.layout_normal);
//        RelativeLayout layoutMedium = view.findViewById(R.id.layout_medium);
//        RelativeLayout layoutBriefness = view.findViewById(R.id.layout_briefness);
//        //高级
//        layoutNormal.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                selectionDifficulty(Type.DIFFICULTY_ADVANCED);
//                userDifficulty.setText(R.string.user_normal);
//                dialog.dismiss();
//            }
//        });
//        //中等
//        layoutMedium.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                selectionDifficulty(Type.DIFFICULTY_MEDIUM);
//                userDifficulty.setText(R.string.user_medium);
//                dialog.dismiss();
//            }
//        });
//        //简单
//        layoutBriefness.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                selectionDifficulty(Type.DIFFICULTY_SIMPLENESS);
//                userDifficulty.setText(R.string.user_briefness);
//                dialog.dismiss();
//            }
//        });
//    }

    //修改语言
    private void showDialogSelectionLanguage() {
        Intent intent = new Intent(this, SettingSetActivity.class);
        intent.putExtra("activity", "use");
        intent.putExtra("title", R.string.settingset_title_language);
        intent.putExtra("user", user);
        startActivity(intent);

    }

    //修改字体大小
    private void showDialogSelectionFontSize() {
        Intent intent = new Intent(this, SettingSetActivity.class);
        intent.putExtra("activity", "use");
        intent.putExtra("title", R.string.settingset_title_fontsize);
        intent.putExtra("user", user);
        startActivity(intent);
    }

//    //修改主题
//    private void showDialogSelectionTheme() {
//        Intent intent = new Intent(this, SelectThemeActivity.class);
//        intent.putExtra("activity", "use");
//        intent.putExtra("title", R.string.settingset_title_theme);
//        intent.putExtra("user", user);
//        startActivity(intent);
//    }

    //----------------------------------以下是数据库操作--------------------------------------------
    //数据库修改密码
    private void selectionPas(PasswordInputView newPas, PasswordInputView newPasToo, AlertDialog dialog) {
        User selectUser = userDaoHelp.selectUser(this, user.getId());
        System.out.println("用户名>>>>>>>>>>>>" + selectUser.getId()
                + "\n密码>>>>>>>>>>>>" + selectUser.getPassword());
        System.out.println("我输入的新密码>>>>>>>>>>>>" + newPas.getText());
        System.out.println("我再次输入的新密码>>>>>>>>>>>>" + newPasToo.getText());
        if (newPas.length() != 4 || newPasToo.length() != 4) {
            newPas.setText(null);
            newPasToo.setText(null);
            numIsNull();
            Toast.makeText(this, getString(R.string.changepas_four), Toast.LENGTH_SHORT).show();
        } else {
            if (!newPas.getText().toString().equals(newPasToo.getText().toString())) {
                newPas.setText(null);
                newPasToo.setText(null);
                numIsNull();
                Toast.makeText(this, getString(R.string.changepas_fourtoo), Toast.LENGTH_SHORT).show();
            } else {
                if (userDaoHelp.selectUser(this,newPasToo.getText().toString())!=null){
                    newPas.setText(null);
                    newPasToo.setText(null);
                    numIsNull();
                    Toast.makeText(this, getString(R.string.passIsUsed), Toast.LENGTH_SHORT).show();
                }else {
                    //修改密码
                    user.setPassword(newPas.getText().toString());
                    userDaoHelp.upDataUser(this, user);
                    dialog.dismiss();
                    Toast.makeText(this, getString(R.string.change_ok), Toast.LENGTH_SHORT).show();
                    System.out.println("我修改后的密码>>>>>>>>>>>>" + selectUser.getPassword()
                            + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                }
            }
        }
    }

    //将输入键盘指定到第一行
    private void numIsNull(){
        virtualKeyboardView1.setVisibility(View.GONE);
        virtualKeyboardView.setFocusable(true);
        virtualKeyboardView.setFocusableInTouchMode(true);

//                virtualKeyboardView.startAnimation(enterAnim);
        virtualKeyboardView.setVisibility(View.VISIBLE);
        virtualKeyboardView.bringToFront();
    }

//    //数据库修改难度
//    private void selectionDifficulty(int dif) {
//        User selectUser = userDaoHelp.selectUser(this, user.getId());
//        System.out.println("原先的操作难度>>>>>>>>>>>>" + selectUser.getLevel());
//        user.setLevel(dif);
//        userDaoHelp.upDataUser(this, user);
//        System.out.println("修改后的操作难度>>>>>>>>>>>>" + selectUser.getLevel()
//                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//        //根据数据库字段来显示操作等级
//        switch (selectUser.getLevel()) {
//            case 0:
//                userDifficulty.setText(R.string.user_normal);
//                break;
//            case 1:
//                userDifficulty.setText(R.string.user_medium);
//                break;
//            case 2:
//                userDifficulty.setText(R.string.user_briefness);
//                break;
//        }
//    }

//    //数据库修改主题
//    private void selectionTheme(int theme) {
//        User selectUser = userDaoHelp.selectUser(this, user.getId());
//        System.out.println("原先的主题>>>>>>>>>>>>" + selectUser.getTheme());
//        user.setTheme(theme);
//        userDaoHelp.upDataUser(this, user);
//        System.out.println("修改后的主题>>>>>>>>>>>>" + selectUser.getTheme()
//                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//        //根据数据库字段来显示主题
//        switch (selectUser.getTheme()) {
//            case 0:
//                userTheme.setText(R.string.theme_simple);
//                break;
//        }
//
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bind.unbind();
    }

    /**
     * 更改用户头像(系统拍照)
     */
    private void changeUserTile() {
//        //跳转到系统拍照
//        File file = new File("/sdcard/intelligentDoor/myImage/");
//        if (!file.exists()) {
//            file.mkdir();//创建文件夹
//        }
//        fileName = new SimpleDateFormat("yyyyMMdd_hhmmss").format(System.currentTimeMillis()) + ".jpg";
////        filePath =file+ fileName;
//        filePath = "/sdcard/intelligentDoor/myImage/" + fileName;
//        File file1 = new File(file, fileName);
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, getUriForFile(UserSettingActivity.this, file1));
//        startActivityForResult(intent, 1);
        //跳转到拍照
        Intent intent = new Intent(this, SelectHeadActivity.class);
        startActivityForResult(intent, 1);
    }

//    public static Uri getUriForFile(Context context, File file) {
//        if (context == null || file == null) {
//            throw new NullPointerException();
//        }
//        Uri uri;
//        if (Build.VERSION.SDK_INT >= 24) {
//            uri = FileProvider.getUriForFile(context.getApplicationContext(), "com.archery.fileprovider", file);
//        } else {
//            uri = Uri.fromFile(file);
//        }
//        return uri;
//    }


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
//        matrix.setRotate(270);
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
        fileName = new SimpleDateFormat("yyyyMMdd_hhmmss").format(System.currentTimeMillis()) + ".jpg";
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

    //删除旧头像
    private void clearFiles(String workspaceRootPath) {
        File file = new File(workspaceRootPath);
        if (file.exists()) {
            file.delete();
        }
    }


    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            Editable ea1 = newPas.getText();
            Log.i("<><><><> ", "SelectionEnd=" + newPas.getSelectionEnd());
            Log.i("<><><><> ", "length=" + ea1.length());
            if (position < 11 && position != 9) {    //点击0~9按钮
                if (newPas.getSelectionEnd() < 4) {

                    String amount = newPas.getText().toString().trim();
                    amount = amount + valueList.get(position).get("name");

                    newPas.setText(amount);

                    Editable ea = newPas.getText();
                    newPas.setSelection(ea.length());
                } else {
                    Toast.makeText(UserSettingActivity.this, getString(R.string.passIsFour), Toast.LENGTH_SHORT).show();
                }
            } else {

                if (position == 9) {      //点击小数点
//                    Toast.makeText(MainActivity.this,"小数点",Toast.LENGTH_SHORT).show();
//                    finish();
//                    String amount = textAmount.getText().toString().trim();
//                    if (!amount.contains(".")) {
//                        amount = amount + valueList.get(position).get("name");
//                        textAmount.setText(amount);
//
//                        Editable ea = textAmount.getText();
//                        textAmount.setSelection(ea.length());
//                    }
                }

                if (position == 11) {      //点击退格键
                    String amount = newPas.getText().toString().trim();
                    if (amount.length() > 0) {
                        amount = amount.substring(0, amount.length() - 1);
                        newPas.setText(amount);

                        Editable ea = newPas.getText();
                        newPas.setSelection(ea.length());
                    }
                }
            }
        }
    };
    private AdapterView.OnItemClickListener onItemClickListener1 = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            Editable ea1 = newPasToo.getText();
            Log.i("<><><><> ", "SelectionEnd=" + newPasToo.getSelectionEnd());
            Log.i("<><><><> ", "length=" + ea1.length());
            if (position < 11 && position != 9) {    //点击0~9按钮
                if (newPasToo.getSelectionEnd() < 4) {

                    String amount = newPasToo.getText().toString().trim();
                    amount = amount + valueList1.get(position).get("name");

                    newPasToo.setText(amount);

                    Editable ea = newPasToo.getText();
                    newPasToo.setSelection(ea.length());
                } else {
                    Toast.makeText(UserSettingActivity.this, getString(R.string.passIsFour), Toast.LENGTH_SHORT).show();
                }
            } else {

                if (position == 9) {      //点击小数点
//                    Toast.makeText(MainActivity.this,"小数点",Toast.LENGTH_SHORT).show();
//                    finish();
//                    String amount = textAmount.getText().toString().trim();
//                    if (!amount.contains(".")) {
//                        amount = amount + valueList.get(position).get("name");
//                        textAmount.setText(amount);
//
//                        Editable ea = textAmount.getText();
//                        textAmount.setSelection(ea.length());
//                    }
                }

                if (position == 11) {      //点击退格键
                    String amount = newPasToo.getText().toString().trim();
                    if (amount.length() > 0) {
                        amount = amount.substring(0, amount.length() - 1);
                        newPasToo.setText(amount);

                        Editable ea = newPasToo.getText();
                        newPasToo.setSelection(ea.length());
                    }
                }
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {

        } else {
            picturePath = data.getStringExtra("picturePath");
            switch (requestCode) {
                case 1:
                    if (resultCode == 0) {
                        File file = new File(picturePath);
                        if (file.exists()) {
                            //压缩过后的图片
                            String bitPath = getBitmap(picturePath);
                            //转换成图片
                            Bitmap bm = BitmapFactory.decodeFile(picturePath);
                            //更改用户头像
                            userHead.getHierarchy().setPlaceholderImage(new BitmapDrawable(null, bm));
                            //更改数据库里的头像地址
                            User selectUser = userDaoHelp.selectUser(this, user.getId());
                            System.out.println("原先的头像地址>>>>>>>>>>>>" + selectUser.getHeadUrl());
                            //把原来的头像删掉
                            clearFiles(selectUser.getHeadUrl());
                            user.setHeadUrl(bitPath);
                            userDaoHelp.upDataUser(this, user);
                            System.out.println("修改后的头像地址>>>>>>>>>>>>" + selectUser.getHeadUrl()
                                    + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                            //把内存大的图片删掉
                            clearFiles(picturePath);
                        }
                    }
                    break;
            }
        }
    }

}

//
//class compressBit extends AsyncTask<Void,Void,String> {
//
//    private Bitmap bit;
//    public compressBit(Bitmap bit) {
//        this.bit=bit;
//    }
//
//    @Override
//    protected String doInBackground(Void... voids) {
//        //创建一个文件夹
//        String VideoImagePath = "/sdcard/intelligentDoor/myImage/compress/";
//        String videoImageName = "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".png";
//        File dir = new File(VideoImagePath);
//        if (!dir.exists()) {
//            dir.mkdir();
//        }
//        File videoImageFile= new File(VideoImagePath, videoImageName);
//        Bitmap bitmap = compressImage(bit);
//        try {
//            FileOutputStream out=new FileOutputStream(videoImageFile);
//            bitmap.compress(Bitmap.CompressFormat.PNG,90,out);
//            out.flush();
//            out.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return videoImageFile.toString();
//    }
//
//
//    private String getBitmap(Bitmap bit) {
//        String nPath = "";
//        Bitmap bitmap = compressImage(bit);// 压缩好比例大小后再进行质量压缩
//
//
//        String newPath = "/sdcard/intelligentDoor/myImage/compress/";
//        File dir = new File(newPath);
//        if (!dir.exists()) {
//            dir.mkdir();
//        }
//        fileName = new SimpleDateFormat("yyyyMMdd_hhmmss").format(System.currentTimeMillis()) + ".jpg";
//        File file = new File(newPath, fileName);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        /* options表示 如果不压缩是100，表示压缩率为0。如果是70，就表示压缩率是70，表示压缩30%; */
//        int options = 100;
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//
//        while (baos.toByteArray().length / 1024 > 200) {
//// 循环判断如果压缩后图片是否大于500kb继续压缩
//
//            baos.reset();
//            options -= 10;
//            if (options < 11) {//为了防止图片大小一直达不到500kb，options一直在递减，当options<0时，下面的方法会报错
//                // 也就是说即使达不到500kb，也就压缩到10了
//                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
//                break;
//            }
//// 这里压缩options%，把压缩后的数据存放到baos中
//            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
//        }
//
//        try {
//            FileOutputStream out = new FileOutputStream(file);
//            out.write(baos.toByteArray());
//            out.flush();
//            out.close();
//            nPath = file.getAbsolutePath();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return nPath;
//    }
//
//
//    @Override
//    protected void onPostExecute(String s) {
////            转换成图片
//        Bitmap bm = BitmapFactory.decodeFile(s);
//        bm=rotateBitmapByDegree(bm,90);
////            设置到头像上去
//        userHead.getHierarchy().setPlaceholderImage(new BitmapDrawable(null, bm));
//
//        //        更改数据库里的头像地址
//        User selectUser = userDaoHelp.selectUser(UserSettingActivity.this, user.getId());
//        clearFiles(user.getHeadUrl());
//        user.setHeadUrl(s);
//        userDaoHelp.upDataUser(UserSettingActivity.this, user);
//        System.out.println("修改后的头像地址>>>>>>>>>>>>" + selectUser.getHeadUrl()
//                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//
//        super.onPostExecute(s);
//    }
//}
//}
