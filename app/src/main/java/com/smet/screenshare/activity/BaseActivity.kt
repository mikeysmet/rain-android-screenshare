package com.smet.screenshare.activity

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.elvishew.xlog.XLog
import com.smet.screenshare.data.other.getLog

abstract class BaseActivity(@LayoutRes contentLayoutId: Int) : AppCompatActivity(contentLayoutId) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        XLog.d(getLog("onCreate", "Invoked"))
    }

    override fun onStart() {
        super.onStart()
        XLog.d(getLog("onStart", "Invoked"))
    }

    override fun onResume() {
        super.onResume()
        XLog.d(getLog("onResume", "Invoked"))
    }

    override fun onPause() {
        XLog.d(getLog("onPause", "Invoked"))
        super.onPause()
    }

    override fun onStop() {
        XLog.d(getLog("onStop", "Invoked"))
        super.onStop()
    }

    override fun onDestroy() {
        XLog.d(getLog("onDestroy", "Invoked"))
        super.onDestroy()
    }
}