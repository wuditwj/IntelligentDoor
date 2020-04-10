package com.njwyt.utils;

import android.content.Context;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by jason_samuel on 2017/8/29.
 */

public class DateUtils {
    /**
     * 日期转星期
     *
     * @param datetime
     * @return
     */
    /*public static String dateToWeek(String datetime, Context context) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        String[] weekDaysEn = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        Calendar cal = Calendar.getInstance(); // 获得一个日历
        Date datet = null;
        try {
            datet = f.parse(datetime);
            cal.setTime(datet);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 指示一个星期中的某天。
        if (w < 0)
            w = 0;


        // 如果切换了语言
        Locale currentLocal = context.getResources().getConfiguration().locale;
        if (currentLocal.equals(Locale.US)) {
            return weekDaysEn[w];
        }
        return weekDays[w];
    }
*/

    /**
     * 日期从横杠转为年月日
     *
     * @param datetime
     * @return
     */
    public static String dateToChinese(String datetime, Context context) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleDateFormat;
        // 如果切换了语言
        Locale currentLocal = context.getResources().getConfiguration().locale;
        if (!currentLocal.equals(Locale.US)) {
            simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 EEEE");
        } else {
            simpleDateFormat = new SimpleDateFormat("EEE MMM dd, yyyy", Locale.US);
            //simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        Date date = null;
        try {
            date = format.parse(datetime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return simpleDateFormat.format(date);
    }

    /**
     * 日期从横杠转为月日
     *
     * @param datetime
     * @return
     */
    public static String dateToMonthDay(String datetime, Context context) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleDateFormat;
        // 如果切换了语言
        Locale currentLocal = context.getResources().getConfiguration().locale;
        if (!currentLocal.equals(Locale.US)) {
            simpleDateFormat = new SimpleDateFormat("MM月dd日 EEEE");
        } else {
            simpleDateFormat = new SimpleDateFormat("EEE MMM dd, yyyy", Locale.US);
            //simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        Date date = null;
        try {
            date = format.parse(datetime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return simpleDateFormat.format(date);
    }

    /**
     * 获取当前时间
     *
     * @return
     */
    public static String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        return formatter.format(curDate);
    }

    /**
     * 提取日期中的时，分
     *
     * @param dateStr
     * @return
     */
    public static String getHourMinute(String dateStr) {

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        Date date = null;
        try {
            date = format.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return simpleDateFormat.format(date);
    }

    /**
     * 判断相差天数
     *
     * @param dateStr1 时间较远的那天
     * @param dateStr2 较近的那天
     * @return 相差天数
     */
    public static int differenceDays(String dateStr1, String dateStr2) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {

            Date date1 = formatter.parse(dateStr1);
            Date date2 = formatter.parse(dateStr2);
            return (int) ((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
