package solutions.dreamforge.krawler.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val IoDispatacher: CoroutineDispatcher
    get() = Dispatchers.IO