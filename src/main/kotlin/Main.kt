import bencode.decode
import bencode.encode
import com.google.gson.Gson
import tcp.performUtpHandshake
import torrent.Torrent
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


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
            val torrent = Torrent.loadFromFile(Paths.get(args[1]))
            println("Tracker URL: ${torrent.metadata.announce}")
            println("Length: ${torrent.metadata.info.length}")
            println("Info Hash: ${torrent.metadata.infohashHex}")
            println("Piece Length: ${torrent.metadata.info.pieceLength}")
            println("Piece Hashes:")
            torrent.metadata.info.pieces.forEach { println(it) }
        }

        "peers" -> {
            val torrent = Torrent.loadFromFile(Paths.get(args[1]))
            torrent.trackerInfo.peers.forEach { println(it) }
        }
        "handshake" -> {
            val torrent = Torrent.loadFromFile(Paths.get(args[1]))
            val (ipAddress, portNumber) = args[2].split(':')
            val socket = Socket(ipAddress, Integer.parseInt(portNumber))
            val peerId = performUtpHandshake(socket, torrent.metadata.infohash)
            println("Peer ID: $peerId")
            socket.close()
        }
        "download_piece" -> {
            val torrentFilePath = Paths.get(args[3])
            val destinationPath = Paths.get(args[2])
            val pieceNumber = args[4].toInt()
            val torrent = Torrent.loadFromFile(torrentFilePath)
            val piece = torrent.downloadPiece(pieceNumber)

            if (piece == null) {
                println("Could not verify hash for Piece $pieceNumber")
                return
            }

            Files.write(destinationPath, piece)
            println("Piece $pieceNumber downloaded to $destinationPath")
        }
        "download" -> {
            val torrentFilePath = Paths.get(args[3])
            val destinationPath = Paths.get(args[2])
            val torrent = Torrent.loadFromFile(torrentFilePath)

            if (!Files.exists(destinationPath)) Files.createFile(destinationPath)

            for (i in torrent.metadata.info.pieces.indices) {
                var piece: ByteArray? = null
                while (piece == null) {
                    piece = torrent.downloadPiece(i)
                }

                Files.write(destinationPath, piece, StandardOpenOption.APPEND)
            }

            println("Downloaded $torrentFilePath to $destinationPath.")
        }

        else -> println("Unknown command $command")
    }
}
