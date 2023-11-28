import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Files
import java.nio.file.Paths


val mapper = ObjectMapper().registerKotlinModule()

@JsonIgnoreProperties(ignoreUnknown = true)
data class TorrentMetadata(
    val announce: String,
    val info: TorrentMetadataInfo

){
   data class TorrentMetadataInfo(
       val length: Long,
       val name: String,

       @get:JsonProperty("piece length")
       val pieceLength: String,
       val pieces: String
   )
}

fun main(args: Array<String>) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    val command = args[0]
    when (command) {
        "decode" -> {
            val bencodedValue = args[1]
            val decoded = decodeBencode(bencodedValue)
            println(mapper.writeValueAsString(decoded))
            return
        }

        "info" -> {
            val filepath = Paths.get(args[1])
            val data = Files.readAllBytes(filepath)
            val bencodedValue = data.map { Char(it.toInt() and 0xFF) }.joinToString("")

            val decoded = decodeBencode(bencodedValue).first
            val metadata = mapper.convertValue(decoded, TorrentMetadata::class.java)
            println("Tracker URL: ${metadata.announce}")
            println("Length: ${metadata.info.length}")
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
            Pair(stringResult, remainder)
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

        bencodedString[0] == 'd' -> {
            val result = mutableMapOf<String, Any?>()
            var remainder: String = bencodedString.substring(1)

            while (remainder[0] != 'e') {
                val decodedKeyResult = decodeBencode(remainder)
                val decodedValueResult = decodeBencode(decodedKeyResult.second)

                result.put(decodedKeyResult.first.toString(), decodedValueResult.first)
                remainder = decodedValueResult.second
            }

            Pair(result, remainder.substring(1))
        }

        else -> throw Exception("Unexpected character when parsing payload: $bencodedString[0]")
    }
}
