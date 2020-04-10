package com.njwyt.intelligentdoor;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import com.njwyt.AppContext;
import com.njwyt.content.Type;
import com.njwyt.intelligentdoor.databinding.ActivitySettingBinding;
import com.njwyt.view.PasswordInputView;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by jason_samuel on 2017/8/20.
 */

public class SettingActivity extends BaseActivity {

    private ActivitySettingBinding binding;

    private PasswordInputView newPas;
    private PasswordInputView newPasToo;
    private GridView gridView;
    private GridView gridView1;
    private ArrayList<Map<String, String>> valueList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContext.getInstance().setDenyOpenOutDoor(true);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        initView();
        initListener();
    }

    private void initView() {
        View.OnClickListener leftListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        };
        binding.settingActionbar.initTitleBar(R.string.ic_back, leftListener, R.string.user_setting);
    }

    private void initListener() {

        binding.llUserManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 进入用户管理页面
                Intent intent = new Intent(SettingActivity.this, UserManagerActivity.class);
                startActivity(intent);
            }
        });

        binding.llUserLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 进入语言选择页面
                Intent intent = new Intent(SettingActivity.this, SettingSetActivity.class);
                intent.putExtra("activity", "set");
                intent.putExtra("title", R.string.settingset_title_language);
                startActivity(intent);
            }
        });

        binding.llUserFontsize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 进入字体选择页面
                Intent intent = new Intent(SettingActivity.this, SettingSetActivity.class);
                intent.putExtra("activity", "set");
                intent.putExtra("title", R.string.settingset_title_fontsize);
                startActivity(intent);
            }
        });

        binding.llUserTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 进入主题选择页面
                Intent intent = new Intent(SettingActivity.this, SelectThemeActivity.class);
                intent.putExtra("activity", "set");
                intent.putExtra("title", R.string.settingset_title_theme);
                startActivity(intent);
            }
        });

        binding.llAdminPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 进入修改管理密码页面
                AppContext.getInstance().setBlurBitmap(null);
                Intent intent = new Intent(SettingActivity.this, PasswordEnterActivity.class);
                intent.putExtra("hintWordRes", R.string.newpassage);
                intent.putExtra("passwordLenght", 6);
                intent.putExtra("mode", Type.PASSWORD_SETTING);
                startActivity(intent);
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        AppContext.getInstance().setDenyOpenOutDoor(false);
        AppContext.getInstance().offLigth();
    }
}
