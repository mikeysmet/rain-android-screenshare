package com.pathfinder.rain.screenshare.service

import com.pathfinder.rain.screenshare.data.model.AppError
import com.pathfinder.rain.screenshare.data.model.HttpClient
import com.pathfinder.rain.screenshare.data.model.NetInterface
import com.pathfinder.rain.screenshare.data.model.TrafficPoint

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