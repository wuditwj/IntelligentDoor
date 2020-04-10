package com.njwyt.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.njwyt.intelligentdoor.MainActivity;

public class BootBroadcastReceiver extends BroadcastReceiver {

    //重写onReceive方法
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, MainActivity.class);
        context.startService(service);

        //启动应用，参数为需要自动启动的应用的包名
        Intent i = context.getPackageManager().getLaunchIntentForPackage("com.njwyt.intelligentdoor");
        context.startActivity(i);
    }

}