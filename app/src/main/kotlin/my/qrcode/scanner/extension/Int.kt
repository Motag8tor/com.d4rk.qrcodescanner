package my.qrcode.scanner.extension
fun Int?.orZero(): Int {
    return this ?: 0
}