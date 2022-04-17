package my.qrcode.scanner.extension
import java.util.Locale
val Locale?.isRussian: Boolean
    get() = this?.language == "ru"