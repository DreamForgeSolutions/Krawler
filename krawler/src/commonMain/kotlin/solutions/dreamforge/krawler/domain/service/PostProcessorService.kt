package solutions.dreamforge.krawler.domain.service

import solutions.dreamforge.krawler.domain.model.PostProcessor

/**
 * Service for post-processing extracted values
 */
interface PostProcessorService {
    /**
     * Process a value through a chain of processors
     */
    suspend fun process(value: String, processors: List<PostProcessor>): String
    
    /**
     * Register a custom processor
     */
    fun registerCustomProcessor(id: String, processor: (String, Map<String, String>) -> String)
}
