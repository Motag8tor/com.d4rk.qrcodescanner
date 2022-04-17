package my.qrcode.scanner.model
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import my.qrcode.scanner.model.schema.BarcodeSchema
import my.qrcode.scanner.usecase.BarcodeDatabaseTypeConverter
import com.google.zxing.BarcodeFormat
import java.io.Serializable
@Entity(tableName = "codes")
@TypeConverters(BarcodeDatabaseTypeConverter::class)
data class Barcode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String? = null,
    val text: String,
    val rawBytes: ByteArray, // Add raw bytes to the Barcode object
    val formattedText: String,
    val format: BarcodeFormat,
    val schema: BarcodeSchema,
    val date: Long,
    val isGenerated: Boolean = false,
    val isFavorite: Boolean = false,
    val errorCorrectionLevel: String? = null,
    val country: String? = null
) : Serializable