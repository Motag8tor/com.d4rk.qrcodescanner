package my.qrcode.scanner.model
import androidx.room.TypeConverters
import my.qrcode.scanner.usecase.BarcodeDatabaseTypeConverter
import com.google.zxing.BarcodeFormat
@TypeConverters(BarcodeDatabaseTypeConverter::class)
data class ExportBarcode(
    val date: Long,
    val format: BarcodeFormat,
    val text: String
)