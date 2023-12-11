package entity

data class TrackerResponse(
    val interval: Long,
    val peers: List<String>
) {
    companion object {
        fun fromDecoded(input: Any?): TrackerResponse {
            return (input as Map<*, *>).let {
                TrackerResponse(
                    interval = input.get("interval") as Long,
                    peers = (input.get("peers") as ByteArray).let { peers ->
                        (0..<peers.size step 6).map {
                            val ipAddress =
                                peers.sliceArray(IntRange(it, it + 3)).map { it.toUByte() }.joinToString(".")
                            val port = ((peers[it + 4].toUByte().toInt() shl 8) + peers[it + 5].toUByte().toInt())
                            "$ipAddress:$port"
                        }


                    }
                )
            }
        }
    }
}
