import bencode.decode
import com.google.gson.Gson
import entity.TorrentMetadata
import entity.TrackerResponse
import http.request
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


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
            val torrentMetadata = loadTorrent(Paths.get(args[1]))
            println("Tracker URL: ${torrentMetadata.announce}")
            println("Length: ${torrentMetadata.info.length}")
            println("Info Hash: ${torrentMetadata.infohash}")
            println("Piece Length: ${torrentMetadata.info.pieceLength}")
            println("Piece Hashes:")
            torrentMetadata.info.pieces.forEach { println(it) }
        }

        "peers" -> {
            val torrentMetadata = loadTorrent(Paths.get(args[1]))
            val trackerInfo = loadTrackerInfo(torrentMetadata)
            trackerInfo.peers.forEach { println(it) }
        }

        else -> println("Unknown command $command")
    }
}

fun loadTorrent(path: Path): TorrentMetadata {
    val bencoded = Files.readAllBytes(path)
    val decoded = decode(bencoded)
    return TorrentMetadata.fromDecoded(decoded)
}

fun loadTrackerInfo(torrentMetadata: TorrentMetadata): TrackerResponse {

    val response = request {
        uri(torrentMetadata.announce) {
            query("info_hash", torrentMetadata.infohashEncoded)
            query("peer_id", "00112233445566778899")
            query("port", 6881)
            query("uploaded", 0)
            query("downloaded", 0)
            query("left", torrentMetadata.info.length)
            query("compact", 1)
        }
    }

    return decode(response.body).let { TrackerResponse.fromDecoded(it as Map<*, *>) }
}
