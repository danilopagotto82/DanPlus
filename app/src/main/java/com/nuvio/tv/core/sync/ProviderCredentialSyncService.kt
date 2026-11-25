package com.nuvio.tv.core.sync

import android.os.SystemClock
import android.util.Log
import com.nuvio.tv.core.auth.AuthManager
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.AnimeSkipSettingsDataStore
import com.nuvio.tv.data.local.DebridSettingsDataStore
import com.nuvio.tv.data.local.MDBListSettingsDataStore
import com.nuvio.tv.data.remote.supabase.SupabaseProviderCredential
import com.nuvio.tv.domain.model.AuthState
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private const val PROVIDER_CREDENTIAL_TAG = "ProviderCredentialSync"
private const val PROVIDER_CREDENTIAL_PUSH_DEBOUNCE_MS = 500L
private const val PROVIDER_CREDENTIAL_FOREGROUND_DELAY_MS = 2500L
private const val PROVIDER_CREDENTIAL_FOREGROUND_MIN_INTERVAL_MS = 60_000L

private data class ProviderCredentialScope(
    val userId: String,
    val profileId: Int
)

private data class ProviderCredentialTarget(val scope: ProviderCredentialScope, val selectionRevision: Long)

private class ProviderCredentialTargetChanged : CancellationException("Provider credential sync target changed")

