package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance shared date formatter to avoid costly SimpleDateFormat
 * allocations during fast LazyColumn scrolling and item binding.
 * Uses concurrent memoization cache to prevent duplicate Date object allocations.
 */
object AppDateFormatter {
    private val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    private val cache = ConcurrentHashMap<Long, String>()

    @Synchronized
    fun format(date: Date): String = cache.getOrPut(date.time) { format.format(date) }

    @Synchronized
    fun format(timeMs: Long): String = cache.getOrPut(timeMs) { format.format(Date(timeMs)) }

    fun clearCache() {
        cache.clear()
    }
}
