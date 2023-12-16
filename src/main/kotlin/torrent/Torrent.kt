package torrent

import bencode.decode
import tcp.*
import torrent.entity.TorrentMetadata
import torrent.entity.TrackerResponse
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path


class Torrent private constructor(val metadata: TorrentMetadata) {

    companion object {
        fun loadFromFile(path: Path): Torrent {
            val bencoded = Files.readAllBytes(path)
            val decoded = decode(bencoded)
            return Torrent(TorrentMetadata.fromDecoded(decoded))
        }
    }

    val trackerInfo by lazy {
        val response = request {
            uri(metadata.announce) {
                query("info_hash", metadata.infohashEncoded)
                query("peer_id", PEER_ID)
                query("port", 6881)
                query("uploaded", 0)
                query("downloaded", 0)
                query("left", metadata.info.length)
                query("compact", 1)
            }
        }
        decode(response.body).let { TrackerResponse.fromDecoded(it as Map<*, *>) }
    }

    fun downloadPiece(pieceNumber: Int): String? {
        val networkLocation = trackerInfo.peers[0]
        val socket = Socket(networkLocation.ipAddress, networkLocation.portNumber)
        var lastReceivedMessage: UtpMessage?
        performUtpHandshake(socket, metadata.infohash)

        lastReceivedMessage = receiveUtpMessage(socket)
        sendUtpMessage(socket, UtpMessage(2, ByteArray(0)))
        lastReceivedMessage = receiveUtpMessage(socket)
        sendUtpRequest(socket, pieceNumber, 0, 16 * 1024)
        lastReceivedMessage = receiveUtpMessage(socket)



        socket.close()
        return null
    }
}
