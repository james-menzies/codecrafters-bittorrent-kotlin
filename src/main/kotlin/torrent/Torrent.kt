package torrent

import bencode.decode
import tcp.*
import torrent.entity.TorrentMetadata
import torrent.entity.TrackerResponse
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest


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

    @OptIn(ExperimentalStdlibApi::class)
    fun downloadPiece(pieceNumber: Int): ByteArray? {
        val networkLocation = trackerInfo.peers[0]
        val socket = Socket(networkLocation.ipAddress, networkLocation.portNumber)
        socket.use {
            performUtpHandshake(it, metadata.infohash)

            receiveUtpMessage(it)
            sendUtpMessage(it, UtpMessage(2, ByteArray(0)))
            receiveUtpMessage(it)

            val packetSize: Long = 16 * 1024 // 16 kB
            val result = mutableListOf<Byte>()
            var lastReceivedMessage: UtpMessage?
            for (begin in 0..<metadata.info.pieceLength step packetSize) {
                val length = if (begin + packetSize > metadata.info.pieceLength) {
                    metadata.info.pieceLength - begin
                } else packetSize

                sendUtpRequest(it, pieceNumber, begin.toInt(), length.toInt())
                receiveUtpMessage(it)?.payload?.forEach { result.add(it) }
            }


            val correctHash = metadata.info.pieces[pieceNumber]
            val unverifiedHash = MessageDigest.getInstance("SHA-1").digest(result.toByteArray()).toHexString()

            return if (correctHash == unverifiedHash) result.toByteArray() else null
        }
    }
}
