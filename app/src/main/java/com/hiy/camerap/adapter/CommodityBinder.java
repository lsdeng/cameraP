package com.hiy.camerap.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.hiy.camerap.bean.Commodity;

import me.drakeet.multitype.ItemViewBinder;

/**
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/4/8
 */
public class CommodityBinder extends ItemViewBinder<Commodity, CommodityHolder> {

    @NonNull
    @Override
    protected CommodityHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        View view = inflater.inflate(CommodityHolder.getLayoutId(), parent, false);
        return new CommodityHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull CommodityHolder holder, @NonNull Commodity item) {
        holder.fillData();
    }
}
