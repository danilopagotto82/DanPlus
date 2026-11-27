package com.nuvio.tv.updater

import java.io.File
import java.security.MessageDigest
import java.util.Locale

internal fun validateUpdateDigest(file: File, expected: String?) {
    if (expected.isNullOrBlank()) return
    val prefix = "sha256:"
    val normalized = expected.trim().lowercase(Locale.ROOT)
    require(normalized.startsWith(prefix) && normalized.removePrefix(prefix).matches(Regex("[a-f0-9]{64}"))) {
        "Invalid update digest"
    }
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    val actual = digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    require(actual == normalized.removePrefix(prefix)) { "Update digest mismatch" }
}
