package com.d4rk.qrcodescanner.extension

import java.util.*

val Locale?.isRussian: Boolean
    get() = this?.language == "ru"