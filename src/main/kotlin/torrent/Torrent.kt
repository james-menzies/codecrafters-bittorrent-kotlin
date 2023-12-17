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

        // Calculate Piece length
        val beginningIndex = pieceNumber * metadata.info.pieceLength
        val remainingBytes = metadata.info.length - beginningIndex
        if (remainingBytes <= 0) return null // invalid piece number


        val pieceLength = remainingBytes.coerceAtMost(metadata.info.pieceLength)


        val networkLocation = trackerInfo.peers[0]
        val socket = Socket(networkLocation.ipAddress, networkLocation.portNumber)
        socket.use {
            performUtpHandshake(it, metadata.infohash)

            receiveUtpMessage(it) // This will be the bitfield message, we can throw this away because we know the peer has all of the bits.
            sendUtpMessage(it, UtpMessage(2, ByteArray(0)))
            receiveUtpMessage(it) // We know that the next message will be the unchoke.

            val packetSize: Long = 16 * 1024 // 16 kB
            val result = mutableListOf<Byte>()
            for (begin in 0..<pieceLength step packetSize) {
                val length = if (begin + packetSize > pieceLength) {
                    pieceLength - begin
                } else packetSize

                sendUtpRequest(it, pieceNumber, begin.toInt(), length.toInt())
                receiveUtpMessage(it)?.payload?.let { payload ->
                    // We can ignore the first 8 bytes because they provide the piece number and index of insertion.
                    // Since we're going bit-by-bit, we already know this.
                    payload.sliceArray(IntRange(8, payload.size - 1)).forEach { byte -> result.add(byte) }
                }
            }


            val correctHash = metadata.info.pieces[pieceNumber]
            val unverifiedHash = MessageDigest.getInstance("SHA-1").digest(result.toByteArray()).toHexString()

            return if (correctHash == unverifiedHash) result.toByteArray() else null
        }
    }
}
