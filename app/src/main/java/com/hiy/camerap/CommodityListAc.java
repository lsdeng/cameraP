package com.hiy.camerap;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hiy.camerap.adapter.CommodityBinder;
import com.hiy.camerap.bean.Commodity;
import com.hiy.camerap.interceptor.BmobInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.drakeet.multitype.MultiTypeAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/4/8
 */
public class CommodityListAc extends BaseAc {

    private RecyclerView mListRv;
    private List<Object> mData;
    private MultiTypeAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_commodity_list);

        mListRv = findViewById(R.id.rv);
        mListRv.setHasFixedSize(true);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        mListRv.setLayoutManager(manager);

        mData = new ArrayList<>();
        mAdapter = new MultiTypeAdapter(mData);

        mAdapter.register(Commodity.class, new CommodityBinder());
        mListRv.setAdapter(mAdapter);

        loadData();
    }

    private void loadData() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new BmobInterceptor())
                .build();

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("api2.bmob.cn")
                .addPathSegments("1/classes/commodity")
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
