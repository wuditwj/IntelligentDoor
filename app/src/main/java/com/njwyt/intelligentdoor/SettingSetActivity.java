package com.njwyt.intelligentdoor;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.njwyt.AppContext;
import com.njwyt.content.Type;
import com.njwyt.db.ReservoirHelper;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.User;
import com.njwyt.view.CustomActionBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SettingSetActivity extends BaseActivity {

    @BindView(R.id.setting_actionBar)
    CustomActionBar actionBar;
    @BindView(R.id.setting_list)
    ListView list;

    private Unbinder bind;
    private UserDaoHelp userDaoHelp;
    private ArrayAdapter<String> adapter;
    private List<String> itemList;
    private User user;
    private int title;
    private String act;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_set);
        bind = ButterKnife.bind(this);
        init();
    }

    private void init() {
        act = getIntent().getStringExtra("activity");
        switch (act){
            case "use":
                System.out.println("<><><><><>"+"111111");
                break;
            case "set":
                System.out.println("<><><><><>"+"222222");
                break;
        }
        initTitle();
        initList();
    }

    private void initTitle() {
        //设置标题栏
        title = getIntent().getIntExtra("title",R.string.user_setting);
        if (act.equals("use")){
            user = (User) getIntent().getSerializableExtra("user");
        }
        View.OnClickListener leftListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        };
        actionBar.initTitleBar(R.string.ic_back, leftListener, title);
    }

    private void initList() {
        userDaoHelp = new UserDaoHelp();
        itemList = new ArrayList<>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, itemList);
        list.setAdapter(adapter);
        switch (title) {
            case R.string.settingset_title_language:
                selectionLanguage();
                break;
            case R.string.settingset_title_fontsize:
                selectionFontSzie();
                break;
        }
    }

    //修改语言
    private void selectionLanguage() {
        itemList.add(getString(R.string.language_chinese));
        itemList.add(getString(R.string.language_english));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        switch (act){
                            case "use":
                                //数据库修改
                                selectionLanguageOnDB(Type.LANGUAGE_CHINESE);
                                //AppContext.getInstance().switchLanguage(Locale.CHINESE);
                                break;
                            case "set":
                                AppContext.getInstance().switchLanguage(Locale.CHINESE);
                                ReservoirHelper.setLanguage(Type.LANGUAGE_CHINESE);
                                finish();
                                break;
                        }
                        break;
                    case 1:
                        switch (act){
                            case "use":
                                //数据库修改
                                selectionLanguageOnDB(Type.LANGUAGE_ENGLISH);
                                //AppContext.getInstance().switchLanguage(Locale.US);
                                break;
                            case "set":
                                AppContext.getInstance().switchLanguage(Locale.US);
                                ReservoirHelper.setLanguage(Type.LANGUAGE_ENGLISH);
                                finish();
                                break;
                        }
                        break;
                }
            }
        });
    }

    //修改字体大小
    private void selectionFontSzie() {
        itemList.add(getString(R.string.fontsize_big));
        itemList.add(getString(R.string.fontsize_medium));
        itemList.add(getString(R.string.fontsize_small));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        //大字体
                        switch (act){
                            case "use":
                                //数据库修改
                                selectionFontSizeOnDB(Type.FONTSIZE_BIG);
                                //AppContext.getInstance().setFontSize(Type.FONTSIZE_BIG);
                                break;
                            case "set":
                                AppContext.getInstance().setFontSize(Type.FONTSIZE_BIG);
                                ReservoirHelper.setFontSize(Type.FONTSIZE_BIG);
                                finish();
                                break;
                        }
                        break;
                    case 1:
                        //中等字体
                        switch (act){
                            case "use":
                                //数据库修改
                                selectionFontSizeOnDB(Type.FONTSIZE_MEDIUM);
                                //AppContext.getInstance().setFontSize(Type.FONTSIZE_MEDIUM);
                                break;
                            case "set":
                                AppContext.getInstance().setFontSize(Type.FONTSIZE_MEDIUM);
                                ReservoirHelper.setFontSize(Type.FONTSIZE_MEDIUM);
                                finish();
                                break;
                        }
                        break;
                    case 2:
                        //小字体
                        switch (act){
                            case "use":
                                //数据库修改
                                selectionFontSizeOnDB(Type.FONTSIZE_SMALL);
                                AppContext.getInstance().setFontSize(Type.FONTSIZE_SMALL);
                                break;
                            case "set":
                                AppContext.getInstance().setFontSize(Type.FONTSIZE_SMALL);
                                ReservoirHelper.setFontSize(Type.FONTSIZE_SMALL);
                                finish();
                                break;
                        }
                        break;
                }
            }
        });
    }


    //-----------------------------------以下都是数据库操作---------------------------------
//数据库修改语言
    private void selectionLanguageOnDB(String lan) {
        User selectUser = userDaoHelp.selectUser(this, user.getId());
        System.out.println("原先的语言>>>>>>>>>>>>" + selectUser.getLanguage());
        user.setLanguage(lan);
        userDaoHelp.upDataUser(this, user);
        System.out.println("修改后的语言>>>>>>>>>>>>" + selectUser.getLanguage()
                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        finish();
    }

    //数据库修改字体大小
    private void selectionFontSizeOnDB(int size) {
        User selectUser = userDaoHelp.selectUser(this, user.getId());
        System.out.println("原先的字体大小>>>>>>>>>>>>" + selectUser.getFontSize());
        user.setFontSize(size);
        userDaoHelp.upDataUser(this, user);
        System.out.println("修改后的字体大小>>>>>>>>>>>>" + selectUser.getFontSize()
                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bind.unbind();
    }
}
