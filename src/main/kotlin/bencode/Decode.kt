package bencode

import java.nio.charset.StandardCharsets

typealias DecodeResult = Pair<*, ByteArray>

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


fun decode(bencoded: String): DecodeResult = decode(bencoded.toByteArray())
fun decode(bencoded: ByteArray): DecodeResult {

    return when (bencoded[0]) {
        in digit0..digit9 -> { // is a digit
            val firstColonIndex = bencoded.indexOfFirst { it == colon }
            val length = bencoded.sliceArray(IntRange(0, firstColonIndex - 1)).let {
                String(it, StandardCharsets.ISO_8859_1)
            }.let {
                Integer.parseInt(it)
            }

            val terminal = firstColonIndex + length
            val stringResult =
                bencoded.sliceArray(IntRange(firstColonIndex + 1, terminal)).let {
                    // We just want to leave this string as a byte array if it cannot be parsed into UTF-8
                    if (isUtf8String(it)) String(it, StandardCharsets.UTF_8) else it
                }

            val remainder = bencoded.sliceArray(IntRange(terminal + 1, bencoded.size - 1))
            Pair(stringResult, remainder)
        }

        i -> {
            // integers are a bit weird in this context, because they should be able to store long values.
            // This is just an inconsistency between the bencode format and the Kotlin specification.
            val terminal = bencoded.indexOfFirst { it == e }
            val integerResult =
                bencoded.sliceArray(IntRange(1, terminal - 1)).let { String(it, StandardCharsets.ISO_8859_1) }.toLong()

            val remainder = bencoded.sliceArray(IntRange(terminal + 1, bencoded.size - 1))
            Pair(integerResult, remainder)
        }

        l -> {
            val result = mutableListOf<Any?>()
            var remainder = bencoded.sliceArray(IntRange(1, bencoded.size - 1))

            while (remainder[0] != e) {

                val listContentsResult = decode(remainder)
                result.add(listContentsResult.first)
                remainder = listContentsResult.second

            }
            Pair(result, remainder.sliceArray(IntRange(1, remainder.size - 1)))
        }

        d -> {
            val result = mutableMapOf<String, Any?>()
            var remainder = bencoded.sliceArray(IntRange(1, bencoded.size - 1))

            while (remainder[0] != e) {
                val decodedKeyResult = decode(remainder)
                val decodedValueResult = decode(decodedKeyResult.second)

                result.put(decodedKeyResult.first.toString(), decodedValueResult.first)
                remainder = decodedValueResult.second
            }

            Pair(result, remainder.sliceArray(IntRange(1, remainder.size - 1)))
        }

        else -> throw Exception("Unexpected character when parsing payload: ${bencoded[0].toInt().toChar()}")
    }

}
