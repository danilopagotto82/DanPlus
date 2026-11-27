package com.nuvio.tv.core.backup

import org.json.JSONObject

/** Portable files expose only the format and authenticated ciphertext, including outside the app. */
internal object PortableBackupEnvelope {
    private const val FORMAT = "danplus-portable-backup"
    private const val VERSION = 1
    private const val MAX_BACKUP_BYTES = 16 * 1024 * 1024
    const val MAX_FILE_BYTES = 24 * 1024 * 1024
    const val MIN_PIN_LENGTH = 12

    fun seal(document: BackupDocument, pin: String): String {
        require(BackupCrypto.isValidPin(pin) && pin.length >= MIN_PIN_LENGTH)
        val text = BackupFormat.encode(document)
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES)
        val box = BackupCrypto.seal(text, pin)
        return JSONObject().put("format", FORMAT).put("version", VERSION)
            .put("kdf", box.kdf).put("iterations", box.iterations)
            .put("salt", box.salt).put("iv", box.iv).put("data", box.data).toString()
    }

    /** A legacy Arc backup is passed to its existing decoder. */
    fun inspect(text: String): SealedBox? {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) return null
        require(root.getInt("version") == VERSION)
        val kdf = root.getString("kdf")
        require(kdf == "PBKDF2WithHmacSHA256" || kdf == "PBKDF2WithHmacSHA1")
        val iterations = root.getInt("iterations")
        require(iterations in 600_000..BackupCrypto.MAX_ITERATIONS)
        val salt = root.getString("salt")
        val iv = root.getString("iv")
        val data = root.getString("data")
        require(salt.length == 24 && iv.length == 16 && data.length in 24..MAX_FILE_BYTES)
        return SealedBox(kdf, iterations, salt, iv, data)
    }

    fun open(box: SealedBox, pin: String): BackupDocument =
        BackupFormat.decode(BackupCrypto.open(box, pin))
}

internal sealed interface PortableBackupInput {
    data class Legacy(val document: BackupDocument) : PortableBackupInput
    data class Encrypted(val box: SealedBox) : PortableBackupInput

    companion object {
        fun parse(text: String): PortableBackupInput =
            PortableBackupEnvelope.inspect(text)?.let(::Encrypted) ?: Legacy(BackupFormat.decode(text))
    }
}
