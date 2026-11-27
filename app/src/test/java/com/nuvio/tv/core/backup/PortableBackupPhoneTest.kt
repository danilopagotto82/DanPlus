package com.nuvio.tv.core.backup

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class PortableBackupPhoneTest {
    private val context = mockk<Context>()
    private val manager = mockk<BackupManager>()
    private val payload = """{"format":"danplus-portable-backup","data":"ciphertext-fixture"}"""

    @Test
    fun encryptedDownloadReturnsOnlyThePreparedFile() {
        val server = BackupServer(context, manager, saving = true, { null }, 0, "backup-encrypted.json" to payload)
        try {
            server.start(5_000, true)
            val download = request(server, "/backup/encrypted?k=${server.key}")
            assertEquals(200, download.responseCode)
            assertEquals(payload, download.inputStream.bufferedReader().use { it.readText() })
            download.disconnect()

            val unauthorized = request(server, "/backup/encrypted?k=wrong")
            assertEquals(403, unauthorized.responseCode)
            unauthorized.disconnect()

            val otherFile = request(server, "/backup/local?k=${server.key}")
            assertEquals(404, otherFile.responseCode)
            otherFile.disconnect()
            verify(exactly = 0) { manager.files }
        } finally {
            server.stop()
        }
    }

    @Test
    fun encryptedPhoneImportPassesTheUnmodifiedEnvelopeToTheManager() {
        every { manager.receive(payload) } returns true
        val server = BackupServer(context, manager, saving = false, { null }, 0)
        try {
            server.start(5_000, true)
            val upload = request(server, "/restore?k=${server.key}")
            upload.requestMethod = "POST"
            upload.setRequestProperty("Content-Type", "application/json")
            upload.doOutput = true
            val bytes = payload.toByteArray(Charsets.UTF_8)
            upload.setFixedLengthStreamingMode(bytes.size)
            upload.outputStream.use { it.write(bytes) }
            assertEquals(200, upload.responseCode)
            upload.disconnect()
            verify(exactly = 1) { manager.receive(payload) }
        } finally {
            server.stop()
        }
    }

    private fun request(server: BackupServer, path: String): HttpURLConnection =
        (URL("http://127.0.0.1:${server.listeningPort}$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
        }
}
