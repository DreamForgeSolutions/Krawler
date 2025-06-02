package solutions.dreamforge.krawler.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val IoDispatacher: CoroutineDispatcher
    get() = Dispatchers.Default