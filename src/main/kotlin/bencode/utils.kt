package bencode

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
