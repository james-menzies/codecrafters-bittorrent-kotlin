package entity

import bencode.encode
import bencode.urlEncode
import java.security.MessageDigest

@OptIn(ExperimentalStdlibApi::class)
data class TorrentMetadata(
    val announce: String,
    val info: TorrentMetadataInfo,
    val infohash: ByteArray

) {

    companion object {
        fun fromDecoded(input: Any?): TorrentMetadata {
            return TorrentMetadata(
                announce = (input as Map<*, *>)["announce"] as String,
                infohash = (input["info"] as Map<*, *>).let {
                    MessageDigest.getInstance("SHA-1").digest(encode(it))
                },
                info = (input["info"] as Map<*, *>).let {
                    TorrentMetadataInfo(
                        length = it["length"] as Long,
                        name = it["name"] as String,
                        pieceLength = it["piece length"] as Long,
                        pieces = (it["pieces"] as ByteArray).let { pieces ->
                            (pieces.indices step 20).map { pieceIndex ->
                                pieces.sliceArray(IntRange(pieceIndex, pieceIndex + 19)).toHexString()
                            }
                        }

                    )
                }
            )
        }
    }

    val infohashHex by lazy {
        infohash.toHexString()
    }

    val infohashEncoded by lazy {
        urlEncode(infohash)
    }

    data class TorrentMetadataInfo(
        val length: Long,
        val name: String,
        val pieceLength: Long,
        val pieces: List<String>
    )
}
