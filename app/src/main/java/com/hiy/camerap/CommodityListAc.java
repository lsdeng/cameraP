package com.hiy.camerap;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hiy.camerap.adapter.CommodityBinder;
import com.hiy.camerap.bean.Commodity;

import java.util.ArrayList;
import java.util.List;

import me.drakeet.multitype.MultiTypeAdapter;

/**
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/4/8
 */
public class CommodityListAc extends BaseAc {

    static {
        System.loadLibrary("native-lib");
    }

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
        Log.d(tag, stringFromJNI());
        BmobAPi.getApi(BmobAPi.S_API_commodity_LIST);
    }

}
