package my.qrcode.scanner.extension
fun Boolean?.orFalse(): Boolean {
    return this ?: false
}