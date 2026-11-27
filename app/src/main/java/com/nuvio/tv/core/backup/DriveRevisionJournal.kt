package com.nuvio.tv.core.backup

import java.security.MessageDigest

internal const val MAX_DRIVE_REVISIONS = 256

internal data class DriveJournalEntry(val parents: Set<String>, val sealed: SealedBox) {
    val id: String
        get() {
            val content = (listOf(parents.size.toString()) + parents.sorted() + listOf(sealed.kdf, sealed.iterations.toString(),
                sealed.salt, sealed.iv, sealed.data))
                .joinToString(separator = "") { "${it.length}:$it" }
            return MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { "%02x".format(it) }
        }
}

internal sealed interface DriveJournalState {
    data object Empty : DriveJournalState
    data class Current(val entry: DriveJournalEntry, val history: Set<String>) : DriveJournalState
    data class Conflict(
        val heads: Set<String>,
        val entries: Map<String, DriveJournalEntry> = emptyMap(),
        val history: Set<String> = emptySet()
    ) : DriveJournalState
    data object CapacityExceeded : DriveJournalState
    data object Invalid : DriveJournalState
}

/**
 * A Drive write adds a file. It cannot replace a snapshot. Concurrent edits create separate
 * heads, and the caller must pause adoption until the viewer resolves the conflict.
 */
internal fun inspectDriveJournal(files: Map<String, DriveJournalEntry>): DriveJournalState {
    if (files.isEmpty()) return DriveJournalState.Empty
    val parents = hashSetOf<String>()
    for ((id, entry) in files) {
        if (entry.id != id || entry.sealed.data.length > 8192) return DriveJournalState.Invalid
        if (entry.parents.any { it !in files }) return DriveJournalState.Invalid
        parents += entry.parents
    }
    val heads = files.keys - parents
    return when (heads.size) {
        0 -> DriveJournalState.Invalid
        1 -> DriveJournalState.Current(files.getValue(heads.single()), files.keys.toSet())
        else -> DriveJournalState.Conflict(heads, files.filterKeys { it in heads }, files.keys.toSet())
    }
}
