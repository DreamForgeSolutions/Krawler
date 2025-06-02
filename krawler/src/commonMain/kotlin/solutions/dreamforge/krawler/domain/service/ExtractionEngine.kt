package solutions.dreamforge.krawler.domain.service

import solutions.dreamforge.krawler.domain.model.ExtractedValue
import solutions.dreamforge.krawler.domain.model.ExtractionRule

/**
 * extraction engine interface
 */
interface ExtractionEngine {
    
    /**
     * Extract data from HTML content using extraction rules
     */
    suspend fun extractData(
        content: String,
        contentType: String,
        rules: List<ExtractionRule>,
        baseUrl: String
    ): Map<String, ExtractedValue>
    
    /**
     * Extract links from HTML content
     */
    suspend fun extractLinks(content: String, baseUrl: String): Set<String>
    
    /**
     * Extract images from HTML content
     */
    suspend fun extractImages(content: String, baseUrl: String): Set<String>
    
    /**
     * Extract metadata from HTML content
     */
    suspend fun extractMetadata(content: String): Map<String, String>
}
