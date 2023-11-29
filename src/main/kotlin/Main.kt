import bencode.decode
import bencode.encode
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest


val mapper = ObjectMapper().registerKotlinModule()

@JsonIgnoreProperties(ignoreUnknown = true)
data class TorrentMetadata(
    val announce: String,
    val info: TorrentMetadataInfo

) {
    data class TorrentMetadataInfo(
        val length: Long,
        val name: String,

        @get:JsonProperty("piece length")
        val pieceLength: Long,

        @field:JsonProperty("pieces")
        private val _pieces: String
    ) {

        @OptIn(ExperimentalStdlibApi::class)
        val pieces = (0.._pieces.length - 1 step 20).map {
            _pieces.substring(it, it + 20 ).toByteArray(StandardCharsets.ISO_8859_1).toHexString()
        }

    }
}

@OptIn(ExperimentalStdlibApi::class)
fun generateHash(input: ByteArray): String =
    MessageDigest.getInstance("SHA-1").digest(input).toHexString()


fun main(args: Array<String>) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    val command = args[0]
    when (command) {
        "decode" -> {
            val bencodedValue = args[1]
            val decoded = decode(bencodedValue)
            println(mapper.writeValueAsString(decoded.first))
            return
        }

        "info" -> {
            val filepath = Paths.get(args[1])
            val bencoded = Files.readAllBytes(filepath)
            val decoded = decode(bencoded).first
            val metadata = mapper.convertValue(decoded, TorrentMetadata::class.java)

            val infohash = mapper.convertValue(metadata.info, Map::class.java).let {
                encode(it)
            }.let { generateHash(it) }

            println("Tracker URL: ${metadata.announce}")
            println("Length: ${metadata.info.length}")
            println("Info Hash: $infohash")
            println("Piece Length: ${metadata.info.pieceLength}")
            println("Piece Hashes:")
            metadata.info.pieces.forEach { println(it) }

        }

        else -> println("Unknown command $command")
    }
}

