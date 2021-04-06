package com.hiy.camerap;

import android.os.Looper;
import android.util.Log;

/**
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/3/30
 */
public class ThreadUtils {
    private final static String tag = "ThreadUtils";

    public static boolean isMainThread() {
        boolean isMainThread = Looper.myLooper() == Looper.getMainLooper();
        Log.d(tag, "是否在主线程 = 【" + isMainThread + "】");
//12
//111
//

        return isMainThread;
    }


}
