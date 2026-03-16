package cdglacier.mytool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class UrlContent(val title: String?, val description: String?)

object UrlFetcher {
    suspend fun fetch(url: String): UrlContent = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val title = extractOgTitle(html) ?: extractTitle(html)
            val description = extractOgDescription(html)
            UrlContent(title, description)
        } catch (e: Exception) {
            UrlContent(null, null)
        }
    }

    private fun extractTitle(html: String): String? =
        Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    private fun extractOgTitle(html: String): String? =
        Regex("""<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?: Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:title["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    private fun extractOgDescription(html: String): String? =
        Regex("""<meta[^>]+property=["']og:description["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?: Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:description["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
}
