package com.hiy.camerap.service;

import android.util.Log;

import com.hiy.camerap.HiyConstant;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommodityService {

    public static final String tag = "CommodityService";

    public void requestCommodityInfo() {
        OkHttpClient client = new OkHttpClient.Builder().build();

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("http")
                .host("www.mxnzp.com")
                .addPathSegments("api/barcode/goods/details")
                .addQueryParameter("barcode", "6922266454295")
                .addQueryParameter("app_id", HiyConstant.S_BARCODE_APP_ID)
                .addQueryParameter("app_secret", HiyConstant.S_BARCODE_APP_SECRET)
                .build();

        Log.d(tag, httpUrl.toString());

        Request request = new Request.Builder().url(httpUrl.toString()).build();

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
