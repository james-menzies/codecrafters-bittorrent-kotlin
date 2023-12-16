package tcp

import java.lang.Exception
import java.net.Socket

const val PEER_ID = "00112233445566778899"
private const val HANDSHAKE_HEADER = "BitTorrent protocol"

@OptIn(ExperimentalStdlibApi::class)
fun handshakeWithPeer(socket: Socket, infohash: ByteArray): String? {

    val handshakePayload = ByteArray(68)
    handshakePayload[0] = 19

    for (i in HANDSHAKE_HEADER.indices) {
        handshakePayload[i + 1] = HANDSHAKE_HEADER[i].code.toByte()
    }

    for (i in infohash.indices) {
        handshakePayload[i + 28] = infohash[i]
    }

    for (i in PEER_ID.indices) {
        handshakePayload[i + 48] = PEER_ID[i].code.toByte()
    }

    val inputBuffer = ByteArray(1024)


    try {
        val outputStream = socket.getOutputStream()
        outputStream.write(handshakePayload)
        outputStream.flush()
        val inputStream = socket.getInputStream()
        inputStream.read(inputBuffer)
    } catch (e: Exception) {
        socket.close()
        return null
    }

    val externalPeerId = inputBuffer.sliceArray(IntRange(48, 67)).toHexString()
    return externalPeerId
}
