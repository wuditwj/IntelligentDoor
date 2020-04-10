package com.njwyt.intelligentdoor;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import com.njwyt.AppContext;
import com.njwyt.content.Type;
import com.njwyt.db.ReservoirHelper;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.MessageEvent;
import com.njwyt.entity.User;
import com.njwyt.intelligentdoor.databinding.ActivityPasswordEnterBinding;
import com.njwyt.utils.ImageUtils;
import com.njwyt.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

/**
 * Created by jason_samuel on 2017/8/31.
 */

public class PasswordEnterActivity extends BaseActivity {

    private ActivityPasswordEnterBinding binding;
    private UserDaoHelp userDaoHelp;
    private int hintWordRes;                        // 页面文字
    private List<View> passwordViewList;            // 密码N个圈
    private int currentEnter = 0;                   // 记录当前输入到第几位数
    private String password = "";                   // 记录下来的密码
    private String newPassword;                     // 修改密码模式中的新密码存放
    private int passwordLenght = 4;                 // 默认密码长度
    private int mode = 0;                           // 使用模式
    private boolean isEnable = true;                // 让键盘停止使用
    private boolean enterPwdAgain = false;          // 第二次输入密码

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_password_enter);
        binding.setActivity(this);

        setBlur();
        initValue();
        initView();
        initListener();
    }

    /**
     * 设置该activity页面背景高斯模糊
     */
    private void setBlur() {
        if (AppContext.getInstance().getBlurBitmap() != null) {
            Bitmap blurBmp = ImageUtils.blurBitmap(this, AppContext.getInstance().getBlurBitmap(), 20);
            binding.ivBackground.setBackground(new BitmapDrawable(null, blurBmp));
        }
    }

    private void initValue() {
        passwordViewList = new ArrayList<>();
        userDaoHelp = new UserDaoHelp();
        hintWordRes = getIntent().getIntExtra("hintWordRes", R.string.title_enter_pwd);
        passwordLenght = getIntent().getIntExtra("passwordLenght", 4);
        mode = getIntent().getIntExtra("mode", Type.PASSWORD_LOGIN);
    }

    private void initView() {

        binding.tvTestEnter.setText(hintWordRes);

        for (int i = 0; i < passwordLenght; i++) {

            View view = new View(this);
            view.setBackgroundResource(R.drawable.circular_hollow_shape);
            int lengthDP = (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());
            int marginDP = (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(lengthDP, lengthDP);
            params.setMargins(marginDP, marginDP, marginDP, marginDP);
            view.setLayoutParams(params);
            binding.llPwd.addView(view);
            passwordViewList.add(view);
        }
    }

    private void initListener() {

        binding.llCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentEnter == 0) {
                    finish();
                } else {
                    if (password.length() != 0) {
                        password = password.substring(0, password.length() - 1);
                    }
                    passwordViewList.get(currentEnter - 1).setBackgroundResource(R.drawable.circular_hollow_shape);
                    currentEnter--;
                    if (currentEnter == 0) {
                        binding.tvCancel.setText(R.string.cancel);
                    }
                }
            }
        });
    }

    /**
     * 密码按下事件
     *
     * @param num
     */
    public void btnClick(int num) {

        if (!isEnable) {
            return;
        }

        if (currentEnter >= passwordViewList.size()) {
            return;
        }
        passwordViewList.get(currentEnter).setBackgroundResource(R.drawable.circular_solid_shape);
        binding.tvCancel.setText(R.string.back);

        password += num;

        if (currentEnter == passwordLenght - 1) {

            if (mode == Type.PASSWORD_SETTING) {

                if (enterPwdAgain) {

                    // 判断两次密码是否一致
                    if (newPassword.equals(password)) {
                        // todo 保存系统密码
                        ReservoirHelper.setSystemPassword(password);
                        finish();
                        return;
                    }
                    // 弹出提示清空密码
                    ToastUtil.showToast(PasswordEnterActivity.this, R.string.changepas_fourtoo);
                    reset();
                    return;
                }

                // 设置新密码
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        newPassword = password;
                        binding.tvTestEnter.setText(R.string.newpassagetoo);
                        enterPwdAgain = true;
                    }
                }, 200);
                reset();
                isEnable = false;

            } else if (mode == Type.PASSWORD_ADMIN) {

                // 打开设置页面
                if (password.equals(ReservoirHelper.getSystemPassword())) {
                    Intent intent = new Intent(PasswordEnterActivity.this, SettingActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    vibrationView(binding.llPwd);
                    reset();
                    //ToastUtil.showToast(PasswordEnterActivity.this, R.string.title_pwd_error);
                }
            } else {

                // 进数据库查询
                User currentUser = userDaoHelp.selectUser(PasswordEnterActivity.this, password);
                // 查询到相应匹配的密码则登录成功
                if (currentUser != null) {
                    EventBus.getDefault().post(new MessageEvent<User>(Type.LOGIN_SUCCESS, currentUser));
                    AppContext.getInstance().setCurrentUser(currentUser);
                    finish();
                } else {
                    vibrationView(binding.llPwd);
                    reset();
                    //ToastUtil.showToast(PasswordEnterActivity.this, R.string.title_pwd_error);
                }
            }
            //ToastUtil.showToast(this, "密码是：" + password);
        }
        currentEnter++;
    }

    /**
     * 左右震动动画
     */
    private void vibrationView(View view) {

        TranslateAnimation animation = new TranslateAnimation(0, -20, 0, 0);
        animation.setInterpolator(new CycleInterpolator(2));
        animation.setDuration(400);
        view.clearAnimation();
        view.startAnimation(animation);
    }

    /**
     * 重置密码
     */
    private void reset() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentEnter = 0;
                password = "";
                binding.tvCancel.setText(R.string.cancel);
                for (View passwordView : passwordViewList) {
                    passwordView.setBackgroundResource(R.drawable.circular_hollow_shape);
                }
                isEnable = true;
            }
        }, 300);
        isEnable = false;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
