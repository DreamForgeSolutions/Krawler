package solutions.dreamforge.krawler.infrastructure.cache

import io.github.reactivecircus.cache4k.Cache
import kotlin.invoke
import kotlin.time.Duration

/**
 * Multiplatform cache wrapper using Cache4K
 */
class MultiplatformCache<K : Any, V : Any>(
    maxSize: Long = 1000,
    expireAfterWrite: Duration? = null,
    expireAfterAccess: Duration? = null
) {
    private val cache: Cache<K, V> = Cache.Builder<K, V>()
        .maximumCacheSize(maxSize)
        .apply {
            expireAfterWrite?.let { expireAfterWrite(it) }
            expireAfterAccess?.let { expireAfterAccess(it) }
        }
        .build()

    fun get(key: K): V? = cache.get(key)

    fun put(key: K, value: V) = cache.put(key, value)

    fun remove(key: K) = cache.invalidate(key)

    fun clear() = cache.invalidateAll()

    fun size(): Long = cache.asMap().size.toLong()

    fun get(key: K, loader: suspend () -> V): suspend () -> V = {
        cache.get(key) { loader() }
    }

    fun asMap(): Map<K, V> = cache.asMap().mapKeys { it.key as K }
}
