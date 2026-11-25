package com.nuvio.tv.data.mdblist

import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.tracking.TrackingCapability
import com.nuvio.tv.core.tracking.TrackingHistoryItem
import com.nuvio.tv.core.tracking.TrackingHistoryWriter
import com.nuvio.tv.core.tracking.TrackingMediaReference
import com.nuvio.tv.core.tracking.TrackingMutationResult
import com.nuvio.tv.core.tracking.TrackingProvider
import com.nuvio.tv.core.tracking.TrackingProviderDescriptor
import com.nuvio.tv.core.tracking.TrackingProviderId
import com.nuvio.tv.core.tracking.TrackingScrobbleAccount
import com.nuvio.tv.core.tracking.TrackingScrobbleAction
import com.nuvio.tv.core.tracking.TrackingScrobbleEvent
import com.nuvio.tv.core.tracking.TrackingScrobbleOutcome
import com.nuvio.tv.core.tracking.TrackingScrobbler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Singleton
class MdbListTrackingProvider @Inject constructor(
    auth: MdbListAuthStore,
    profiles: ProfileManager,
    override val scrobbler: MdbListTrackingScrobbler
) : TrackingProvider {
    override val descriptor = TrackingProviderDescriptor(
        TrackingProviderId.MDBLIST,
        "MDBList",
        setOf(TrackingCapability.AUTHENTICATION, TrackingCapability.LIBRARY_READ, TrackingCapability.LIBRARY_WRITE, TrackingCapability.WATCHED_READ,
            TrackingCapability.WATCHED_WRITE, TrackingCapability.PROGRESS_READ,
            TrackingCapability.PROGRESS_WRITE, TrackingCapability.SCROBBLE)
    )
    override val isAuthenticated = combine(auth.state, profiles.activeProfileId) { authorization, profileId ->
        authorization.isAuthenticated && authorization.scope.profileId == profileId
    }.stateIn(
        CoroutineScope(SupervisorJob() + Dispatchers.IO), SharingStarted.Eagerly,
        auth.state.value.isAuthenticated && auth.scope().profileId == profiles.activeProfileId.value
    )
}

@Singleton
class MdbListTrackingScrobbler @Inject constructor(
    private val auth: MdbListAuthStore,
    private val sync: MdbListSyncRepository,
    private val service: MdbListScrobbleService,
    private val episodeMappings: MdbListEpisodeMappingService
) : TrackingScrobbler {
    override val providerId = TrackingProviderId.MDBLIST
    override val completesOnPause = true

    override suspend fun currentScrobbleAccount(): TrackingScrobbleAccount? =
        auth.state.value.takeIf { it.isAuthenticated }?.scope?.let {
            TrackingScrobbleAccount(it.profileId, it.generation)
        }

    override suspend fun scrobble(
        action: TrackingScrobbleAction,
        event: TrackingScrobbleEvent
    ): TrackingScrobbleOutcome {
        val authorization = auth.state.value
        if (!authorization.isAuthenticated) return TrackingScrobbleOutcome.UNAVAILABLE
        val owner = authorization.scope
        if (!event.belongsTo(TrackingScrobbleAccount(owner.profileId, owner.generation))) {
            return TrackingScrobbleOutcome.OWNER_CHANGED
        }
        return try {
            sync.ensureLoaded()
            val media = episodeMappings.toMdbListNumbering(event.media, sync.currentProjection().mediaIndex)
            val outcome = service.scrobble(owner, action, event.copy(media = media))
            if (auth.scope() == owner) outcome else TrackingScrobbleOutcome.OWNER_CHANGED
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            if (auth.scope() != owner) TrackingScrobbleOutcome.OWNER_CHANGED else throw error
        }
    }
}

@Singleton
class MdbListTrackingHistoryWriter @Inject constructor(
    private val sync: MdbListSyncRepository,
    private val service: MdbListHistoryService,
    private val episodeMappings: MdbListEpisodeMappingService
) : TrackingHistoryWriter {
    override val providerId = TrackingProviderId.MDBLIST

    override suspend fun addToHistory(profileId: Int, items: Collection<TrackingHistoryItem>): TrackingMutationResult {
        val scope = sync.currentScope()
        if (scope.profileId != profileId) return TrackingMutationResult(0)
        // MDBList answers a show-level mark by marking every episode watched, same as Simkl.
        val pushableItems = items.filterNot(TrackingHistoryItem::isWholeSeriesMark)
        if (pushableItems.isEmpty()) return TrackingMutationResult(0)
        return service.add(scope, pushableItems.map { item -> item.copy(media = item.media.inMdbListNumbering()) })
    }

    override suspend fun removeFromHistory(profileId: Int, items: Collection<TrackingMediaReference>): TrackingMutationResult {
        val scope = sync.currentScope()
        if (scope.profileId != profileId) return TrackingMutationResult(0)
        return service.remove(scope, items.map { it.inMdbListNumbering() })
    }

    private suspend fun TrackingMediaReference.inMdbListNumbering(): TrackingMediaReference {
        sync.ensureLoaded()
        return episodeMappings.toMdbListNumbering(this, sync.currentProjection().mediaIndex)
    }
}
