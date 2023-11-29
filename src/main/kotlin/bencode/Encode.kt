package bencode

import java.nio.charset.StandardCharsets

fun encode(input: Any): ByteArray {
    val result = mutableListOf<Byte>()
    encodeSegment(input, result)
    return result.toByteArray()
}


private fun encodeSegment(input: Any?, partialResult: MutableList<Byte>) {

    when (input) {
        is Map<*, *> -> {
            partialResult.add(d)
            input.forEach { (key, value) ->
                encodeSegment(key, partialResult)
                encodeSegment(value, partialResult)
            }
            partialResult.add(e)
        }
        is List<*> -> {
            partialResult.add(l)
            input.forEach { encodeSegment(it, partialResult) }
            partialResult.add(e)
        }
        is String -> {
            input.toByteArray(StandardCharsets.ISO_8859_1).let {
                val sizeSegment = it.size.toString().toByteArray()

                partialResult.addAll(sizeSegment.toList())
                partialResult.add(colon)
                partialResult.addAll(it.toList())
            }
        }
        is Long -> {
            partialResult.add(i)
            partialResult.addAll(input.toString().toByteArray(StandardCharsets.ISO_8859_1).toList())
            partialResult.add(e)
        }
    }
}
