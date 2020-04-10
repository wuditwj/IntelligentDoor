package com.njwyt.content;

/**
 * Created by jason_samuel on 2017/8/29.
 */

public class Type {

    // 登录成功识别率
    public static final int MATCH_RATIO = 80;

    // 扫描图缩放比例
    public static final float FACE_SCALE = 0.7f;

    // 主页面上手抬起事件
    public static final int ON_MAIN_KEY_UP = 1000;

    // 登录成功事件
    public static final int LOGIN_SUCCESS = 1002;

    // 字体变化
    public static final int CHANGE_FONT_SIZE = 1003;

    // 语言变换
    public static final int CHANGE_LANGUAGE = 1004;

    // 发送距离
    public static final int CHANGE_DISTANCE = 1005;

    // 刷新FPS
    public static final int REFLASH_FPS = 1006;

    // 门外触发感应
    public static final int OUTDOOR_RESPONSE = 1007;

    // 开始访客录音
    public static final int START_GUEST_RECORDING = 1006;
    public static final int STOP_GUEST_RECORDING = 1007;

    // 人脸识别结果通知
    public static final int FACE_RESULT = 2000;

    // 密码页面打开模式
    public static final int PASSWORD_SETTING = 3001;    // 设置密码模式
    public static final int PASSWORD_ADMIN = 3002;      // 打开设置页面
    public static final int PASSWORD_LOGIN = 3003;      // 密码登录

    // 人眼距离
    public static final int DISTANCE_FAR = 50;        // 中远距离分割点
    public  static final int DISTANCE_CLOSE = 65;      // 中近距离分割点

    // 识别模式
    public static final int RECOGNITION_DEFAULT = 10;   // 不识别
    public static final int RECOGNITION_LOGIN = 11;     // 识别并登录
    public static final int RECOGNITION_REGISTER = 12;     // 识别并注册

    //操作难度
    public static final int DIFFICULTY_ADVANCED = 2;//高级
    public static final int DIFFICULTY_MEDIUM = 1;//中等
    public static final int DIFFICULTY_SIMPLENESS= 0;//简单

    //语言
    public static final String LANGUAGE_CHINESE = "ch";//中文
    public static final String LANGUAGE_ENGLISH = "en";//英语

    //字体大小
    public static final int FONTSIZE_BIG = 2;//大
    public static final int FONTSIZE_SMALL = 1;//小
    public static final int FONTSIZE_MEDIUM = 0;//中

    //主题
    public static final int THEME_SIMPLE = 0;//简约

    public static final String DOOR_OPEN = "P";  // 开门
    public static final String LIGHT_OFF = "D";  // 让灯关闭
    public static final String LIGHT_ON = "B";    // 让灯打开
    public static final String LIGHT_INSIDE = "I";    // 控制门内灯亮度I01~I99
    public static final String LIGHT_OUTSIDE = "O";    // 控制门外灯亮度O01~O99
    public static final String LIGHT_ON_RESULT = "Y";   // 开灯回调
}
