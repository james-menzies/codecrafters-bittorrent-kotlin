import bencode.decode
import com.google.gson.Gson
import entity.TorrentMetadata
import entity.TrackerResponse
import http.request
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


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

        "handshake" -> {
            val torrentMetadata = loadTorrent(Paths.get(args[1]))
            val handshakeHeader = "BitTorrent protocol"
            val peerId = "00112233445566778899"

            val handshakePayload = ByteArray(68)
            handshakePayload[0] = 19

            for (i in handshakeHeader.indices) {
                handshakePayload[i + 1] = handshakeHeader[i].code.toByte()
            }

            for (i in torrentMetadata.infohash.indices) {
                handshakePayload[i + 28] = torrentMetadata.infohash[i]
            }

            for (i in peerId.indices) {
                handshakePayload[i + 48] = peerId[i].code.toByte()
            }

            val inputBuffer = ByteArray(1024)

            val (ipAddress, portNumber) = args[2].split(':')
            val clientSocket = Socket(ipAddress, Integer.parseInt(portNumber))
            val outputStream = clientSocket.getOutputStream()
            outputStream.write(handshakePayload)
            outputStream.flush()
            val inputStream = clientSocket.getInputStream()

            inputStream.read(inputBuffer)

            val serverPeerId = inputBuffer.sliceArray(IntRange(48, 67)).toHexString()
            println("Peer ID: $serverPeerId")
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
