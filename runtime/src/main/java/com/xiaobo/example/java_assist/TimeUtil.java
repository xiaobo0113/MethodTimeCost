package com.xiaobo.example.java_assist;

import android.util.Log;

import java.util.LinkedList;

import static java.lang.Thread.currentThread;

/**
 * Created by moxiaobo on 17/7/3.
 */

public class TimeUtil {

    private TimeUtil() {
        // do nothing.
    }

    public static void push(LinkedList<Long> start_time_list) {
        if (currentThread().getName().equals("main")) {
            start_time_list.push(System.currentTimeMillis());
        }
    }

    public static void pop(LinkedList<Long> start_time_list, String methodName, String tag, long costBiggerThan) {
        if (currentThread().getName().equals("main")) {
            Long start = start_time_list.pop();
            Long cost = System.currentTimeMillis() - start;
            if (cost >= costBiggerThan) {
                Log.d(tag, methodName + " cost: " + cost + " ms");
            }
        }
    }

}
