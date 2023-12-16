package torrent.entity

data class TrackerResponse(
    val interval: Long,
    val peers: List<NetworkLocation>
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
                        val portNumber =
                            ((peers[peersIndex + 4].toUByte().toInt() shl 8) + peers[peersIndex + 5].toUByte()
                                .toInt())
                        NetworkLocation(ipAddress, portNumber)
                    }


                }
            )
        }
    }
}

data class NetworkLocation (val ipAddress: String, val portNumber: Int) {
    override fun toString() = "$ipAddress:$portNumber"
}
