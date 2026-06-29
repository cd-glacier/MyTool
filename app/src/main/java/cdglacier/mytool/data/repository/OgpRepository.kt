package cdglacier.mytool.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class Ogp(
    val title: String?,
    val imageUrl: String?,
)

interface OgpRepository {
    suspend fun fetch(url: String): Ogp?
}

@Singleton
class OgpRepositoryImpl @Inject constructor() : OgpRepository {
    private val cache = ConcurrentHashMap<String, Ogp>()

    override suspend fun fetch(url: String): Ogp? = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }
        val (html, finalUrl) = runCatching { downloadHtml(url) }.getOrNull() ?: return@withContext null
        val rawImage = extractMeta(html, "og:image")
            ?: extractMeta(html, "og:image:url")
            ?: extractMeta(html, "twitter:image")
        val ogp = Ogp(
            title = extractMeta(html, "og:title") ?: extractTitle(html),
            imageUrl = rawImage?.let { resolveUrl(finalUrl, it) },
        )
        cache[url] = ogp
        ogp
    }

    private fun downloadHtml(url: String): Pair<String, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Accept-Encoding", "gzip, identity")
            setRequestProperty("Accept-Language", "ja,en;q=0.8")
        }
        try {
            val rawStream = conn.inputStream
            val stream = if (conn.contentEncoding?.equals("gzip", ignoreCase = true) == true) {
                GZIPInputStream(rawStream)
            } else rawStream
            val bytes = stream.use { s ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                var total = 0
                while (total < MAX_BYTES) {
                    val n = s.read(buf, 0, minOf(buf.size, MAX_BYTES - total))
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    total += n
                }
                out.toByteArray()
            }
            val charset = detectCharset(conn.contentType, bytes)
            return String(bytes, charset) to (conn.url?.toString() ?: url)
        } finally {
            conn.disconnect()
        }
    }

    private fun detectCharset(contentType: String?, bytes: ByteArray): java.nio.charset.Charset {
        val fromHeader = contentType
            ?.substringAfter("charset=", "")
            ?.takeIf { it.isNotBlank() }
            ?.trim('"', ' ', ';')
        val head = String(bytes, 0, minOf(bytes.size, 2048), Charsets.UTF_8)
        val fromMeta = Regex("""charset\s*=\s*["']?([A-Za-z0-9_\-]+)""", RegexOption.IGNORE_CASE)
            .find(head)?.groupValues?.get(1)
        val name = fromHeader ?: fromMeta ?: "UTF-8"
        return runCatching { java.nio.charset.Charset.forName(name) }.getOrDefault(Charsets.UTF_8)
    }

    private fun extractMeta(html: String, property: String): String? {
        val patterns = listOf(
            Regex("""<meta[^>]+(?:property|name)=["']${Regex.escape(property)}["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]*(?:property|name)=["']${Regex.escape(property)}["']""", RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1)?.let(::decodeEntities) }
    }

    private fun extractTitle(html: String): String? =
        Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.let(::decodeEntities)

    private fun resolveUrl(base: String, href: String): String? = runCatching {
        URI(base).resolve(href).toString()
    }.getOrNull()

    private fun decodeEntities(s: String): String =
        s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")

    companion object {
        private const val MAX_BYTES = 128 * 1024
    }
}
