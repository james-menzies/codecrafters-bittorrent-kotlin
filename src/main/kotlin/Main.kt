import bencode.decode
import bencode.encode
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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

        @get:JsonProperty("pieces")
        val concatenatedPieces: String
    ) {

        @OptIn(ExperimentalStdlibApi::class)
        @field:JsonIgnore
        val separatedPieces = (0..concatenatedPieces.length - 1 step 20).map {
            concatenatedPieces.substring(it, it + 20).toByteArray(StandardCharsets.ISO_8859_1).toHexString()
        }
    }
}




@OptIn(ExperimentalStdlibApi::class)
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
            }.let {
                MessageDigest.getInstance("SHA-1").digest(it).toHexString()
            }

            println("Tracker URL: ${metadata.announce}")
            println("Length: ${metadata.info.length}")
            println("Info Hash: $infohash")
            println("Piece Length: ${metadata.info.pieceLength}")
            println("Piece Hashes:")
            metadata.info.separatedPieces.forEach { println(it) }
        }

        "peers" -> {
            val filepath = Paths.get(args[1])
            val bencoded = Files.readAllBytes(filepath)
            val decoded = decode(bencoded).first
            val metadata = mapper.convertValue(decoded, TorrentMetadata::class.java)
            val infohash = mapper.convertValue(metadata.info, Map::class.java).let {
                encode(it)
            }.let {
                MessageDigest.getInstance("SHA-1").digest(it).let { String(it, )}
            }

            val uri = "${metadata.announce}?info_hash=${URLEncoder.}&peer_id=0011223344556677889900&port=6881&uploaded=0&downloaded=0left=${metadata.info.length}&compact=1"
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI(uri))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
            println()


        }

        else -> println("Unknown command $command")
    }
}

