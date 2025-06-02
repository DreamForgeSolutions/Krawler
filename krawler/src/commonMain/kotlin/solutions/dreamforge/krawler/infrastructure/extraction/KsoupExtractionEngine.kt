package solutions.dreamforge.krawler.infrastructure.extraction

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.select.Elements
import solutions.dreamforge.krawler.domain.model.*
import solutions.dreamforge.krawler.domain.service.ExtractionEngine
import solutions.dreamforge.krawler.domain.service.PostProcessorService
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger
import solutions.dreamforge.krawler.domain.model.ExtractedValue
import solutions.dreamforge.krawler.domain.model.ExtractionRule
import solutions.dreamforge.krawler.domain.model.ExtractionType
import solutions.dreamforge.krawler.domain.model.Selector

private val logger = Logger.withTag("KsoupExtractionEngine")

/**
 * extraction engine using Ksoup for multiplatform support
 */
class KsoupExtractionEngine(
    private val postProcessorService: PostProcessorService
) : ExtractionEngine {
    
    override suspend fun extractData(
        content: String,
        contentType: String,
        rules: List<ExtractionRule>,
        baseUrl: String
    ): Map<String, ExtractedValue> = withContext(Dispatchers.Default) {
        
        try {
            val extractedData = mutableMapOf<String, ExtractedValue>()
            
            when {
                contentType.contains("html", ignoreCase = true) -> {
                    val document = Ksoup.parse(content, baseUrl)
                    extractFromHtml(document, rules, extractedData)
                }
                contentType.contains("json", ignoreCase = true) -> {
                    extractFromJson(content, rules, extractedData)
                }
                else -> {
                    extractFromText(content, rules, extractedData)
                }
            }
            
            extractedData.toMap()
        } catch (e: Exception) {
            logger.e(e) { "Error extracting data from content" }
            emptyMap()
        }
    }
    
    private suspend fun extractFromHtml(
        document: Document,
        rules: List<ExtractionRule>,
        extractedData: MutableMap<String, ExtractedValue>
    ) {
        for (rule in rules) {
            try {
                when (rule.selector) {
                    is Selector.CssSelector -> {
                        val elements = document.select(rule.selector.query)
                        processElements(elements, rule, extractedData)
                    }
                    is Selector.XPathSelector -> {
                        // Ksoup doesn't support XPath, log warning
                        logger.w { "XPath selectors not supported in Ksoup engine: ${rule.selector.expression}" }
                    }
                    is Selector.RegexSelector -> {
                        val text = document.text()
                        processRegexExtraction(text, rule, extractedData)
                    }
                    else -> {
                        logger.w { "Unsupported selector type for HTML: ${rule.selector}" }
                    }
                }
            } catch (e: Exception) {
                logger.d { "Error processing rule ${rule.name}: ${e.message}" }
                if (rule.required) {
                    // For required fields, add empty value instead of throwing
                    extractedData[rule.name] = if (rule.multiple) {
                        ExtractedValue.List(emptyList())
                    } else {
                        ExtractedValue.Text("")
                    }
                }
            }
        }
    }
    
    private suspend fun processElements(
        elements: Elements,
        rule: ExtractionRule,
        extractedData: MutableMap<String, ExtractedValue>
    ) {
        if (elements.isEmpty()) {
            if (rule.required) {
                logger.w { "Required rule ${rule.name} found no elements - providing empty value" }
                extractedData[rule.name] = if (rule.multiple) {
                    ExtractedValue.List(emptyList())
                } else {
                    ExtractedValue.Text("")
                }
            }
            return
        }
        
        val values = elements.mapNotNull { element ->
            try {
                val rawValue = when (rule.extractionType) {
                    ExtractionType.TEXT -> element.text()
                    ExtractionType.HTML -> element.html()
                    ExtractionType.ATTRIBUTE -> element.attr("href") // Default to href, should be configurable
                    ExtractionType.LINK -> element.absUrl("href")
                    ExtractionType.IMAGE_SRC -> element.absUrl("src")
                    else -> element.text()
                }
                
                // Skip empty values
                if (rawValue.isBlank()) return@mapNotNull null
                
                // Apply post-processors
                postProcessorService.process(rawValue, rule.postProcessors)
            } catch (e: Exception) {
                logger.d { "Error processing element for rule ${rule.name}: ${e.message}" }
                null
            }
        }.filter { it.isNotBlank() }
        
        val extractedValue = if (rule.multiple) {
            ExtractedValue.List(values.map { ExtractedValue.Text(it) })
        } else {
            ExtractedValue.Text(values.firstOrNull() ?: "")
        }
        
        extractedData[rule.name] = extractedValue
    }
    
    private suspend fun processRegexExtraction(
        text: String,
        rule: ExtractionRule,
        extractedData: MutableMap<String, ExtractedValue>
    ) {
        val regexSelector = rule.selector as Selector.RegexSelector
        val regex = Regex(regexSelector.pattern)
        val matches = regex.findAll(text)
        
        val values = mutableListOf<String>()
        for (match in matches) {
            val value = if (regexSelector.group > 0 && regexSelector.group <= match.groups.size - 1) {
                match.groups[regexSelector.group]?.value ?: ""
            } else {
                match.value
            }
            
            val processedValue = postProcessorService.process(value, rule.postProcessors)
            values.add(processedValue)
            
            if (!rule.multiple) break
        }
        
        if (values.isEmpty() && rule.required) {
            logger.w { "Required regex rule ${rule.name} found no matches - providing empty value" }
            extractedData[rule.name] = if (rule.multiple) {
                ExtractedValue.List(emptyList())
            } else {
                ExtractedValue.Text("")
            }
            return
        }
        
        val extractedValue = if (rule.multiple) {
            ExtractedValue.List(values.map { ExtractedValue.Text(it) })
        } else {
            ExtractedValue.Text(values.firstOrNull() ?: "")
        }
        
        extractedData[rule.name] = extractedValue
    }
    
    private suspend fun extractFromJson(
        content: String,
        rules: List<ExtractionRule>,
        extractedData: MutableMap<String, ExtractedValue>
    ) {
        try {
            val jsonElement = Json.parseToJsonElement(content)
            
            for (rule in rules) {
                try {
                    when (rule.selector) {
                        is Selector.JsonPathSelector -> {
                            // Simple JsonPath implementation
                            val value = extractJsonPath(jsonElement, rule.selector.path)
                            if (value != null) {
                                val processedValue = processJsonValue(value, rule)
                                extractedData[rule.name] = processedValue
                            } else if (rule.required) {
                                extractedData[rule.name] = if (rule.multiple) {
                                    ExtractedValue.List(emptyList())
                                } else {
                                    ExtractedValue.Text("")
                                }
                            }
                        }
                        is Selector.RegexSelector -> {
                            processRegexExtraction(content, rule, extractedData)
                        }
                        else -> {
                            logger.w { "Unsupported selector type for JSON: ${rule.selector}" }
                        }
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Error extracting JSON data for rule ${rule.name}" }
                    if (rule.required) {
                        extractedData[rule.name] = if (rule.multiple) {
                            ExtractedValue.List(emptyList())
                        } else {
                            ExtractedValue.Text("")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error parsing JSON content" }
        }
    }
    
    private suspend fun extractFromText(
        content: String,
        rules: List<ExtractionRule>,
        extractedData: MutableMap<String, ExtractedValue>
    ) {
        for (rule in rules) {
            if (rule.selector is Selector.RegexSelector) {
                processRegexExtraction(content, rule, extractedData)
            }
        }
    }
    
    override suspend fun extractLinks(content: String, baseUrl: String): Set<String> = withContext(Dispatchers.Default) {
        try {
            val document = Ksoup.parse(content, baseUrl)
            val links = mutableSetOf<String>()
            
            // Extract from <a> tags, but filter out non-content links
            document.select("a[href]").forEach { element ->
                val href = element.absUrl("href")
                if (href.isNotBlank() && isValidContentUrl(href)) {
                    links.add(href)
                }
            }
            
            links
        } catch (e: Exception) {
            logger.e(e) { "Error extracting links" }
            emptySet()
        }
    }
    
    private fun isValidContentUrl(url: String): Boolean {
        return try {
            // Must be HTTP/HTTPS
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return false
            }
            
            // Filter out common non-content URLs
            val lowerUrl = url.lowercase()
            val invalidPatterns = listOf(
                ".js", ".css", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico",
                ".woff", ".woff2", ".ttf", ".eot", ".otf",
                ".pdf", ".zip", ".tar", ".gz", ".mp3", ".mp4", ".avi",
                "/static/", "/assets/", "/images/", "/_static/",
                "javascript:", "mailto:", "#"
            )
            
            !invalidPatterns.any { lowerUrl.contains(it) }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun extractImages(content: String, baseUrl: String): Set<String> = withContext(Dispatchers.Default) {
        try {
            val document = Ksoup.parse(content, baseUrl)
            val images = mutableSetOf<String>()
            
            // Extract from <img> tags
            document.select("img[src]").forEach { element ->
                val src = element.absUrl("src")
                if (src.isNotBlank() && isValidHttpUrl(src)) {
                    images.add(src)
                }
            }
            
            // Extract from srcset attributes
            document.select("img[srcset]").forEach { element ->
                val srcset = element.attr("srcset")
                extractUrlsFromSrcset(srcset, baseUrl).forEach { url ->
                    if (isValidHttpUrl(url)) {
                        images.add(url)
                    }
                }
            }
            
            images
        } catch (e: Exception) {
            logger.e(e) { "Error extracting images" }
            emptySet()
        }
    }
    
    override suspend fun extractMetadata(content: String): Map<String, String> = withContext(Dispatchers.Default) {
        try {
            val document = Ksoup.parse(content)
            val metadata = mutableMapOf<String, String>()
            
            // Basic metadata
            document.title()?.let { metadata["title"] = it }
            
            // Meta tags
            document.select("meta").forEach { meta ->
                val name = meta.attr("name").ifBlank { meta.attr("property") }
                val content = meta.attr("content")
                
                if (name.isNotBlank() && content.isNotBlank()) {
                    metadata[name] = content
                }
            }
            
            // Character encoding
            document.select("meta[charset]").firstOrNull()?.let { meta ->
                metadata["charset"] = meta.attr("charset")
            }
            
            // Language
            document.select("html[lang]").firstOrNull()?.let { html ->
                metadata["language"] = html.attr("lang")
            }
            
            metadata
        } catch (e: Exception) {
            logger.e(e) { "Error extracting metadata" }
            emptyMap()
        }
    }
    
    private fun isValidHttpUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
    
    private fun extractUrlsFromSrcset(srcset: String, baseUrl: String): List<String> {
        return srcset.split(",")
            .map { it.trim().split("\\s+".toRegex()).firstOrNull() ?: "" }
            .filter { it.isNotBlank() }
            .map { url ->
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    // Resolve relative URLs
                    try {
                        val base = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
                        val path = if (url.startsWith("/")) url else "/$url"
                        "$base$path"
                    } catch (e: Exception) {
                        url
                    }
                }
            }
    }
    
    private fun extractJsonPath(element: JsonElement, path: String): JsonElement? {
        val parts = path.trim('$').split('.')
        var current: JsonElement? = element
        
        for (part in parts) {
            current = when {
                current is JsonObject && current.containsKey(part) -> current[part]
                current is JsonArray && part.toIntOrNull() != null -> {
                    val index = part.toInt()
                    if (index in 0 until current.size) current[index] else null
                }
                else -> null
            }
            
            if (current == null) break
        }
        
        return current
    }
    
    private suspend fun processJsonValue(
        jsonElement: JsonElement,
        rule: ExtractionRule
    ): ExtractedValue {
        return when (jsonElement) {
            is JsonPrimitive -> {
                val value = when {
                    jsonElement.isString -> jsonElement.content
                    else -> jsonElement.toString()
                }
                val processed = postProcessorService.process(value, rule.postProcessors)
                ExtractedValue.Text(processed)
            }
            is JsonArray -> {
                if (rule.multiple) {
                    val values = jsonElement.mapNotNull { element ->
                        when (element) {
                            is JsonPrimitive -> {
                                val value = if (element.isString) element.content else element.toString()
                                val processed = postProcessorService.process(value, rule.postProcessors)
                                ExtractedValue.Text(processed)
                            }
                            else -> null
                        }
                    }
                    ExtractedValue.List(values)
                } else {
                    // Take first element for non-multiple rules
                    jsonElement.firstOrNull()?.let { processJsonValue(it, rule) }
                        ?: ExtractedValue.Text("")
                }
            }
            is JsonObject -> {
                ExtractedValue.Text(jsonElement.toString())
            }
            is JsonNull -> ExtractedValue.Null
        }
    }
}
