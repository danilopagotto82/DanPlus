package com.nuvio.tv.core.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DrivePortableSettingsSyncTest {
    private val scope = DriveCinemaScope("drive-test", "account-test", "profile-a")
    private val defaults = DrivePortableSettings(null, scoutInMenu = false, scoutListEmojis = false)
    private val transport = FakeTransport()
    private val sync = DrivePortableSettingsSync(transport)

    @Test
    fun mergesIndependentChanges() = runBlocking {
        val remote = defaults.copy(scoutInMenu = true)
        val local = defaults.copy(scoutListEmojis = true, scoutShowWatched = false)
        sync.reconcile(scope, "test", "123456", remote, null) { true }
        assertEquals(
            DrivePortableResult.InSync(remote.copy(scoutListEmojis = true, scoutShowWatched = false)),
            sync.reconcile(scope, "test", "123456", local, defaults) { true }
        )
        assertEquals(2, transport.writes)
    }

    @Test
    fun preservesRemoteOnConflictAndWrongPin() = runBlocking {
        sync.reconcile(scope, "test", "123456", defaults.copy(favoriteGenre = 18), null) { true }
        assertEquals(
            DrivePortableResult.Conflict,
            sync.reconcile(scope, "test", "123456", defaults.copy(favoriteGenre = 28), defaults) { true }
        )
        assertEquals(
            DrivePortableResult.Invalid,
            sync.reconcile(scope, "test", "654321", defaults, defaults) { true }
        )
        assertEquals(
            DrivePortableResult.Invalid,
            sync.reconcile(scope.copy(profile = "profile-b"), "test", "123456", defaults, defaults) { true }
        )
        assertEquals(1, transport.writes)
    }

    @Test
    fun stopsAfterSessionChangesDuringRead() = runBlocking {
        var current = true
        transport.onRead = { current = false }
        assertEquals(
            DrivePortableResult.Unavailable,
            sync.reconcile(scope, "test", "123456", defaults, null) { current }
        )
        assertEquals(0, transport.writes)
    }

    @Test
    fun reportsConcurrentWriteWithoutRetrying() = runBlocking {
        transport.rejectWrite = true
        assertEquals(
            DrivePortableResult.Conflict,
            sync.reconcile(scope, "test", "123456", defaults, null) { true }
        )
        assertEquals(0, transport.writes)
    }

    @Test
    fun twoClientsReconcileAfterOfflineEditAndRejectARevokedGrant() = runBlocking {
        val secondClient = DrivePortableSettingsSync(transport)
        val remoteEdit = defaults.copy(scoutShowWatched = false)
        val offlineEdit = defaults.copy(scoutInMenu = true)
        assertEquals(
            DrivePortableResult.InSync(defaults),
            sync.reconcile(scope, "first-device", "123456", defaults, null) { true }
        )
        assertEquals(
            DrivePortableResult.InSync(remoteEdit),
            sync.reconcile(scope, "first-device", "123456", remoteEdit, defaults) { true }
        )
        val expected = remoteEdit.copy(scoutInMenu = true)
        assertEquals(
            DrivePortableResult.InSync(expected),
            secondClient.reconcile(scope, "second-device", "123456", offlineEdit, defaults) { true }
        )
        transport.onRead = { transport.revokedDevice = "first-device" }
        assertEquals(
            DrivePortableResult.Unavailable,
            sync.reconcile(scope, "first-device", "123456", remoteEdit, remoteEdit) { true }
        )
        assertEquals(3, transport.writes)
    }

    private class FakeTransport : DriveCinemaTransport {
        var writes = 0
        var rejectWrite = false
        var revokedDevice: String? = null
        var onRead: () -> Unit = {}
        private var revision: DrivePreferenceRevision? = null

        override suspend fun isAuthorized(scope: DriveCinemaScope, device: String) = device != revokedDevice

        override suspend fun read(scope: DriveCinemaScope): DrivePreferenceRevision? {
            onRead()
            return revision
        }

        override suspend fun compareAndSet(scope: DriveCinemaScope, expectedTag: String?, sealed: SealedBox): Boolean {
            if (rejectWrite || revision?.tag != expectedTag) return false
            writes++
            revision = DrivePreferenceRevision(writes.toString(), sealed)
            return true
        }
    }
}
