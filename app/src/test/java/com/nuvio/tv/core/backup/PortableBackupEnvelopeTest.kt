package com.nuvio.tv.core.backup

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PortableBackupEnvelopeTest {
    private val pin = "531602947284"
    private val document = BackupDocument(
        BackupHeader(
            createdAtMs = 1_762_600_000_000L,
            appVersion = "2.1.2",
            device = BackupDevice("Test", "Private device", 30),
            profileNames = listOf("Private profile"),
            parts = setOf(BackupPart.SETTINGS)
        ),
        mapOf(BackupPart.SETTINGS to listOf(BackupEntry(
            BackupStoreKind.PROFILE_STORE, "scout", 1, mapOf("key" to BackupValue.Text("Private setting"))
        )))
    )

    @Test
    fun encryptsProfileMetadataAndSettingsAndRestoresTheSameDocument() {
        val text = PortableBackupEnvelope.seal(document, pin)
        assertFalse(text.contains("Private"))
        val box = checkNotNull(PortableBackupEnvelope.inspect(text))
        assertEquals(document, PortableBackupEnvelope.open(box, pin))
        assertThrows(WrongBackupPinException::class.java) {
            PortableBackupEnvelope.open(box, "531602947285")
        }
    }

    @Test
    fun rejectsTamperingAndExcessiveKeyDerivationWork() {
        val text = PortableBackupEnvelope.seal(document, pin)
        val box = checkNotNull(PortableBackupEnvelope.inspect(text))
        val replacement = if (box.data.first() == 'A') 'B' else 'A'
        assertThrows(WrongBackupPinException::class.java) {
            PortableBackupEnvelope.open(box.copy(data = replacement + box.data.drop(1)), pin)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupEnvelope.inspect(JSONObject(text).put("iterations", Int.MAX_VALUE).toString())
        }
    }

    @Test
    fun legacyBackupRemainsRecognizableAndShortPinCannotExport() {
        assertNull(PortableBackupEnvelope.inspect(BackupFormat.encode(document)))
        assertThrows(IllegalArgumentException::class.java) { PortableBackupEnvelope.seal(document, "123456") }
    }
}
