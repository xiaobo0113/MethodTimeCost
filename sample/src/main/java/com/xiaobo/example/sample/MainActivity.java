package com.xiaobo.example.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.xiaobo.example.lib_test.LibTest1;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new LibTest1().add(1, 2);
        new LibTest1().recursion();
    }

}
