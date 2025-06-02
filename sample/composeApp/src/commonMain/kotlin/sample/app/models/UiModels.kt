package sample.app.models

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import solutions.dreamforge.krawler.domain.model.CrawlStatus


data class MassiveCrawlerUiState(
    val isRunning: Boolean = false,
    val totalProcessed: Long = 0,
    val successCount: Long = 0,
    val failureCount: Long = 0,
    val currentRps: Double = 0.0,
    val averageRps: Double = 0.0,
    val queueSize: Int = 0,
    val selectedCategories: Set<CrawlCategory> = setOf(
        CrawlCategory.DOCUMENTATION,
        CrawlCategory.TECH_BLOGS,
        CrawlCategory.TUTORIALS
    ),
    val sourceStats: Map<String, Int> = emptyMap(),
    val recentResults: List<CrawlResultUi> = emptyList(),
    val crawlStartTime: Long? = null,
    val engineStats: EngineStats? = null
)

data class EngineStats(
    val queueSize: Long,
    val successRate: Double,
    val averageRequestsPerSecond: Double,
    val activeConnections: Int
)

enum class CrawlCategory(
    val displayName: String,
    val emoji: String,
    val color: Color,
    val sources: List<CrawlSource>
) {
    DOCUMENTATION(
        displayName = "Documentation",
        emoji = "📚",
        color = Color(0xFF4CAF50),
        sources = listOf(
            CrawlSource("Mozilla_MDN", "MDN Web Docs", "https://developer.mozilla.org/en-US/docs/Web/JavaScript"),
            CrawlSource("W3Schools", "W3Schools", "https://www.w3schools.com/html/default.asp"),
            CrawlSource("Kotlin_Docs", "Kotlin Docs", "https://kotlinlang.org/docs/getting-started.html"),
            CrawlSource("Python_Docs", "Python Docs", "https://docs.python.org/3/tutorial/index.html")
        )
    ),
    TECH_BLOGS(
        displayName = "Tech Blogs",
        emoji = "📰",
        color = Color(0xFF2196F3),
        sources = listOf(
            CrawlSource("TechCrunch", "TechCrunch", "https://techcrunch.com"),
            CrawlSource("GitHub_Blog", "GitHub Blog", "https://github.blog"),
            CrawlSource("CSS_Tricks", "CSS-Tricks", "https://css-tricks.com")
        )
    ),
    TUTORIALS(
        displayName = "Tutorials",
        emoji = "🎓",
        color = Color(0xFF9C27B0),
        sources = listOf(
            CrawlSource("DigitalOcean", "DigitalOcean", "https://www.digitalocean.com/community/tutorials")
        )
    ),
    REFERENCE(
        displayName = "Reference",
        emoji = "📖",
        color = Color(0xFFFF9800),
        sources = listOf(
            CrawlSource("Wikipedia", "Wikipedia", "https://en.wikipedia.org/wiki/Programming_language")
        )
    )
}

data class CrawlSource(
    val id: String,
    val name: String,
    val url: String
)

data class CrawlResultUi(
    val id: String = generateUuid(),
    val url: String,
    val source: String,
    val status: CrawlStatus,
    val title: String?,
    val content: String?,
    val error: String?,
    val timestamp: Instant = Clock.System.now(),
    val responseTime: Long = 0
)

// Simple UUID generation for KMP
fun generateUuid(): String {
    val chars = "0123456789abcdef"
    return buildString {
        repeat(8) { append(chars.random()) }
        append('-')
        repeat(4) { append(chars.random()) }
        append('-')
        append('4') // Version 4 UUID
        repeat(3) { append(chars.random()) }
        append('-')
        append(listOf('8', '9', 'a', 'b').random()) // Variant
        repeat(3) { append(chars.random()) }
        append('-')
        repeat(12) { append(chars.random()) }
    }
}
