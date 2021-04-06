package com.hiy.camerap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SplashAc extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View view = findViewById(android.R.id.content);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashAc.this, MainActivity.class);
                startActivity(intent);
            }
        }, 11150);
    }

    @Override
    protected void onResume() {
        super.onResume();


    }
}