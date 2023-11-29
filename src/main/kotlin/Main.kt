import bencode.decode
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Files
import java.nio.file.Paths


val mapper = ObjectMapper().registerKotlinModule()

@JsonIgnoreProperties(ignoreUnknown = true)
data class TorrentMetadata(
    val announce: String,
    val info: TorrentMetadataInfo

){
   data class TorrentMetadataInfo(
       val length: Long,
       val name: String,

       @get:JsonProperty("piece length")
       val pieceLength: String,
       val pieces: String
   )
}

fun main(args: Array<String>) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    val command = args[0]
    when (command) {
        "decode" -> {
            val bencodedValue = args[1]
            val decoded = decode(bencodedValue)
            println(mapper.writeValueAsString(decoded.first))
            return
        }

        "info" -> {
            val filepath = Paths.get(args[1])
            val bencoded = Files.readAllBytes(filepath)
            val decoded = decode(bencoded).first
            val metadata = mapper.convertValue(decoded, TorrentMetadata::class.java)
            println("Tracker URL: ${metadata.announce}")
            println("Length: ${metadata.info.length}")
        }

        else -> println("Unknown command $command")
    }
}

