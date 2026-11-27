package com.nuvio.tv.core.backup

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.security.SecureRandom

/**
 * The phone page for backups, open only while its QR code is on the TV. [saving] shows the backups to
 * download; otherwise the page takes a backup file to restore. Every request needs the key in the QR address.
 */
class BackupServer(
    private val context: Context,
    private val manager: BackupManager,
    private val saving: Boolean,
    private val logoProvider: () -> ByteArray?,
    port: Int,
    encryptedExport: Pair<String, String>? = null
) : NanoHTTPD(port) {
    private val encryptedBytes = encryptedExport?.second?.toByteArray(Charsets.UTF_8)
    private val encryptedFile = encryptedExport?.let { (name, _) ->
        SavedBackup("encrypted", name, System.currentTimeMillis(), checkNotNull(encryptedBytes).size.toLong())
    }
    val key: String = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        if (uri == "/logo.png") return logo()
        if (session.parms["k"] != key) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
        return when {
            session.method == Method.GET && uri == "/" ->
                newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", BackupWebPage.html(context, saving, key, backups()))
            session.method == Method.GET && uri.startsWith("/backup/") && saving -> download(uri.removePrefix("/backup/"))
            session.method == Method.POST && uri == "/restore" && !saving -> receive(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun backups(): List<SavedBackup> = when {
        !saving -> emptyList()
        encryptedFile != null -> listOf(encryptedFile)
        else -> manager.files.list()
    }

    private fun download(id: String): Response {
        val backup = backups().firstOrNull { it.id == id }
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        val bytes = (if (encryptedFile != null) encryptedBytes else manager.files.readBytes(backup.id))
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        return newFixedLengthResponse(Response.Status.OK, BackupFiles.MIME, bytes.inputStream(), bytes.size.toLong()).apply {
            addHeader("Content-Disposition", "attachment; filename=\"${backup.displayName.replace("\"", "")}\"")
        }
    }

    private fun receive(session: IHTTPSession): Response {
        val length = session.headers["content-length"]?.toLongOrNull() ?: 0L
        if (length <= 0L || length > PortableBackupEnvelope.MAX_FILE_BYTES) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "size")
        }
        val bytes = ByteArray(length.toInt())
        var read = 0
        while (read < bytes.size) {
            val count = session.inputStream.read(bytes, read, bytes.size - read)
            if (count < 0) break
            read += count
        }
        val accepted = read == bytes.size && manager.receive(bytes.toString(Charsets.UTF_8))
        return if (accepted) {
            newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok")
        } else {
            newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "not a backup")
        }
    }

    private fun logo(): Response {
        val bytes = logoProvider() ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "")
        return newFixedLengthResponse(Response.Status.OK, "image/png", bytes.inputStream(), bytes.size.toLong())
    }

    companion object {
        private const val SOCKET_READ_TIMEOUT = 5000

        fun startOnAvailablePort(
            context: Context,
            manager: BackupManager,
            saving: Boolean,
            logoProvider: () -> ByteArray?,
            startPort: Int = 8093,
            maxAttempts: Int = 10,
            encryptedExport: Pair<String, String>? = null
        ): BackupServer? {
            for (port in startPort until startPort + maxAttempts) {
                val server = BackupServer(context, manager, saving, logoProvider, port, encryptedExport)
                try {
                    server.start(SOCKET_READ_TIMEOUT, false)
                    return server
                } catch (error: Exception) {
                }
            }
            return null
        }
    }
}
