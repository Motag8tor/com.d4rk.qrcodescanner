package my.qrcode.scanner.extension
fun Long?.orZero(): Long {
    return this ?: 0L
}