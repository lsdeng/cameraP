package com.hiy.camerap;

import android.util.Log;

import com.hiy.camerap.interceptor.BmobInterceptor;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/4/8
 */
class BmobAPi {

    private static final String tag = "api";

    public static final String S_API_commodity_LIST = "1/classes/commodity";


    public static void getApi(String api) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new BmobInterceptor())
                .build();

        String url = HiyConstant.S_BMOB_HOST + File.separator + api;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(tag, "onFailure-" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resString = response.body().string();
                Log.d(tag, "response-" + resString);
            }
        });
    }


}
