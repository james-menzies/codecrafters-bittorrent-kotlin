package torrent

import bencode.decode
import tcp.PEER_ID
import tcp.request
import torrent.entity.TorrentMetadata
import torrent.entity.TrackerResponse
import java.lang.Exception
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


    fun downloadPieceFromPeer(peerId: String): String? {
        var lastMessage = receiveMessage(peerId)
        sendMessage(peerId, Message(2, ByteArray(0)))
        lastMessage = receiveMessage(peerId)
        println(lastMessage)
        return null
    }

    data class Message(val typeId: Int, val payload: ByteArray)

    private fun receiveMessage(peerId: String): Message? {
        val socket = connections[peerId]
        if (socket == null) return null

        val inputBuffer = ByteArray(1024)
        try {
            val inputStream = socket.getInputStream()
            inputStream.read(inputBuffer)
        } catch (e: Exception) {
            socket.close()
            connections.remove(peerId)
            return null
        }

        return Message(inputBuffer[4].toInt(), ByteArray(0))
    }

    private fun sendMessage(peerId: String, message: Message) {

        val messagePayload = ByteArray(5 + message.payload.size)

        val sizeSegment = message.payload.size + 1 // the byte containing type ID is included in the message length.

        for (i in 0..<4) {
            val shifted = sizeSegment shr (8 * i) and 0xFF
            messagePayload[3 - i] = shifted.toByte()
        }
        messagePayload[4] = message.typeId.toByte()
        for (i in message.payload.indices) {
            messagePayload[5 + i] = message.payload[i]
        }

        val socket = connections[peerId]
        if (socket == null) return

        try {
            val inputStream = socket.getOutputStream()
            inputStream.write(messagePayload)
        } catch (e: Exception) {
            socket.close()
            connections.remove(peerId)
        }

    }
}
