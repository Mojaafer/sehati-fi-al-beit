package com.example.data.supabase

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/**
 * Shared polling loop behind every live Supabase feed: fetch, emit, exponential backoff on
 * failure up to a minute. A failed poll keeps the last successful emission alive (the flow
 * simply does not emit again), which is what lets screens keep rendering stale rows through
 * network glitches instead of flashing empty states.
 *
 * Failures are logged but never rethrown — the backoff plus the ViewModel-side stale-data
 * messaging is the recovery path. Resetting [failures] to zero after any success keeps a
 * single blip from permanently slowing a feed.
 */
internal fun <T> pollFeed(
    intervalMs: Long,
    fetch: suspend () -> T
): Flow<T> = flow {
    var failures = 0
    while (currentCoroutineContext().isActive) {
        try {
            emit(fetch())
            failures = 0
        } catch (e: Exception) {
            failures++
            android.util.Log.w("SehatiPoll", "poll failed ($failures consecutive)", e)
        }
        val multiplier = 1L shl failures.coerceIn(0, 4)
        delay((intervalMs * multiplier).coerceAtMost(60_000L))
    }
}
