package com.pathfinder.rain.screenshare.data.model

data class HttpClient(
    val id: Long,
    val clientAddress: String,
    val isSlowConnection: Boolean,
    val isDisconnected: Boolean,
    val isBlocked: Boolean
)