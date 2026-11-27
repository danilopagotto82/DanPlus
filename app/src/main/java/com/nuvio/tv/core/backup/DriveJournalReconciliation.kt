package com.nuvio.tv.core.backup

import org.json.JSONArray
import org.json.JSONException

internal sealed interface DriveJournalDecision {
    data object Invalid : DriveJournalDecision
    data object Conflict : DriveJournalDecision
    data class Adopt(val settings: DrivePortableSettings) : DriveJournalDecision
    data class Append(val entry: DriveJournalEntry) : DriveJournalDecision
}

internal data class DriveJournalBaseline(val revision: String, val settings: DrivePortableSettings)

/** Computes an append or adoption. Persistence and remote I/O are owned by the session coordinator. */
internal fun reconcileDriveJournal(
    scope: DriveCinemaScope,
    pin: String,
    state: DriveJournalState,
    local: DrivePortableSettings,
    baseline: DriveJournalBaseline?
): DriveJournalDecision {
    if (!BackupCrypto.isValidPin(pin)) return DriveJournalDecision.Invalid
    if (state is DriveJournalState.Conflict) return DriveJournalDecision.Conflict
    if (state == DriveJournalState.Invalid) return DriveJournalDecision.Invalid
    val journal = state as? DriveJournalState.Current
    if (baseline != null && journal?.history?.contains(baseline.revision) != true) {
        return DriveJournalDecision.Conflict
    }
    val current = journal?.entry
    val parents = current?.let { setOf(it.id) }.orEmpty()
    val remote = current?.let {
        try {
            decodeDriveJournalEntry(it, scope, pin)
        } catch (_: WrongBackupPinException) {
            return DriveJournalDecision.Invalid
        } catch (_: IllegalArgumentException) {
            return DriveJournalDecision.Invalid
        } catch (_: JSONException) {
            return DriveJournalDecision.Invalid
        }
    }
    if (remote == local) return DriveJournalDecision.Adopt(local)
    val merged = when {
        remote == null && baseline == null -> local
        remote == null || baseline == null -> return DriveJournalDecision.Conflict
        else -> mergeDriveSettings(local, remote, baseline.settings) ?: return DriveJournalDecision.Conflict
    }
    if (merged == remote) return DriveJournalDecision.Adopt(merged)
    val payload = encodeDriveSettings(scope, merged).put("parents", JSONArray(parents.sorted()))
    return DriveJournalDecision.Append(DriveJournalEntry(parents, BackupCrypto.seal(payload.toString(), pin)))
}
