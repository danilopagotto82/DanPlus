package com.nuvio.tv.ui.scout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.scout.CinemaPresenceTicket
import com.nuvio.tv.core.scout.scoutReadPolicy
import com.nuvio.tv.core.scout.scoutSelection
import com.nuvio.tv.core.scout.scoutSelections
import com.nuvio.tv.data.local.ScoutStore
import com.nuvio.tv.domain.model.ScoutAskTarget
import com.nuvio.tv.domain.model.ScoutProfile
import com.nuvio.tv.domain.model.ScoutRating
import com.nuvio.tv.domain.model.ScoutSettings
import com.nuvio.tv.domain.model.ScoutTitle
import com.nuvio.tv.domain.model.scoutShouldAsk
import dagger.hilt.android.lifecycle.HiltViewModel
import com.nuvio.tv.core.tracking.TrackingProviderId
import com.nuvio.tv.core.tracking.TrackingProviderRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ScoutRatingViewModel @Inject constructor(
    private val store: ScoutStore,
    private val resolver: com.nuvio.tv.core.tmdb.TmdbCollectionSourceResolver,
    private val mixSession: com.nuvio.tv.core.player.ScoutMixSession = com.nuvio.tv.core.player.ScoutMixSession(),
    private val trackers: TrackingProviderRegistry,
    private val profileManager: ProfileManager,
    private val cinemaTitleIds: com.nuvio.tv.core.tmdb.TmdbIdStore
) : ViewModel() {
    val profile: StateFlow<ScoutProfile> = store.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScoutProfile())

    /** Whether this profile is signed in to a tracker that takes ratings. */
    val trackerSignedIn: StateFlow<Boolean> = trackers.providers()
        .filter { it.descriptor.id != TrackingProviderId.FLOPPY }
        .map { it.isAuthenticated }
        .let { flows -> if (flows.isEmpty()) flowOf(false) else combine(flows) { states -> states.any { it } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val pendingAsk: StateFlow<ScoutAskTarget?> = store.pendingAsk

    internal val cinemaAttendance = combine(
        store.cinemaPresence.pending, profileManager.scoutSelections(), profileManager.profileHistoryGenerationChanges
    ) { pending, owner, generation ->
        pending?.takeIf { it.owner == owner && it.historyGeneration == generation }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    internal suspend fun enterCinemaPlayback(contentId: String): CinemaPresenceTicket? {
        val owner = profileManager.scoutSelection()
        val generation = profileManager.profileHistoryGenerationChanges.value
        val title = store.cinemaPresence.armedTitle(owner, generation, android.os.SystemClock.elapsedRealtime()) ?: return null
        val verified = when {
            contentId == title -> title
            !contentId.matches(Regex("tt[0-9]+")) -> null
            else -> {
                val imdb = withContext(Dispatchers.IO) { cinemaTitleIds.load()[title.substringAfter(':') + ":movie"] }
                title.takeIf { contentId == imdb }
            }
        }
        if (owner != profileManager.scoutSelection() || generation != profileManager.profileHistoryGenerationChanges.value) return null
        return store.cinemaPresence.enter(owner, generation, contentId, android.os.SystemClock.elapsedRealtime(), verified)
    }

    internal fun finishCinemaPlayback(ticket: CinemaPresenceTicket?, contentId: String?, completed: Boolean) {
        store.cinemaPresence.finish(ticket, profileManager.scoutSelection(), profileManager.profileHistoryGenerationChanges.value, contentId, completed)
    }

    internal fun answerCinemaAttendance(ticket: CinemaPresenceTicket, selected: Set<Int>, guest: Boolean) {
        if (!store.cinemaPresence.consume(ticket, profileManager.scoutSelection(), profileManager.profileHistoryGenerationChanges.value)) return
        if (selected.isEmpty() && !guest) return
        viewModelScope.launch {
            val currentProfiles = profileManager.profiles.value.mapTo(hashSetOf()) { it.id }
            scoutReadPolicy {
                store.recordCinemaAttendance(ticket, selected.intersect(currentProfiles), guest) {
                    ticket.owner == profileManager.scoutSelection() &&
                        ticket.historyGeneration == profileManager.profileHistoryGenerationChanges.value
                }
            }
        }
    }

    fun setRating(title: ScoutTitle, rating: ScoutRating?) {
        viewModelScope.launch { store.setRating(title, rating) }
    }

    fun updateSettings(transform: (ScoutSettings) -> ScoutSettings) {
        viewModelScope.launch { store.update(transform) }
    }

    fun observeShowWatched(profileId: Int) = store.observeShowWatched(profileId)

    fun setShowWatched(profileId: Int, enabled: Boolean) {
        viewModelScope.launch { store.setShowWatched(profileId, enabled) }
    }

    fun offerAsk(target: ScoutAskTarget?) {
        if (target != null && !mixSession.active) store.offerAsk(target)
    }

    fun answerAsk(target: ScoutAskTarget, rating: ScoutRating?) {
        store.clearAsk()
        viewModelScope.launch {
            store.markAsked(target.askKey)
            if (rating != null) store.setRating(target.title, rating)
        }
    }

    suspend fun shouldAsk(target: ScoutAskTarget): Boolean = scoutShouldAsk(store.current(), target)

    fun dropAsk() = store.clearAsk()

    suspend fun genres(): Map<Int, String> = runCatching {
        resolver.genres(com.nuvio.tv.domain.model.TmdbCollectionMediaType.MOVIE) +
            resolver.genres(com.nuvio.tv.domain.model.TmdbCollectionMediaType.TV)
    }.getOrDefault(emptyMap())
}
