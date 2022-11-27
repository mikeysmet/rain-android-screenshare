package com.smet.screenshare.service

import com.smet.screenshare.data.model.AppError
import com.smet.screenshare.data.model.HttpClient
import com.smet.screenshare.data.model.NetInterface
import com.smet.screenshare.data.model.TrafficPoint

sealed class ServiceMessage {
    object FinishActivity : ServiceMessage()

    data class ServiceState(
        val isStreaming: Boolean, val isBusy: Boolean, val isWaitingForPermission: Boolean,
        val netInterfaces: List<NetInterface>,
        val appError: AppError?
    ) : ServiceMessage()

    data class Clients(val clients: List<HttpClient>) : ServiceMessage()
    data class TrafficHistory(val trafficHistory: List<TrafficPoint>) : ServiceMessage() {
        override fun toString(): String = javaClass.simpleName
    }

    override fun toString(): String = javaClass.simpleName
}