package http

import java.net.HttpURLConnection


fun request(init: Request.() -> Unit): Response {

    val request = Request()
    request.init()
    return request.send()
}


data class Response(
    val code: Int,
    val body: ByteArray
)

class Request {
    var method = "GET"

    private lateinit var url: String
    fun uri(domain: String, init: URL.() -> Unit) {
        val url = URL(domain);
        url.init();
        this.url = url.toString()
    }

    fun send(): Response {
        val conn = java.net.URL(url).openConnection() as HttpURLConnection
        val input = conn.inputStream
        val buffer = ByteArray(2048)

        val body = buildList<Byte> {
            while (input.read(buffer) != -1) {
                this.addAll(buffer.toList())
            }
        }.toByteArray()

        return Response(conn.responseCode, body)
    }
}


class URL(private val baseUrl: String) {

    val sb = StringBuilder(baseUrl)

    private val params: MutableList<Pair<String, Any>> = mutableListOf()

    fun query(key: String, value: Any) = params.add(Pair(key, value))

    override fun toString(): String {

        if (params.size > 0) {
            sb.append('?')
            sb.append(params[0].first)
            sb.append('=')
            sb.append(params[0].second)
        }

        if (params.size > 1) {
            for (i in 1..<params.size) {
                sb.append('&')
                sb.append(params[i].first)
                sb.append('=')
                sb.append(params[i].second)
            }
        }

        return sb.toString()
    }

}
