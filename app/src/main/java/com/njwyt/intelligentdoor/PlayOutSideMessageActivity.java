package com.njwyt.intelligentdoor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.njwyt.entity.GuestRecording;
import com.njwyt.fragment.PlayOutSideMessageFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayOutSideMessageActivity extends AppCompatActivity {

    @BindView(R.id.start)
    Button start;
    @BindView(R.id.stop)
    Button stop;
    private PlayOutSideMessageFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_out_side_message);
        ButterKnife.bind(this);
        fragment=new PlayOutSideMessageFragment();

        getFragmentManager().beginTransaction().replace(R.id.fragment_layout,fragment).commit();
        getFragmentManager().beginTransaction().hide(fragment).commit();

    }

    @OnClick({R.id.start, R.id.stop})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.start:
                fragment.startPlay(null);
                break;
            case R.id.stop:
                fragment.stopPlay();
                break;
        }
    }
}
