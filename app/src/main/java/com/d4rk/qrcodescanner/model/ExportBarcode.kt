package com.d4rk.qrcodescanner.model

import androidx.room.TypeConverters
import com.d4rk.qrcodescanner.usecase.BarcodeDatabaseTypeConverter
import com.google.zxing.BarcodeFormat

@TypeConverters(BarcodeDatabaseTypeConverter::class)
data class ExportBarcode(
    val date: Long,
    val format: BarcodeFormat,
    val text: String
)