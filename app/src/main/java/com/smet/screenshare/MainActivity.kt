package com.smet.screenshare

import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.strictmode.FragmentStrictMode
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.coroutineScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.flattener.ClassicFlattener
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import com.smet.screenshare.data.other.getLog
import com.smet.screenshare.logging.getLogFolder
import com.smet.screenshare.service.AppService
import com.smet.screenshare.service.ServiceMessage
import com.smet.screenshare.service.helper.IntentAction
import info.dvkr.screenstream.logging.DateSuffixFileNameGenerator
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val KEY_CAST_PERMISSION_PENDING = "KEY_CAST_PERMISSION_PENDING"

        fun getAppActivityIntent(context: Context): Intent = Intent(context.applicationContext, MainActivity::class.java)

        fun getStartIntent(context: Context): Intent = getAppActivityIntent(context)
    }

    private var permissionsErrorDialog: MaterialDialog? = null
    private var isCastPermissionsPending: Boolean = false

    private val startMediaProjection =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                XLog.d(getLog("registerForActivityResult", "Cast permission granted"))
                IntentAction.CastIntent(result.data!!).sendToAppService(this.applicationContext)
            } else {
                XLog.w(getLog("registerForActivityResult", "Cast permission denied"))
                IntentAction.CastPermissionsDenied.sendToAppService(this.applicationContext)
                permissionsErrorDialog?.dismiss()
                showErrorDialog(
                    R.string.permission_activity_cast_permission_required_title,
                    R.string.permission_activity_cast_permission_required
                )
            }
        }

    private fun showErrorDialog(@StringRes titleRes: Int, @StringRes messageRes: Int) {
        permissionsErrorDialog = MaterialDialog(this).show {
            lifecycleOwner(this@MainActivity)
            icon(R.drawable.ic_launcher_background)
            title(titleRes)
            message(messageRes)
            positiveButton(android.R.string.ok)
            cancelable(false)
            cancelOnTouchOutside(false)
            onDismiss { permissionsErrorDialog = null }
        }
    }

    private var isStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        initLogger()
        if(!isStarted){
            start()
        }
        routeIntentAction(intent)

    }

    private fun start() {
        serviceMessageLiveData.observe(this) { message -> message?.let { onServiceMessage(it) } }
        bindService(AppService.getAppServiceIntent(this), serviceConnection, Context.BIND_AUTO_CREATE)

        IntentAction.StartStream.sendToAppService(this)
        //IntentAction.StartStream.sendToAppService(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        XLog.d(getLog("onSaveInstanceState", "isCastPermissionsPending: $isCastPermissionsPending"))
        outState.putBoolean(KEY_CAST_PERMISSION_PENDING, isCastPermissionsPending)
        super.onSaveInstanceState(outState)
    }

    private fun initLogger() {
        System.setProperty("kotlinx.coroutines.debug", "on")

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads()
                .permitDiskWrites()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )

        FragmentStrictMode.defaultPolicy =
            FragmentStrictMode.Policy.Builder()
                .detectFragmentReuse()
                .detectFragmentTagUsage()
                .detectRetainInstanceUsage()
                .detectSetUserVisibleHint()
                .detectTargetFragmentUsage()
                .detectWrongFragmentContainer()
                .build()

        val logConfiguration = LogConfiguration.Builder().tag("SSApp").build()
        XLog.init(logConfiguration, AndroidPrinter(), filePrinter)
    }

    val filePrinter: FilePrinter by lazy {
        FilePrinter.Builder(getLogFolder())
            .fileNameGenerator(DateSuffixFileNameGenerator(this.hashCode().toString()))
            .cleanStrategy(FileLastModifiedCleanStrategy(86400000)) // One day
            .flattener(ClassicFlattener())
            .build()
    }

    private fun routeIntentAction(intent: Intent?) {
        val intentAction = IntentAction.fromIntent(intent)
        intentAction != null || return
        XLog.d(getLog("routeIntentAction", "IntentAction: $intentAction"))

        if (intentAction is IntentAction.StartStream) {
            IntentAction.StartStream.sendToAppService(this)
        }
    }

    private val serviceMessageLiveData = MutableLiveData<ServiceMessage>()
    private var serviceMessageFlowJob: Job? = null
    private var isBound: Boolean = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            XLog.d(this@MainActivity.getLog("onServiceConnected"))
            serviceMessageFlowJob =
                lifecycle.coroutineScope.launch(CoroutineName("ServiceActivity.ServiceMessageFlow")) {
                    (service as AppService.AppServiceBinder).getServiceMessageFlow()
                        .onEach { serviceMessage ->
                            XLog.v(this@MainActivity.getLog("onServiceMessage", "$serviceMessage"))
                            serviceMessageLiveData.value = serviceMessage
                        }
                        .catch { cause -> XLog.e(this@MainActivity.getLog("onServiceMessage"), cause) }
                        .collect()
                }

            isBound = true
            IntentAction.GetServiceState.sendToAppService(this@MainActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            XLog.w(this@MainActivity.getLog("onServiceDisconnected"))
            serviceMessageFlowJob?.cancel()
            serviceMessageFlowJob = null
            isBound = false
        }
    }

    fun getServiceMessageLiveData(): LiveData<ServiceMessage> = serviceMessageLiveData

    @CallSuper
    open fun onServiceMessage(serviceMessage: ServiceMessage) {
        if (serviceMessage is ServiceMessage.ServiceState) {
            if (serviceMessage.isWaitingForPermission.not()) {
                isCastPermissionsPending = false
                return
            }

            if (isCastPermissionsPending) {
                XLog.i(getLog("onServiceMessage", "Ignoring: isCastPermissionsPending == true"))
                return
            }

            permissionsErrorDialog?.dismiss()
            isCastPermissionsPending = true
            try {
                val projectionManager = ContextCompat.getSystemService(this, MediaProjectionManager::class.java)!!
                startMediaProjection.launch(projectionManager.createScreenCaptureIntent())
            } catch (ignore: ActivityNotFoundException) {
                IntentAction.CastPermissionsDenied.sendToAppService(this@MainActivity)
                showErrorDialog(
                    R.string.permission_activity_error_title_activity_not_found,
                    R.string.permission_activity_error_activity_not_found
                )
            }
        }


        if (serviceMessage is ServiceMessage.FinishActivity) finishAndRemoveTask()
    }
}