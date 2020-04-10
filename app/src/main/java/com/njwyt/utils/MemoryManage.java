package com.njwyt.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.io.File;
import java.util.List;

public class MemoryManage {
    /**
     * 清理内存
     *
     * @param context
     */
    public static void clearMemory(Context context) {
        ActivityManager activityManger = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = activityManger.getRunningAppProcesses();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ActivityManager.RunningAppProcessInfo apinfo = list.get(i);
                String[] pkgList = apinfo.pkgList;
                if (apinfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    for (int j = 0; j < pkgList.length; j++) {
                        /**清理不可用的内容空间**/
                        activityManger.killBackgroundProcesses(pkgList[j]);
                    }
                }
            }
        }
    }

    /**
     * 清理应用缓存
     *
     * @param context
     */
    public static void clearAppCache(Context context) {
        File[] dir = context.getCacheDir().listFiles();
        if (dir != null) {
            for (File f : dir) {
                f.delete();
            }
        }
    }
}
