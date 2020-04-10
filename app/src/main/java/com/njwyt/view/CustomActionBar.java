package com.njwyt.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.njwyt.intelligentdoor.R;

/**
 * Created by Administrator on 2017/8/31.
 */

public class CustomActionBar extends RelativeLayout {
    private RelativeLayout actionbar_left,actionbar_center,actionbar_right;
    private TextView tv_left,tv_center,tv_right;
    public CustomActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.custom_actionbar,this);
        if (isInEditMode()){
            return;
        }
        init();
    }

    private void init() {
        actionbar_left=findViewById(R.id.actionbar_left);
        actionbar_center=findViewById(R.id.actionbar_center);
        actionbar_right=findViewById(R.id.actionbar_right);
        tv_left=findViewById(R.id.tv_left);
        tv_center=findViewById(R.id.tv_center);
        tv_right=findViewById(R.id.tv_right);
    }

    /**
     * 初始化标题栏
     * @param resLeftId 左边图片ID
     * @param leftListener 左边监听
     * @param resCenterId 中间标题
     * @param resRightId 右边图片ID
     * @param rightListener 右边监听
     */
    public void initTitleBar(int resLeftId,OnClickListener leftListener, int resCenterId, int resRightId,OnClickListener rightListener
    ){
        setLeftImageResource(resLeftId);
        setLeftListener(leftListener);
        setRightImageResource(resRightId);
        setRightListener(rightListener);
        setCenterText(resCenterId);
    }
    /**
     * 初始化标题栏
     * @param resLeftId 左边图片ID
     * @param leftListener 左边监听
     * @param resCenterId 中间标题
     */
    public void initTitleBar(int resLeftId,OnClickListener leftListener, int resCenterId){
        setLeftImageResource(resLeftId);
        setLeftListener(leftListener);
        setCenterText(resCenterId);
        actionbar_right.setVisibility(View.INVISIBLE);
    }
    /**
     * 初始化标题栏
     * @param resLeftId 左边图片ID
     * @param leftListener 左边监听
     * @param resCenterId 中间标题
     */
    public void initTitleBar(int resLeftId,OnClickListener leftListener, String resCenterId){
        setLeftImageResource(resLeftId);
        setLeftListener(leftListener);
        setCenterText(resCenterId);
        actionbar_right.setVisibility(View.INVISIBLE);
    }
    /**
     * 初始化标题栏
     * @param resCenterId 中间标题
     * @param resRightId 右边图片ID
     * @param rightListener 右边监听
     */
    public void initTitleBar(int resCenterId, int resRightId,OnClickListener rightListener ){
        setRightImageResource(resRightId);
        setRightListener(rightListener);
        setCenterText(resCenterId);
        actionbar_left.setVisibility(View.INVISIBLE);
    }
    /**
     * 初始化标题栏
     * @param resCenterId 中间标题
     */
    public void initTitleBar(int resCenterId){
        setCenterText(resCenterId);
        actionbar_right.setVisibility(View.INVISIBLE);
        actionbar_left.setVisibility(View.INVISIBLE);
    }

    /**
     * 左边点击监听
     * @param listener
     */
    private void setLeftListener(OnClickListener listener){
        actionbar_left.setOnClickListener(listener);

    }

    /**
     * 右边点击监听
     * @param listener
     */
    private void setRightListener(OnClickListener listener){
        actionbar_right.setOnClickListener(listener);

    }

    /**
     * 设置左边文字
     * @param resId
     */
    private void setLeftImageResource(int resId){
        tv_left.setText(resId);
    }

    /**
     * 设置右边文字
     * @param resId
     */
    private void setRightImageResource(int resId){
        tv_right.setText(resId);
    }

    /**
     * 设置标题
     * @param resCenterId
     */
    private void setCenterText(int resCenterId){
        tv_center.setText(resCenterId);

    }
    /**
     * 设置标题
     * @param resCenterId
     */
    private void setCenterText(String resCenterId){
        tv_center.setText(resCenterId);

    }
}
