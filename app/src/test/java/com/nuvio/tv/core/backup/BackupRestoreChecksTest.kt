package com.nuvio.tv.core.backup

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreChecksTest {
    private fun spec(kind: BackupStoreKind, name: String) = requireNotNull(BackupCatalog.spec(kind, name))

    @Test
    fun `key names found in the review stay sealed`() {
        assertEquals(BackupPart.SECRETS, BackupCatalog.partFor(spec(BackupStoreKind.PROFILE_STORE, "layout_settings"), "custom_poster_url_pattern"))
        assertEquals(BackupPart.SECRETS, BackupCatalog.partFor(spec(BackupStoreKind.PROFILE_STORE, "track_preference"), "sub_addon_url|tt5491994"))
        assertEquals(BackupPart.SECRETS, BackupCatalog.partFor(spec(BackupStoreKind.PREFS, "network_dns"), "custom_url"))
        assertEquals(BackupPart.SECRETS, BackupCatalog.partFor(spec(BackupStoreKind.PREFS, "server_configuration"), "publishable_key"))
        assertEquals(BackupPart.SETTINGS, BackupCatalog.partFor(spec(BackupStoreKind.PROFILE_STORE, "layout_settings"), "hero_catalog_key"))
    }

    @Test
    fun `a protected entry with no text is refused before writing`() {
        val entry = BackupEntry(BackupStoreKind.PROTECTED, "media_servers", 1)
        assertThrows(BackupFormatException::class.java) {
            BackupRestoreChecks.validate(mapOf(BackupPart.SECRETS to listOf(entry)), setOf(BackupPart.SECRETS))
        }
    }

    @Test
    fun `a broken profile list is refused, an unchosen part is not looked at`() {
        val entry = BackupEntry(
            BackupStoreKind.BOX_STORE, "profile_settings", values = mapOf("profiles_json" to BackupValue.Text("not json"))
        )
        val entries = mapOf(BackupPart.PROFILES to listOf(entry))
        assertThrows(BackupFormatException::class.java) { BackupRestoreChecks.validate(entries, setOf(BackupPart.PROFILES)) }
        BackupRestoreChecks.validate(entries, setOf(BackupPart.SETTINGS))
    }

    @Test
    fun `a store from a newer version is left for the restore to skip`() {
        val entry = BackupEntry(BackupStoreKind.PREFS, "from_a_later_version")
        BackupRestoreChecks.validate(mapOf(BackupPart.SETTINGS to listOf(entry)), setOf(BackupPart.SETTINGS))
    }

    @Test
    fun `the viewer is only warned when some items came back and some did not`() {
        assertTrue(BackupRestoreChecks.isPartial(written = 5, failed = 1))
        assertFalse(BackupRestoreChecks.isPartial(written = 5, failed = 0))
        assertFalse(BackupRestoreChecks.isPartial(written = 0, failed = 3))
    }

    @Test
    fun `a file is replaced whole and leaves no part file`() {
        val folder = Files.createTempDirectory("backup").toFile()
        val file = File(folder, "scout/taste_1.json")
        val replace = { source: File, destination: File ->
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
            Unit
        }
        BackupRestoreChecks.writeWhole(file, "one", replace)
        BackupRestoreChecks.writeWhole(file, "two", replace)
        assertEquals("two", file.readText())
        assertEquals(listOf(file.name), file.parentFile!!.list()?.toList())
        assertTrue(folder.deleteRecursively())
    }

    @Test
    fun `a failed replacement preserves the earlier backup and removes the partial file`() {
        val folder = Files.createTempDirectory("backup-failed").toFile()
        try {
            val file = File(folder, "previous.json").apply { writeText("previous backup") }
            assertThrows(IOException::class.java) {
                BackupRestoreChecks.writeWhole(file, "new backup") { temp, destination ->
                    assertEquals("new backup", temp.readText())
                    assertEquals("previous backup", destination.readText())
                    throw IOException("Simulated rename failure")
                }
            }
            assertEquals("previous backup", file.readText())
            assertEquals(listOf(file.name), folder.list()?.toList())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun `an absurd iteration count in a file is capped, a normal one is kept`() {
        fun document(iterations: Long) = """{"format":"arc-backup","version":1,"header":{},"parts":{},
            "secrets":{"kdf":"PBKDF2WithHmacSHA256","iterations":$iterations,"salt":"a","iv":"b","data":"c"}}"""
        assertEquals(BackupCrypto.MAX_ITERATIONS, BackupFormat.decode(document(2_000_000_000)).sealedSecrets!!.iterations)
        assertEquals(600_000, BackupFormat.decode(document(600_000)).sealedSecrets!!.iterations)
    }
}
