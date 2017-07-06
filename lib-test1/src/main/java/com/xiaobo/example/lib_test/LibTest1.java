package com.xiaobo.example.lib_test;

/**
 * Created by moxiaobo on 17/7/5.
 */

public class LibTest1 {

    private int mCount = 0;

    public int add(int a, int b) {
        return a + b;
    }

    public void recursion() {
        if (mCount >= 10) {
            mCount = 0;
            return;
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mCount++;
            recursion();
        }
    }

}
