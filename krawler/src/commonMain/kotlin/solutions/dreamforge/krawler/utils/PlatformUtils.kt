package solutions.dreamforge.krawler.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Clock

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

expect val IoDispatacher : CoroutineDispatcher

