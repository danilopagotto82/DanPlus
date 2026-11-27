package com.nuvio.tv.core.backup

import android.content.Context
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.core.auth.AuthManager
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.profile.ProfileScopedCredentialStore
import com.nuvio.tv.core.profile.ProtectedProfilePreferences
import com.nuvio.tv.data.local.ScoutStore
import com.nuvio.tv.domain.model.ServerConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal data class DriveStoredGrant(
    val actor: String,
    val scope: DriveCinemaScope,
    val clientId: String,
    val refresh: String,
    val pin: String,
    val automatic: Boolean = true,
    val lastSuccessfulSync: Long? = null
) {
    override fun toString() = "DriveStoredGrant"

    fun matches(actor: String, owner: String, profile: String) =
        this.actor == actor && scope.appAccount == owner && scope.profile == profile
    fun encode(): String = JSONObject().put("actor", actor).put("drive", scope.driveAccount)
        .put("owner", scope.appAccount).put("profile", scope.profile).put("client", clientId)
        .put("refresh", refresh).put("pin", pin).put("automatic", automatic)
        .put("lastSuccessfulSync", lastSuccessfulSync).toString()
    companion object {
        fun decode(text: String): DriveStoredGrant {
            val j = JSONObject(text)
            return DriveStoredGrant(j.getString("actor"), DriveCinemaScope(j.getString("drive"),
                j.getString("owner"), j.getString("profile")), j.getString("client"),
                j.getString("refresh"), j.getString("pin"), j.optBoolean("automatic", true),
                j.optLong("lastSuccessfulSync").takeIf { it > 0 }).also {
                require(it.pin.length >= 12 && BackupCrypto.isValidPin(it.pin) &&
                    it.refresh.isNotBlank() && it.clientId.isNotBlank() && it.scope.driveAccount.isNotBlank())
            }
        }
    }
}

/** Local profile slots are never remote identities; deletion also clears journal progress. */
@Singleton
class DriveGrantVault @Inject constructor(@ApplicationContext context: Context) : ProfileScopedCredentialStore {
    private val grants = ProtectedProfilePreferences(context, "drive_grant", "drive_grant_v1")
    private val progress = ProtectedProfilePreferences(context, "drive_journal_progress", "drive_journal_progress_v1")
    private val lock = Any()
    private val _revision = MutableStateFlow(0L)
    internal val revision = _revision.asStateFlow()
    internal fun load(id: Int): DriveStoredGrant? = synchronized(lock) {
        grants.read(id)?.let { DriveStoredGrant.decode(it) }
    }
    internal fun save(id: Int, grant: DriveStoredGrant, current: () -> Boolean): Boolean = synchronized(lock) {
        if (!current()) return@synchronized false
        // Do not discard a previous journal baseline on reconnect to the same scope.
        val old = grants.read(id)?.let { DriveStoredGrant.decode(it) }
        if (old != null && old.scope != grant.scope) return@synchronized false
        grants.write(id, grant.encode())
        _revision.value++
        true
    }
    internal fun rotate(id: Int, before: DriveStoredGrant, token: String, current: () -> Boolean) = synchronized(lock) {
        check(current())
        val saved = checkNotNull(grants.read(id)?.let { DriveStoredGrant.decode(it) })
        check(saved.scope == before.scope && saved.actor == before.actor && saved.refresh == before.refresh)
        grants.write(id, saved.copy(refresh = token).encode())
    }
    internal fun recordSuccess(id: Int, time: Long, current: () -> Boolean) = synchronized(lock) {
        if (!current()) return@synchronized
        val saved = grants.read(id)?.let { DriveStoredGrant.decode(it) } ?: return@synchronized
        grants.write(id, saved.copy(lastSuccessfulSync = time).encode())
    }
    internal fun setAutomatic(id: Int, enabled: Boolean, current: () -> Boolean): Boolean = synchronized(lock) {
        if (!current()) return@synchronized false
        val saved = grants.read(id)?.let { DriveStoredGrant.decode(it) } ?: return@synchronized false
        grants.write(id, saved.copy(automatic = enabled).encode())
        _revision.value++
        true
    }
    override fun removeProfile(profileId: Int): Unit = synchronized(lock) {
        grants.write(profileId, null)
        progress.write(profileId, null)
        _revision.value++
    }
    override fun clearAllProfiles(): Unit = synchronized(lock) {
        grants.clear()
        progress.clear()
        _revision.value++
    }
    /** Disconnecting locally preserves the encrypted journal and does not delete Google files. */
    internal fun disconnect(id: Int, grant: DriveStoredGrant, current: () -> Boolean): Boolean = synchronized(lock) {
        if (!current()) return@synchronized false
        val saved = grants.read(id)?.let { DriveStoredGrant.decode(it) }
        if (saved == null || saved.scope != grant.scope || saved.actor != grant.actor || saved.refresh != grant.refresh) return@synchronized false
        grants.write(id, null)
        _revision.value++
        true
    }
}

