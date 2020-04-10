package com.njwyt.db;

import com.anupcowkur.reservoir.Reservoir;
import com.njwyt.intelligentdoor.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jason_samuel on 2017/10/9.
 */

public class ReservoirHelper {

    // 语言
    public static String getLanguage() {
        String language = "";
        try {
            language = Reservoir.get("language", String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return language;
    }

    public static void setLanguage(String language) {
        try {
            Reservoir.put("language", language);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 难度
    public static void setLevel(int level) {
        try {
            Reservoir.put("level", level);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getLevel() {
        int level = 0;
        try {
            level = Reservoir.get("level", Integer.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return level;
    }

    // 字体大小
    public static void setFontSize(int fontSize) {
        try {
            Reservoir.put("fontSize", fontSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getFontSize() {
        int fontSize = 0;
        try {
            fontSize = Reservoir.get("fontSize", Integer.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fontSize;
    }

    // 主题
    public static void setTheme(int theme) {
        try {
            Reservoir.put("theme", theme);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getTheme() {
        int theme = R.drawable.theme_star_0;  // 默认值
        try {
            theme = Reservoir.get("theme", Integer.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return theme;
    }

    // 主题队列
    public static void setThemeList(List<Integer> themeList) {
        try {
            Reservoir.put("themeList", themeList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Integer> getThemeList() {

        List<Integer> themeList = null;  // 默认值
        try {
            if (Reservoir.contains("themeList")) {
                themeList = new ArrayList<>();
                // Reservoir拿出来的是Double类型，这里要强转一下
                List<Double> conversionList = Reservoir.get("themeList", List.class);
                for (Double d : conversionList) {
                    themeList.add(d.intValue());
                }
            } else {
                themeList = new ArrayList<>();
                themeList.add(R.drawable.theme_star_0);
                themeList.add(R.drawable.theme_star_1);
                themeList.add(R.drawable.theme_star_2);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return themeList;
    }


    // 设置管理员密码
    public static void setSystemPassword(String systemPassword) {
        try {
            Reservoir.put("systemPassword", systemPassword);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getSystemPassword() {
        String systemPassword = "";  // 默认值
        try {
            systemPassword = Reservoir.get("systemPassword", String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return systemPassword;
    }

    // 首次安装
    public static void setFirstLoad(boolean firstLoad) {
        try {
            Reservoir.put("firstLoad", firstLoad);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean getFirstLoad() {
        boolean firstLoad = true;  // 默认值
        try {
            firstLoad = Reservoir.get("firstLoad", Boolean.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return firstLoad;
    }
}
