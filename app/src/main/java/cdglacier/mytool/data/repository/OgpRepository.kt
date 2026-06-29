package cdglacier.mytool.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
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
        val html = runCatching { downloadHtml(url) }.getOrNull() ?: return@withContext null
        val ogp = Ogp(
            title = extractMeta(html, "og:title") ?: extractTitle(html),
            imageUrl = extractMeta(html, "og:image"),
        )
        cache[url] = ogp
        ogp
    }

    private fun downloadHtml(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 MyTool/1.0")
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
        }
        try {
            conn.inputStream.use { input ->
                val buffer = ByteArray(MAX_BYTES)
                var total = 0
                while (total < MAX_BYTES) {
                    val read = input.read(buffer, total, MAX_BYTES - total)
                    if (read <= 0) break
                    total += read
                }
                return String(buffer, 0, total, Charsets.UTF_8)
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun extractMeta(html: String, property: String): String? {
        val patterns = listOf(
            Regex("""<meta[^>]+property=["']${Regex.escape(property)}["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]*property=["']${Regex.escape(property)}["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+name=["']${Regex.escape(property)}["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1)?.let(::decodeEntities) }
    }

    private fun extractTitle(html: String): String? =
        Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.let(::decodeEntities)

    private fun decodeEntities(s: String): String =
        s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")

    companion object {
        private const val MAX_BYTES = 64 * 1024
    }
}
