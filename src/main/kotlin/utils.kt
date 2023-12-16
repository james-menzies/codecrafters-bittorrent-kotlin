fun isUtf8String(bytes: ByteArray): Boolean {

    var remainingBytes = 0

    for (i in bytes.indices) {

        val current = bytes[i].toInt()

        if (remainingBytes > 0) {
            if (current shr 6 != 0b10) return false
            remainingBytes--
        } else when {
            current shr 7 == 0 -> {}
            current shr 5 == 0b110 -> remainingBytes = 1
            current shr 4 == 0b1110 -> remainingBytes = 2
            current shr 3 == 0b11110 -> remainingBytes = 3
            else -> return false
        }
    }

    return remainingBytes == 0
}

private val hexArray = "0123456789ABCDEF".toCharArray()

fun urlEncode(bytes: ByteArray): String {

    val charArray = CharArray(bytes.size * 3)
    val hexArray = "0123456789ABCDEF".toCharArray()

    for (i in bytes.indices) {
        val currentValue = bytes[i].toUByte().toInt() and 0xFF
        charArray[i * 3] = '%'
        charArray[i * 3 + 1] = hexArray[currentValue shr 4]
        charArray[i * 3 + 2] = hexArray[currentValue and 0x0F]
    }
    return String(charArray)
}

fun ByteArray.insert(value: Int, offset: Int) {
    for (i in 0..<4) {
        val shifted = value shr (8 * i) and 0xFF
        this[offset + 3 - i] = shifted.toByte()
    }
}
