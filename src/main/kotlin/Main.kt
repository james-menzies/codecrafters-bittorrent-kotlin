import bencode.decode
import bencode.encode
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import http.uri
import java.net.Inet4Address
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
        val concatenatedPieces: ByteArray
    ) {

        @OptIn(ExperimentalStdlibApi::class)
        @field:JsonIgnore
        val separatedPieces = (0..<concatenatedPieces.size step 20).map {
            concatenatedPieces.sliceArray(IntRange(it, it + 19)).toHexString()
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TrackerResponse(
    val interval: Long,

    @get:JsonProperty("peers")
    val concatenatedPeers: String
) {

    @field:JsonIgnore
    val separatedPeers = (0..2).map {
        Peer(
            concatenatedPeers.substring(it, it + 4).map { it.code.toString() }.joinToString("."),
            ((concatenatedPeers[it + 4].code shl 8) + concatenatedPeers[it + 5].code).toShort()
        )
    }

    data class Peer(
        val ip: String,
        val short: Short
    )
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
                MessageDigest.getInstance("SHA-1").digest(it)
            }.let {
                String(it, StandardCharsets.ISO_8859_1)
            }

            val uri = uri(metadata.announce) {
                query("info_hash", URLEncoder.encode(infohash, StandardCharsets.ISO_8859_1))
                query("peer_id", "00112233445566778899")
                query("port", 6881)
                query("uploaded", 0)
                query("downloaded", 0)
                query("left", metadata.info.length)
                query("compact", 1)

            }


            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString()).body().let{
                decode(it)
            }.let {
                mapper.convertValue(it, TrackerResponse::class.java)
            }

            println()


        }

        else -> println("Unknown command $command")
    }
}

