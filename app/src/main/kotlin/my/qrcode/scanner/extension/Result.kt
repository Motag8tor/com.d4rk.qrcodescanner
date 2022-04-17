package my.qrcode.scanner.extension
import my.qrcode.scanner.model.Barcode
import com.google.zxing.Result
fun Result.equalTo(barcode: Barcode?): Boolean {
    return barcodeFormat == barcode?.format && text == barcode?.text
}