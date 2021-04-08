package com.hiy.camerap.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hiy.camerap.R;

/**
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/4/8
 */
public class CommodityHolder extends RecyclerView.ViewHolder {
    public static int getLayoutId() {
        return R.layout.item_view_setting;
    }

    public CommodityHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void fillData() {

    }

}
