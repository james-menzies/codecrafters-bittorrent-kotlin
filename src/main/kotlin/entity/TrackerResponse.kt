package entity

data class TrackerResponse(
    val interval: Long,
    val peers: List<String>
) {
    companion object {
        fun fromDecoded(input: Map<*, *>): TrackerResponse {
            return TrackerResponse(
                interval = input["interval"] as Long,
                peers = (input["peers"] as ByteArray).let { peers ->
                    (peers.indices step 6).map { peersIndex ->
                        val ipAddress =
                            peers.sliceArray(IntRange(peersIndex, peersIndex + 3)).map { it.toUByte() }
                                .joinToString(".")
                        val port =
                            ((peers[peersIndex + 4].toUByte().toInt() shl 8) + peers[peersIndex + 5].toUByte()
                                .toInt())
                        "$ipAddress:$port"
                    }


                }
            )
        }
    }
}
