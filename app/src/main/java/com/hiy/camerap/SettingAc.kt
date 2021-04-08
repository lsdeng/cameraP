package com.hiy.camerap

import android.os.Bundle
import android.util.Log
import android.view.View

/**
 *
 *
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/4/7
 */
class SettingAc : BaseAc() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ac_setting)

        var tv = this.findViewById<View>(R.id.textView)
        tv.setOnClickListener { _ -> Log.d(tag, "${tv.id}") }
    }
}