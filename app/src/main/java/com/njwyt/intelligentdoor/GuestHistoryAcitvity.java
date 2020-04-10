package com.njwyt.intelligentdoor;

import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;

import com.njwyt.AppContext;
import com.njwyt.adapter.UniversalAdapter;
import com.njwyt.db.GuestRecordingDaoHelp;
import com.njwyt.entity.GuestRecording;
import com.njwyt.fragment.PlayOutSideMessageFragment;
import com.njwyt.intelligentdoor.databinding.ActivityGuestHistoryBinding;
import com.njwyt.intelligentdoor.databinding.ItemGuestHistoryBinding;
import com.njwyt.utils.DateUtils;
import com.njwyt.utils.ImageUtils;
import com.njwyt.view.ItemDecoration;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by jason_samuel on 2018/1/2.
 * 访客语音留言列表页面，所有已登录的用户均可查看
 */

public class GuestHistoryAcitvity extends BaseActivity {

    private final String TAG = "GuestHistoryAcitvity";
    private ActivityGuestHistoryBinding binding;

    private GuestRecordingDaoHelp mGuestRecordingDaoHelp;
    private ArrayList<GuestRecording> mGuestRecordingList;
    private UniversalAdapter<GuestRecording> mGuestRecordingAdapter;

    private PlayOutSideMessageFragment mPlayOutSideMessageFragment;        // 播放页面

    private boolean isEditMode;        // 进入编辑模式(比如删除)

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_guest_history);

        setBlur();
        initValue();
        initFragment();
        initListener();
        initRecyclerView();
        getMessageHistoryList();
    }

    /**
     * 设置该activity页面背景高斯模糊
     */
    private void setBlur() {
        Bitmap blurBmp = ImageUtils.blurBitmap(this, AppContext.getInstance().getBlurBitmap(), 20);
        binding.ivBackground.setBackground(new BitmapDrawable(null, blurBmp));
    }

    private void initView() {

    }

    private void initFragment() {
        mPlayOutSideMessageFragment = new PlayOutSideMessageFragment();
        getFragmentManager().beginTransaction().replace(R.id.rl_recording, mPlayOutSideMessageFragment).commit();
        getFragmentManager().beginTransaction().hide(mPlayOutSideMessageFragment).commit();
    }

    private void initValue() {

        mGuestRecordingList = new ArrayList<>();
        mGuestRecordingDaoHelp = new GuestRecordingDaoHelp(this);

    }

    private void initListener() {
        // 返回按钮事件
        binding.tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEditMode) {
                    // 退出编辑模式
                    isEditMode = false;
                    mGuestRecordingAdapter.refresh(mGuestRecordingList);
                    binding.tvEdit.setTextColor(getResources().getColor(R.color.colorWhite));
                } else {
                    finish();
                }
            }
        });

        // 编辑
        binding.tvEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isEditMode) {
                    // 进入编辑模式
                    isEditMode = true;
                    // 设置为全部不选择
                    for (int i = 0; i < mGuestRecordingList.size(); i++) {
                        mGuestRecordingList.get(i).setSelected(false);
                    }
                    mGuestRecordingAdapter.refresh(mGuestRecordingList);
                    binding.tvEdit.setTextColor(getResources().getColor(R.color.colorRed));
                } else {
                    // 删除并退出编辑模式
                    isEditMode = false;

                    // 逐个删除
                    for (int i = 0; i < mGuestRecordingList.size(); i++) {
                        GuestRecording guestRecording = mGuestRecordingList.get(i);
                        if (guestRecording.isSelected()) {
                            mGuestRecordingDaoHelp.deleteGuestRecording(guestRecording.getUuid());
                            mGuestRecordingList.remove(i);
                            mGuestRecordingAdapter.notifyItemRemoved(i + 1);
                        }
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mGuestRecordingAdapter.refresh(mGuestRecordingList);
                            binding.tvEdit.setTextColor(getResources().getColor(R.color.colorWhite));
                        }
                    }, 400);
                }
            }
        });
    }

    private void initRecyclerView() {

        // 配置当日历史列表适配器
        mGuestRecordingAdapter = new UniversalAdapter<>(mGuestRecordingList, R.layout.item_guest_history, BR.guestRecording, new UniversalAdapter.AdapterView() {
            @Override
            public void getViewDataBinding(UniversalAdapter.ViewHolder viewHolder, final int position) {
                final ItemGuestHistoryBinding binding = (ItemGuestHistoryBinding) viewHolder.getBinding();
                final GuestRecording guestRecording = mGuestRecordingList.get(position);

                String date = guestRecording.getDatatime();
                binding.tvMessageDatatime.setText(DateUtils.dateToChinese(date, GuestHistoryAcitvity.this));
                binding.tvTime.setText(DateUtils.getHourMinute(date));
                binding.sdvGuestImage.setImageURI("file://" + guestRecording.getGuestPicUrl());

                binding.llRoot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //GuestHistoryAcitvity.this.binding.viewRecording.setVisibility(View.VISIBLE);
                        mPlayOutSideMessageFragment.startPlay(guestRecording);
                    }
                });

                binding.llRoot.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        showDeleteDialog(position, guestRecording);
                        return true;
                    }
                });

                binding.cbSelect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (binding.cbSelect.isChecked()) {
                            mGuestRecordingList.get(position).setSelected(true);
                        } else {
                            mGuestRecordingList.get(position).setSelected(false);
                        }
                    }
                });

                if (isEditMode) {
                    binding.cbSelect.setVisibility(View.VISIBLE);
                } else {
                    binding.cbSelect.setVisibility(View.GONE);
                }

                if (guestRecording.isSelected()) {
                    binding.cbSelect.setChecked(true);
                } else {
                    binding.cbSelect.setChecked(false);
                }
            }
        });

        // 配置列表样式
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.rvGuestHistory.setLayoutManager(linearLayoutManager);
        binding.rvGuestHistory.addItemDecoration(new ItemDecoration(20));
        binding.rvGuestHistory.setAdapter(mGuestRecordingAdapter);
        // 禁止上拉刷新和下拉加载
        binding.rvGuestHistory.setLoadingMoreEnabled(false);
        binding.rvGuestHistory.setPullRefreshEnabled(false);
    }


    /**
     * 删除提示框
     */
    private void showDeleteDialog(final int position, final GuestRecording guestRecording) {
        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_history, null);
        deleteDialog.setView(dialogView);
        deleteDialog.setCancelable(false);
        final Dialog dialog = deleteDialog.show();

        // 是
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGuestRecordingDaoHelp.deleteGuestRecording(guestRecording.getUuid());
                mGuestRecordingList.remove(position);
                mGuestRecordingAdapter.notifyItemRemoved(position + 1);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGuestRecordingAdapter.refresh(mGuestRecordingList);
                    }
                }, 400);
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

    /**
     * 从数据可中获取历史列表
     */
    private void getMessageHistoryList() {

        // 刷新适配器
        mGuestRecordingList = (ArrayList<GuestRecording>) mGuestRecordingDaoHelp.getGuestRecordingList();

        for (GuestRecording guestRecording : mGuestRecordingList) {
            int days = DateUtils.differenceDays(guestRecording.getDatatime(), DateUtils.getCurrentTime());

            // 删除三十天以前的留言
            if (days >= 30) {
                mGuestRecordingDaoHelp.deleteGuestRecording(guestRecording.getUuid());
                mGuestRecordingList.remove(guestRecording);
            }
        }

        mGuestRecordingAdapter.refresh(mGuestRecordingList);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
