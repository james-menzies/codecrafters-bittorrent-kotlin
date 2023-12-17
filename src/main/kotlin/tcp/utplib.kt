package tcp

import insert
import java.io.DataInputStream
import java.net.Socket

const val PEER_ID = "00112233445566778899"
private const val HANDSHAKE_HEADER = "BitTorrent protocol"

@OptIn(ExperimentalStdlibApi::class)
fun performUtpHandshake(socket: Socket, infohash: ByteArray): String? {

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

data class UtpMessage(val typeId: Int, val payload: ByteArray)

fun sendUtpRequest(socket: Socket, index: Int, begin: Int, length: Int) {

    val payload = ByteArray(12)
    payload.insert(index, 0)
    payload.insert(begin, 4)
    payload.insert(length, 8)

    sendUtpMessage(socket, UtpMessage(6, payload))
}

fun receiveUtpMessage(socket: Socket): UtpMessage? {
    return try {
        val inputStream = socket.getInputStream().let { DataInputStream(it) }
        val size = inputStream.readInt()
        val type = inputStream.readByte().toUByte().toInt()

        val inputBuffer = ByteArray(size - 1) // exclude byte that contains message type
        if(size > 1) {
            inputStream.readFully(inputBuffer)
        }
        UtpMessage(type, inputBuffer)
    } catch (e: Exception) {
        println("Could not receive message")
        null
    }
}

fun sendUtpMessage(socket: Socket, message: UtpMessage) {
    val messagePayload = ByteArray(5 + message.payload.size)

    val sizeSegment = message.payload.size + 1 // the byte containing type ID is included in the message length.
    messagePayload.insert(sizeSegment, 0)

    messagePayload[4] = message.typeId.toByte()
    for (i in message.payload.indices) {
        messagePayload[5 + i] = message.payload[i]
    }

    try {
        val inputStream = socket.getOutputStream()
        inputStream.write(messagePayload)
    } catch (e: Exception) {
        println("Could not send message: $message")
        socket.close()
    }
}
