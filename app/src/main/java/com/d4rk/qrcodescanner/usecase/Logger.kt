package com.d4rk.qrcodescanner.usecase

import androidx.multidex.BuildConfig
import io.sentry.core.Sentry

object Logger {
    var isEnabled = BuildConfig.DEBUG

    fun log(error: Throwable) {
        if (isEnabled) {
            Sentry.captureException(error)
        }
    }
}