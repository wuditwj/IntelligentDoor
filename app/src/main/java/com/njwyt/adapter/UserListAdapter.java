package com.njwyt.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.icu.text.IDNA;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.njwyt.entity.User;
import com.njwyt.intelligentdoor.R;
import com.pkmmte.view.CircularImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/9/2.
 */

public class UserListAdapter extends BaseAdapter {
    public List<User> list;
    private Context context;
    //声明接口
    private OnButtonClickListener onButtonClick;

    public UserListAdapter(Context context) {
        this.context=context;
        list=new ArrayList<>();
    }

    public void add(List<User> list){
        this.list=list;
    }

    public void add(User user){
        list.add(user);
    }

    public void del(int position){
        list.remove(position);
    }


    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, final ViewGroup viewGroup) {
        ViewHold viewHold;
        if (view==null){
            viewHold=new ViewHold();
            view=View.inflate(context, R.layout.item_user_list,null);
            viewHold.userSet=view.findViewById(R.id.user_set);
            viewHold.userDel=view.findViewById(R.id.del_user);
            viewHold.head=view.findViewById(R.id.sdv_send_head);
            view.setTag(viewHold);
        }else {
            viewHold= (ViewHold) view.getTag();
        }
        final User user=list.get(i);
        Bitmap bm = BitmapFactory.decodeFile(user.getHeadUrl());
        if (bm==null){
            viewHold.head.getHierarchy().setPlaceholderImage(R.drawable.first);
        }else {
            viewHold.head.getHierarchy().setPlaceholderImage(new BitmapDrawable(null, bm));
        }
        viewHold.userSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onButtonClick!=null){
                    onButtonClick.OnSetClickListeners(user,i);
                }

            }
        });

        final View finalView = view;
        viewHold.userDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onButtonClick!=null){
                    onButtonClick.OnDelClickListeners(user,i, finalView);
                }
            }
        });


        return view;
    }

    public class ViewHold{
        private SimpleDraweeView head;
        private Button userSet,userDel;
    }

    public interface OnButtonClickListener{
        void OnSetClickListeners(User user,int position);
        void OnDelClickListeners(User user,int position,View view);
    }
    public void setOnButtonClickListener(OnButtonClickListener onButtonClickListener){
        onButtonClick=onButtonClickListener;
    }
}
