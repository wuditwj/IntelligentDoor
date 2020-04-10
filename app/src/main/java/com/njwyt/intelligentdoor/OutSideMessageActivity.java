package com.njwyt.intelligentdoor;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.njwyt.fragment.OutSideMessageFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class OutSideMessageActivity extends AppCompatActivity {

    @BindView(R.id.start)
    Button start;
    @BindView(R.id.stop)
    Button stop;
    @BindView(R.id.frag_layout)
    FrameLayout fragLayout;
    private Unbinder bind;
    private OutSideMessageFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_out_side_message);
        fragment = new OutSideMessageFragment();
        getFragmentManager().beginTransaction().replace(R.id.frag_layout,fragment).commit();
        getFragmentManager().beginTransaction().hide(fragment).commit();
        bind = ButterKnife.bind(this);
    }

    @OnClick({R.id.start, R.id.stop})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.start:
                fragment.startRecorder(null);
                break;
            case R.id.stop:
                fragment.stopRecorder();
                break;
        }
    }


    @Override
    protected void onDestroy() {
        bind.unbind();
        super.onDestroy();
    }
}

