package solutions.dreamforge.krawler.http

import solutions.dreamforge.krawler.utils.Logger

/**
 * Custom Ktor logger that integrates with our Logger implementation
 */
class CustomKtorLogger : io.ktor.client.plugins.logging.Logger {
    private val logger = Logger("KtorClient")
    
    override fun log(message: String) {
        logger.debug { message }
    }
}
