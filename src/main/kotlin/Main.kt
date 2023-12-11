@file:OptIn(ExperimentalStdlibApi::class)

import bencode.decode
import bencode.encode
import com.google.gson.Gson
import entity.TorrentMetadata
import entity.TrackerResponse
import http.request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest


@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    when (val command = args[0]) {
        "decode" -> {
            val bencodedValue = args[1]
            val decoded = decode(bencodedValue)
            println(Gson().toJson(decoded))
            return
        }

        "info" -> {
            val filepath = Paths.get(args[1])
            val bencoded = Files.readAllBytes(filepath)
            val decoded = decode(bencoded)

            val metadata = TorrentMetadata.fromDecoded(decoded)
            val infohash = encode((decoded as Map<*, *>)["info"] as Any).let {
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
            val decoded = decode(bencoded)

            val metadata = TorrentMetadata.fromDecoded(decoded)
            val infohash = encode((decoded as Map<*, *>)["info"] as Any).let { encodedInfo ->
                MessageDigest.getInstance("SHA-1").digest(encodedInfo).let { String(it, StandardCharsets.ISO_8859_1) }
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

            decode(response.body).let { TrackerResponse.fromDecoded(it).peers }.forEach {
                println(it)
            }
        }

        else -> println("Unknown command $command")
    }
}

