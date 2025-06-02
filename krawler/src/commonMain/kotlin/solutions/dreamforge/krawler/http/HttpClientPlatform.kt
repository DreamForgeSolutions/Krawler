package solutions.dreamforge.krawler.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration

/**
 * Platform-specific HTTP client engine factory
 */
expect fun httpClientEngine(): HttpClientEngine

/**
 * Common HTTP client configuration for all platforms
 */
fun createHttpClient(connectTimeout: Duration? = null, readTimeout: Duration? = null): HttpClient =
    HttpClient(httpClientEngine()) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = connectTimeout?.inWholeMilliseconds ?: 30_000
            socketTimeoutMillis = readTimeout?.inWholeMilliseconds ?: 30_000
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }

        install(Logging) {
            logger = CustomKtorLogger()
            level = LogLevel.HEADERS
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        followRedirects = true

        defaultRequest {
            // Default headers for all requests
            headers {
                append("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                append("Accept-Language", "en-US,en;q=0.9")
                append("Accept-Encoding", "gzip, deflate")
                append("Cache-Control", "no-cache")
                append("Pragma", "no-cache")
            }
        }
    }