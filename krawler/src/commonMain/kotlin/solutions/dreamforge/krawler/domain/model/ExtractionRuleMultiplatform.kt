package solutions.dreamforge.krawler.domain.model

import kotlinx.serialization.Serializable

/**
 * Multiplatform extraction rule model
 */
@Serializable
data class ExtractionRule(
    val name: String,
    val selector: Selector,
    val extractionType: ExtractionType = ExtractionType.TEXT,
    val postProcessors: List<PostProcessor> = emptyList(),
    val required: Boolean = false,
    val multiple: Boolean = false
)

@Serializable
sealed class Selector {
    @Serializable
    data class CssSelector(val query: String) : Selector()
    
    @Serializable
    data class XPathSelector(val expression: String) : Selector()
    
    @Serializable
    data class RegexSelector(val pattern: String, val group: Int = 0) : Selector()
    
    @Serializable
    data class JsonPathSelector(val path: String) : Selector()
}

@Serializable
enum class ExtractionType {
    TEXT,
    HTML,
    ATTRIBUTE,
    LINK,
    IMAGE_SRC,
    JSON
}

@Serializable
sealed class PostProcessor {
    @Serializable
    object Trim : PostProcessor()
    
    @Serializable
    object UpperCase : PostProcessor()
    
    @Serializable
    object LowerCase : PostProcessor()
    
    @Serializable
    data class Replace(val pattern: String, val replacement: String) : PostProcessor()
    
    @Serializable
    data class Extract(val pattern: String, val group: Int = 0) : PostProcessor()
    
    @Serializable
    data class Substring(val start: Int, val end: Int? = null) : PostProcessor()
    
    @Serializable
    data class CustomProcessor(val processorId: String, val config: Map<String, String> = emptyMap()) : PostProcessor()
}

/**
 * Represents extracted values from web pages
 */
@Serializable
sealed class ExtractedValue {
    @Serializable
    data class Text(val value: String) : ExtractedValue()
    
    @Serializable
    data class Number(val value: Double) : ExtractedValue()
    
    @Serializable
    data class Boolean(val value: kotlin.Boolean) : ExtractedValue()
    
    @Serializable
    data class List(val values: kotlin.collections.List<ExtractedValue>) : ExtractedValue()
    
    @Serializable
    data class Map(val values: kotlin.collections.Map<String, ExtractedValue>) : ExtractedValue()
    
    @Serializable
    object Null : ExtractedValue()
}