internal data class DriveSyncStatus(val available: Boolean = false, val connected: Boolean = false, val automatic: Boolean = false)

@Singleton
class DriveSyncRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val auth: Auth,
    private val server: ServerConfiguration,
    private val profiles: ProfileManager,
    private val scout: ScoutStore,
    private val vault: DriveGrantVault
) {
    private val endpoint = driveBrokerEndpoint(BuildConfig.DANPLUS_D12_SUPABASE_URL)
        ?.takeIf { it.host == "wnzmojaliguncdbtuula.supabase.co" }
    private val configured = endpoint != null && BuildConfig.DANPLUS_D12_SUPABASE_KEY.startsWith("sb_publishable_") &&
        !server.isCustom && server.backendUrl.trimEnd('/') == "https://api.nuvio.tv"
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(20, TimeUnit.SECONDS).build()
    private val google = GoogleDriveDeviceAuthorization()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val visible = MutableStateFlow(false)
    private val sessions = MutableStateFlow<DriveProfileSession?>(null)
    private val loop = DriveProfileSyncLoop(scout)
    private val _status = MutableStateFlow(DriveSyncStatus())
    internal val status = _status.asStateFlow()
    internal val outcome = loop.outcome
    internal val lastSuccess = loop.lastSuccess
    private val operation = Mutex()

    private class Binding(val actor: String, val owner: String, val id: Int, val remoteProfile: String,
        val revision: Long, val vaultRevision: Long)

    init {
        scope.launch { loop.run(sessions) }
        scope.launch {
            combine(
                combine(visible, authManager.authState, profiles.activeProfileReady, profiles.activeProfileId, profiles.profileSelectionRevision) {
                    v, a, ready, id, rev -> listOf(v, a, ready, id, rev)
                }, profiles.profiles, vault.revision
            ) { left, list, rev -> Triple(left, list, rev) }.collectLatest {
                sessions.value = null
                _status.value = DriveSyncStatus()
                try {
                    val binding = binding() ?: return@collectLatest
                    val grant = vault.load(binding.id)
                    if (!current(binding)) return@collectLatest
                    if (grant == null || !grant.matches(binding.actor, binding.owner, binding.remoteProfile)) {
                        _status.value = DriveSyncStatus(available = true)
                        return@collectLatest
                    }
                    val token = tokenProvider(binding, grant)
                    val valid = { current(binding) }
                    val coordinator = DriveJournalCoordinator(
                        GoogleDriveRevisionStore(http, driveNamespace(grant.scope), token, valid),
                        ProtectedDriveJournalProgress(context, binding.id, valid))
                    _status.value = DriveSyncStatus(available = true, connected = true, automatic = grant.automatic)
                    sessions.value = DriveProfileSession(grant.scope, binding.id, grant.pin, coordinator,
                        grant.automatic, grant.lastSuccessfulSync, { time ->
                            runCatching { vault.recordSuccess(binding.id, time, valid) }
                        }, valid)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    sessions.value = null
                    _status.value = DriveSyncStatus()
                }
            }
        }
    }

    internal fun setVisible(value: Boolean) { visible.value = value }
    internal fun syncNow() = loop.syncNow()
    internal fun confirmSession(): () -> Boolean {
        val selected = sessions.value
        return { selected != null && sessions.value === selected && selected.isCurrent() }
    }

    private suspend fun binding(): Binding? {
        if (!configured || !visible.value || !profiles.activeProfileReady.value || !authManager.hasLiveSession()) return null
        val actor = authManager.currentUserId ?: return null
        val id = profiles.activeProfileId.value
        val revision = profiles.profileSelectionRevision.value
        val remote = profiles.activeProfile?.serverProfileId?.takeIf { it.isNotBlank() } ?: return null
        val version = vault.revision.value
        val owner = authManager.getEffectiveUserId(fallbackToOwnIdOnFailure = false) ?: return null
        return Binding(actor, owner, id, remote, revision, version).takeIf(::current)
    }
    private fun current(b: Binding) = configured && visible.value && authManager.hasLiveSession() &&
        authManager.currentUserId == b.actor && profiles.activeProfileReady.value &&
        profiles.activeProfileId.value == b.id && profiles.activeProfile?.serverProfileId == b.remoteProfile &&
        profiles.profileSelectionRevision.value == b.revision && vault.revision.value == b.vaultRevision

    internal suspend fun connect(pin: String, display: suspend (DriveBrokerChallenge) -> Unit): Boolean = operation.withLock {
        require(pin.length >= 12 && BackupCrypto.isValidPin(pin))
        val b = binding() ?: return@withLock false
        val client = DriveBrokerClient(http, checkNotNull(endpoint), {
            val session = auth.currentSessionOrNull()
            session?.takeIf { current(b) && it.user?.id == b.actor }?.let {
                DriveBrokerHeaders(BuildConfig.DANPLUS_D12_SUPABASE_KEY, server.publishableKey, it.accessToken)
            }
        }, { current(b) })
        val code = client.start()
        display(code)
        val grant = client.awaitGrant(code) ?: return@withLock false
        val account = driveAccountId(http, grant.tokens.accessToken) { current(b) }
        val stored = DriveStoredGrant(b.actor, DriveCinemaScope(account, b.owner, b.remoteProfile),
            grant.clientId, grant.tokens.refreshToken, pin)
        withContext(Dispatchers.IO) { vault.save(b.id, stored) { current(b) } }
    }

    internal suspend fun setAutomatic(enabled: Boolean): Boolean = operation.withLock {
        val b = binding() ?: return@withLock false
        withContext(Dispatchers.IO) {
            val grant = vault.load(b.id) ?: return@withContext false
            if (!grant.matches(b.actor, b.owner, b.remoteProfile)) return@withContext false
            vault.setAutomatic(b.id, enabled) { current(b) }
        }
    }

    internal suspend fun disconnect(revoke: Boolean, confirmed: () -> Boolean): Boolean = operation.withLock {
        if (!confirmed()) return@withLock false
        val b = binding() ?: return@withLock false
        val grant = withContext(Dispatchers.IO) { vault.load(b.id) } ?: return@withLock false
        if (!grant.matches(b.actor, b.owner, b.remoteProfile) || !current(b) || !confirmed()) return@withLock false
        if (revoke) google.revoke(grant.refresh)
        withContext(Dispatchers.IO) { vault.disconnect(b.id, grant) { current(b) } }
    }

    internal suspend fun previewConflict(): DriveConflictPreview? {
        val session = sessions.value ?: return null
        if (!session.isCurrent() || outcome.value != DriveJournalOutcome.CONFLICT) return null
        return withContext(Dispatchers.IO) {
            session.coordinator.previewConflict(session.scope, session.pin, scout.observeDriveSettings(session.localProfileId).first(), session.isCurrent)
        }
    }

    internal suspend fun resolveConflict(preview: DriveConflictPreview, selected: String?, confirmed: () -> Boolean): Boolean {
        val session = sessions.value ?: return false
        if (!session.isCurrent() || !confirmed()) return false
        val result = withContext(Dispatchers.IO) {
            session.coordinator.resolveConflict(session.scope, session.pin, preview, selected, { session.isCurrent() && confirmed() }) {
                scout.observeDriveSettings(session.localProfileId).first()
            }
        }
        loop.syncNow()
        return result == DriveJournalOutcome.AWAITING_REMOTE
    }

    private fun tokenProvider(b: Binding, initial: DriveStoredGrant): suspend () -> String? {
        val mutex = Mutex()
        var stored = initial
        var access: String? = null
        var until = 0L
        return {
            mutex.withLock {
                if (!current(b)) return@withLock null
                val now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime())
                if (now < until) return@withLock access
                val refreshed = google.refresh(stored.clientId, stored.refresh)
                if (driveAccountId(http, refreshed.accessToken) { current(b) } != stored.scope.driveAccount) return@withLock null
                if (!current(b)) return@withLock null
                if (refreshed.refreshToken != stored.refresh) {
                    vault.rotate(b.id, stored, refreshed.refreshToken) { current(b) }
                    stored = stored.copy(refresh = refreshed.refreshToken)
                }
                access = refreshed.accessToken
                until = now + (refreshed.expiresInSeconds - 60).coerceAtLeast(0) * 1000
                access
            }
        }
    }
}
