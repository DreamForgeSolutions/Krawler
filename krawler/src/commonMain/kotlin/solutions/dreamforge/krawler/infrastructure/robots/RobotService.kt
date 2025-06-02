package solutions.dreamforge.krawler.infrastructure.robots

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import solutions.dreamforge.krawler.domain.service.RobotsService
import solutions.dreamforge.krawler.http.HttpClient
import solutions.dreamforge.krawler.infrastructure.cache.MultiplatformCache
import solutions.dreamforge.krawler.utils.Logger
import kotlin.time.Duration.Companion.hours

private val logger = Logger("SimpleRobotsService")

/**
 * Simple implementation of RobotsService for multiplatform
 */
class RobotService(
    private val httpClient: HttpClient
) : RobotsService {
    
    private val robotsCache = MultiplatformCache<String, RobotsRules>(
        maxSize = 1000,
        expireAfterWrite = 6.hours
    )
    
    private val mutex = Mutex()
    
    override suspend fun isAllowed(url: String, userAgent: String): Boolean {
        val host = extractHost(url) ?: return true
        val path = extractPath(url)
        
        val rules = getRobotsRules(host)
        
        // Check user-agent specific rules first
        val userAgentRules = rules.userAgentRules[userAgent.lowercase()] 
            ?: rules.userAgentRules["*"]
            ?: return true
        
        // Check disallow rules
        for (disallowedPath in userAgentRules.disallowPaths) {
            if (path.startsWith(disallowedPath)) {
                return false
            }
        }
        
        return true
    }
    
    override suspend fun getCrawlDelay(domain: String, userAgent: String): Long? {
        val rules = getRobotsRules(domain)
        
        val userAgentRules = rules.userAgentRules[userAgent.lowercase()] 
            ?: rules.userAgentRules["*"]
        
        return userAgentRules?.crawlDelay
    }
    
    override suspend fun getSitemaps(domain: String): List<String> {
        val rules = getRobotsRules(domain)
        return rules.sitemaps
    }
    
    override suspend fun refreshRobotsTxt(domain: String) {
        mutex.withLock {
            robotsCache.remove(domain)
        }
        // Pre-fetch the new rules
        getRobotsRules(domain)
    }
    
    override suspend fun prefetchRobots(host: String) {
        getRobotsRules(host)
    }
    
    override suspend fun clearCache() {
        mutex.withLock {
            robotsCache.clear()
        }
    }
    
    private suspend fun getRobotsRules(host: String): RobotsRules {
        // Try to get from cache first
        robotsCache.get(host)?.let { return it }
        
        // Fetch and parse robots.txt
        return mutex.withLock {
            // Double-check after acquiring lock
            robotsCache.get(host)?.let { return it }
            
            val rules = fetchAndParseRobots(host)
            robotsCache.put(host, rules)
            rules
        }
    }
    
    private suspend fun fetchAndParseRobots(host: String): RobotsRules {
        try {
            val robotsUrl = "https://$host/robots.txt"
            val result = httpClient.fetch(robotsUrl)
            
            if (result.isSuccessful && result.body != null) {
                return parseRobotsTxt(result.body)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch robots.txt for $host" }
        }
        
        // Return empty rules if fetch fails (allow all)
        return RobotsRules()
    }
    
    private fun parseRobotsTxt(content: String): RobotsRules {
        val rules = RobotsRules()
        var currentUserAgent: String? = null
        val currentDisallowPaths = mutableListOf<String>()
        var currentCrawlDelay: Long? = null
        
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            
            when {
                trimmedLine.isEmpty() || trimmedLine.startsWith("#") -> {
                    // Skip empty lines and comments
                }
                
                trimmedLine.startsWith("User-agent:", ignoreCase = true) -> {
                    // Save previous user-agent rules if any
                    currentUserAgent?.let { agent ->
                        rules.userAgentRules[agent.lowercase()] = UserAgentRules(
                            disallowPaths = currentDisallowPaths.toList(),
                            crawlDelay = currentCrawlDelay
                        )
                    }
                    
                    // Start new user-agent section
                    currentUserAgent = trimmedLine.substringAfter(":").trim()
                    currentDisallowPaths.clear()
                    currentCrawlDelay = null
                }
                
                trimmedLine.startsWith("Disallow:", ignoreCase = true) -> {
                    val path = trimmedLine.substringAfter(":").trim()
                    if (path.isNotEmpty()) {
                        currentDisallowPaths.add(path)
                    }
                }
                
                trimmedLine.startsWith("Crawl-delay:", ignoreCase = true) -> {
                    val delayStr = trimmedLine.substringAfter(":").trim()
                    currentCrawlDelay = delayStr.toDoubleOrNull()?.times(1000)?.toLong()
                }
                
                trimmedLine.startsWith("Sitemap:", ignoreCase = true) -> {
                    val sitemap = trimmedLine.substringAfter(":").trim()
                    if (sitemap.isNotEmpty()) {
                        rules.sitemaps.add(sitemap)
                    }
                }
            }
        }
        
        // Save last user-agent rules
        currentUserAgent?.let { agent ->
            rules.userAgentRules[agent.lowercase()] = UserAgentRules(
                disallowPaths = currentDisallowPaths.toList(),
                crawlDelay = currentCrawlDelay
            )
        }
        
        return rules
    }
    
    private fun extractHost(url: String): String? {
        return try {
            val withoutProtocol = url.substringAfter("://")
            withoutProtocol.substringBefore("/").substringBefore(":")
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractPath(url: String): String {
        return try {
            val withoutProtocol = url.substringAfter("://")
            "/" + withoutProtocol.substringAfter("/", "")
        } catch (e: Exception) {
            "/"
        }
    }
    
    private data class RobotsRules(
        val userAgentRules: MutableMap<String, UserAgentRules> = mutableMapOf(),
        val sitemaps: MutableList<String> = mutableListOf()
    )
    
    private data class UserAgentRules(
        val disallowPaths: List<String>,
        val crawlDelay: Long? = null
    )
}
