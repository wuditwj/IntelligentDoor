package com.njwyt.intelligentdoor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.Unbinder;

public class ScreenActivity extends BaseActivity {

    @BindView(R.id.screen_image)
    ImageView screenImage;
    private Unbinder bind;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        bind = ButterKnife.bind(this);
    }

    @OnClick(R.id.screen_image)
    public void onViewClicked() {
        Intent intent=new Intent(this,MainActivity.class);
        intent.putExtra("Screen",true);
        startActivity(intent);
    }

    @OnLongClick(R.id.screen_image)
    public boolean onViewLongClicked(){
        Intent intent=new Intent(this,MainActivity.class);
        intent.putExtra("Screen",false);
        startActivity(intent);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bind.unbind();
    }


}
