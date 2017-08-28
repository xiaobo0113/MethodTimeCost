package com.xiaobo.example.java_assist;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static java.lang.Thread.currentThread;

/**
 * Created by moxiaobo on 17/7/3.
 */

public class TimeUtil {

    private static LinkedHashMap<String, LinkedList<Long>> sTimes = new LinkedHashMap<>();

    private static String sTag = null;
    private static long sCostBiggerThan = Long.MIN_VALUE;

    private TimeUtil() {
        // do nothing.
    }

    public static void push(LinkedList<Long> start_time_list) {
        if (currentThread().getName().equals("main")) {
            start_time_list.push(System.currentTimeMillis());
        }
    }

    public static void pop(LinkedList<Long> start_time_list) {
        if (currentThread().getName().equals("main")) {
            Long end = System.currentTimeMillis();

            // elements[3] 即为调用 push 的方法
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            String callFrom = getCallFrom(elements[3]);
            Long start = start_time_list.pop();
            // 当 (end - start) 大于多少时才输出
            if ((end - start) >= getCostBiggerThan()) {
                String lineNumber = elements[3].getLineNumber() != -1 ? ":" + elements[3].getLineNumber() : "";
                if (!lineNumber.equals("")) {
                    callFrom = callFrom.substring(0, callFrom.length() - 1) + lineNumber + ")";
                }
                Log.d(getTag(), callFrom + " cost: " + (end - start) + " ms");
            }
        }
    }

    public static String getTag() {
        if (sTag == null) {
            Class c = TimeUtil.class;
            try {
                Field field = c.getDeclaredField("sTag_Real");
                sTag = (String) field.get(null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return sTag;
    }

    public static long getCostBiggerThan() {
        if (sCostBiggerThan == Long.MIN_VALUE) {
            Class c = TimeUtil.class;
            try {
                Field field = c.getDeclaredField("sCostBiggerThan_Real");
                sCostBiggerThan = (long) field.get(null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return sCostBiggerThan;
    }

    public static String getCallFrom(StackTraceElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append(element.getClassName()).
                append(".").
                append(element.getMethodName()).
                append("(").
                append(element.getFileName()).
                append(")");

        return sb.toString();
    }

}