@Singleton
class ProviderCredentialSyncService @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val authManager: AuthManager,
    private val profileManager: ProfileManager,
    private val debridSettingsDataStore: DebridSettingsDataStore,
    private val mdbListSettingsDataStore: MDBListSettingsDataStore,
    private val animeSkipSettingsDataStore: AnimeSkipSettingsDataStore,
    private val syncClientIdentity: SyncClientIdentity
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val stateLock = Any()
    private val observedSnapshots = mutableMapOf<ProviderCredentialScope, ProviderCredentialSnapshot>()
    private val baselineSnapshots = mutableMapOf<ProviderCredentialScope, ProviderCredentialSnapshot>()
    private val pendingScopes = mutableSetOf<ProviderCredentialScope>()
    private var foregroundPullJob: Job? = null
    private var lastForegroundPullAtMs: Long = 0L

    init {
        observeLocalCredentials()
        observeAuthState()
    }

    private suspend fun <T> withJwtRefreshRetry(target: ProviderCredentialTarget, block: suspend (Postgrest) -> T): T {
        suspend fun request(): T {
            requireCurrentTarget(target)
            val session = auth.currentSessionOrNull()?.takeIf { it.user?.id == target.scope.userId }
                ?: throw ProviderCredentialTargetChanged()
            // A request-scoped plugin reuses the existing HTTP client and cannot resolve
            // a different account's token after the payload has already been prepared.
            val config = Postgrest.createConfig {
                defaultSchema = postgrest.config.defaultSchema
                customUrl = postgrest.config.customUrl
                propertyConversionMethod = postgrest.config.propertyConversionMethod
                serializer = postgrest.serializer
                jwtToken = session.accessToken
            }
            val result = block(Postgrest.create(postgrest.supabaseClient, config))
            requireCurrentTarget(target)
            return result
        }
        return try {
            request()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            requireCurrentTarget(target)
            if (!authManager.refreshSessionIfJwtExpired(error)) throw error
            request()
        }
    }

    suspend fun syncFromRemote(
        profileId: Int = profileManager.activeProfileId.value
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val target = currentTarget(profileId) ?: return@withContext Result.success(false)
        syncMutex.withLock {
            try {
                requireCurrentTarget(target)
                val credentialScope = target.scope
                val localSnapshot = currentSnapshot(profileId)
                requireCurrentTarget(target)
                val shouldPush = synchronized(stateLock) {
                    val baseline = baselineSnapshots.getOrPut(credentialScope) {
                        observedSnapshots[credentialScope] ?: localSnapshot
                    }
                    credentialScope in pendingScopes || baseline != localSnapshot
                }
                if (shouldPush) {
                    pushSnapshot(localSnapshot, target)
                    synchronized(stateLock) {
                        pendingScopes.remove(credentialScope)
                        baselineSnapshots[credentialScope] = localSnapshot
                    }
                }

                val rows = pullRows(target)
                if (shouldSeedProviderCredentials(localSnapshot, rows)) {
                    seedSnapshot(localSnapshot, target)
                }
                requireCurrentTarget(target)
                val remoteSnapshot = localSnapshot.mergeRemote(rows)
                val applied = remoteSnapshot != localSnapshot
                if (applied) {
                    applySnapshot(remoteSnapshot, target)
                }
                requireCurrentTarget(target)
                synchronized(stateLock) {
                    observedSnapshots[credentialScope] = remoteSnapshot
                    baselineSnapshots[credentialScope] = remoteSnapshot
                    pendingScopes.remove(credentialScope)
                }
                lastForegroundPullAtMs = SystemClock.elapsedRealtime()
                Log.d(
                    PROVIDER_CREDENTIAL_TAG,
                    "Synchronized ${remoteSnapshot.values.size} credentials for profile $profileId applied=$applied"
                )
                Result.success(applied)
            } catch (_: ProviderCredentialTargetChanged) {
                Result.success(false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(PROVIDER_CREDENTIAL_TAG, "Provider credential sync failed: ${error.javaClass.simpleName}")
                Result.failure(error)
            }
        }
    }

    fun requestForegroundPull(force: Boolean = false) {
        if (!authManager.isAuthenticated) return
        val now = SystemClock.elapsedRealtime()
        if (!force && foregroundPullJob?.isActive == true) return
        if (!force && now - lastForegroundPullAtMs < PROVIDER_CREDENTIAL_FOREGROUND_MIN_INTERVAL_MS) return

        foregroundPullJob = scope.launch {
            if (!force) {
                delay(PROVIDER_CREDENTIAL_FOREGROUND_DELAY_MS)
            }
            if (!authManager.isAuthenticated) return@launch
            syncFromRemote()
        }
    }

    private suspend fun seedSnapshot(snapshot: ProviderCredentialSnapshot, target: ProviderCredentialTarget) {
        val params = credentialParams(snapshot)
        withJwtRefreshRetry(target) { client ->
            client.rpc("sync_seed_provider_credentials", params)
        }
    }

    private suspend fun pushSnapshot(snapshot: ProviderCredentialSnapshot, target: ProviderCredentialTarget) {
        val params = credentialParams(snapshot)
        withJwtRefreshRetry(target) { client ->
            client.rpc("sync_push_provider_credentials", params)
        }
        Log.d(
            PROVIDER_CREDENTIAL_TAG,
            "Pushed ${snapshot.values.size} credentials for profile ${snapshot.profileId}"
        )
    }

    private suspend fun pullRows(target: ProviderCredentialTarget): List<SupabaseProviderCredential> {
        val params = buildJsonObject {
            put("p_profile_id", target.scope.profileId)
        }
        val response = withJwtRefreshRetry(target) { client ->
            client.rpc("sync_pull_provider_credentials", params)
        }
        return response.decodeList()
    }

    private fun credentialParams(snapshot: ProviderCredentialSnapshot) = buildJsonObject {
        put("p_profile_id", snapshot.profileId)
        put("p_credentials", buildJsonArray {
            snapshot.values.forEach { credential ->
                addJsonObject {
                    put("provider", credential.provider)
                    put("credential_json", credential.credentialJson())
                }
            }
        })
        putSyncOriginClientId(syncClientIdentity)
    }

    private suspend fun currentSnapshot(profileId: Int): ProviderCredentialSnapshot {
        val debrid = debridSettingsDataStore.settingsForProfile(profileId).first()
        val mdbList = mdbListSettingsDataStore.settingsForProfile(profileId).first()
        val animeSkipClientId = animeSkipSettingsDataStore.clientIdForProfile(profileId).first()
        return localProviderCredentials(profileId, debrid, mdbList.apiKey, animeSkipClientId)
    }

    private suspend fun applySnapshot(snapshot: ProviderCredentialSnapshot, target: ProviderCredentialTarget) {
        val profileId = target.scope.profileId
        val requireCurrent = { requireCurrentTarget(target) }
        snapshot.values.forEach { credential ->
            when {
                credential.provider.startsWith("debrid:") -> {
                    val providerId = credential.provider.substringAfter("debrid:")
                    debridSettingsDataStore.setProviderApiKeyForProfile(profileId, providerId, credential.value, requireCurrent)
                }
                credential.provider == ProviderCredentialIds.MDBLIST -> {
                    mdbListSettingsDataStore.setApiKeyForProfile(profileId, credential.value, requireCurrent)
                }
                credential.provider == ProviderCredentialIds.ANIMESKIP -> {
                    animeSkipSettingsDataStore.setClientIdForProfile(profileId, credential.value, requireCurrent)
                }
            }
        }
    }

    private fun currentTarget(profileId: Int): ProviderCredentialTarget? {
        val state = authManager.authState.value as? AuthState.FullAccount ?: return null
        if (auth.currentSessionOrNull()?.user?.id != state.userId) return null
        if (profileManager.activeProfileId.value != profileId) return null
        return ProviderCredentialTarget(ProviderCredentialScope(state.userId, profileId), profileManager.profileSelectionRevision.value)
    }

    private fun requireCurrentTarget(expected: ProviderCredentialTarget) {
        if (currentTarget(expected.scope.profileId) != expected) {
            throw ProviderCredentialTargetChanged()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeLocalCredentials() {
        scope.launch {
            combine(authManager.authState, profileManager.activeProfileId, profileManager.profileSelectionRevision) { _, profileId, _ ->
                currentTarget(profileId)
            }.distinctUntilChanged()
                .flatMapLatest { target ->
                    if (target == null) return@flatMapLatest emptyFlow()
                    val profileId = target.scope.profileId
                    combine(
                        debridSettingsDataStore.settingsForProfile(profileId),
                        mdbListSettingsDataStore.settingsForProfile(profileId),
                        animeSkipSettingsDataStore.clientIdForProfile(profileId)
                    ) { debrid, mdbList, animeSkipClientId ->
                        target to localProviderCredentials(profileId, debrid, mdbList.apiKey, animeSkipClientId)
                    }
                }
                .distinctUntilChanged()
                .debounce(PROVIDER_CREDENTIAL_PUSH_DEBOUNCE_MS)
                .collect { (target, snapshot) ->
                    handleLocalSnapshot(target, snapshot)
                }
        }
    }

    private suspend fun handleLocalSnapshot(target: ProviderCredentialTarget, snapshot: ProviderCredentialSnapshot) {
        syncMutex.withLock {
            if (currentTarget(snapshot.profileId) != target) return
            val credentialScope = target.scope
            val previous = synchronized(stateLock) {
                observedSnapshots.put(credentialScope, snapshot)
            }
            if (previous == null) {
                synchronized(stateLock) {
                    baselineSnapshots.putIfAbsent(credentialScope, snapshot)
                }
                return
            }
            if (previous == snapshot) return
            val baseline = synchronized(stateLock) {
                baselineSnapshots.getOrPut(credentialScope) { previous }
            }
            if (baseline == snapshot) return

            try {
                pushSnapshot(snapshot, target)
                synchronized(stateLock) {
                    baselineSnapshots[credentialScope] = snapshot
                    pendingScopes.remove(credentialScope)
                }
            } catch (_: ProviderCredentialTargetChanged) {
                return@withLock
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                synchronized(stateLock) {
                    pendingScopes.add(credentialScope)
                }
                Log.w(
                    PROVIDER_CREDENTIAL_TAG,
                    "Provider credential push failed for profile ${snapshot.profileId}: ${error.javaClass.simpleName}"
                )
            }
        }
    }

    private fun observeAuthState() {
        scope.launch {
            authManager.authState.collect { state ->
                if (state is AuthState.FullAccount) return@collect
                synchronized(stateLock) {
                    observedSnapshots.clear()
                    baselineSnapshots.clear()
                    pendingScopes.clear()
                }
            }
        }
    }
}
