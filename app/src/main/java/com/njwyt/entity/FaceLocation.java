package com.njwyt.entity;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

/**
 * Created by jason_samuel on 2017/10/17.
 * 头像坐标封装类
 * CameraSurfaceView识别到面部后通过这个类封装信息传递给MainActivity
 *
 * 如有更多需要传递的参数，在此类中修改
 */

public class FaceLocation {

    private int x;
    private int y;
    private int height;
    private int width;
    private int matchRatio;
    private Mat faceImage;
    private Mat faceDetectedImage;
    private Bitmap faceBitmap;

    public Mat getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(Mat faceImage) {
        this.faceImage = faceImage;
    }

    public Mat getFaceDetectedImage() {
        return faceDetectedImage;
    }

    public void setFaceDetectedImage(Mat faceDetectedImage) {
        this.faceDetectedImage = faceDetectedImage;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getMatchRatio() {
        return matchRatio;
    }

    public void setMatchRatio(int matchRatio) {
        this.matchRatio = matchRatio;
    }

    public Bitmap getFaceBitmap() {
        return faceBitmap;
    }

    public void setFaceBitmap(Bitmap faceBitmap) {
        this.faceBitmap = faceBitmap;
    }
}
