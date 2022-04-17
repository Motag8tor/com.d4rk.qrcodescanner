package my.qrcode.scanner.model.schema
import my.qrcode.scanner.extension.removePrefixIgnoreCase
import my.qrcode.scanner.extension.startsWithAnyIgnoreCase
import my.qrcode.scanner.extension.unsafeLazy
class App(val url: String) : Schema {
    companion object {
        private val PREFIXES = listOf("market://details?id=", "market://search", "http://play.google.com/", "https://play.google.com/")
        fun parse(text: String): App? {
            if (text.startsWithAnyIgnoreCase(PREFIXES).not()) {
                return null
            }
            return App(text)
        }
        fun fromPackage(packageName: String): App {
            return App(PREFIXES[0] + packageName)
        }
    }
    override val schema = BarcodeSchema.APP
    override fun toFormattedText(): String = url
    override fun toBarcodeText(): String = url
    val appPackage by unsafeLazy {
        url.removePrefixIgnoreCase(PREFIXES[0])
    }
}