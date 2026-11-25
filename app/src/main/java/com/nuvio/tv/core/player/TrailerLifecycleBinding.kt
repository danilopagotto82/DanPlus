package com.nuvio.tv.core.player

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** Pause synchronously: a background Compose frame may never run. */
internal class TrailerLifecycleBinding(
    private val lifecycle: Lifecycle,
    private val resume: () -> Unit,
    private val pause: () -> Unit,
    private val stopped: () -> Unit = {}
) : AutoCloseable {
    private var active = false
    private var acquiredSinceStop = false
    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> if (!active) {
                active = true
                acquiredSinceStop = true
                resume()
            }
            Lifecycle.Event.ON_PAUSE -> stop()
            Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                stop()
                if (acquiredSinceStop) {
                    acquiredSinceStop = false
                    stopped()
                }
            }
            else -> Unit
        }
    }

    init { lifecycle.addObserver(observer) }

    private fun stop() {
        if (active) {
            active = false
            pause()
        }
    }

    override fun close() {
        lifecycle.removeObserver(observer)
        stop()
    }
}
