package bencode

import java.nio.charset.StandardCharsets


fun decode(bencoded: String): Any? = decode(bencoded.toByteArray())

fun decode(bencoded: ByteArray): Any? {

    val result = decodeSegment(bencoded)
    if (result.second.isNotEmpty() && result.second[0].toInt() != 0) {
        throw Exception("Could not parse bencoded string. Input has more than one root element.")
    }

    return result.first
}


typealias DecodeResult = Pair<*, ByteArray>
private fun decodeSegment(bencoded: ByteArray): DecodeResult {

    return when (bencoded[0]) {
        in digit0..digit9 -> { // is a digit
            val firstColonIndex = bencoded.indexOfFirst { it == colon }
            val length = String(bencoded.sliceArray(IntRange(0, firstColonIndex - 1)), StandardCharsets.ISO_8859_1).let {
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
                String(bencoded.sliceArray(IntRange(1, terminal - 1)), StandardCharsets.ISO_8859_1).toLong()

            val remainder = bencoded.sliceArray(IntRange(terminal + 1, bencoded.size - 1))
            Pair(integerResult, remainder)
        }

        l -> {
            val result = mutableListOf<Any?>()
            var remainder = bencoded.sliceArray(IntRange(1, bencoded.size - 1))

            while (remainder[0] != e) {

                val listContentsResult = decodeSegment(remainder)
                result.add(listContentsResult.first)
                remainder = listContentsResult.second

            }
            Pair(result, remainder.sliceArray(IntRange(1, remainder.size - 1)))
        }

        d -> {
            val result = mutableMapOf<String, Any?>()
            var remainder = bencoded.sliceArray(IntRange(1, bencoded.size - 1))

            while (remainder[0] != e) {
                val decodedKeyResult = decodeSegment(remainder)
                val decodedValueResult = decodeSegment(decodedKeyResult.second)

                result[decodedKeyResult.first.toString()] = decodedValueResult.first
                remainder = decodedValueResult.second
            }

            Pair(result, remainder.sliceArray(IntRange(1, remainder.size - 1)))
        }

        else -> throw Exception("Unexpected character when parsing payload: ${bencoded[0].toInt().toChar()}")
    }

}
