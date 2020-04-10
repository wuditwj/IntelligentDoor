package com.njwyt.intelligentdoor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import com.njwyt.adapter.ThemeRecyclerViewAdapter;
import com.njwyt.db.UserDaoHelp;
import com.njwyt.entity.Theme;
import com.njwyt.entity.User;
import com.njwyt.view.CustomActionBar;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SelectThemeActivity extends BaseActivity {

    @BindView(R.id.select_theme_recyclerview)
    RecyclerView recyclerView;
    @BindView(R.id.theme_actionBar)
    CustomActionBar actionBar;
    private Unbinder bind;
    private ThemeRecyclerViewAdapter adapter;
    private User user;
    private int title;
    private String act;
    private UserDaoHelp userDaoHelp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_theme);
        bind = ButterKnife.bind(this);
        initTitle();
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    private void initTitle() {
        act = getIntent().getStringExtra("activity");
        userDaoHelp = new UserDaoHelp();
        //设置标题栏
        title = getIntent().getIntExtra("title", R.string.user_setting);
        if (act.equals("use")) {
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

    private void init() {
        //设置RecyclerView的LayoutManager
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
//        //设置item之间的间隔
//        SpacesItemDecoration decoration = new SpacesItemDecoration(13);
//        recyclerView.addItemDecoration(decoration);
        adapter = new ThemeRecyclerViewAdapter(this);
        recyclerView.setAdapter(adapter);
        initImage();
        adapter.setOnItemClickListener(new ThemeRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void OnItemClickListeners(Theme theme, int position) {
                switch (act) {
                    case "use": // 从用户个人设置中进入
                        selectionThemeOnDB(position);
                        break;
                    case "set": // 从系统设置中进入
                        // 打开预览Activity
                        Intent intent = new Intent(SelectThemeActivity.this, ThemePreviewActivity.class);
                        intent.putExtra("theme", theme.getImage());
                        intent.putExtra("themeList", theme.getImageList());
                        startActivity(intent);
                        break;
                }
            }
        });
    }

    private void selectionThemeOnDB(int t) {
        User selectUser = userDaoHelp.selectUser(this, user.getId());
        System.out.println("原先的主题>>>>>>>>>>>>" + selectUser.getTheme());
        user.setTheme(t);
        userDaoHelp.upDataUser(this, user);
        System.out.println("修改后的主题>>>>>>>>>>>>" + selectUser.getTheme()
                + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

    }

    /**
     * 载入图片，加载入adapter
     */
    private void initImage() {

        ArrayList<Integer> themeStartList = new ArrayList<>();
        themeStartList.add(R.drawable.theme_star_0);
        themeStartList.add(R.drawable.theme_star_1);
        themeStartList.add(R.drawable.theme_star_2);
        Theme themeStar = new Theme(R.drawable.theme_star_0, "星空", themeStartList);

        ArrayList<Integer> themeGrassList = new ArrayList<>();
        themeGrassList.add(R.drawable.theme_grass_0);
        themeGrassList.add(R.drawable.theme_grass_1);
        themeGrassList.add(R.drawable.theme_grass_2);
        Theme themeGrass = new Theme(R.drawable.theme_grass_0, "田野", themeGrassList);

        ArrayList<Integer> themeRoadList = new ArrayList<>();
        themeRoadList.add(R.drawable.theme_road_0);
        themeRoadList.add(R.drawable.theme_road_1);
        themeRoadList.add(R.drawable.theme_road_2);
        Theme themeRoad = new Theme(R.drawable.theme_road_0, "小路", themeRoadList);

        adapter.add(themeStar);
        adapter.add(themeGrass);
        adapter.add(themeRoad);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bind.unbind();
    }
}
