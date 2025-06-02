package solutions.dreamforge.krawler.infrastructure.extraction

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import solutions.dreamforge.krawler.domain.model.PostProcessor
import solutions.dreamforge.krawler.domain.service.PostProcessorService
import solutions.dreamforge.krawler.utils.Logger
import co.touchlab.stately.collections.ConcurrentMutableMap
import kotlin.invoke
import kotlin.text.get

private val logger = Logger("DefaultPostProcessorService")

/**
 * Default implementation of post-processor service
 */
class DefaultPostProcessorService : PostProcessorService {
    
    private val customProcessors = ConcurrentMutableMap<String, (String, Map<String, String>) -> String>()
    
    init {
        registerBuiltInProcessors()
    }
    
    override suspend fun process(value: String, processors: List<PostProcessor>): String = withContext(Dispatchers.Default) {
        var result = value
        
        for (processor in processors) {
            try {
                result = when (processor) {
                    is PostProcessor.Trim -> result.trim()
                    is PostProcessor.UpperCase -> result.uppercase()
                    is PostProcessor.LowerCase -> result.lowercase()
                    is PostProcessor.Replace -> {
                        result.replace(Regex(processor.pattern), processor.replacement)
                    }
                    is PostProcessor.Extract -> {
                        val regex = Regex(processor.pattern)
                        val match = regex.find(result)
                        if (match != null) {
                            if (processor.group > 0 && processor.group <= match.groupValues.size - 1) {
                                match.groupValues[processor.group]
                            } else {
                                match.value
                            }
                        } else {
                            result
                        }
                    }
                    is PostProcessor.Substring -> {
                        val start = maxOf(0, processor.start)
                        val end = processor.end?.let { minOf(result.length, it) } ?: result.length
                        if (start < end) {
                            result.substring(start, end)
                        } else {
                            result
                        }
                    }
                    is PostProcessor.CustomProcessor -> {
                        val customProcessor = customProcessors[processor.processorId]
                        if (customProcessor != null) {
                            customProcessor(result, processor.config)
                        } else {
                            logger.warn { "Custom processor ${processor.processorId} not found" }
                            result
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error applying post-processor $processor to value: $value" }
                // Continue with current result on error
            }
        }
        
        result
    }
    
    override fun registerCustomProcessor(id: String, processor: (String, Map<String, String>) -> String) {
        customProcessors[id] = processor
        logger.info { "Registered custom post-processor: $id" }
    }
    
    private fun registerBuiltInProcessors() {
        // URL cleanup processor
        registerCustomProcessor("clean_url") { value, config ->
            val keepParams = config["keep_params"]?.split(",")?.map { it.trim() } ?: emptyList()
            cleanUrl(value, keepParams)
        }
        
        // Text normalization processor
        registerCustomProcessor("normalize_text") { value, _ ->
            normalizeText(value)
        }
        
        // Number extraction processor
        registerCustomProcessor("extract_number") { value, config ->
            val pattern = config["pattern"] ?: "\\d+(?:\\.\\d+)?"
            extractNumber(value, pattern)
        }
        
        // HTML tag removal processor
        registerCustomProcessor("strip_html") { value, _ ->
            value.replace(Regex("<[^>]*>"), "")
        }
        
        // Whitespace normalization processor
        registerCustomProcessor("normalize_whitespace") { value, _ ->
            value.replace(Regex("\\s+"), " ").trim()
        }
    }
    
    private fun cleanUrl(url: String, keepParams: List<String>): String {
        return try {
            val questionIndex = url.indexOf('?')
            if (questionIndex == -1 || keepParams.isEmpty()) {
                url.substringBefore('?')
            } else {
                val baseUrl = url.substring(0, questionIndex)
                val queryString = url.substring(questionIndex + 1)
                
                val filteredParams = queryString.split("&")
                    .map { it.split("=", limit = 2) }
                    .filter { parts -> parts.isNotEmpty() && keepParams.contains(parts[0]) }
                    .joinToString("&") { parts -> parts.joinToString("=") }
                
                if (filteredParams.isNotEmpty()) {
                    "$baseUrl?$filteredParams"
                } else {
                    baseUrl
                }
            }
        } catch (e: Exception) {
            url
        }
    }
    
    private fun normalizeText(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[\\u00A0\\u2000-\\u200B\\u2028\\u2029]"), " ")
            .replace(Regex("[\\u201C\\u201D]"), "\"")
            .replace(Regex("[\\u2018\\u2019]"), "'")
            .replace(Regex("\\u2026"), "...")
            .trim()
    }
    
    private fun extractNumber(text: String, pattern: String): String {
        return try {
            val regex = Regex(pattern)
            val match = regex.find(text)
            match?.value ?: text
        } catch (e: Exception) {
            text
        }
    }
}
