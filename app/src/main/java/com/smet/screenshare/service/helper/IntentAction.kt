package com.smet.screenshare.service.helper

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.elvishew.xlog.XLog
import com.smet.screenshare.MainActivity
import com.smet.screenshare.data.other.getLog
import com.smet.screenshare.service.AppService
import kotlinx.parcelize.Parcelize

sealed class IntentAction : Parcelable {
    internal companion object {
        private const val EXTRA_PARCELABLE = "EXTRA_PARCELABLE"
        fun fromIntent(intent: Intent?): IntentAction? = intent?.getParcelableExtra(EXTRA_PARCELABLE)
    }

    fun toAppServiceIntent(context: Context): Intent = AppService.getAppServiceIntent(context).apply {
        putExtra(EXTRA_PARCELABLE, this@IntentAction)
    }

    fun toAppActivityIntent(context: Context): Intent = MainActivity.getAppActivityIntent(context).apply {
        putExtra(EXTRA_PARCELABLE, this@IntentAction)
    }

    fun sendToAppService(context: Context) {
        XLog.i(context.getLog("sendToAppService", this.toString()))
        if (this is StartOnBoot)
            AppService.startForeground(context, this.toAppServiceIntent(context))
        else
            AppService.startService(context, this.toAppServiceIntent(context))
    }

    @Parcelize object GetServiceState : IntentAction()
    @Parcelize object StartStream : IntentAction()
    @Parcelize object StopStream : IntentAction()
    @Parcelize object Exit : IntentAction()
    @Parcelize data class CastIntent(val intent: Intent) : IntentAction()
    @Parcelize object CastPermissionsDenied : IntentAction()
    @Parcelize object StartOnBoot : IntentAction()
    @Parcelize object RecoverError : IntentAction()

    override fun toString(): String = javaClass.simpleName
}