package com.hiy.camerap;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SplashAc extends AppCompatActivity {

    private static String tag = null;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        tag = getClass().getSimpleName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View view = findViewById(android.R.id.content);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(tag, "start MainAc");
                Intent intent = new Intent(SplashAc.this, MainActivity.class);
                startActivity(intent);
            }
        }, 150);
    }

    @Override
    protected void onResume() {
        super.onResume();


    }
}