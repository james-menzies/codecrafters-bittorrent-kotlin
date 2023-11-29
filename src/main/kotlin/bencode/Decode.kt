package bencode

import java.nio.charset.StandardCharsets


const val digit0: Byte = 0x30
const val digit9: Byte = 0x39
const val d: Byte = 0x64
const val e: Byte = 0x65
const val i: Byte = 0x69
const val l: Byte = 0x6C
const val colon: Byte = 0x3A

typealias DecodeResult = Pair<*, ByteArray>

fun decode(bencodedString: String): DecodeResult = decode(bencodedString.toByteArray())
fun decode(bencoded: ByteArray): DecodeResult {

    return when (bencoded[0]) {
        in digit0..digit9 -> { // is a digit
            val firstColonIndex = bencoded.indexOfFirst { it == colon }
            val length = bencoded.sliceArray(IntRange(0, firstColonIndex - 1)).let {
                String(it, StandardCharsets.UTF_8)
            }.let {
                Integer.parseInt(it)
            }

            val terminal = firstColonIndex + length
            val stringResult =
                bencoded.sliceArray(IntRange(firstColonIndex + 1, terminal)).let { String(it, StandardCharsets.UTF_8) }

            val remainder = bencoded.sliceArray(IntRange(terminal + 1, bencoded.size - 1))
            Pair(stringResult, remainder)
        }

        i -> {
            // integers are a bit weird in this context, because they should be able to store long values.
            // This is just an inconsistency between the bencode format and the Kotlin specification.
            val terminal = bencoded.indexOfFirst { it == e }
            val integerResult =
                bencoded.sliceArray(IntRange(1, terminal - 1)).let { String(it, StandardCharsets.UTF_8) }
                    .let { Integer.parseInt(it) }

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
            Pair(result, remainder)
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

            Pair(result, remainder)
        }

        else -> throw Exception("Unexpected character when parsing payload: ${bencoded[0].toInt().toChar()}")
    }

}
