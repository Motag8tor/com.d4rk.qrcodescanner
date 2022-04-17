package my.qrcode.scanner.usecase
import my.qrcode.scanner.model.Barcode
import my.qrcode.scanner.model.schema.Schema
import my.qrcode.scanner.model.schema.App
import my.qrcode.scanner.model.schema.Youtube
import my.qrcode.scanner.model.schema.Geo
import my.qrcode.scanner.model.schema.GoogleMaps
import my.qrcode.scanner.model.schema.Phone
import my.qrcode.scanner.model.schema.Mms
import my.qrcode.scanner.model.schema.Sms
import my.qrcode.scanner.model.schema.Email
import my.qrcode.scanner.model.schema.Other
import my.qrcode.scanner.model.schema.VCard
import my.qrcode.scanner.model.schema.BoardingPass
import my.qrcode.scanner.model.schema.NZCovidTracer
import my.qrcode.scanner.model.schema.Wifi
import my.qrcode.scanner.model.schema.MeCard
import my.qrcode.scanner.model.schema.OtpAuth
import my.qrcode.scanner.model.schema.Cryptocurrency
import my.qrcode.scanner.model.schema.Url
import my.qrcode.scanner.model.schema.Bookmark
import my.qrcode.scanner.model.schema.VEvent
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
object BarcodeParser {
    fun parseResult(result: Result): Barcode {
        val schema = parseSchema(result.barcodeFormat, result.text)
        return Barcode(
            text = result.text,
            rawBytes = result.rawBytes,
            formattedText = schema.toFormattedText(),
            format = result.barcodeFormat,
            schema = schema.schema,
            date = result.timestamp,
            errorCorrectionLevel = result.resultMetadata?.get(ResultMetadataType.ERROR_CORRECTION_LEVEL) as? String,
            country = result.resultMetadata?.get(ResultMetadataType.POSSIBLE_COUNTRY) as? String
        )
    }
    fun parseSchema(format: BarcodeFormat, text: String): Schema {
        if (format != BarcodeFormat.QR_CODE) {
            return BoardingPass.parse(text)
                ?: Other(text)
        }
        return App.parse(text)
            ?: Youtube.parse(text)
            ?: GoogleMaps.parse(text)
            ?: Url.parse(text)
            ?: Phone.parse(text)
            ?: Geo.parse(text)
            ?: Bookmark.parse(text)
            ?: Sms.parse(text)
            ?: Mms.parse(text)
            ?: Wifi.parse(text)
            ?: Email.parse(text)
            ?: Cryptocurrency.parse(text)
            ?: VEvent.parse(text)
            ?: MeCard.parse(text)
            ?: VCard.parse(text)
            ?: OtpAuth.parse(text)
            ?: NZCovidTracer.parse(text)
            ?: BoardingPass.parse(text)
            ?: Other(text)
    }
}