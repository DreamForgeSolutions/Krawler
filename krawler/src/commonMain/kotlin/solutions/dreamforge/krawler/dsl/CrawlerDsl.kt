package solutions.dreamforge.krawler.dsl

import solutions.dreamforge.krawler.engine.CrawlerEngineConfig
import kotlinx.datetime.Clock
import solutions.dreamforge.krawler.domain.model.CrawlPolicy
import solutions.dreamforge.krawler.domain.model.CrawlRequest
import solutions.dreamforge.krawler.domain.model.ExtractionRule
import solutions.dreamforge.krawler.domain.model.ExtractionType
import solutions.dreamforge.krawler.domain.model.PostProcessor
import solutions.dreamforge.krawler.domain.model.Selector

/**
 * DSL marker to prevent scope leakage
 */
@DslMarker
annotation class CrawlerDslMarker

/**
 * Main crawler configuration DSL
 */
@CrawlerDslMarker
class CrawlerConfiguration {
    var name: String = "DreamForge-Crawler"
    var maxConcurrency: Int = 50
    var queueCapacity: Int = 10000
    var userAgent: String = "DreamForge-Crawler/1.0"
    
    private val sources = mutableListOf<SourceConfiguration>()
    private val globalExtractionRules = mutableListOf<ExtractionRule>()
    private var globalCrawlPolicy = CrawlPolicy()
    
    /**
     * Configure global crawl policy
     */
    fun policy(block: CrawlPolicyBuilder.() -> Unit) {
        val builder = CrawlPolicyBuilder()
        builder.block()
        globalCrawlPolicy = builder.build()
    }
    
    /**
     * Configure global extraction rules
     */
    fun extract(block: ExtractionRulesBuilder.() -> Unit) {
        val builder = ExtractionRulesBuilder()
        builder.block()
        globalExtractionRules.addAll(builder.build())
    }
    
    /**
     * Configure a crawling source
     */
    fun source(name: String, block: SourceBuilder.() -> Unit) {
        val builder = SourceBuilder(name, globalCrawlPolicy, globalExtractionRules.toList())
        builder.block()
        sources.add(builder.build())
    }
    
    internal fun build(): List<CrawlRequest> {
        return sources.flatMap { sourceConfig ->
            sourceConfig.urls.map { url ->
                CrawlRequest(
                    id = generateRequestId(),
                    url = url,
                    maxDepth = sourceConfig.maxDepth,
                    extractionRules = sourceConfig.extractionRules,
                    crawlPolicy = sourceConfig.crawlPolicy,
                    priority = sourceConfig.priority,
                    metadata = mapOf("source" to sourceConfig.name),
                    timestamp = Clock.System.now()
                )
            }
        }
    }
    
    internal fun buildEngineConfig(): CrawlerEngineConfig {
        return CrawlerEngineConfig(
            maxConcurrency = maxConcurrency,
            queueCapacity = queueCapacity
        )
    }
    
