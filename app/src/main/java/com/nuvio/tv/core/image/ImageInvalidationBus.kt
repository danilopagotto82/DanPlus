package com.nuvio.tv.core.image

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Emits URLs of images that were refreshed by background revalidation.
 * Composables observe this to reload visible posters in-place.
 */
object ImageInvalidationBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val versions = ImageRevalidationVersions()

    fun version(url: String): Int = versions.version(url)

    fun notifyInvalidated(url: String) {
        versions.advance(url)
        _events.tryEmit(url)
    }
}

/** Bound the metadata retained for images that are no longer displayed. */
internal class ImageRevalidationVersions(private val limit: Int = 512) {
    private val versions = LinkedHashMap<String, Int>()
    private var forgottenVersion = 0

    @Synchronized fun version(url: String): Int = versions[url] ?: forgottenVersion

    @Synchronized fun advance(url: String) {
        val next = (versions.remove(url) ?: forgottenVersion) + 1
        versions[url] = next
        if (versions.size > limit) {
            val oldest = versions.entries.iterator()
            forgottenVersion = maxOf(forgottenVersion, oldest.next().value)
            oldest.remove()
        }
    }
}
