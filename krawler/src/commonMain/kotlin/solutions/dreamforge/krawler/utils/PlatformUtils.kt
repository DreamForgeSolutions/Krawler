package solutions.dreamforge.krawler.utils

import kotlinx.datetime.Clock

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