    private fun generateRequestId(): String {
        return "req_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}

/**
 * Source configuration builder
 */
@CrawlerDslMarker
class SourceBuilder(
    private val sourceName: String,
    private val defaultPolicy: CrawlPolicy,
    private val defaultRules: List<ExtractionRule>
) {
    private val urls = mutableListOf<String>()
    private var maxDepth: Int = 3
    private var priority: CrawlRequest.Priority = CrawlRequest.Priority.NORMAL
    private var crawlPolicy: CrawlPolicy = defaultPolicy
    private val extractionRules = mutableListOf<ExtractionRule>()
    
    init {
        extractionRules.addAll(defaultRules)
    }
    
    /**
     * Add seed URLs
     */
    fun urls(vararg seedUrls: String) {
        urls.addAll(seedUrls)
    }
    
    /**
     * Add seed URLs from list
     */
    fun urls(seedUrls: List<String>) {
        urls.addAll(seedUrls)
    }
    
    /**
     * Set maximum crawl depth
     */
    fun depth(maxDepth: Int) {
        this.maxDepth = maxDepth
    }
    
    /**
     * Set crawl priority
     */
    fun priority(priority: CrawlRequest.Priority) {
        this.priority = priority
    }
    
    /**
     * Configure source-specific crawl policy
     */
    fun policy(block: CrawlPolicyBuilder.() -> Unit) {
        val builder = CrawlPolicyBuilder(crawlPolicy)
        builder.block()
        crawlPolicy = builder.build()
    }
    
    /**
     * Configure source-specific extraction rules
     */
    fun extract(block: ExtractionRulesBuilder.() -> Unit) {
        val builder = ExtractionRulesBuilder()
        builder.block()
        extractionRules.addAll(builder.build())
    }
    
    internal fun build(): SourceConfiguration {
        return SourceConfiguration(
            name = sourceName,
            urls = urls.toList(),
            maxDepth = maxDepth,
            priority = priority,
            crawlPolicy = crawlPolicy,
            extractionRules = extractionRules.toList()
        )
    }
}

/**
 * Crawl policy builder
 */
@CrawlerDslMarker
class CrawlPolicyBuilder(private val base: CrawlPolicy = CrawlPolicy()) {
    var respectRobotsTxt: Boolean = base.respectRobotsTxt
    var followRedirects: Boolean = base.followRedirects
    var maxRetries: Int = base.maxRetries
    var userAgent: String = base.userAgent
    var delayBetweenRequests: Long = base.delayMillis
    var timeout: Long = base.timeoutMillis
    var maxContentLength: Long = base.maxContentLength
    
    private val allowedContentTypes = mutableSetOf<String>()
    private val customHeaders = base.headers.toMutableMap()
    private var contentTypesSet = false
    
    /**
     * Add allowed content types
     */
    fun allowContentTypes(vararg types: String) {
        if (!contentTypesSet) {
            allowedContentTypes.clear()
            contentTypesSet = true
        }
        allowedContentTypes.addAll(types)
    }
    
    /**
     * Add custom headers
     */
    fun headers(block: MutableMap<String, String>.() -> Unit) {
        customHeaders.block()
    }
    
    /**
     * Set delay between requests in milliseconds
     */
    fun delay(milliseconds: Long) {
        delayBetweenRequests = milliseconds
    }
    
    internal fun build(): CrawlPolicy {
        return CrawlPolicy(
            respectRobotsTxt = respectRobotsTxt,
            followRedirects = followRedirects,
            maxRetries = maxRetries,
            userAgent = userAgent,
            delayMillis = delayBetweenRequests,
            timeoutMillis = timeout,
            maxContentLength = maxContentLength,
            allowedContentTypes = if (contentTypesSet) allowedContentTypes.toSet() else base.allowedContentTypes,
            headers = customHeaders.toMap()
        )
    }
}

/**
 * Extraction rules builder
 */
@CrawlerDslMarker
class ExtractionRulesBuilder {
    private val rules = mutableListOf<ExtractionRule>()
    
    /**
     * Extract text using CSS selector
     */
    fun text(name: String, cssSelector: String, block: (ExtractionRuleBuilder.() -> Unit)? = null) {
        val builder = ExtractionRuleBuilder(name, Selector.CssSelector(cssSelector), ExtractionType.TEXT)
        block?.let { builder.it() }
        rules.add(builder.build())
    }
    
    /**
     * Extract HTML using CSS selector
     */
    fun html(name: String, cssSelector: String, block: (ExtractionRuleBuilder.() -> Unit)? = null) {
        val builder = ExtractionRuleBuilder(name, Selector.CssSelector(cssSelector), ExtractionType.HTML)
        block?.let { builder.it() }
        rules.add(builder.build())
    }
    
    /**
     * Extract links using CSS selector
     */
    fun links(name: String, cssSelector: String, block: (ExtractionRuleBuilder.() -> Unit)? = null) {
        val builder = ExtractionRuleBuilder(name, Selector.CssSelector(cssSelector), ExtractionType.LINK)
        block?.let { builder.it() }
        rules.add(builder.build())
    }
    
    /**
     * Extract using regex pattern
     */
    fun regex(name: String, pattern: String, group: Int = 0, block: (ExtractionRuleBuilder.() -> Unit)? = null) {
        val builder = ExtractionRuleBuilder(name, Selector.RegexSelector(pattern, group), ExtractionType.TEXT)
        block?.let { builder.it() }
        rules.add(builder.build())
    }
    
    /**
     * Extract using XPath expression
     */
    fun xpath(name: String, expression: String, block: (ExtractionRuleBuilder.() -> Unit)? = null) {
        val builder = ExtractionRuleBuilder(name, Selector.XPathSelector(expression), ExtractionType.TEXT)
        block?.let { builder.it() }
        rules.add(builder.build())
    }
    
    /**
     * Add a custom extraction rule
     */
    fun rule(block: ExtractionRuleBuilder.() -> Unit) {
        val builder = ExtractionRuleBuilder("", Selector.CssSelector(""), ExtractionType.TEXT)
        builder.block()
        rules.add(builder.build())
    }
    
    internal fun build(): List<ExtractionRule> = rules.toList()
}

/**
 * Individual extraction rule builder
 */
@CrawlerDslMarker
class ExtractionRuleBuilder(
    private var name: String,
    private var selector: Selector,
    private var extractionType: ExtractionType
) {
    var required: Boolean = false
    var multiple: Boolean = false
    
    private val postProcessors = mutableListOf<PostProcessor>()
    
    /**
     * Set rule name
     */
    fun name(name: String) {
        this.name = name
    }
    
    /**
     * Mark rule as required
     */
    fun required() {
        required = true
    }
    
    /**
     * Allow multiple values
     */
    fun multiple() {
        multiple = true
    }
    
    /**
     * Add post-processors
     */
    fun process(block: PostProcessorBuilder.() -> Unit) {
        val builder = PostProcessorBuilder()
        builder.block()
        postProcessors.addAll(builder.build())
    }
    
    internal fun build(): ExtractionRule {
        return ExtractionRule(
            name = name,
            selector = selector,
            extractionType = extractionType,
            postProcessors = postProcessors.toList(),
            required = required,
            multiple = multiple
        )
    }
}

/**
 * Post-processor builder
 */
@CrawlerDslMarker
class PostProcessorBuilder {
    private val processors = mutableListOf<PostProcessor>()
    
    fun trim() {
        processors.add(PostProcessor.Trim)
    }
    
    fun uppercase() {
        processors.add(PostProcessor.UpperCase)
    }
    
    fun lowercase() {
        processors.add(PostProcessor.LowerCase)
    }
    
    fun replace(pattern: String, replacement: String) {
        processors.add(PostProcessor.Replace(pattern, replacement))
    }
    
    fun extract(pattern: String, group: Int = 0) {
        processors.add(PostProcessor.Extract(pattern, group))
    }
    
    fun substring(start: Int, end: Int? = null) {
        processors.add(PostProcessor.Substring(start, end))
    }
    
    fun custom(processorId: String, config: Map<String, String> = emptyMap()) {
        processors.add(PostProcessor.CustomProcessor(processorId, config))
    }
    
    internal fun build(): List<PostProcessor> = processors.toList()
}

/**
 * Internal data classes
 */
internal data class SourceConfiguration(
    val name: String,
    val urls: List<String>,
    val maxDepth: Int,
    val priority: CrawlRequest.Priority,
    val crawlPolicy: CrawlPolicy,
    val extractionRules: List<ExtractionRule>
)

/**
 * Main DSL entry point
 */
fun crawler(block: CrawlerConfiguration.() -> Unit): CrawlerConfiguration {
    val config = CrawlerConfiguration()
    config.block()
    return config
}