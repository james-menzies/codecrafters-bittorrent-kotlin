package http

import java.net.URI


fun uri(domain: String, init: URL.() -> Unit): URI {

    val url = URL(domain);
    url.init();
    return URI(url.toString())
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
