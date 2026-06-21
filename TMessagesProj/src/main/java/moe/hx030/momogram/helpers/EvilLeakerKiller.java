package moe.hx030.momogram.helpers;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

public class EvilLeakerKiller {
    private static EvilLeakerKiller instance;
    private ActivityManager activityManager;
    private int[] pid = new int[] { android.os.Process.myPid() };
    public int PSS = -1;

    public static int threshold = (int) (1.2 * 1048576); // 1.2GB


    public static EvilLeakerKiller getInstance(Context context) {
        if (instance == null && context != null) {
            return (instance = new EvilLeakerKiller(context));
        }
        return instance;
    }

    private EvilLeakerKiller(Context context) {
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        instance = this;
        checkRamUsage();
    }

    public int checkRamUsage() {
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
//        activityManager.getMemoryInfo(info);
//        long totalRam = info.totalMem;
//        long availableRam = info.availMem;

        Debug.MemoryInfo[] infos = activityManager.getProcessMemoryInfo(pid);
//        int totalPrivateDirty = infos[0].getTotalPrivateDirty(); // in KB
//        int totalSharedDirty = infos[0].getTotalSharedDirty(); // in KB
        PSS = infos[0].getTotalPss(); // in KB

        Log.d("030-ram", String.valueOf(PSS));
        return PSS;
    }

    public static int setThreshold(float GB) {
        return threshold = (int) (GB * 1048576);
    }

}
