@file:OptIn(ExperimentalStdlibApi::class)

import bencode.decode
import bencode.encode
import com.google.gson.Gson
import http.request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest


data class TorrentMetadata(
    val announce: String,
    val info: TorrentMetadataInfo

) {
    companion object {
        fun fromDecoded(input: Any?): TorrentMetadata {
            return (input as Map<*, *>).let {
                TorrentMetadata(
                    announce = it.get("announce") as String,
                    info = (input.get("info") as Map<*, *>).let {
                        TorrentMetadataInfo(
                            length = it.get("length") as Long,
                            name = it.get("name") as String,
                            pieceLength = it.get("piece length") as Long,
                            pieces = (it.get("pieces") as ByteArray).let { pieces ->
                                (0..<pieces.size step 20).map {
                                    pieces.sliceArray(IntRange(it, it + 19)).toHexString()
                                }
                            }

                        )
                    }
                )
            }
        }
    }

    data class TorrentMetadataInfo(
        val length: Long,
        val name: String,
        val pieceLength: Long,
        val pieces: List<String>
    )
}

data class TrackerResponse(
    val interval: Long,
    val peers: List<String>
) {
    companion object {
        fun fromDecoded(input: Any?): TrackerResponse {
            return (input as Map<*, *>).let {
                TrackerResponse(
                    interval = input.get("interval") as Long,
                    peers = (input.get("peers") as ByteArray).let { peers ->
                        (0..<peers.size step 6).map {
                            val ipAddress =
                                peers.sliceArray(IntRange(it, it + 3)).map { it.toUByte() }.joinToString(".")
                            val port = ((peers[it + 4].toUByte().toInt() shl 8) + peers[it + 5].toUByte().toInt())
                            "$ipAddress:$port"
                        }


                    }
                )
            }
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
            Gson().toJson(decoded)
            return
        }

        "info" -> {
            val filepath = Paths.get(args[1])
            val bencoded = Files.readAllBytes(filepath)
            val decoded = decode(bencoded).first

            val metadata = TorrentMetadata.fromDecoded(decoded)
            val infohash = encode((decoded as Map<*, *>).get("info") as Any).let {
                MessageDigest.getInstance("SHA-1").digest(it).toHexString()
            }

            println("Tracker URL: ${metadata.announce}")
            println("Length: ${metadata.info.length}")
            println("Info Hash: $infohash")
            println("Piece Length: ${metadata.info.pieceLength}")
            println("Piece Hashes:")
            metadata.info.pieces.forEach { println(it) }
        }

        "peers" -> {
            val filepath = Paths.get(args[1])
            val bencoded = Files.readAllBytes(filepath)
            val decoded = decode(bencoded).first

            val metadata = TorrentMetadata.fromDecoded(decoded)
            val infohash = encode((decoded as Map<*, *>).get("info") as Any).let {
                MessageDigest.getInstance("SHA-1").digest(it).let { String(it, StandardCharsets.ISO_8859_1) }
            }

            val response = request {
                uri(metadata.announce) {
                    query("info_hash", URLEncoder.encode(infohash, "ISO-8859-1"))
                    query("peer_id", "00112233445566778899")
                    query("port", 6881)
                    query("uploaded", 0)
                    query("downloaded", 0)
                    query("left", metadata.info.length)
                    query("compact", 1)
                }
            }

            decode(response.body).first.let { TrackerResponse.fromDecoded(it).peers }.forEach {
                println(it)
            }
        }

        else -> println("Unknown command $command")
    }
}

