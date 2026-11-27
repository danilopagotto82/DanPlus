package com.nuvio.tv.core.scout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class CinemaPresenceTicket(
    val owner: ScoutSelection,
    val historyGeneration: Long,
    val contentId: String,
    val companions: List<Pair<Int, String>>,
    val guest: Boolean,
    val expiresAt: Long
)

internal class ScoutCinemaPresence {
    private var armed: CinemaPresenceTicket? = null
    private var watching: CinemaPresenceTicket? = null
    private var playbackId: String? = null
    private val _pending = MutableStateFlow<CinemaPresenceTicket?>(null)
    val pending = _pending.asStateFlow()

    @Synchronized
    fun arm(ticket: CinemaPresenceTicket?) {
        armed = ticket
        watching = null
        playbackId = null
        _pending.value = null
    }

    @Synchronized
    fun cancelUnstarted() {
        armed = null
    }

    @Synchronized
    fun armedTitle(owner: ScoutSelection, generation: Long, now: Long): String? =
        armed?.takeIf { it.owner == owner && it.historyGeneration == generation && now <= it.expiresAt }?.contentId

    @Synchronized
    fun enter(owner: ScoutSelection, historyGeneration: Long, contentId: String?, now: Long, verifiedTitle: String? = contentId): CinemaPresenceTicket? {
        val ticket = armed
        armed = null
        watching = ticket?.takeIf { it.owner == owner && it.historyGeneration == historyGeneration && it.contentId == verifiedTitle && now <= it.expiresAt }
        playbackId = contentId.takeIf { watching != null }
        return watching
    }

    @Synchronized
    fun finish(ticket: CinemaPresenceTicket?, owner: ScoutSelection, historyGeneration: Long, contentId: String?, completed: Boolean) {
        if (ticket == null || watching !== ticket) return
        val samePlayback = playbackId == contentId
        watching = null
        playbackId = null
        if (completed && samePlayback && ticket.owner == owner && ticket.historyGeneration == historyGeneration) {
            _pending.value = ticket
        }
    }

    @Synchronized
    fun consume(ticket: CinemaPresenceTicket, owner: ScoutSelection, historyGeneration: Long): Boolean {
        if (_pending.value !== ticket) return false
        _pending.value = null
        return ticket.owner == owner && ticket.historyGeneration == historyGeneration
    }
}
