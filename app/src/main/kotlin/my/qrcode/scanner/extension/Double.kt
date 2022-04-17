package my.qrcode.scanner.extension
fun Double?.orZero(): Double {
    return this ?: 0.0
}