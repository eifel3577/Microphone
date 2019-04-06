package com.sapphire.microphone.model;


import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import com.sapphire.microphone.MicrofonApp;
import com.yandex.metrica.Counter;

import java.util.ArrayList;
import java.util.List;

public class Dump {
    private long availMemory = 0;
    private long totalMemory = 0;
    private String connectionType = "WIFI";
    private String role = "MIC";
    private String errorMessage = "";
    private List<String> processInfoList;


    public static void createDump(final String connectionType, final String errorMessage, final String role) {
        final Dump dump = new Dump();
        dump.connectionType = connectionType;
        dump.errorMessage = errorMessage;
        ActivityManager activityManager = (ActivityManager) MicrofonApp.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        dump.availMemory = memoryInfo.availMem/1024/1024;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            dump.totalMemory = memoryInfo.totalMem/1024/1024;
        else
            dump.totalMemory = Runtime.getRuntime().maxMemory()/1024/1024;
        final List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        dump.processInfoList = new ArrayList<String>(processes.size());
        for (ActivityManager.RunningAppProcessInfo process : processes) {
             dump.processInfoList.add(process.processName);
        }
        dump.role = role;
        Counter.sharedInstance().reportError(Build.MODEL, new Exception(dump.toString()));
    }


    @Override
    public String toString() {
        return "Dump{" +
                "availMemory=" + availMemory +
                ", totalMemory=" + totalMemory +
                ", connectionType='" + connectionType + '\'' +
                ", role='" + role + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", processInfoList=" + processInfoList +
                '}';
    }
}
