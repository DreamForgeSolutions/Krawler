package solutions.dreamforge.krawler.utils

import co.touchlab.kermit.Logger as KermitLogger

/**
 * logger
 */
class Logger(tag: String) {
    private val logger = KermitLogger.withTag(tag)
    
    fun debug(throwable: Throwable? = null, message: () -> String) {
        if (throwable != null) {
            logger.d(message(), throwable)
        } else {
            logger.d(message())
        }
    }
    
    fun info(throwable: Throwable? = null, message: () -> String) {
        if (throwable != null) {
            logger.i(message(), throwable)
        } else {
            logger.i(message())
        }
    }
    
    fun warn(throwable: Throwable? = null, message: () -> String) {
        if (throwable != null) {
            logger.w(message(), throwable)
        } else {
            logger.w(message())
        }
    }
    
    fun error(throwable: Throwable? = null, message: () -> String) {
        if (throwable != null) {
            logger.e(message(), throwable)
        } else {
            logger.e(message())
        }
    }
}
