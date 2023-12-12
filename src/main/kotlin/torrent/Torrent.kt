package torrent

import bencode.decode
import http.request
import torrent.entity.TorrentMetadata
import torrent.entity.TrackerResponse
import java.lang.Exception
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path

private const val PEER_ID = "00112233445566778899"
private const val HANDSHAKE_HEADER = "BitTorrent protocol"

class Torrent private constructor(val metadata: TorrentMetadata) {

    companion object {
        fun loadFromFile(path: Path): Torrent {
            val bencoded = Files.readAllBytes(path)
            val decoded = decode(bencoded)
            return Torrent(TorrentMetadata.fromDecoded(decoded))
        }
    }

    private val connections = mutableMapOf<String, Socket>()

    fun closeAllConnections() = connections.values.forEach { it.close() }

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
    fun handshakeWithPeer(ipAddress: String, port: Int): String {

        val handshakePayload = ByteArray(68)
        handshakePayload[0] = 19

        for (i in HANDSHAKE_HEADER.indices) {
            handshakePayload[i + 1] = HANDSHAKE_HEADER[i].code.toByte()
        }

        for (i in metadata.infohash.indices) {
            handshakePayload[i + 28] = metadata.infohash[i]
        }

        for (i in PEER_ID.indices) {
            handshakePayload[i + 48] = PEER_ID[i].code.toByte()
        }

        val inputBuffer = ByteArray(1024)

        val clientSocket = Socket(ipAddress, port)

        try {
            val outputStream = clientSocket.getOutputStream()
            outputStream.write(handshakePayload)
            outputStream.flush()
            val inputStream = clientSocket.getInputStream()

            inputStream.read(inputBuffer)
        } catch (e: Exception) {
            clientSocket.close()
        }

        val externalPeerId =  inputBuffer.sliceArray(IntRange(48, 67)).toHexString()
        connections[externalPeerId] = clientSocket
        return externalPeerId
    }
}
