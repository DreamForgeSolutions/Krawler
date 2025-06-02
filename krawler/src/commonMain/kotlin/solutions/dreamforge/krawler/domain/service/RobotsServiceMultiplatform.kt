package solutions.dreamforge.krawler.domain.service

/**
 * interface for robots.txt handling
 */
interface RobotsService {
    /**
     * Check if a URL is allowed to be crawled according to robots.txt
     */
    suspend fun isAllowed(url: String, userAgent: String): Boolean
    
    /**
     * Get the crawl delay for a specific host
     */
    suspend fun getCrawlDelay(domain: String, userAgent: String): Long?
    
    /**
     * Get sitemap URLs for the given domain
     */
    suspend fun getSitemaps(domain: String): List<String>
    
    /**
     * Refresh robots.txt cache for domain
     */
    suspend fun refreshRobotsTxt(domain: String)
    
    /**
     * Prefetch and cache robots.txt for a host
     */
    suspend fun prefetchRobots(host: String)
    
    /**
     * Clear cached robots.txt data
     */
    suspend fun clearCache()
}
