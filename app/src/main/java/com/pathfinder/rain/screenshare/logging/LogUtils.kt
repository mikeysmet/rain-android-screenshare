package com.pathfinder.rain.screenshare.logging

import android.content.Context

internal fun Context.getLogFolder(): String = this.cacheDir.absolutePath

