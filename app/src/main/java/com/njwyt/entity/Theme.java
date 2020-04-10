package com.njwyt.entity;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/10/9.
 */

public class Theme {
    private int image;                  // 预览图
    private String text;                // 名称
    private ArrayList<Integer> imageList;    // 所有图片

    public Theme(int image, String text, ArrayList<Integer> imageList) {
        this.image = image;
        this.text = text;
        this.imageList = imageList;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ArrayList<Integer> getImageList() {
        return imageList;
    }

    public void setImageList(ArrayList<Integer> imageList) {
        this.imageList = imageList;
    }
}
