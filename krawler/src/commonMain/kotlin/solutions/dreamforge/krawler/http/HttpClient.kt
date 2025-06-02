package solutions.dreamforge.krawler.http

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import solutions.dreamforge.krawler.utils.Logger
import kotlin.time.Duration

private val logger = Logger("HttpClient")

/**
 * HTTP client using Ktor with common configuration
 */
class HttpClient(
    private val userAgent: String = "WebCrawler/1.0",
    connectTimeout: Duration,
    readTimeout: Duration,
) {

     private val ktorClient = createHttpClient(connectTimeout, readTimeout)
    
    suspend fun fetch(url: String): FetchResult = withContext(Dispatchers.Default) {
        try {
            val response = ktorClient.get(url) {
                headers {
                    append(HttpHeaders.UserAgent, userAgent)
                }
            }
            
            val body = response.bodyAsText()
            
            // Normalize headers to handle case sensitivity
            val normalizedHeaders = mutableMapOf<String, List<String>>()
            response.headers.forEach { name, values ->
                normalizedHeaders[name.lowercase()] = values
            }
            
            logger.debug { "Fetched $url - Status: ${response.status.value}, Headers: ${normalizedHeaders.keys}" }
            
            FetchResult(
                url = url,
                statusCode = response.status.value,
                body = body,
                headers = normalizedHeaders,
                isSuccessful = response.status.isSuccess()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch $url: ${e.message}" }
            FetchResult(
                url = url,
                error = e.message
            )
        }
    }
}

data class FetchResult(
    val url: String,
    val statusCode: Int? = null,
    val body: String? = null,
    val headers: Map<String, List<String>> = emptyMap(),
    val isSuccessful: Boolean = false,
    val error: String? = null
)
