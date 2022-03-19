package com.d4rk.qrcodescanner.model.schema
import com.d4rk.qrcodescanner.extension.joinToStringNotNullOrBlankWithLineSeparator
import com.d4rk.qrcodescanner.extension.startsWithIgnoreCase
import com.d4rk.qrcodescanner.extension.unsafeLazy
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
class BoardingPass(
    val name: String? = null,
    val pnr: String? = null,
    val from: String? = null,
    val to: String? = null,
    val carrier: String? = null,
    val flight: String? = null,
    val date: String? = null,
    val cabin: String? = null,
    val seat: String? = null,
    val seq: String? = null,
    val ticket: String? = null,
    val selectee: String? = null,
    val ffAirline: String? = null,
    val ffNo: String? = null,
    val fasttrack: String? = null,
    val blob: String? = null,
) : Schema {
    companion object {
        private const val TAG = "QRandBAR"
        private val DATE_FORMATTER by unsafeLazy { SimpleDateFormat("d MMMM", Locale.ENGLISH) }
        fun parse(text: String): BoardingPass? {
            if (text.startsWithIgnoreCase("M1").not()) {
                return null
            }
            if (text[22] != 'E') {
                return null
            }
            val fieldSize: Int = text.slice(58..59).toInt(16)
            if (fieldSize != 0 && text[60] != '>') {
                return null
            }
            if (text[60+fieldSize] != '^') {
                return null
            }
            val name: String = text.slice(2..21).trim()
            val pnr: String = text.slice(23..29).trim()
            val from: String = text.slice(30..32)
            val to: String = text.slice(33..35)
            val carrier: String = text.slice(36..38).trim()
            val flight: String = text.slice(39..43).trim()
            val dateJ: String = text.slice(44..46)
            val cabin: String = text.slice(47..47)
            val seat: String = text.slice(48..51).trim()
            val seq: String = text.slice(52..56).trim()
            val today = Calendar.getInstance()
            today.set(Calendar.DAY_OF_YEAR, dateJ.toInt())
            val date: String = DATE_FORMATTER.format(today.getTime())
            var selectee : String = ""
            var ticket : String = ""
            var ffAirline : String = ""
            var ffNo : String = ""
            var fasttrack: String = ""
            if (fieldSize != 0) {
                val size: Int = text.slice(62..63).toInt(16)
                if (size != 0 && size != 24) {
                    return null
                }
                val size1: Int = text.slice(64+size..65+size).toInt(16)
                if (size1 != 0 && size1 != 41 && size1 != 42) {
                    return null
                } else {
                    ticket = text.slice(66+size..78+size).trim()
                    selectee = text.slice(79+size..79+size)
                    ffAirline = text.slice(84+size..86+size).trim()
                    ffNo = text.slice(87+size..102+size).trim()
                    if (size1 == 42) {
                        fasttrack = text.slice(107+size..107+size)
                    }
                }
            }
            return BoardingPass(name, pnr, from, to, carrier, flight, date,
                cabin, seat, seq, ticket, selectee,
                ffAirline, ffNo, fasttrack,
                text)
        }
    }
    override val schema = BarcodeSchema.BOARDINGPASS
    override fun toFormattedText(): String = listOf(name, pnr, "$from->$to", "$carrier$flight", date, cabin, seat, seq, ticket, selectee, "$ffAirline$ffNo", fasttrack).joinToStringNotNullOrBlankWithLineSeparator()
    override fun toBarcodeText(): String = "$blob"
}