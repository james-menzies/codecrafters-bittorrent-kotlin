package entity

data class TorrentMetadata(
    val announce: String,
    val info: TorrentMetadataInfo

) {
    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun fromDecoded(input: Any?): TorrentMetadata {
            return TorrentMetadata(
                announce = (input as Map<*, *>)["announce"] as String,
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

    data class TorrentMetadataInfo(
        val length: Long,
        val name: String,
        val pieceLength: Long,
        val pieces: List<String>
    )
}
