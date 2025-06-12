package moe.hx030.momogram.util;

public class ThreadUtil {

    public static boolean sleep(long ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignore) {
                return false;
            }
        }
        return true;
    }

}
