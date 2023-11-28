import com.google.gson.Gson;
// import com.dampcake.bencode.Bencode; - available if you need it!

val gson = Gson()

fun main(args: Array<String>) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    val command = args[0]
    when (command) {
        "decode" -> {
             val bencodedValue = args[1]
             val decoded = decodeBencode(bencodedValue)
             println(gson.toJson(decoded.first))
             return
        }
        else -> println("Unknown command $command")
    }
}

typealias DecodeResult = Pair<*, String>

fun decodeBencode(bencodedString: String): DecodeResult {
    return when {
        Character.isDigit(bencodedString[0]) -> {
            val firstColonIndex = bencodedString.indexOfFirst { it == ':' }
            val length = Integer.parseInt(bencodedString.substring(0, firstColonIndex))
            val terminal = firstColonIndex + 1 + length
            val stringResult = bencodedString.substring(firstColonIndex + 1, terminal)
            val remainder = bencodedString.substring(terminal)
            Pair(stringResult, remainder )
        }
        bencodedString[0] == 'i' -> {
            val terminal = bencodedString.indexOfFirst { it == 'e' }
            val integerResult = bencodedString.substring(1, terminal).toLong()
            val remainder = bencodedString.substring(terminal + 1)
            Pair(integerResult, remainder)
        }
        bencodedString[0] == 'l' -> {
            val result = mutableListOf<Any?>()
            var remainder: String = bencodedString.substring(1)

            while (remainder[0] != 'e') {
                val listContentsResult = decodeBencode(remainder)
                result.add(listContentsResult.first)
                remainder = listContentsResult.second

            }
            Pair(result, remainder.substring(1))
        }
        else -> TODO("Only strings, numbers and lists are supported at the moment")
    }
}
